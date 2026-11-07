import Foundation
import Observation
import simd

/// One independent tooth object in the comparison workspace. Never merged with other teeth.
@Observable
final class PlacedTooth: Identifiable {
    let key: Int
    let tooth: Tooth
    fileprivate(set) var variant: ModelVariant
    fileprivate(set) var visible = true

    var id: Int { key }
    var modelId: String { tooth.modelId(variant) }

    // Per-frame state written by Workspace.update() and gestures; deliberately not observed.
    @ObservationIgnored fileprivate(set) var rotation: simd_quatf
    @ObservationIgnored fileprivate(set) var position: SIMD3<Float> = .zero
    @ObservationIgnored fileprivate(set) var scale: Float = 1
    /// Bounds in the current orientation, relative to `position`. Nil until metadata is known.
    @ObservationIgnored fileprivate(set) var localBounds: Box3?
    var worldBounds: Box3? { localBounds?.translated(position) }

    @ObservationIgnored fileprivate var slotPosition: SIMD3<Float> = .zero
    @ObservationIgnored fileprivate var placed = false
    @ObservationIgnored fileprivate var anim: RotationAnim?

    fileprivate init(key: Int, tooth: Tooth, variant: ModelVariant) {
        self.key = key
        self.tooth = tooth
        self.variant = variant
        rotation = SurfacePreset.facial.orientation(tooth)
    }
}

private struct RotationAnim {
    let from: simd_quatf
    let to: simd_quatf
    let start: TimeInterval
    let duration: TimeInterval
}

/// Comparison workspace: the set of teeth on screen and everything the user does to them.
/// Pure state + rules; rendering lives in Render/.
@Observable
final class Workspace {
    static let uniformHeightMm: Float = 22

    private(set) var teeth: [PlacedTooth] = []
    private(set) var selectedKey: Int?

    /// Gestures and presets act on every visible tooth, not only the selected one.
    var syncRotation = true
    /// In synchronised mode, teeth from the opposite side get the mirrored rotation, so e.g. 14 and 24 both turn mesial.
    var mirrorSync = true
    /// Positions follow `arrangement` automatically. Turns off as soon as the user drags a tooth.
    private(set) var autoLayout = true

    var arrangement = Arrangement.auto
    var spacingMm: Float = 4
    var scaleMode = ScaleMode.anatomical
    /// Vertical room reserved under each tooth for its label (0 when labels are hidden).
    var labelSpaceMm: Float = 0
    /// Extra horizontal gap so long labels (full names) of neighbouring teeth do not touch.
    var labelGapMm: Float = 0

    /// The surface view all rotation targets are showing, or nil after free rotation.
    private(set) var currentPreset: SurfacePreset? = .facial

    /// Incremented whenever the camera should re-frame the teeth (new set, auto arrange, preset…).
    private(set) var framingRequest = 0

    @ObservationIgnored private let meta: [String: ModelMeta]
    @ObservationIgnored private var nextKey = 1
    @ObservationIgnored private var now: TimeInterval = 0

    init(meta: [String: ModelMeta]) {
        self.meta = meta
    }

    var selected: PlacedTooth? { teeth.first { $0.key == selectedKey } }
    var visibleTeeth: [PlacedTooth] { teeth.filter(\.visible) }
    var isAnimating: Bool { teeth.contains { $0.anim != nil } }

    // MARK: - Set management

    /// Syncs the workspace with the selector. Keeps teeth (and their pose) that are still selected.
    func setTeeth(_ refs: [ToothRef]) {
        var remaining = teeth
        var result: [PlacedTooth] = []
        var kept = false
        var added = false
        for ref in refs.sorted(by: { ($0.fdi, $0.variant) < ($1.fdi, $1.variant) }) {
            let existing = remaining.filter { $0.tooth.fdi == ref.fdi && $0.variant == ref.variant }
            if existing.isEmpty {
                result.append(PlacedTooth(key: nextKey, tooth: ref.tooth, variant: ref.variant))
                added = true
                nextKey += 1
            } else {
                result += existing
                kept = true
                remaining.removeAll { r in existing.contains { $0 === r } }
            }
        }
        teeth = result
        // New teeth start in the facial view; mixed with turned teeth there is no common view.
        if !kept { currentPreset = .facial } else if added && currentPreset != .facial { currentPreset = nil }
        if !teeth.contains(where: { $0.key == selectedKey }) { selectedKey = nil }
        requestFraming()
    }

    func refs() -> [ToothRef] {
        var seen = Set<ToothRef>()
        return teeth.map { ToothRef(fdi: $0.tooth.fdi, variant: $0.variant) }.filter { seen.insert($0).inserted }
    }

    func select(_ key: Int?) { selectedKey = key }

    func toggleSelect(_ key: Int) { selectedKey = selectedKey == key ? nil : key }

    func setVisible(_ key: Int, _ visible: Bool) {
        guard let t = find(key) else { return }
        t.visible = visible
        if !visible && selectedKey == key { selectedKey = nil }
        if autoLayout { requestFraming() }
    }

    func duplicate(_ key: Int) {
        guard let src = find(key), let index = teeth.firstIndex(where: { $0 === src }) else { return }
        let copy = PlacedTooth(key: nextKey, tooth: src.tooth, variant: src.variant)
        nextKey += 1
        copy.rotation = src.rotation
        teeth.insert(copy, at: index + 1)
        selectedKey = copy.key
        requestFraming()
    }

