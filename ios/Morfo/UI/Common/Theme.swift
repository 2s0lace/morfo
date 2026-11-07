import SwiftUI

// Design rules:
// - Content (charts, lists, 3D) sits on solid graphite surfaces.
// - Controls float above it in a few Liquid Glass islands; nothing else uses glass.
// - FDI numbers are set in SF Mono, everything else in SF Pro with tabular figures.

/// Minimal clinical palette: graphite neutrals, one enamel-ivory accent (same as the Android app).
enum MorfoColors {
    static let background = Color(hex: 0x0E0F11)
    static let surface = Color(hex: 0x17181B)
    static let surfaceRaised = Color(hex: 0x202125)
    static let separator = Color.white.opacity(0.08)
    static let text = Color(hex: 0xECEAE6)
    static let textMuted = Color(hex: 0x8F8D89)
    static let textFaint = Color(hex: 0x5E5D5A)
    static let accent = Color(hex: 0xE9DFC8)
    static let onAccent = Color(hex: 0x16140F)

    static let good = Color(hex: 0x7FB38A)
    static let partial = Color(hex: 0xD9B26A)
    static let bad = Color(hex: 0xD77A6E)

    /// Colour for an accuracy value: none / weak / medium / good.
    static func accuracy(_ value: Double?) -> Color? {
        guard let value else { return nil }
        return value >= 0.8 ? good : value >= 0.5 ? partial : bad
    }
}

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

extension Font {
    /// FDI numbers and other figures that should read as data.
    static func fdi(_ size: CGFloat, weight: Font.Weight = .semibold) -> Font {
        .system(size: size, weight: weight, design: .monospaced)
    }
}

/// Content width limit so screens stay readable on an iPad.
let maxContentWidth: CGFloat = 640

// MARK: - Floating islands

/// A floating Liquid Glass panel. The only container that uses glass.
struct Island<Content: View>: View {
    var cornerRadius: CGFloat = 26
    var padding: CGFloat = 6
    @ViewBuilder var content: Content

    var body: some View {
        content
            .padding(padding)
            .glassEffect(.regular, in: .rect(cornerRadius: cornerRadius))
    }
}

/// Round glass button for the corners of a floating layout (back, settings…).
struct CircleButton: View {
    let systemImage: String
    let label: String
    var active = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: 16, weight: .semibold))
                .frame(width: 40, height: 40)
        }
        .buttonStyle(.glass)
        .buttonBorderShape(.circle)
        .tint(active ? MorfoColors.accent : nil)
        .foregroundStyle(active ? MorfoColors.accent : MorfoColors.text)
        .accessibilityLabel(label)
    }
}

/// Plain icon button used inside an island.
struct IslandIconButton: View {
    let systemImage: String
    let label: String
    var active = false
    var role: ButtonRole?
    let action: () -> Void

    var body: some View {
        Button(role: role, action: action) {
            Image(systemName: systemImage)
                .font(.system(size: 17, weight: .medium))
                .frame(maxWidth: .infinity, minHeight: 40)
                .contentShape(.rect)
        }
        .buttonStyle(PressHighlightStyle())
        .foregroundStyle(role == .destructive ? MorfoColors.bad : active ? MorfoColors.accent : MorfoColors.text)
        .accessibilityLabel(label)
    }
}

/// Subtle rounded highlight while pressed; no resting background.
struct PressHighlightStyle: ButtonStyle {
    var cornerRadius: CGFloat = 14

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(Color.white.opacity(configuration.isPressed ? 0.12 : 0), in: .rect(cornerRadius: cornerRadius))
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

/// Segmented control drawn inside an island: one shared capsule, a sliding highlight on the active item.
/// With `selection == nil` it acts as a row of actions (e.g. surface views after free rotation).
struct IslandSegments<Option: Hashable>: View {
    let options: [Option]
    let selection: Option?
    let title: (Option) -> String
    var subtitle: ((Option) -> String?)? = nil
    let onSelect: (Option) -> Void
    @Namespace private var highlight

