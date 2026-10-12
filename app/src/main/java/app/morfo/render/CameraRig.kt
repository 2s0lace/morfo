package app.morfo.render

import androidx.compose.ui.geometry.Offset
import app.morfo.math.Box3
import app.morfo.math.Vec3
import com.google.android.filament.Camera
import kotlin.math.exp
import kotlin.math.tan

/**
 * Fixed-orientation camera looking down -Z. It only pans and zooms; teeth rotate instead,
 * so lighting and surface presets stay consistent. Units: millimetres and pixels.
 */
class CameraRig {
    var width = 1; private set
    var height = 1; private set

    /** Screen space covered by UI at the top/bottom; framing keeps teeth out from under it. */
    var insetTopPx = 0f
    var insetBottomPx = 0f
    /** Side panels (e.g. the preset column in landscape). */
    var insetLeftPx = 0f
    var insetRightPx = 0f

    private var centerX = 0f
    private var centerY = 0f
    private var distance = 120f

    private var goalX = 0f
    private var goalY = 0f
    private var goalDistance = 120f

    /** While true the camera keeps the teeth framed as they rotate or move; any manual pan/zoom stops it. */
    var following = true
        private set

    /** A single tooth the camera stays zoomed on (double tap) until the user pans or zooms. */
    var focus: Box3? = null
        private set

    private val focal get() = (height / 2f) / tan(Math.toRadians(FOV_Y / 2).toFloat())
    private val mmPerPixel get() = distance / focal

    fun setViewport(w: Int, h: Int) {
        width = w.coerceAtLeast(1)
        height = h.coerceAtLeast(1)
    }

    fun startFollowing() {
        following = true
        focus = null
    }

    /** Computes the framing goal for [bounds]; the camera eases towards it in [update]. */
    fun frame(bounds: Box3, jump: Boolean = false) {
        val usable = (height - insetTopPx - insetBottomPx).coerceAtLeast(height * 0.3f)
        val size = bounds.size
        val halfH = size.y / 2 * MARGIN
        val halfW = size.x / 2 * MARGIN
        val f = focal
        val dForH = halfH * f / (usable / 2)
        val usableW = (width - insetLeftPx - insetRightPx).coerceAtLeast(width * 0.3f)
        val dForW = halfW * f / (usableW / 2f)
        goalDistance = (maxOf(dForH, dForW) + bounds.max.z).coerceIn(MIN_DISTANCE, MAX_DISTANCE)
        val mm = goalDistance / f
        // Centre the teeth in the screen area not covered by UI panels.
        goalX = bounds.center.x + (insetRightPx - insetLeftPx) / 2 * mm
        // Centre the teeth in the screen area not covered by UI bars.
        goalY = bounds.center.y - (insetBottomPx - insetTopPx) / 2 * mm
        if (jump) {
            centerX = goalX; centerY = goalY; distance = goalDistance
        }
    }

    fun pan(dxPx: Float, dyPx: Float) {
        following = false
        focus = null
        centerX -= dxPx * mmPerPixel
        centerY += dyPx * mmPerPixel
        goalX = centerX; goalY = centerY; goalDistance = distance
    }

    /** Zooms by [factor] (>1 = closer) keeping the point under [focus] fixed on screen. */
    fun zoom(factor: Float, pivot: Offset) {
        following = false
        focus = null
        val before = mmPerPixel
        val fx = pivot.x - width / 2f
        val fy = pivot.y - height / 2f
        val wx = centerX + fx * before
        val wy = centerY - fy * before
        distance = (distance / factor).coerceIn(MIN_DISTANCE, MAX_DISTANCE)
        val after = mmPerPixel
        centerX = wx - fx * after
        centerY = wy + fy * after
        goalX = centerX; goalY = centerY; goalDistance = distance
    }

    /** Moves the camera to show [bounds] large (e.g. on double tap). */
    fun focusOn(bounds: Box3) {
        following = false
        focus = bounds
    }

    /** Eases towards the goal. Returns true while still moving. */
    fun update(dtSeconds: Float): Boolean {
        // Re-evaluated every frame so framing adapts when UI bars appear or resize.
        focus?.let { frame(it) }
        val k = 1f - exp(-dtSeconds * 10f)
        centerX += (goalX - centerX) * k
        centerY += (goalY - centerY) * k
        distance += (goalDistance - distance) * k
        return kotlin.math.abs(goalDistance - distance) > 0.01f ||
            kotlin.math.abs(goalX - centerX) > 0.005f || kotlin.math.abs(goalY - centerY) > 0.005f
    }

    fun apply(camera: Camera) {
        val aspect = width.toDouble() / height
        camera.setProjection(FOV_Y, aspect, (distance * 0.1).coerceAtLeast(1.0), distance * 4.0 + 200.0, Camera.Fov.VERTICAL)
        camera.lookAt(
            centerX.toDouble(), centerY.toDouble(), distance.toDouble(),
            centerX.toDouble(), centerY.toDouble(), 0.0,
            0.0, 1.0, 0.0,
        )
    }

    /** World (mm) → screen (px), or null if behind the camera. */
    fun project(p: Vec3): Offset? {
        val depth = distance - p.z
        if (depth <= 1f) return null
        val f = focal / depth
        return Offset(width / 2f + (p.x - centerX) * f, height / 2f - (p.y - centerY) * f)
    }

    /** Converts a screen drag into a world-space displacement on the plane z = [planeZ]. */
    fun screenDeltaToWorld(dxPx: Float, dyPx: Float, planeZ: Float = 0f): Vec3 {
        val mm = (distance - planeZ) / focal
        return Vec3(dxPx * mm, -dyPx * mm, 0f)
    }

    /** A cheap fingerprint of the camera pose, used to skip rendering unchanged frames. */
    fun signature() = centerX * 31f + centerY * 17f + distance * 7f + width + height * 3f

    companion object {
        const val FOV_Y = 24.0
        private const val MARGIN = 1.12f
        private const val MIN_DISTANCE = 12f
        private const val MAX_DISTANCE = 900f
    }
}
