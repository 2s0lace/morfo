package app.morfo.model

import android.content.res.AssetManager
import app.morfo.math.Box3
import app.morfo.math.Quat
import app.morfo.math.Vec3
import org.json.JSONObject

/**
 * Lightweight per-model metadata produced by tools/asset-pipeline (assets/models/meta.json).
 * Loaded up front so layout can work before any geometry is on the GPU.
 */
class ModelMeta(val id: String, val bounds: Box3, private val support: FloatArray) {

    /** Tight bounds of the model after rotation (and optional mirror / scale), relative to its origin. */
    fun rotatedBounds(rotation: Quat, mirrored: Boolean, scale: Float): Box3 {
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        val sx = if (mirrored) -scale else scale
        var i = 0
        while (i < support.size) {
            val p = rotation.rotate(Vec3(support[i] * sx, support[i + 1] * scale, support[i + 2] * scale))
            if (p.x < minX) minX = p.x; if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y; if (p.y > maxY) maxY = p.y
            if (p.z < minZ) minZ = p.z; if (p.z > maxZ) maxZ = p.z
            i += 3
        }
        return Box3(Vec3(minX, minY, minZ), Vec3(maxX, maxY, maxZ))
    }

    companion object {
        fun loadAll(assets: AssetManager): Map<String, ModelMeta> {
            val json = JSONObject(assets.open("models/meta.json").bufferedReader().use { it.readText() })
            return json.keys().asSequence().associateWith { id ->
                val o = json.getJSONObject(id)
                val min = o.getJSONArray("boundsMin")
                val max = o.getJSONArray("boundsMax")
                val pts = o.getJSONArray("support")
                val support = FloatArray(pts.length() * 3)
                for (k in 0 until pts.length()) {
                    val p = pts.getJSONArray(k)
                    for (c in 0..2) support[k * 3 + c] = p.getDouble(c).toFloat()
                }
                ModelMeta(
                    id,
                    Box3(
                        Vec3(min.getDouble(0).toFloat(), min.getDouble(1).toFloat(), min.getDouble(2).toFloat()),
                        Vec3(max.getDouble(0).toFloat(), max.getDouble(1).toFloat(), max.getDouble(2).toFloat()),
                    ),
                    support,
                )
            }
        }
    }
}
