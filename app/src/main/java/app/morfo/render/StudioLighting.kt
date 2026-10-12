package app.morfo.render

import app.morfo.math.Vec3
import app.morfo.workspace.LightingMode
import com.google.android.filament.ColorGrading
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Scene
import com.google.android.filament.ToneMapper
import com.google.android.filament.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * Neutral studio lighting tuned for reading tooth morphology rather than photo-realism.
 *
 * The camera never orbits (teeth rotate instead), so lights stay fixed relative to the viewer:
 * a surface always receives the same light when rotated to the same view, which keeps
 * side-by-side comparisons fair.
 *
 * Each mode = one directional key light (optionally shadowed) + a soft studio environment
 * projected to spherical harmonics + screen-space ambient occlusion for pits and grooves.
 * Scene units are millimetres, camera exposure is 1.0, so intensities are relative values.
 */
class StudioLighting(private val engine: Engine, private val scene: Scene, private val view: View) {

    /** A soft light lobe in the environment: `intensity · max(0, n·dir)^sharpness`. */
    private class Lobe(from: Vec3, val intensity: Float, val sharpness: Float) {
        val dir = from.normalized()
    }

    private class Preset(
        val keyFrom: Vec3,
        val keyIntensity: Float,
        val sky: Float,
        val ground: Float,
        val lobes: List<Lobe>,
        val shadows: Boolean,
        val aoIntensity: Float,
        val aoPower: Float,
        val aoRadiusMm: Float,
        val contactShadows: Boolean,
        val contrast: Float,
    )

    private val key = EntityManager.get().create()
    private var indirect: IndirectLight? = null
    private var grading: ColorGrading? = null

