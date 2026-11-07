import Foundation
import simd

/// Axis-aligned box in world millimetres.
struct Box3: Equatable {
    var min: SIMD3<Float>
    var max: SIMD3<Float>

    var center: SIMD3<Float> { (min + max) * 0.5 }
    var size: SIMD3<Float> { max - min }

    func union(_ o: Box3) -> Box3 { Box3(min: simd_min(min, o.min), max: simd_max(max, o.max)) }
    func translated(_ d: SIMD3<Float>) -> Box3 { Box3(min: min + d, max: max + d) }

    /// Distance along a ray to the box, or nil if missed (slab method).
    func intersect(origin: SIMD3<Float>, direction: SIMD3<Float>) -> Float? {
        var tMin: Float = 0
        var tMax: Float = .greatestFiniteMagnitude
        for axis in 0..<3 {
            let d = direction[axis]
            if abs(d) < 1e-8 {
                if origin[axis] < min[axis] || origin[axis] > max[axis] { return nil }
                continue
            }
            var t1 = (min[axis] - origin[axis]) / d
            var t2 = (max[axis] - origin[axis]) / d
            if t1 > t2 { swap(&t1, &t2) }
            tMin = Swift.max(tMin, t1)
            tMax = Swift.min(tMax, t2)
            if tMin > tMax { return nil }
        }
        return tMin
    }
}

extension simd_quatf {
    static let identity = simd_quatf(ix: 0, iy: 0, iz: 0, r: 1)

    static func degrees(_ axis: SIMD3<Float>, _ deg: Float) -> simd_quatf {
        simd_quatf(angle: deg * .pi / 180, axis: simd_normalize(axis))
    }

    /// Conjugate by the sagittal mirror (x → -x). A rotation R applied to a left tooth
    /// corresponds to mirroredX(R) applied to its mirrored right-side counterpart.
    var mirroredX: simd_quatf { simd_quatf(ix: imag.x, iy: -imag.y, iz: -imag.z, r: real) }

    /// Angular distance in radians.
    func angle(to o: simd_quatf) -> Float {
        2 * acos(Swift.min(abs(simd_dot(vector, o.vector)), 1))
    }

    /// Uniformly distributed random rotation (Shoemake's method).
    static func random(using rng: inout some RandomNumberGenerator) -> simd_quatf {
        let u1 = Float.random(in: 0..<1, using: &rng)
        let u2 = Float.random(in: 0..<(2 * .pi), using: &rng)
        let u3 = Float.random(in: 0..<(2 * .pi), using: &rng)
        let a = sqrt(1 - u1)
        let b = sqrt(u1)
        return simd_quatf(ix: a * sin(u2), iy: a * cos(u2), iz: b * sin(u3), r: b * cos(u3))
    }
}

/// Deterministic generator for tests (SplitMix64).
struct SeededGenerator: RandomNumberGenerator {
    private var state: UInt64

    init(seed: UInt64) { state = seed }

    mutating func next() -> UInt64 {
        state &+= 0x9E37_79B9_7F4A_7C15
        var z = state
        z = (z ^ (z >> 30)) &* 0xBF58_476D_1CE4_E5B9
        z = (z ^ (z >> 27)) &* 0x94D0_49BB_1331_11EB
        return z ^ (z >> 31)
    }
}

/// Type-erased generator so long-lived objects can hold either the system or a seeded generator.
struct AnyRandomGenerator: RandomNumberGenerator {
    private var base: any RandomNumberGenerator

    init(_ base: some RandomNumberGenerator) { self.base = base }

    mutating func next() -> UInt64 { base.next() }
}
