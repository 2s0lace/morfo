package app.morfo.ui.compare

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.atan2

/** Callbacks for the 3D viewport. All offsets are in pixels. */
interface ViewportGestureHandler {
    fun onTap(position: Offset)
    fun onDoubleTap(position: Offset)
    /** One finger drag: rotate. */
    fun onRotateDrag(delta: Offset)
    /** Long press, then drag: move the tooth under the finger. */
    fun onMoveStart(position: Offset)
    fun onMoveDrag(delta: Offset)
    fun onMoveEnd()
    /** Two fingers: pan + pinch zoom the camera, twist rotates around the viewing axis. */
    fun onTransform(pan: Offset, zoom: Float, twistRadians: Float, centroid: Offset)
}

fun Modifier.viewportGestures(handler: ViewportGestureHandler): Modifier = pointerInput(handler) {
    var lastTapTime = 0L
    var lastTapPosition = Offset.Zero

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val slop = viewConfiguration.touchSlop
        val longPressMs = viewConfiguration.longPressTimeoutMillis
        var dragging = false
        var multi = false
        var moving = false
        var travelled = 0f
        var upPosition = down.position
        var latestUptime = down.uptimeMillis

        var prevCentroid: Offset? = null
        var prevSpan = 0f
        var prevAngle = 0f
        var prevCount = 0
        var twistTotal = 0f
        var twisting = false

        while (true) {
            val waitingForLongPress = !dragging && !multi && !moving
            val event: PointerEvent? = if (waitingForLongPress) {
                val remaining = longPressMs - (latestUptime - down.uptimeMillis)
                if (remaining <= 0) null else withTimeoutOrNull(remaining) { awaitPointerEvent() }
            } else {
                awaitPointerEvent()
            }
            if (event == null) {
                moving = true
                handler.onMoveStart(down.position)
                continue
            }
            latestUptime = event.changes.maxOf { it.uptimeMillis }
            val pressed = event.changes.filter { it.pressed }
            if (pressed.isEmpty()) {
                upPosition = event.changes.first().position
                break
            }

            if (pressed.size >= 2 && !moving) {
                multi = true
                val a = pressed[0].position
                val b = pressed[1].position
                val centroid = (a + b) / 2f
                val span = (a - b).getDistance().coerceAtLeast(1f)
                val angle = atan2(b.y - a.y, b.x - a.x)
                if (prevCount == pressed.size && prevCentroid != null) {
                    var dAngle = angle - prevAngle
                    if (dAngle > PI) dAngle -= (2 * PI).toFloat()
                    if (dAngle < -PI) dAngle += (2 * PI).toFloat()
                    // Twist only after a deliberate rotation, so pinching does not turn teeth by accident.
                    twistTotal += dAngle
                    if (!twisting && kotlin.math.abs(twistTotal) > TWIST_THRESHOLD) twisting = true
                    handler.onTransform(centroid - prevCentroid, span / prevSpan, if (twisting) dAngle else 0f, centroid)
                }
                prevCentroid = centroid
                prevSpan = span
                prevAngle = angle
                prevCount = pressed.size
            } else if (!multi) {
                val change = pressed[0]
                val delta = change.position - change.previousPosition
                travelled += delta.getDistance()
                if (moving) {
                    handler.onMoveDrag(delta)
                } else {
                    if (!dragging && travelled > slop) dragging = true
                    if (dragging) handler.onRotateDrag(delta)
                }
            } else {
                prevCount = pressed.size // lifted one of two fingers: wait for release
            }
            event.changes.forEach { it.consume() }
        }

        when {
            moving -> handler.onMoveEnd()
            !dragging && !multi -> {
                val now = latestUptime
                if (now - lastTapTime < viewConfiguration.doubleTapTimeoutMillis &&
                    (upPosition - lastTapPosition).getDistance() < 80f
                ) {
                    handler.onDoubleTap(upPosition)
                    lastTapTime = 0L
                } else {
                    handler.onTap(upPosition)
                    lastTapTime = now
                    lastTapPosition = upPosition
                }
            }
        }
    }
}

private const val TWIST_THRESHOLD = 0.2f
