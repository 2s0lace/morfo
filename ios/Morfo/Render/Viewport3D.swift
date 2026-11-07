import RealityKit
import SwiftUI

/// RealityView for a workspace, with gesture layer and tooth labels on top.
struct Viewport3D: View {
    let controller: ViewportController
    var labelMode: LabelMode
    var gestures: ViewportGestureHandler

    var body: some View {
        RealityView { content in
            controller.makeScene(&content)
        }
        .overlay { ViewportGestureLayer(handler: gestures) }
        .overlay(alignment: .topLeading) {
            ToothLabels(controller: controller, mode: labelMode).allowsHitTesting(false)
        }
        .onGeometryChange(for: CGSize.self, of: \.size) { controller.resize($0) }
    }
}

/// Small labels centred under each tooth; positions come from the controller every frame.
private struct ToothLabels: View {
    let controller: ViewportController
    let mode: LabelMode

    var body: some View {
        let workspace = controller.workspace
        ZStack(alignment: .topLeading) {
            Color.clear
            ForEach(mode == .none ? [] : workspace.teeth.filter(\.visible)) { t in
                if let anchor = controller.labelAnchors[t.key] {
                    let selected = t.key == workspace.selectedKey
                    VStack(spacing: 0) {
                        Text("\(t.tooth.fdi)" + (t.variant == .carabelli ? " C" : ""))
                            .font(.fdi(13, weight: .bold))
                            .foregroundStyle(selected ? MorfoColors.accent : MorfoColors.text)
                        if let name = mode == .fdiEn ? t.tooth.nameEn : mode == .fdiPl ? t.tooth.namePl : nil {
                            Text(name)
                                .font(.system(size: 10, weight: .medium))
                                .lineSpacing(-1)
                                .lineLimit(2)
                                .multilineTextAlignment(.center)
                                .foregroundStyle(selected ? MorfoColors.accent.opacity(0.8) : MorfoColors.textMuted)
                        }
                    }
                    .shadow(color: .black.opacity(0.6), radius: 3)
                    .frame(width: 130)
                    .fixedSize(horizontal: false, vertical: true)
                    // Offsets do not take part in layout, so labels never resize the viewport.
                    .offset(x: anchor.x - 65, y: anchor.y + 4)
                }
            }
        }
    }
}
