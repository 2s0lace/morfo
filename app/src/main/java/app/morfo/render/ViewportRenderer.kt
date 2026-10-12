package app.morfo.render

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import app.morfo.math.Vec3
import app.morfo.workspace.LightingMode
import app.morfo.workspace.Workspace
import com.google.android.filament.Camera
import com.google.android.filament.EntityManager
import com.google.android.filament.Renderer
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.gltfio.FilamentInstance
import java.nio.ByteBuffer

/**
 * Renders a [Workspace] into a SurfaceView. Each placed tooth gets its own glTF instance with its
 * own transform; teeth are never merged. Owns the scene, view and camera; models come from the
 * shared [ModelRepository]. Main-thread only.
 */
class ViewportRenderer(private val runtime: FilamentRuntime, private val workspace: Workspace) {

    private val engine = runtime.engine
    private val renderer: Renderer = engine.createRenderer()
    private val scene = engine.createScene()
    private val view: View = engine.createView()
    private val cameraEntity = EntityManager.get().create()
    private val camera: Camera = engine.createCamera(cameraEntity)
    private val skybox = Skybox.Builder().color(0.0060f, 0.0065f, 0.0075f, 1f).build(engine)
    private val lighting: StudioLighting

    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
    private var displayHelper: DisplayHelper? = null
    private var swapChain: SwapChain? = null

    val rig = CameraRig()

    private class Bound(val modelId: String, val instance: FilamentInstance) {
        var inScene = false
        var uniform = false
        /** Original material of each (renderable, primitive), to restore after uniform colouring. */
        val originals = ArrayList<Triple<Int, Int, MaterialInstance>>()
        val uniformMaterials = HashMap<MaterialInstance, MaterialInstance>()
    }

    /** Replace photo textures by a plain enamel colour so only the shape is visible. */
    var uniformColor = false
        set(value) {
            field = value
            lastSignature = Float.NaN
        }

    private val white: Texture = Texture.Builder()
        .width(1).height(1).levels(1)
        .sampler(Texture.Sampler.SAMPLER_2D)
        .format(Texture.InternalFormat.SRGB8_A8)
        .build(engine)
        .also {
            val px = ByteBuffer.allocateDirect(4).put(byteArrayOf(-1, -1, -1, -1)).also { b -> b.flip() }
            it.setImage(engine, 0, Texture.PixelBufferDescriptor(px, Texture.Format.RGBA, Texture.Type.UBYTE))
        }

    private val bound = HashMap<Long, Bound>()
    private val entityToKey = HashMap<Int, Long>()
    private val transform = FloatArray(16)

    /** Screen anchors for the label under each visible tooth, updated every frame. */
    val labelAnchors = mutableStateMapOf<Long, Offset>()
    var isLoading by mutableStateOf(true)
        private set

    private var lightingMode: LightingMode? = null
    private var lastFraming = -1
    private var lastFrameNanos = 0L
    private var lastSignature = Float.NaN
    private var running = false
    private val stats = FrameStats()
    private val dynamicResolution: View.DynamicResolutionOptions
    private val fullResolution = View.DynamicResolutionOptions().apply { enabled = false }
    private var settled = false

    init {
        view.scene = scene
        view.camera = camera
        view.setShadowingEnabled(true)
        view.multiSampleAntiAliasingOptions = View.MultiSampleAntiAliasingOptions().apply {
            enabled = true
            sampleCount = 4
        }
        // While the user interacts, Filament may lower the render resolution if the GPU cannot keep up
        // (e.g. on the tablet); once the scene settles, a final frame is always drawn at full resolution.
        dynamicResolution = View.DynamicResolutionOptions().apply {
            enabled = true
            minScale = 0.6f
            maxScale = 1f
            quality = View.QualityLevel.MEDIUM
            homogeneousScaling = true
        }
        view.dynamicResolutionOptions = dynamicResolution
        scene.skybox = skybox
        camera.setExposure(1f)
        lighting = StudioLighting(engine, scene, view)
    }

