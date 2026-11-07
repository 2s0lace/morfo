import SwiftUI
import simd

/// Comparison workspace: teeth side by side in 3D under two floating islands —
/// navigation and the tooth list on top, the selected tooth and surface views at the bottom.
struct CompareScreen: View {
    @Environment(AppModel.self) private var app
    @Environment(\.dismiss) private var dismiss
    @State private var controller: ViewportController
    @State private var showSettings = false
    @State private var infoKey: Int?
    @State private var movingKey: Int?
    @State private var topInset: CGFloat = 0
    @State private var bottomInset: CGFloat = 0
    @State private var rightInset: CGFloat = 0

    init(app: AppModel) {
        _controller = State(initialValue: ViewportController(workspace: app.workspace, models: app.models))
    }

    private var workspace: Workspace { app.workspace }

    var body: some View {
        GeometryReader { geo in
            // Landscape phone: too little height for islands above and below the teeth.
            let compact = geo.size.height < 480
            let screenBottom = geo.frame(in: .global).maxY + geo.safeAreaInsets.bottom
            ZStack {
                Viewport3D(controller: controller, labelMode: app.labelMode, gestures: gestures)
                    .ignoresSafeArea()

                VStack(spacing: 0) {
                    topBar
                        .onGeometryChange(for: CGFloat.self, of: { $0.frame(in: .global).maxY }) { topInset = $0 }
                    Spacer(minLength: 0)
                    GlassEffectContainer(spacing: 10) {
                        VStack(spacing: 10) {
                            if let t = workspace.selected {
                                SelectedToothIsland(tooth: t, onInfo: { infoKey = t.key })
                                    .transition(.move(edge: .bottom).combined(with: .opacity))
                            }
                            if !compact { PresetIsland(workspace: workspace) }
                        }
                    }
                    .frame(maxWidth: 560)
                    .padding(.horizontal, 12)
                    .padding(.bottom, 6)
                    .onGeometryChange(for: CGFloat.self, of: { $0.frame(in: .global).minY }) { bottomInset = screenBottom - $0 }
                }
                .padding(.trailing, compact ? rightInset : 0)
                .animation(.smooth(duration: 0.3), value: workspace.selectedKey)

                if compact {
                    HStack {
                        Spacer()
                        PresetIsland(workspace: workspace, vertical: true)
                            .frame(width: 112)
                            .padding(.trailing, 8)
                            .onGeometryChange(for: CGFloat.self, of: \.size.width) { rightInset = $0 + 16 }
                    }
                }

                if controller.isLoading {
                    Island(cornerRadius: 20, padding: 14) {
                        HStack(spacing: 10) {
                            ProgressView().tint(MorfoColors.accent)
                            Text(app.s.loadingModels).font(.subheadline).foregroundStyle(MorfoColors.textMuted)
                        }
                    }
                    .transition(.opacity)
                }
            }
            .animation(.easeOut(duration: 0.2), value: controller.isLoading)
            .onChange(of: [topInset, bottomInset, compact ? rightInset : 0], initial: true) {
                controller.rig.insets = EdgeInsetsF(
                    top: Float(topInset), left: 0, bottom: Float(max(bottomInset, 0)), right: Float(compact ? rightInset : 0)
                )
            }
        }
        .background(MorfoColors.background)
        .toolbar(.hidden, for: .navigationBar)
        .onAppear {
            controller.lightingMode = app.lightingMode
            applyLabelSpacing()
        }
        .onDisappear { app.compareClosed() }
        .onChange(of: app.labelMode) { applyLabelSpacing() }
        .onChange(of: app.lightingMode) { controller.lightingMode = app.lightingMode }
        .sheet(isPresented: $showSettings) { ViewSettingsSheet(inWorkspace: true) }
        .sheet(item: Binding(get: { infoKey.flatMap(workspace.find) }, set: { infoKey = $0?.key })) { tooth in
            ToothInfoSheet(tooth: tooth)
        }
    }

    // MARK: Top island

