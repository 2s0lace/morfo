import Foundation
import simd

/// Lightweight per-model metadata produced by tools/asset-pipeline (models/meta.json).
/// Loaded up front so layout can work before any geometry is loaded.
struct ModelMeta {
    let id: String
    let bounds: Box3
    /// Points on the convex hull, enough to compute tight bounds in any orientation.
    let support: [SIMD3<Float>]

    /// Tight bounds of the model after rotation (and optional mirror / scale), relative to its origin.
    func rotatedBounds(rotation: simd_quatf, mirrored: Bool, scale: Float) -> Box3 {
        let s = SIMD3<Float>(mirrored ? -scale : scale, scale, scale)
        var lo = SIMD3<Float>(repeating: .greatestFiniteMagnitude)
        var hi = SIMD3<Float>(repeating: -.greatestFiniteMagnitude)
        for p in support {
            let r = rotation.act(p * s)
            lo = simd_min(lo, r)
            hi = simd_max(hi, r)
        }
        return Box3(min: lo, max: hi)
    }

    static func loadAll(bundle: Bundle = .main) -> [String: ModelMeta] {
        guard let url = bundle.url(forResource: "meta", withExtension: "json"),
              let data = try? Data(contentsOf: url) else { return [:] }
        return parse(data)
    }

    static func parse(_ data: Data) -> [String: ModelMeta] {
        struct Entry: Decodable {
            let boundsMin: [Float]
            let boundsMax: [Float]
            let support: [[Float]]
        }
        guard let entries = try? JSONDecoder().decode([String: Entry].self, from: data) else { return [:] }
        return entries.reduce(into: [:]) { out, item in
            let e = item.value
            out[item.key] = ModelMeta(
                id: item.key,
                bounds: Box3(min: SIMD3(e.boundsMin[0], e.boundsMin[1], e.boundsMin[2]),
                             max: SIMD3(e.boundsMax[0], e.boundsMax[1], e.boundsMax[2])),
                support: e.support.map { SIMD3($0[0], $0[1], $0[2]) }
            )
        }
    }
}
