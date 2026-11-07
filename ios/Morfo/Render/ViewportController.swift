import Foundation
import Observation
import RealityKit
import SwiftUI

/// Renders a `Workspace` with RealityKit. Each placed tooth gets its own entity with its own
/// transform; teeth are never merged. Owns the scene graph and camera; models come from the shared
/// `ModelRepository`. Mirrors ViewportRenderer.kt on Android.
@Observable
final class ViewportController {
    /// Screen anchors (points) for the label under each visible tooth, updated every frame.
    private(set) var labelAnchors: [Int: CGPoint] = [:]
    private(set) var isLoading = true

    @ObservationIgnored let workspace: Workspace
    @ObservationIgnored let rig = CameraRig()
    @ObservationIgnored private let models: ModelRepository
    @ObservationIgnored private let lighting = StudioLighting()
    @ObservationIgnored private let root = Entity()
    @ObservationIgnored private let camera = PerspectiveCamera()

    private struct Bound {
        let modelId: String
        let entity: ModelEntity
        var uniform: Bool
    }

    @ObservationIgnored private var bound: [Int: Bound] = [:]
    @ObservationIgnored private var lastFraming = -1
    @ObservationIgnored private var hasSize = false
    @ObservationIgnored private var subscription: EventSubscription?

    /// Replace photo textures by a plain enamel colour so only the shape is visible.
    @ObservationIgnored var uniformColor = false
    @ObservationIgnored var lightingMode = LightingMode.clinical {
        didSet { Task { await lighting.apply(lightingMode) } }
    }

    init(workspace: Workspace, models: ModelRepository) {
        self.workspace = workspace
        self.models = models
    }

    func makeScene(_ content: inout RealityViewCameraContent) {
        content.camera = .virtual
        camera.camera.fieldOfViewInDegrees = CameraRig.fovY
        camera.camera.fieldOfViewOrientation = .vertical
        content.add(camera)
        content.add(lighting.key)
        content.add(lighting.environment)
        content.add(root)
        Task { await lighting.apply(lightingMode) }
        subscription = content.subscribe(to: SceneEvents.Update.self) { [weak self] event in
            self?.tick(dt: Float(min(event.deltaTime, 0.1)))
        }
    }

    func resize(_ size: CGSize) {
        guard size.width > 0, size.height > 0 else { return }
        rig.setViewport(size)
        if let b = workspace.visibleBounds() { rig.frame(b, jump: true) }
        hasSize = true
    }

    // MARK: - Frame

    private var clock: TimeInterval = 0

    private func tick(dt: Float) {
        clock += Double(dt)
        workspace.update(time: clock)
        syncEntities()

        if workspace.framingRequest != lastFraming {
            let first = lastFraming < 0
            lastFraming = workspace.framingRequest
            rig.startFollowing()
            if first, let b = workspace.visibleBounds() { rig.frame(b, jump: true) }
        }
        if rig.following, let b = workspace.visibleBounds() { rig.frame(b) }
        rig.update(dt: dt)

        camera.position = rig.position
        camera.camera.near = rig.near
        camera.camera.far = rig.far

        for t in workspace.teeth {
            guard let b = bound[t.key] else { continue }
            // Right-side teeth already use a mirrored mesh, so the scale stays positive.
            b.entity.transform = Transform(scale: SIMD3(repeating: t.scale), rotation: t.rotation, translation: t.position)
        }
        updateLabels()
        let loading = models.isBusy || workspace.visibleTeeth.contains { bound[$0.key]?.entity.parent == nil }
        if loading != isLoading { isLoading = loading }
    }

    private func syncEntities() {
        let keys = Set(workspace.teeth.map(\.key))
        // Drop entities for removed teeth or changed variants.
        for (key, b) in bound where !keys.contains(key) || workspace.find(key)?.modelId != b.modelId {
            b.entity.removeFromParent()
            bound[key] = nil
        }
        for t in workspace.teeth {
            if bound[t.key] == nil {
                guard let r = models.resources(t.modelId) else { continue }
                let entity = ModelEntity(mesh: t.tooth.isMirrored ? r.mirroredMesh : r.mesh, materials: [uniformColor ? r.uniform : r.textured])
                entity.components.set(lighting.receiver)
                bound[t.key] = Bound(modelId: t.modelId, entity: entity, uniform: uniformColor)
            }
            guard var b = bound[t.key] else { continue }
            if b.uniform != uniformColor, let r = models.resources(t.modelId) {
                b.entity.model?.materials = [uniformColor ? r.uniform : r.textured]
                b.uniform = uniformColor
                bound[t.key] = b
            }
            let shouldShow = hasSize && t.visible && t.localBounds != nil
            if shouldShow && b.entity.parent == nil { root.addChild(b.entity) }
            if !shouldShow && b.entity.parent != nil { b.entity.removeFromParent() }
        }
    }

    private func updateLabels() {
        var anchors: [Int: CGPoint] = [:]
        for t in workspace.teeth where t.visible {
            guard let box = t.worldBounds, let p = rig.project(SIMD3(box.center.x, box.min.y, box.max.z)) else { continue }
            anchors[t.key] = p
        }
        let changed = anchors.count != labelAnchors.count || anchors.contains { key, p in
            guard let old = labelAnchors[key] else { return true }
            return abs(old.x - p.x) > 0.5 || abs(old.y - p.y) > 0.5
        }
        if changed { labelAnchors = anchors }
    }

    // MARK: - Picking

    /// Finds the tooth under a screen point (points, top-left origin).
    func pick(_ point: CGPoint) -> Int? {
        let ray = rig.ray(through: point)
        return workspace.visibleTeeth
            .compactMap { t in t.worldBounds?.intersect(origin: ray.origin, direction: ray.direction).map { (t.key, $0) } }
            .min { $0.1 < $1.1 }?.0
    }
}
