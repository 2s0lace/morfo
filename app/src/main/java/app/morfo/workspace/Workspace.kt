package app.morfo.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.morfo.dentition.ModelVariant
import app.morfo.dentition.Tooth
import app.morfo.dentition.ToothRef
import app.morfo.math.Box3
import app.morfo.math.Quat
import app.morfo.math.Vec3
import app.morfo.model.ModelMeta

/** One independent tooth object in the comparison workspace. Never merged with other teeth. */
class PlacedTooth internal constructor(val key: Long, val tooth: Tooth, variant: ModelVariant) {
    var variant by mutableStateOf(variant)
        internal set
    var visible by mutableStateOf(true)
        internal set

    val modelId get() = tooth.modelId(variant)

    // Per-frame state written by Workspace.update() and gestures; deliberately not Compose state.
    var rotation: Quat = SurfacePreset.FACIAL.orientation(tooth)
        internal set
    var position: Vec3 = Vec3.ZERO
        internal set
    var scale: Float = 1f
        internal set

    /** Bounds in the current orientation, relative to [position]. Null until metadata is known. */
    var localBounds: Box3? = null
        internal set
    val worldBounds: Box3? get() = localBounds?.translated(position)

    internal var slotPosition: Vec3 = Vec3.ZERO
    internal var placed = false
    internal var anim: RotationAnim? = null
}

internal class RotationAnim(val from: Quat, val to: Quat, val startNanos: Long, val durationNanos: Long)

/**
 * Comparison workspace: the set of teeth on screen and everything the user does to them.
 * Pure state + rules; rendering lives in app.morfo.render.
 */
class Workspace(private val meta: Map<String, ModelMeta>) {

    val teeth = mutableStateListOf<PlacedTooth>()

    var selectedKey by mutableStateOf<Long?>(null)
        private set

    /** Gestures and presets act on every visible tooth, not only the selected one. */
    var syncRotation by mutableStateOf(true)

    /** In synchronised mode, teeth from the opposite side get the mirrored rotation, so e.g. 14 and 24 both turn mesial. */
    var mirrorSync by mutableStateOf(true)

    /** Positions follow [arrangement] automatically. Turns off as soon as the user drags a tooth. */
    var autoLayout by mutableStateOf(true)
        private set

    var arrangement by mutableStateOf(Arrangement.AUTO)
    var spacingMm by mutableFloatStateOf(4f)
    var scaleMode by mutableStateOf(ScaleMode.ANATOMICAL)

    /** Vertical room reserved under each tooth for its label (0 when labels are hidden). */
    var labelSpaceMm by mutableFloatStateOf(0f)

    /** Extra horizontal gap so long labels (full names) of neighbouring teeth do not touch. */
    var labelGapMm by mutableFloatStateOf(0f)

    /** Incremented whenever the camera should re-frame the teeth (new set, auto arrange, preset…). */
    var framingRequest by mutableIntStateOf(0)
        private set

    private var nextKey = 1L
    private var nowNanos = 0L

    val selected: PlacedTooth? get() = teeth.firstOrNull { it.key == selectedKey }
    val visibleTeeth: List<PlacedTooth> get() = teeth.filter { it.visible }
    val isAnimating get() = teeth.any { it.anim != null }

    // ---- set management -------------------------------------------------------------------

    /** Syncs the workspace with the selector. Keeps teeth (and their pose) that are still selected. */
    fun setTeeth(refs: List<ToothRef>) {
        val remaining = teeth.toMutableList()
        val result = mutableListOf<PlacedTooth>()
        for (ref in refs.sortedWith(compareBy({ it.fdi }, { it.variant.ordinal }))) {
            val existing = remaining.filter { it.tooth.fdi == ref.fdi && it.variant == ref.variant }
            if (existing.isNotEmpty()) {
                result += existing
                remaining.removeAll(existing)
            } else {
                result += PlacedTooth(nextKey++, ref.tooth, ref.variant)
            }
        }
        teeth.clear()
        teeth.addAll(result)
        if (teeth.none { it.key == selectedKey }) selectedKey = null
        requestFraming()
    }

    fun refs(): List<ToothRef> = teeth.map { ToothRef(it.tooth.fdi, it.variant) }.distinct()

    fun select(key: Long?) {
        selectedKey = key
    }

    fun toggleSelect(key: Long) {
        selectedKey = if (selectedKey == key) null else key
    }

    fun setVisible(key: Long, visible: Boolean) {
        val t = find(key) ?: return
        t.visible = visible
        if (!visible && selectedKey == key) selectedKey = null
        if (autoLayout) requestFraming()
    }

    fun duplicate(key: Long) {
        val src = find(key) ?: return
        val copy = PlacedTooth(nextKey++, src.tooth, src.variant).also { it.rotation = src.rotation }
        teeth.add(teeth.indexOf(src) + 1, copy)
        selectedKey = copy.key
        requestFraming()
    }