    var body: some View {
        HStack(spacing: 2) {
            ForEach(options, id: \.self) { option in
                let selected = option == selection
                Button { onSelect(option) } label: {
                    VStack(spacing: 1) {
                        Text(title(option))
                            .font(.system(size: 13, weight: selected ? .semibold : .medium))
                        if let sub = subtitle?(option) {
                            Text(sub)
                                .font(.system(size: 8, weight: .semibold))
                                .tracking(0.6)
                                .foregroundStyle(selected ? MorfoColors.accent.opacity(0.7) : MorfoColors.textFaint)
                        }
                    }
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
                    .foregroundStyle(selected ? MorfoColors.accent : MorfoColors.text)
                    .frame(maxWidth: .infinity, minHeight: 40)
                    .padding(.horizontal, 2)
                    .background {
                        if selected {
                            Capsule().fill(MorfoColors.accent.opacity(0.14))
                                .matchedGeometryEffect(id: "highlight", in: highlight)
                        }
                    }
                    .contentShape(.capsule)
                }
                .buttonStyle(PressHighlightStyle(cornerRadius: 20))
            }
        }
        .animation(.snappy(duration: 0.25), value: selection)
    }
}

// MARK: - Content layer

/// Filter / option chip on the content layer: solid fill, accent when on.
struct Chip: View {
    let text: String
    var active = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.subheadline.weight(.medium))
                .lineLimit(1)
                .padding(.horizontal, 14)
                .frame(height: 32)
                .foregroundStyle(active ? MorfoColors.onAccent : MorfoColors.text)
                .background(active ? MorfoColors.accent : MorfoColors.surfaceRaised, in: .capsule)
        }
        .buttonStyle(.plain)
        .animation(.easeOut(duration: 0.15), value: active)
    }
}

/// Wrapping row of views (filters, chips).
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let rows = arrange(width: proposal.width ?? .infinity, subviews: subviews)
        let height = rows.map(\.height).reduce(0, +) + spacing * CGFloat(max(rows.count - 1, 0))
        let width = rows.map(\.width).max() ?? 0
        return CGSize(width: proposal.width ?? width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var y = bounds.minY
        for row in arrange(width: bounds.width, subviews: subviews) {
            var x = bounds.minX
            for i in row.indices {
                let size = subviews[i].sizeThatFits(.unspecified)
                subviews[i].place(at: CGPoint(x: x, y: y + (row.height - size.height) / 2), proposal: .unspecified)
                x += size.width + spacing
            }
            y += row.height + spacing
        }
    }

    private struct Row {
        var indices: [Int] = []
        var width: CGFloat = 0
        var height: CGFloat = 0
    }

    private func arrange(width: CGFloat, subviews: Subviews) -> [Row] {
        var rows: [Row] = [Row()]
        for i in subviews.indices {
            let size = subviews[i].sizeThatFits(.unspecified)
            if !rows[rows.count - 1].indices.isEmpty && rows[rows.count - 1].width + spacing + size.width > width {
                rows.append(Row())
            }
            var row = rows[rows.count - 1]
            row.width += (row.indices.isEmpty ? 0 : spacing) + size.width
            row.height = max(row.height, size.height)
            row.indices.append(i)
            rows[rows.count - 1] = row
        }
        return rows
    }
}

/// Numbers side by side with hairline dividers, e.g. "12 | 85% | 4/32".
struct MetricStrip: View {
    let metrics: [(label: String, value: String)]

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Array(metrics.enumerated()), id: \.offset) { i, m in
                if i > 0 { Rectangle().fill(MorfoColors.separator).frame(width: 1, height: 32) }
                VStack(spacing: 2) {
                    Text(m.value).font(.title3.weight(.semibold).monospacedDigit())
                    Text(m.label).font(.caption).foregroundStyle(MorfoColors.textMuted)
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.vertical, 4)
    }
}

/// Grouped list styling shared by every list screen.
extension View {
    func morfoList() -> some View {
        scrollContentBackground(.hidden)
            .background(MorfoColors.background)
            .listSectionSpacing(20)
    }

    func morfoRow() -> some View {
        listRowBackground(MorfoColors.surface)
    }
}
