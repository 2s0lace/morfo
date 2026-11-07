import CoreGraphics
import Foundation
import RealityKit

/// Neutral studio lighting tuned for reading tooth morphology rather than photo-realism.
///
/// The camera never orbits (teeth rotate instead), so lights stay fixed relative to the viewer:
/// a surface always receives the same light when rotated to the same view, which keeps
/// side-by-side comparisons fair.
///
/// Each mode = one directional key light (optionally shadowed) + a soft studio environment used as
/// image-based light. Same presets as StudioLighting.kt on Android.
@MainActor
final class StudioLighting {
    /// A soft light lobe in the environment: `intensity · max(0, n·dir)^sharpness`.
    private struct Lobe {
        let dir: SIMD3<Float>
        let intensity: Float
        let sharpness: Float
        init(_ from: SIMD3<Float>, _ intensity: Float, _ sharpness: Float) {
            dir = simd_normalize(from); self.intensity = intensity; self.sharpness = sharpness
        }
    }

    private struct Preset {
        let keyFrom: SIMD3<Float>
        let keyIntensity: Float
        let sky: Float
        let ground: Float
        let lobes: [Lobe]
        let shadows: Bool
    }

    /// Lux per unit of the Android key intensity, and the IBL exposure; tuned by eye against the Filament build.
    private static let luxPerUnit: Float = 1400
    private static let environmentExponent: Float = 1.6

    let key = DirectionalLight()
    let environment = Entity()
    private var cache: [LightingMode: EnvironmentResource] = [:]
    private(set) var mode: LightingMode?

    init() {
        key.light.color = .init(red: 1, green: 0.985, blue: 0.96, alpha: 1)
    }

    func apply(_ mode: LightingMode) async {
        guard mode != self.mode else { return }
        self.mode = mode
        let p = Self.presets[mode]!
        key.light.intensity = p.keyIntensity * Self.luxPerUnit
        key.look(at: .zero, from: p.keyFrom * 100, relativeTo: nil)
        key.shadow = p.shadows ? DirectionalLightComponent.Shadow(maximumDistance: 1500, depthBias: 2) : nil

        let resource: EnvironmentResource
        if let cached = cache[mode] {
            resource = cached
        } else {
            guard let image = Self.environmentImage(p),
                  let generated = try? await EnvironmentResource(equirectangular: image) else { return }
            cache[mode] = generated
            resource = generated
        }
        guard self.mode == mode else { return }
        environment.components.set(ImageBasedLightComponent(source: .single(resource), intensityExponent: Self.environmentExponent))
    }

    /// Component that makes a tooth receive the studio environment light.
    var receiver: ImageBasedLightReceiverComponent { ImageBasedLightReceiverComponent(imageBasedLight: environment) }

    /// Renders the procedural studio environment into a small HDR equirectangular image.
    private static func environmentImage(_ p: Preset) -> CGImage? {
        let w = 128, h = 64
        var pixels = [Float](repeating: 1, count: w * h * 4)
        for j in 0..<h {
            let theta = (Float(j) + 0.5) / Float(h) * .pi
            for i in 0..<w {
                let phi = (Float(i) + 0.5) / Float(w) * 2 * .pi
                let n = SIMD3<Float>(sin(theta) * cos(phi), cos(theta), sin(theta) * sin(phi))
                let t = (n.y + 1) / 2
                var l = p.ground + (p.sky - p.ground) * t
                for lobe in p.lobes { l += lobe.intensity * pow(max(0, simd_dot(n, lobe.dir)), lobe.sharpness) }
                let o = (j * w + i) * 4
                pixels[o] = l; pixels[o + 1] = l; pixels[o + 2] = l
            }
        }
        guard let space = CGColorSpace(name: CGColorSpace.extendedLinearSRGB) else { return nil }
        let data = pixels.withUnsafeBytes { Data($0) } as CFData
        guard let provider = CGDataProvider(data: data) else { return nil }
        return CGImage(
            width: w, height: h, bitsPerComponent: 32, bitsPerPixel: 128, bytesPerRow: w * 16, space: space,
            bitmapInfo: CGBitmapInfo(rawValue: CGBitmapInfo.floatComponents.rawValue | CGBitmapInfo.byteOrder32Little.rawValue
                | CGImageAlphaInfo.premultipliedLast.rawValue),
            provider: provider, decode: nil, shouldInterpolate: true, intent: .defaultIntent
        )
    }

    private static let presets: [LightingMode: Preset] = [
        .clinical: Preset(
            keyFrom: [-0.65, 0.70, 0.35], keyIntensity: 2.5, sky: 0.14, ground: 0.04,
            lobes: [Lobe([0.8, 0.1, 0.5], 0.10, 2), Lobe([0.3, 0.6, -0.8], 0.25, 4)], shadows: true
        ),
        .highContrast: Preset(
            keyFrom: [-0.75, 0.50, 0.45], keyIntensity: 3.0, sky: 0.07, ground: 0.02,
            lobes: [Lobe([0.8, 0.1, 0.5], 0.03, 2), Lobe([0.3, 0.5, -0.9], 0.35, 6)], shadows: true
        ),
        .soft: Preset(
            keyFrom: [-0.4, 0.8, 0.45], keyIntensity: 1.1, sky: 0.30, ground: 0.12,
            lobes: [Lobe([0.7, 0.2, 0.6], 0.20, 1), Lobe([0.2, 0.5, -0.9], 0.20, 3)], shadows: false
        ),
        .morphology: Preset(
            keyFrom: [-0.70, 0.45, 0.55], keyIntensity: 2.7, sky: 0.13, ground: 0.04,
            lobes: [Lobe([0.9, 0.1, 0.3], 0.06, 2), Lobe([0.2, 0.6, -0.8], 0.30, 5)], shadows: true
        ),
    ]
}
