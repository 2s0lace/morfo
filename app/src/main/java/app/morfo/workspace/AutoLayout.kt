package app.morfo.workspace

import app.morfo.math.Box3
import app.morfo.math.Vec3
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Places teeth so their axis-aligned bounds (in their *current* orientation) never intersect.
 * Because disjoint boxes imply disjoint meshes, automatic placement can never overlap teeth.
 */
object AutoLayout {

    /** Resolves [Arrangement.AUTO] and invalid choices to a concrete arrangement + column count. */
    fun resolve(arrangement: Arrangement, count: Int): Pair<Arrangement, Int> = when {
        arrangement == Arrangement.ROW -> Arrangement.ROW to count
        arrangement == Arrangement.TRIANGLE && count == 3 -> Arrangement.TRIANGLE to 2
        arrangement == Arrangement.GRID -> Arrangement.GRID to ceil(sqrt(count.toFloat())).toInt().coerceAtLeast(1)
        count <= 3 -> Arrangement.ROW to count
        count == 4 -> Arrangement.GRID to 2
        count <= 9 -> Arrangement.GRID to 3
        else -> Arrangement.GRID to 4
    }

    /**
     * @param boxes bounds of each tooth relative to its own origin, in current orientation.
     * @param rowGap extra vertical gap between rows (room for labels under the upper row).
     * @return world position of each tooth origin, centred around (0, 0, 0).
     */
    fun arrange(boxes: List<Box3>, arrangement: Arrangement, gap: Float, rowGap: Float = gap): List<Vec3> {
        if (boxes.isEmpty()) return emptyList()
        val (kind, columns) = resolve(arrangement, boxes.size)
        val rows: List<List<Int>> = when (kind) {
            Arrangement.TRIANGLE -> listOf(listOf(0, 1), listOf(2))
            Arrangement.ROW -> listOf(boxes.indices.toList())
            else -> boxes.indices.chunked(columns)
        }
        // In a grid, columns share a width so teeth line up vertically; row/triangle rows are packed.
        val colWidth = FloatArray(columns)
        if (kind == Arrangement.GRID) {
            for (row in rows) row.forEachIndexed { c, i -> colWidth[c] = maxOf(colWidth[c], boxes[i].size.x) }
        }
        val rowHeights = rows.map { row -> row.maxOf { boxes[it].size.y } }
        val totalHeight = rowHeights.sum() + rowGap * (rows.size - 1)

        val out = arrayOfNulls<Vec3>(boxes.size)
        var top = totalHeight / 2
        rows.forEachIndexed { r, row ->
            val widths = row.mapIndexed { c, i -> if (kind == Arrangement.GRID) colWidth[c] else boxes[i].size.x }
            val rowWidth = if (kind == Arrangement.GRID) colWidth.sum() + gap * (columns - 1) else widths.sum() + gap * (row.size - 1)
            var x = -rowWidth / 2
            val cy = top - rowHeights[r] / 2
            row.forEachIndexed { c, i ->
                val cx = x + widths[c] / 2
                val b = boxes[i]
                out[i] = Vec3(cx - b.center.x, cy - b.center.y, -b.center.z)
                x += widths[c] + gap
            }
            top -= rowHeights[r] + rowGap
        }
        return out.map { it!! }
    }
}
