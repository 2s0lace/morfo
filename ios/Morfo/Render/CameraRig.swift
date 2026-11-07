import CoreGraphics
import Foundation
import simd

/// Fixed-orientation camera looking down -Z. It only pans and zooms; teeth rotate instead,
/// so lighting and surface presets stay consistent. Units: millimetres and points.
final class CameraRig {
    static let fovY: Float = 24
    private static let margin: Float = 1.12
    private static let minDistance: Float = 12
    private static let maxDistance: Float = 900

    private(set) var width: Float = 1
    private(set) var height: Float = 1

    /// Screen space covered by UI at the edges; framing keeps teeth out from under it.
    var insets = EdgeInsetsF()

    private(set) var centerX: Float = 0
    private(set) var centerY: Float = 0
    private(set) var distance: Float = 120

    private var goalX: Float = 0
    private var goalY: Float = 0
    private var goalDistance: Float = 120

    /// While true the camera keeps the teeth framed as they rotate or move; any manual pan/zoom stops it.
    private(set) var following = true
    /// A single tooth the camera stays zoomed on (double tap) until the user pans or zooms.
    private(set) var focus: Box3?

    private var focal: Float { (height / 2) / tan(Self.fovY / 2 * .pi / 180) }
    private var mmPerPoint: Float { distance / focal }

    func setViewport(_ size: CGSize) {
        width = max(Float(size.width), 1)
        height = max(Float(size.height), 1)
    }

    func startFollowing() {
        following = true
        focus = nil
    }

    /// Computes the framing goal for `bounds`; the camera eases towards it in `update`.
    func frame(_ bounds: Box3, jump: Bool = false) {
        let usableH = max(height - insets.top - insets.bottom, height * 0.3)
        let usableW = max(width - insets.left - insets.right, width * 0.3)
        let halfH = bounds.size.y / 2 * Self.margin
        let halfW = bounds.size.x / 2 * Self.margin
        let f = focal
        let dForH = halfH * f / (usableH / 2)
        let dForW = halfW * f / (usableW / 2)
        goalDistance = min(max(max(dForH, dForW) + bounds.max.z, Self.minDistance), Self.maxDistance)
        let mm = goalDistance / f
        // Centre the teeth in the screen area not covered by UI panels.
        goalX = bounds.center.x + (insets.right - insets.left) / 2 * mm
        goalY = bounds.center.y - (insets.bottom - insets.top) / 2 * mm
        if jump {
            centerX = goalX; centerY = goalY; distance = goalDistance
        }
    }

    func pan(dx: Float, dy: Float) {
        following = false
        focus = nil
        centerX -= dx * mmPerPoint
        centerY += dy * mmPerPoint
        goalX = centerX; goalY = centerY; goalDistance = distance
    }

    /// Zooms by `factor` (>1 = closer) keeping the point under `pivot` fixed on screen.
    func zoom(_ factor: Float, pivot: CGPoint) {
        following = false
        focus = nil
        let before = mmPerPoint
        let fx = Float(pivot.x) - width / 2
        let fy = Float(pivot.y) - height / 2
        let wx = centerX + fx * before
        let wy = centerY - fy * before
        distance = min(max(distance / factor, Self.minDistance), Self.maxDistance)
        let after = mmPerPoint
        centerX = wx - fx * after
        centerY = wy + fy * after
        goalX = centerX; goalY = centerY; goalDistance = distance
    }

    /// Moves the camera to show `bounds` large (e.g. on double tap).
    func focus(on bounds: Box3) {
        following = false
        focus = bounds
    }

    /// Eases towards the goal. Returns true while still moving.
    @discardableResult
    func update(dt: Float) -> Bool {
        // Re-evaluated every frame so framing adapts when UI bars appear or resize.
        if let focus { frame(focus) }
        let k = 1 - exp(-dt * 10)
        centerX += (goalX - centerX) * k
        centerY += (goalY - centerY) * k
        distance += (goalDistance - distance) * k
        return abs(goalDistance - distance) > 0.01 || abs(goalX - centerX) > 0.005 || abs(goalY - centerY) > 0.005
    }

    var position: SIMD3<Float> { SIMD3(centerX, centerY, distance) }
    var near: Float { max(distance * 0.1, 1) }
    var far: Float { distance * 4 + 200 }

    /// World (mm) → screen (points), or nil if behind the camera.
    func project(_ p: SIMD3<Float>) -> CGPoint? {
        let depth = distance - p.z
        guard depth > 1 else { return nil }
        let f = focal / depth
        return CGPoint(x: CGFloat(width / 2 + (p.x - centerX) * f), y: CGFloat(height / 2 - (p.y - centerY) * f))
    }

    /// Ray through a screen point, for picking.
    func ray(through point: CGPoint) -> (origin: SIMD3<Float>, direction: SIMD3<Float>) {
        let x = (Float(point.x) - width / 2) / focal
        let y = -(Float(point.y) - height / 2) / focal
        return (position, simd_normalize(SIMD3(x, y, -1)))
    }

    /// Converts a screen drag into a world-space displacement on the plane z = `planeZ`.
    func screenDeltaToWorld(dx: Float, dy: Float, planeZ: Float = 0) -> SIMD3<Float> {
        let mm = (distance - planeZ) / focal
        return SIMD3(dx * mm, -dy * mm, 0)
    }
}

struct EdgeInsetsF: Equatable {
    var top: Float = 0
    var left: Float = 0
    var bottom: Float = 0
    var right: Float = 0
}