    fun remove(key: Long) {
        teeth.removeAll { it.key == key }
        if (selectedKey == key) selectedKey = null
        requestFraming()
    }

    fun setVariant(key: Long, variant: ModelVariant) {
        val t = find(key) ?: return
        if (variant in t.tooth.variants) t.variant = variant
    }

    // ---- orientation ----------------------------------------------------------------------

    /** Teeth affected by rotation gestures and presets. */
    fun rotationTargets(): List<PlacedTooth> {
        val sel = selected
        return if (syncRotation || sel == null || !sel.visible) visibleTeeth else listOf(sel)
    }

    /** Applies a world-space rotation increment (from a drag) to every target, each around its own centre. */
    fun rotateBy(delta: Quat) {
        val targets = rotationTargets()
        val reference = (selected?.takeIf { it in targets } ?: targets.firstOrNull())?.tooth?.isMirrored ?: false
        for (t in targets) {
            val d = if (mirrorSync && t.tooth.isMirrored != reference) delta.mirrorX() else delta
            t.anim = null
            t.rotation = (d * t.rotation).normalized()
        }
    }

    fun applyPreset(preset: SurfacePreset) {
        animate(rotationTargets()) { preset.orientation(it.tooth) }
        requestFraming()
    }

    /** Sets a tooth's orientation directly (no animation). */
    fun setRotation(key: Long, rotation: Quat) {
        val t = find(key) ?: return
        t.anim = null
        t.rotation = rotation.normalized()
    }

    fun resetOrientation(key: Long) {
        val t = find(key) ?: return
        animate(listOf(t)) { SurfacePreset.FACIAL.orientation(it.tooth) }
    }

    private fun animate(targets: List<PlacedTooth>, to: (PlacedTooth) -> Quat) {
        for (t in targets) {
            val goal = to(t)
            val angle = t.rotation.angleTo(goal)
            val duration = (220 + angle / Math.PI * 260).toLong() * 1_000_000L
            t.anim = RotationAnim(t.rotation, goal, nowNanos, duration)
        }
    }

    // ---- position -------------------------------------------------------------------------

    fun moveBy(key: Long, delta: Vec3) {
        val t = find(key) ?: return
        autoLayout = false
        t.position = t.position + delta
    }

    fun resetPosition(key: Long) {
        val t = find(key) ?: return
        t.position = t.slotPosition
    }

    fun autoArrange() {
        autoLayout = true
        requestFraming()
    }

    fun requestFraming() {
        framingRequest++
    }

    // ---- per frame ------------------------------------------------------------------------

    /** Advances animations and recomputes bounds / automatic positions. Call once per frame. */
    fun update(frameTimeNanos: Long) {
        nowNanos = frameTimeNanos
        for (t in teeth) {
            val a = t.anim ?: continue
            val p = ((frameTimeNanos - a.startNanos).toFloat() / a.durationNanos).coerceIn(0f, 1f)
            t.rotation = a.from.slerp(a.to, easeInOut(p))
            if (p >= 1f) t.anim = null
        }

        for (t in teeth) {
            t.scale = if (scaleMode == ScaleMode.UNIFORM) UNIFORM_HEIGHT_MM / t.tooth.averageLengthMm else 1f
            t.localBounds = meta[t.modelId]?.rotatedBounds(t.rotation, t.tooth.isMirrored, t.scale)
        }

        val laidOut = teeth.filter { it.visible && it.localBounds != null }
        val slots = AutoLayout.arrange(laidOut.map { it.localBounds!! }, arrangement, spacingMm + labelGapMm, spacingMm + labelSpaceMm)
        laidOut.forEachIndexed { i, t -> t.slotPosition = slots[i] }

        if (autoLayout) {
            laidOut.forEach { it.position = it.slotPosition; it.placed = true }
        } else {
            // Free layout: never move what the user placed; put newcomers to the right of everything.
            for (t in laidOut.filter { !it.placed }) {
                val occupied = laidOut.filter { it.placed }.mapNotNull { it.worldBounds }.reduceOrNull(Box3::union)
                val b = t.localBounds!!
                t.position = if (occupied == null) t.slotPosition else
                    Vec3(occupied.max.x + spacingMm - b.min.x, occupied.center.y - b.center.y, -b.center.z)
                t.placed = true
            }
        }
    }

    /** Bounds of all visible teeth plus room for their labels, or null if nothing is laid out yet. */
    fun visibleBounds(): Box3? = teeth.filter { it.visible }.mapNotNull { it.worldBounds }.reduceOrNull(Box3::union)
        ?.let { Box3(Vec3(it.min.x, it.min.y - labelSpaceMm, it.min.z), it.max) }

    fun find(key: Long) = teeth.firstOrNull { it.key == key }

    private fun easeInOut(t: Float) = if (t < 0.5f) 4 * t * t * t else 1 - (-2 * t + 2).let { it * it * it } / 2

    companion object {
        const val UNIFORM_HEIGHT_MM = 22f
    }
}