    // ---- surface ----------------------------------------------------------------------------

    fun attach(surfaceView: SurfaceView) {
        uiHelper.renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface)
                displayHelper = DisplayHelper(surfaceView.context).also {
                    it.attach(renderer, surfaceView.display)
                }
                lastSignature = Float.NaN
            }

            override fun onDetachedFromSurface() {
                displayHelper?.detach()
                displayHelper = null
                swapChain?.let {
                    engine.destroySwapChain(it)
                    engine.flushAndWait()
                }
                swapChain = null
            }

            override fun onResized(width: Int, height: Int) {
                view.viewport = Viewport(0, 0, width, height)
                rig.setViewport(width, height)
                workspace.visibleBounds()?.let { rig.frame(it, jump = true) }
                lastSignature = Float.NaN
            }
        }
        uiHelper.attachTo(surfaceView)
    }

    fun detach() {
        uiHelper.detach()
    }

    // ---- frame loop -------------------------------------------------------------------------

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            Choreographer.getInstance().postFrameCallback(this)
            renderFrame(frameTimeNanos)
        }
    }

    fun resume() {
        if (running) return
        running = true
        lastSignature = Float.NaN
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun pause() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    fun setLighting(mode: LightingMode) {
        if (mode == lightingMode) return
        lightingMode = mode
        lighting.apply(mode)
        lastSignature = Float.NaN
    }

    private fun renderFrame(frameTimeNanos: Long) {
        val cpuStart = System.nanoTime()
        val dt = if (lastFrameNanos == 0L) 0.016f else ((frameTimeNanos - lastFrameNanos) / 1e9f).coerceIn(0f, 0.1f)
        lastFrameNanos = frameTimeNanos

        runtime.models.update()
        workspace.update(frameTimeNanos)
        syncInstances()

        if (workspace.framingRequest != lastFraming) {
            lastFraming = workspace.framingRequest
            rig.startFollowing()
        }
        if (rig.following) workspace.visibleBounds()?.let { rig.frame(it) }
        rig.update(dt)
        rig.apply(camera)

        applyTransforms()
        updateLabels()
        isLoading = runtime.models.isBusy || workspace.visibleTeeth.any { bound[it.key]?.inScene != true }

        // Skip rendering when nothing on screen changed (saves battery on a static comparison),
        // but draw one last full-resolution frame when motion stops.
        val signature = frameSignature()
        val changed = signature != lastSignature || isLoading
        if (!changed && settled) return
        lastSignature = signature
        settled = !changed
        view.dynamicResolutionOptions = if (changed) dynamicResolution else fullResolution

        val chain = swapChain ?: return
        if (!uiHelper.isReadyToRender) return
        val rendered = renderer.beginFrame(chain, frameTimeNanos)
        if (rendered) {
            renderer.render(view)
            renderer.endFrame()
        }
        stats.record(rendered, System.nanoTime() - cpuStart, frameTimeNanos)
    }

    private fun syncInstances() {
        val placedKeys = workspace.teeth.map { it.key }.toSet()
        // Drop instances for removed teeth or changed variants.
        val stale = bound.filter { (key, b) -> key !in placedKeys || workspace.find(key)?.modelId != b.modelId }
        for ((key, b) in stale) releaseBinding(key, b)

        for (t in workspace.teeth) {
            var b = bound[t.key]
            if (b == null) {
                runtime.models.prefetch(t.modelId)
                val instance = runtime.models.acquire(t.modelId) ?: continue
                b = Bound(t.modelId, instance)
                bound[t.key] = b
                instance.entities.forEach { entityToKey[it] = t.key }
                val rm = engine.renderableManager
                for (e in instance.entities) {
                    if (!rm.hasComponent(e)) continue
                    val ri = rm.getInstance(e)
                    rm.setCastShadows(ri, true)
                    rm.setReceiveShadows(ri, true)
                }
            }
            if (b.uniform != uniformColor) applyColorMode(b, uniformColor)
            val shouldShow = t.visible && t.localBounds != null
            if (shouldShow != b.inScene) {
                if (shouldShow) scene.addEntities(b.instance.entities) else scene.removeEntities(b.instance.entities)
                b.inScene = shouldShow
            }
        }
    }

    private fun applyColorMode(b: Bound, uniform: Boolean) {
        val rm = engine.renderableManager
        if (b.originals.isEmpty()) {
            for (e in b.instance.entities) {
                if (!rm.hasComponent(e)) continue
                val ri = rm.getInstance(e)
                for (p in 0 until rm.getPrimitiveCount(ri)) b.originals += Triple(e, p, rm.getMaterialInstanceAt(ri, p))
            }
        }
        for ((e, p, original) in b.originals) {
            val mi = if (!uniform) original else b.uniformMaterials.getOrPut(original) {
                MaterialInstance.duplicate(original, null).apply {
                    setParameter("baseColorMap", white, TextureSampler())
                    setParameter("baseColorFactor", 0.86f, 0.84f, 0.79f, 1f)
                }
            }
            rm.setMaterialInstanceAt(rm.getInstance(e), p, mi)
        }
        b.uniform = uniform
    }

    private fun releaseBinding(key: Long, b: Bound) {
        if (b.uniform) applyColorMode(b, false)
        b.uniformMaterials.values.forEach { engine.destroyMaterialInstance(it) }
        if (b.inScene) scene.removeEntities(b.instance.entities)
        b.instance.entities.forEach { entityToKey.remove(it) }
        runtime.models.release(b.instance)
        bound.remove(key)
    }

    private fun applyTransforms() {
        val tm = engine.transformManager
        for (t in workspace.teeth) {
            val b = bound[t.key] ?: continue
            val sx = if (t.tooth.isMirrored) -t.scale else t.scale
            t.rotation.toTransform(t.position, sx, t.scale, transform)
            tm.setTransform(tm.getInstance(b.instance.root), transform)
        }
    }

    private fun updateLabels() {
        val present = HashSet<Long>()
        for (t in workspace.teeth) {
            if (!t.visible) continue
            val box = t.worldBounds ?: continue
            val anchor = rig.project(Vec3(box.center.x, box.min.y, box.max.z)) ?: continue
            present += t.key
            val old = labelAnchors[t.key]
            if (old == null || (old - anchor).getDistanceSquared() > 0.25f) labelAnchors[t.key] = anchor
        }
        labelAnchors.keys.retainAll(present)
    }

    private fun frameSignature(): Float {
        var s = rig.signature() + (workspace.selectedKey ?: 0L).toFloat() * 13f + if (uniformColor) 0.5f else 0f
        for (t in workspace.teeth) {
            val r = t.rotation
            val p = t.position
            s += (r.x * 3 + r.y * 5 + r.z * 7 + r.w * 11 + p.x * 0.13f + p.y * 0.17f + p.z * 0.19f + t.scale) *
                (t.key % 97 + 1) + if (bound[t.key]?.inScene == true) t.key.toFloat() else 0f
        }
        return s
    }

    // ---- picking ----------------------------------------------------------------------------

    /** Finds the tooth under a screen point (px, top-left origin). */
    fun pick(x: Float, y: Float, onResult: (Long?) -> Unit) {
        val vy = view.viewport.height - y.toInt()
        view.pick(x.toInt(), vy, pickHandler) { result -> onResult(entityToKey[result.renderable]) }
        lastSignature = Float.NaN // picking runs as part of the next rendered frame
    }

    private val pickHandler = Handler(Looper.getMainLooper())

    fun destroy() {
        pause()
        for ((key, b) in bound.toMap()) releaseBinding(key, b)
        lighting.destroy()
        detach()
        engine.destroyRenderer(renderer)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroySkybox(skybox)
        engine.destroyTexture(white)
        engine.destroyCameraComponent(cameraEntity)
        EntityManager.get().destroy(cameraEntity)
    }
}