    func remove(_ key: Int) {
        teeth.removeAll { $0.key == key }
        if selectedKey == key { selectedKey = nil }
        requestFraming()
    }

    func setVariant(_ key: Int, _ variant: ModelVariant) {
        guard let t = find(key), t.tooth.variants.contains(variant) else { return }
        t.variant = variant
    }

    // MARK: - Orientation

    /// Teeth affected by rotation gestures and presets.
    func rotationTargets() -> [PlacedTooth] {
        if let sel = selected, sel.visible, !syncRotation { return [sel] }
        return visibleTeeth
    }

    /// Applies a world-space rotation increment (from a drag) to every target, each around its own centre.
    func rotate(by delta: simd_quatf) {
        let targets = rotationTargets()
        let referenceTooth = (selected.flatMap { s in targets.contains { $0 === s } ? s : nil } ?? targets.first)
        let reference = referenceTooth?.tooth.isMirrored ?? false
        if currentPreset != nil { currentPreset = nil }
        for t in targets {
            let d = mirrorSync && t.tooth.isMirrored != reference ? delta.mirroredX : delta
            t.anim = nil
            t.rotation = simd_normalize(d * t.rotation)
        }
    }

    func apply(_ preset: SurfacePreset) {
        animate(rotationTargets()) { preset.orientation($0.tooth) }
        currentPreset = preset
        requestFraming()
    }

    /// Sets a tooth's orientation directly (no animation).
    func setRotation(_ key: Int, _ rotation: simd_quatf) {
        guard let t = find(key) else { return }
        t.anim = nil
        t.rotation = simd_normalize(rotation)
        currentPreset = SurfacePreset.allCases.first { $0.orientation(t.tooth) == t.rotation }
    }

    func resetOrientation(_ key: Int) {
        guard let t = find(key) else { return }
        animate([t]) { SurfacePreset.facial.orientation($0.tooth) }
        currentPreset = rotationTargets().count == 1 ? .facial : nil
    }

    private func animate(_ targets: [PlacedTooth], to goal: (PlacedTooth) -> simd_quatf) {
        for t in targets {
            let g = goal(t)
            let angle = Double(t.rotation.angle(to: g))
            t.anim = RotationAnim(from: t.rotation, to: g, start: now, duration: (220 + angle / .pi * 260) / 1000)
        }
    }

    // MARK: - Position

    func move(_ key: Int, by delta: SIMD3<Float>) {
        guard let t = find(key) else { return }
        autoLayout = false
        t.position += delta
    }

    func resetPosition(_ key: Int) {
        guard let t = find(key) else { return }
        t.position = t.slotPosition
    }

    func autoArrange() {
        autoLayout = true
        requestFraming()
    }

    func requestFraming() { framingRequest += 1 }

    // MARK: - Per frame

    /// Advances animations and recomputes bounds / automatic positions. Call once per frame.
    func update(time: TimeInterval) {
        now = time
        for t in teeth {
            guard let a = t.anim else { continue }
            let p = Float(min(max((time - a.start) / a.duration, 0), 1))
            t.rotation = simd_slerp(a.from, a.to, Self.easeInOut(p))
            if p >= 1 { t.anim = nil }
        }

        for t in teeth {
            t.scale = scaleMode == .uniform ? Self.uniformHeightMm / t.tooth.averageLengthMm : 1
            t.localBounds = meta[t.modelId]?.rotatedBounds(rotation: t.rotation, mirrored: t.tooth.isMirrored, scale: t.scale)
        }

        let laidOut = teeth.filter { $0.visible && $0.localBounds != nil }
        let slots = AutoLayout.arrange(laidOut.map { $0.localBounds! }, arrangement: arrangement,
                                       gap: spacingMm + labelGapMm, rowGap: spacingMm + labelSpaceMm)
        for (i, t) in laidOut.enumerated() { t.slotPosition = slots[i] }

        if autoLayout {
            for t in laidOut {
                t.position = t.slotPosition
                t.placed = true
            }
        } else {
            // Free layout: never move what the user placed; put newcomers to the right of everything.
            for t in laidOut where !t.placed {
                let occupied = laidOut.filter(\.placed).compactMap(\.worldBounds).reduce(nil as Box3?) { $0?.union($1) ?? $1 }
                let b = t.localBounds!
                t.position = occupied.map { SIMD3($0.max.x + spacingMm - b.min.x, $0.center.y - b.center.y, -b.center.z) }
                    ?? t.slotPosition
                t.placed = true
            }
        }
    }

    /// Bounds of all visible teeth plus room for their labels, or nil if nothing is laid out yet.
    func visibleBounds() -> Box3? {
        guard let b = visibleTeeth.compactMap(\.worldBounds).reduce(nil as Box3?, { $0?.union($1) ?? $1 }) else { return nil }
        return Box3(min: SIMD3(b.min.x, b.min.y - labelSpaceMm, b.min.z), max: b.max)
    }

    func find(_ key: Int) -> PlacedTooth? { teeth.first { $0.key == key } }

    private static func easeInOut(_ t: Float) -> Float {
        t < 0.5 ? 4 * t * t * t : 1 - pow(-2 * t + 2, 3) / 2
    }
}
