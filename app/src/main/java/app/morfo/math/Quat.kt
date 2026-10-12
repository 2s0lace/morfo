package app.morfo.math

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Unit quaternion (x, y, z, w). Composition `a * b` applies b first, then a. */
data class Quat(val x: Float, val y: Float, val z: Float, val w: Float) {

    operator fun times(o: Quat) = Quat(
        w * o.x + x * o.w + y * o.z - z * o.y,
        w * o.y - x * o.z + y * o.w + z * o.x,
        w * o.z + x * o.y - y * o.x + z * o.w,
        w * o.w - x * o.x - y * o.y - z * o.z,
    )

    fun normalized(): Quat {
        val n = sqrt(x * x + y * y + z * z + w * w)
        return if (n < 1e-8f) IDENTITY else Quat(x / n, y / n, z / n, w / n)
    }

    fun rotate(v: Vec3): Vec3 {
        // v' = v + 2w(q×v) + 2 q×(q×v)
        val q = Vec3(x, y, z)
        val t = q.cross(v) * 2f
        return v + t * w + q.cross(t)
    }

    /**
     * Conjugate by the sagittal mirror (x → -x). A rotation R applied to a left tooth
     * corresponds to mirrorX(R) applied to its mirrored right-side counterpart.
     */
    fun mirrorX() = Quat(x, -y, -z, w)

    fun slerp(to: Quat, t: Float): Quat {
        var b = to
        var d = x * b.x + y * b.y + z * b.z + w * b.w
        if (d < 0f) { b = Quat(-b.x, -b.y, -b.z, -b.w); d = -d }
        if (d > 0.9995f) {
            return Quat(x + (b.x - x) * t, y + (b.y - y) * t, z + (b.z - z) * t, w + (b.w - w) * t).normalized()
        }
        val theta = acos(d.coerceIn(-1f, 1f))
        val s = sin(theta)
        val wa = sin((1 - t) * theta) / s
        val wb = sin(t * theta) / s
        return Quat(x * wa + b.x * wb, y * wa + b.y * wb, z * wa + b.z * wb, w * wa + b.w * wb)
    }

    /** Angular distance in radians. */
    fun angleTo(o: Quat): Float {
        val d = abs(x * o.x + y * o.y + z * o.z + w * o.w).coerceAtMost(1f)
        return 2f * acos(d)
    }

    /** Column-major 4×4 of T · R · S, as expected by Filament's TransformManager. */
    fun toTransform(translation: Vec3, scaleX: Float, scaleYZ: Float, out: FloatArray = FloatArray(16)): FloatArray {
        val xx = x * x; val yy = y * y; val zz = z * z
        val xy = x * y; val xz = x * z; val yz = y * z
        val wx = w * x; val wy = w * y; val wz = w * z
        out[0] = (1 - 2 * (yy + zz)) * scaleX; out[1] = 2 * (xy + wz) * scaleX; out[2] = 2 * (xz - wy) * scaleX; out[3] = 0f
        out[4] = 2 * (xy - wz) * scaleYZ; out[5] = (1 - 2 * (xx + zz)) * scaleYZ; out[6] = 2 * (yz + wx) * scaleYZ; out[7] = 0f
        out[8] = 2 * (xz + wy) * scaleYZ; out[9] = 2 * (yz - wx) * scaleYZ; out[10] = (1 - 2 * (xx + yy)) * scaleYZ; out[11] = 0f
        out[12] = translation.x; out[13] = translation.y; out[14] = translation.z; out[15] = 1f
        return out
    }

    companion object {
        val IDENTITY = Quat(0f, 0f, 0f, 1f)

        fun axisAngle(axis: Vec3, radians: Float): Quat {
            val a = axis.normalized()
            val s = sin(radians / 2)
            return Quat(a.x * s, a.y * s, a.z * s, cos(radians / 2))
        }

        fun degrees(axis: Vec3, deg: Float) = axisAngle(axis, Math.toRadians(deg.toDouble()).toFloat())
    }
}

/** Uniformly distributed random rotation (Shoemake's method). */
fun randomRotation(random: kotlin.random.Random = kotlin.random.Random.Default): Quat {
    val u1 = random.nextFloat()
    val u2 = random.nextFloat() * 2f * Math.PI.toFloat()
    val u3 = random.nextFloat() * 2f * Math.PI.toFloat()
    val a = kotlin.math.sqrt(1f - u1)
    val b = kotlin.math.sqrt(u1)
    return Quat(a * sin(u2), a * cos(u2), b * sin(u3), b * cos(u3))
}
