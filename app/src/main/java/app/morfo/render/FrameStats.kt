package app.morfo.render

import android.util.Log

/** Logs rendered frames per second, main-thread cost and GPU-dropped frames every 2 s of activity. */
internal class FrameStats {
    private var windowStart = 0L
    private var rendered = 0
    private var dropped = 0
    private var cpuNanos = 0L

    fun record(wasRendered: Boolean, cpu: Long, now: Long) {
        if (windowStart == 0L) windowStart = now
        if (wasRendered) rendered++ else dropped++
        cpuNanos += cpu
        val elapsed = now - windowStart
        if (elapsed >= 2_000_000_000L) {
            if (rendered + dropped > 0) {
                Log.d(
                    "MorfoPerf",
                    "fps=%.1f dropped=%d cpu=%.2fms/frame".format(
                        rendered * 1e9 / elapsed, dropped, cpuNanos / 1e6 / (rendered + dropped),
                    ),
                )
            }
            windowStart = now; rendered = 0; dropped = 0; cpuNanos = 0
        }
    }
}