    init {
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.985f, 0.96f)
            .intensity(2.4f)
            .direction(0.5f, -0.6f, -0.6f)
            .castShadows(true)
            .shadowOptions(LightManager.ShadowOptions().apply {
                mapSize = 2048
                constantBias = 0.05f
                normalBias = 0.8f
                shadowCascades = 1
            })
            .build(engine, key)
        scene.addEntity(key)
    }

    fun apply(mode: LightingMode) {
        val p = presets.getValue(mode)
        val lm = engine.lightManager
        val li = lm.getInstance(key)
        val d = (p.keyFrom * -1f).normalized()
        lm.setDirection(li, d.x, d.y, d.z)
        lm.setIntensity(li, p.keyIntensity)
        lm.setShadowCaster(li, p.shadows)

        indirect?.let { engine.destroyIndirectLight(it) }
        indirect = IndirectLight.Builder()
            .radiance(3, projectSh(p))
            .intensity(1f)
            .build(engine)
        scene.indirectLight = indirect

        view.ambientOcclusionOptions = View.AmbientOcclusionOptions().apply {
            enabled = true
            radius = p.aoRadiusMm
            intensity = p.aoIntensity
            power = p.aoPower
            bilateralThreshold = 0.3f
            resolution = 1f
            quality = View.QualityLevel.HIGH
            lowPassFilter = View.QualityLevel.HIGH
            upsampling = View.QualityLevel.HIGH
            ssctEnabled = p.contactShadows
            if (p.contactShadows) {
                ssctLightDirection = floatArrayOf(d.x, d.y, d.z)
                ssctShadowDistance = 2.5f
                ssctContactDistanceMax = 5f
                ssctIntensity = 0.85f
                ssctDepthBias = 0.05f
                ssctSampleCount = 6
            }
        }

        grading?.let { engine.destroyColorGrading(it) }
        grading = ColorGrading.Builder()
            .toneMapper(ToneMapper.PBRNeutralToneMapper())
            .contrast(p.contrast)
            .build(engine)
        view.colorGrading = grading
    }

    fun destroy() {
        scene.removeEntity(key)
        engine.destroyEntity(key)
        EntityManager.get().destroy(key)
        indirect?.let { engine.destroyIndirectLight(it) }
        grading?.let { engine.destroyColorGrading(it) }
    }

    /** Projects the environment radiance onto 3 bands of real SH, in Filament's basis order. */
    private fun projectSh(p: Preset): FloatArray {
        val sh = FloatArray(27)
        val nTheta = 48
        val nPhi = 96
        val dTheta = PI / nTheta
        val dPhi = 2 * PI / nPhi
        for (i in 0 until nTheta) {
            val theta = (i + 0.5) * dTheta
            for (j in 0 until nPhi) {
                val phi = (j + 0.5) * dPhi
                val x = (sin(theta) * cos(phi)).toFloat()
                val y = cos(theta).toFloat()
                val z = (sin(theta) * sin(phi)).toFloat()
                val w = (sin(theta) * dTheta * dPhi).toFloat()
                val radiance = environment(p, Vec3(x, y, z)) * w
                val basis = floatArrayOf(
                    0.282095f,
                    0.488603f * y, 0.488603f * z, 0.488603f * x,
                    1.092548f * x * y, 1.092548f * y * z, 0.315392f * (3 * z * z - 1),
                    1.092548f * x * z, 0.546274f * (x * x - y * y),
                )
                for (k in 0 until 9) {
                    val v = radiance * basis[k]
                    // Neutral white studio: identical RGB.
                    sh[k * 3] += v; sh[k * 3 + 1] += v; sh[k * 3 + 2] += v
                }
            }
        }
        return sh
    }

    private fun environment(p: Preset, n: Vec3): Float {
        val t = (n.y + 1f) / 2f
        var l = p.ground + (p.sky - p.ground) * t
        for (lobe in p.lobes) l += lobe.intensity * max(0f, n.dot(lobe.dir)).pow(lobe.sharpness)
        return l
    }

    private companion object {
        val presets = mapOf(
            LightingMode.CLINICAL to Preset(
                keyFrom = Vec3(-0.65f, 0.70f, 0.35f), keyIntensity = 2.5f,
                sky = 0.14f, ground = 0.04f,
                lobes = listOf(Lobe(Vec3(0.8f, 0.1f, 0.5f), 0.10f, 2f), Lobe(Vec3(0.3f, 0.6f, -0.8f), 0.25f, 4f)),
                shadows = true, aoIntensity = 1.2f, aoPower = 1.5f, aoRadiusMm = 1.0f,
                contactShadows = false, contrast = 1.05f,
            ),
            LightingMode.HIGH_CONTRAST to Preset(
                keyFrom = Vec3(-0.75f, 0.50f, 0.45f), keyIntensity = 3.0f,
                sky = 0.07f, ground = 0.02f,
                lobes = listOf(Lobe(Vec3(0.8f, 0.1f, 0.5f), 0.03f, 2f), Lobe(Vec3(0.3f, 0.5f, -0.9f), 0.35f, 6f)),
                shadows = true, aoIntensity = 1.4f, aoPower = 1.8f, aoRadiusMm = 1.0f,
                contactShadows = true, contrast = 1.15f,
            ),
            LightingMode.SOFT to Preset(
                keyFrom = Vec3(-0.4f, 0.8f, 0.45f), keyIntensity = 1.1f,
                sky = 0.30f, ground = 0.12f,
                lobes = listOf(Lobe(Vec3(0.7f, 0.2f, 0.6f), 0.20f, 1f), Lobe(Vec3(0.2f, 0.5f, -0.9f), 0.20f, 3f)),
                shadows = false, aoIntensity = 0.9f, aoPower = 1.2f, aoRadiusMm = 1.4f,
                contactShadows = false, contrast = 1.0f,
            ),
            LightingMode.MORPHOLOGY to Preset(
                keyFrom = Vec3(-0.70f, 0.45f, 0.55f), keyIntensity = 2.7f,
                sky = 0.13f, ground = 0.04f,
                lobes = listOf(Lobe(Vec3(0.9f, 0.1f, 0.3f), 0.06f, 2f), Lobe(Vec3(0.2f, 0.6f, -0.8f), 0.30f, 5f)),
                shadows = true, aoIntensity = 1.8f, aoPower = 2.2f, aoRadiusMm = 0.7f,
                contactShadows = true, contrast = 1.1f,
            ),
        )
    }
}
