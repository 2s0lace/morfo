package app.morfo.math

import kotlin.math.sqrt

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun length() = sqrt(dot(this))
    fun normalized(): Vec3 = length().let { if (it < 1e-8f) this else this * (1f / it) }
    fun lerp(o: Vec3, t: Float) = Vec3(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t)

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
        val X = Vec3(1f, 0f, 0f)
        val Y = Vec3(0f, 1f, 0f)
        val Z = Vec3(0f, 0f, 1f)
    }
}

/** Axis-aligned box in world millimetres. */
data class Box3(val min: Vec3, val max: Vec3) {
    val center get() = (min + max) * 0.5f
    val size get() = max - min

    fun union(o: Box3) = Box3(
        Vec3(minOf(min.x, o.min.x), minOf(min.y, o.min.y), minOf(min.z, o.min.z)),
        Vec3(maxOf(max.x, o.max.x), maxOf(max.y, o.max.y), maxOf(max.z, o.max.z)),
    )

    fun translated(d: Vec3) = Box3(min + d, max + d)
}