    private var topBar: some View {
        let s = app.s
        return HStack(spacing: 10) {
            CircleButton(systemImage: "chevron.left", label: s.back) { dismiss() }
            Island(cornerRadius: 22, padding: 4) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 2) {
                        ForEach(workspace.teeth) { t in toothTab(t) }
                    }
                }
                .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
            }
            .fixedSize(horizontal: false, vertical: true)
            Menu {
                Toggle(isOn: Binding(get: { workspace.syncRotation }, set: { workspace.syncRotation = $0 })) {
                    Label(s.syncAllTeethTitle, systemImage: "arrow.triangle.2.circlepath")
                }
                Button { workspace.autoArrange() } label: {
                    Label(s.autoLayout, systemImage: workspace.autoLayout ? "checkmark" : "rectangle.3.group")
                }
                Divider()
                Button { showSettings = true } label: { Label(s.viewSettings, systemImage: "slider.horizontal.3") }
            } label: {
                Image(systemName: "ellipsis")
                    .font(.system(size: 16, weight: .semibold))
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(.glass)
            .buttonBorderShape(.circle)
            .accessibilityLabel(s.viewSettings)
        }
        .padding(.horizontal, 12)
        .padding(.top, 4)
    }

    private func toothTab(_ t: PlacedTooth) -> some View {
        let selected = t.key == workspace.selectedKey
        return Button {
            if !t.visible { workspace.setVisible(t.key, true) }
            workspace.toggleSelect(t.key)
        } label: {
            Text("\(t.tooth.fdi)" + (t.variant == .carabelli ? "C" : ""))
                .font(.fdi(15))
                .strikethrough(!t.visible)
                .foregroundStyle(selected ? MorfoColors.onAccent : t.visible ? MorfoColors.text : MorfoColors.textFaint)
                .padding(.horizontal, 12)
                .frame(height: 32)
                .background(selected ? MorfoColors.accent : .clear, in: .capsule)
                .contentShape(.capsule)
        }
        .buttonStyle(.plain)
        .animation(.snappy(duration: 0.2), value: selected)
    }

    private func applyLabelSpacing() {
        workspace.labelSpaceMm = switch app.labelMode {
        case .none: 0
        case .fdi: 4
        default: 7
        }
        workspace.labelGapMm = app.labelMode == .fdiEn || app.labelMode == .fdiPl ? 4 : 0
        workspace.requestFraming()
    }

    /// Maps raw gestures to workspace and camera operations.
    private var gestures: ViewportGestureHandler {
        let rig = controller.rig
        return ViewportGestureHandler(
            onTap: { p in
                if let key = controller.pick(p) { workspace.toggleSelect(key) } else { workspace.select(nil) }
            },
            onDoubleTap: { p in
                if let key = controller.pick(p), let bounds = workspace.find(key)?.worldBounds {
                    workspace.select(key)
                    rig.focus(on: bounds)
                } else {
                    workspace.requestFraming()
                }
            },
            onRotateDrag: { d in workspace.rotate(by: dragRotation(d, rig: rig)) },
            onMoveStart: { p in
                movingKey = controller.pick(p)
                if let movingKey { workspace.select(movingKey) }
            },
            onMoveDrag: { d in
                guard let key = movingKey, let t = workspace.find(key) else { return }
                workspace.move(key, by: rig.screenDeltaToWorld(dx: Float(d.width), dy: Float(d.height), planeZ: t.position.z))
            },
            onMoveEnd: { movingKey = nil },
            onTransform: { pan, zoom, twist, centroid in
                applyCameraTransform(rig: rig, workspace: workspace, pan: pan, zoom: zoom, twist: twist, centroid: centroid)
            }
        )
    }
}

/// One-finger drag → rotation increment (same sensitivity as on Android).
func dragRotation(_ d: CGSize, rig: CameraRig) -> simd_quatf {
    let k = Float.pi / rig.width * 1.1
    return simd_quatf(angle: Float(d.width) * k, axis: [0, 1, 0]) * simd_quatf(angle: Float(d.height) * k, axis: [1, 0, 0])
}

