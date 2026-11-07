import Foundation
import simd

/// Places teeth so their axis-aligned bounds (in their *current* orientation) never intersect.
/// Because disjoint boxes imply disjoint meshes, automatic placement can never overlap teeth.
enum AutoLayout {

    /// Resolves `.auto` and invalid choices to a concrete arrangement + column count.
    static func resolve(_ arrangement: Arrangement, count: Int) -> (Arrangement, Int) {
        switch arrangement {
        case .row: return (.row, count)
        case .triangle where count == 3: return (.triangle, 2)
        case .grid: return (.grid, max(Int(ceil(sqrt(Double(count)))), 1))
        default:
            if count <= 3 { return (.row, count) }
            if count == 4 { return (.grid, 2) }
            if count <= 9 { return (.grid, 3) }
            return (.grid, 4)
        }
    }

    /// - Parameters:
    ///   - boxes: bounds of each tooth relative to its own origin, in current orientation.
    ///   - rowGap: extra vertical gap between rows (room for labels under the upper row).
    /// - Returns: world position of each tooth origin, centred around (0, 0, 0).
    static func arrange(_ boxes: [Box3], arrangement: Arrangement, gap: Float, rowGap: Float? = nil) -> [SIMD3<Float>] {
        guard !boxes.isEmpty else { return [] }
        let rowGap = rowGap ?? gap
        let (kind, columns) = resolve(arrangement, count: boxes.count)
        let rows: [[Int]] = switch kind {
        case .triangle: [[0, 1], [2]]
        case .row: [Array(boxes.indices)]
        default: stride(from: 0, to: boxes.count, by: columns).map { Array($0..<min($0 + columns, boxes.count)) }
        }
        // In a grid, columns share a width so teeth line up vertically; row/triangle rows are packed.
        var colWidth = [Float](repeating: 0, count: columns)
        if kind == .grid {
            for row in rows { for (c, i) in row.enumerated() { colWidth[c] = max(colWidth[c], boxes[i].size.x) } }
        }
        let rowHeights = rows.map { row in row.map { boxes[$0].size.y }.max() ?? 0 }
        let totalHeight = rowHeights.reduce(0, +) + rowGap * Float(rows.count - 1)

        var out = [SIMD3<Float>](repeating: .zero, count: boxes.count)
        var top = totalHeight / 2
        for (r, row) in rows.enumerated() {
            let widths = row.enumerated().map { c, i in kind == .grid ? colWidth[c] : boxes[i].size.x }
            let rowWidth = kind == .grid
                ? colWidth.reduce(0, +) + gap * Float(columns - 1)
                : widths.reduce(0, +) + gap * Float(row.count - 1)
            var x = -rowWidth / 2
            let cy = top - rowHeights[r] / 2
            for (c, i) in row.enumerated() {
                let cx = x + widths[c] / 2
                let b = boxes[i]
                out[i] = SIMD3(cx - b.center.x, cy - b.center.y, -b.center.z)
                x += widths[c] + gap
            }
            top -= rowHeights[r] + rowGap
        }
        return out
    }
}