func applyCameraTransform(rig: CameraRig, workspace: Workspace, pan: CGSize, zoom: CGFloat, twist: CGFloat, centroid: CGPoint) {
    if pan != .zero { rig.pan(dx: Float(pan.width), dy: Float(pan.height)) }
    if zoom != 1 { rig.zoom(Float(zoom), pivot: centroid) }
    if twist != 0 { workspace.rotate(by: simd_quatf(angle: -Float(twist), axis: [0, 0, 1])) }
}

// MARK: - Bottom islands

/// The selected tooth: its name and every action on it, in one island.
private struct SelectedToothIsland: View {
    @Environment(AppModel.self) private var app
    let tooth: PlacedTooth
    let onInfo: () -> Void

    var body: some View {
        let ws = app.workspace
        let s = app.s
        Island(cornerRadius: 26, padding: 6) {
            VStack(spacing: 4) {
                HStack(spacing: 10) {
                    Text("\(tooth.tooth.fdi)").font(.fdi(20, weight: .bold)).foregroundStyle(MorfoColors.accent)
                    VStack(alignment: .leading, spacing: 0) {
                        Text(tooth.tooth.name(app.language)).font(.subheadline.weight(.medium)).lineLimit(1)
                        if tooth.variant == .carabelli {
                            Text(s.carabelliCusp).font(.caption).foregroundStyle(MorfoColors.textMuted)
                        }
                    }
                    Spacer(minLength: 0)
                    Button { ws.select(nil) } label: {
                        Image(systemName: "xmark").font(.system(size: 12, weight: .bold))
                            .foregroundStyle(MorfoColors.textMuted)
                            .frame(width: 28, height: 28)
                            .background(Color.white.opacity(0.08), in: .circle)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(s.close)
                }
                .padding(.horizontal, 10)
                .padding(.top, 6)

                HStack(spacing: 0) {
                    IslandIconButton(systemImage: "info.circle", label: s.info, action: onInfo)
                    IslandIconButton(systemImage: "arrow.counterclockwise", label: s.resetRotation) { ws.resetOrientation(tooth.key) }
                    IslandIconButton(systemImage: "arrow.up.and.down.and.arrow.left.and.right", label: s.resetPosition) {
                        ws.resetPosition(tooth.key)
                    }
                    IslandIconButton(systemImage: "eye.slash", label: s.hide) { ws.setVisible(tooth.key, false) }
                    IslandIconButton(systemImage: "plus.square.on.square", label: s.duplicate) { ws.duplicate(tooth.key) }
                    if tooth.tooth.variants.count > 1 {
                        IslandIconButton(systemImage: "c.circle", label: s.carabelliCusp, active: tooth.variant == .carabelli) {
                            ws.setVariant(tooth.key, tooth.variant == .standard ? .carabelli : .standard)
                        }
                    }
                    IslandIconButton(systemImage: "trash", label: s.delete, role: .destructive) { ws.remove(tooth.key) }
                }
            }
        }
    }
}

/// The five surface views as one segmented island; the active view stays highlighted until the teeth are turned.
struct PresetIsland: View {
    @Environment(AppModel.self) private var app
    let workspace: Workspace
    var vertical = false

    var body: some View {
        let teeth = workspace.rotationTargets().map(\.tooth)
        let pl = app.language == .pl
        Island(cornerRadius: vertical ? 22 : 26, padding: 4) {
            if vertical {
                VStack(spacing: 2) {
                    ForEach(SurfacePreset.allCases) { preset in
                        IslandSegments(
                            options: [preset], selection: workspace.currentPreset,
                            title: { $0.label(teeth, app.language) },
                            subtitle: pl ? { $0.labelEn(teeth) } : nil,
                            onSelect: workspace.apply
                        )
                    }
                }
            } else {
                IslandSegments(
                    options: SurfacePreset.allCases, selection: workspace.currentPreset,
                    title: { $0.label(teeth, app.language) },
                    subtitle: pl ? { $0.labelEn(teeth) } : nil,
                    onSelect: workspace.apply
                )
            }
        }
        .disabled(teeth.isEmpty)
    }
}
