import SwiftUI

/// Permanent dentition chart in the clinician's view: patient's right on the left,
/// maxillary arch on top (incisors up), mandibular arch below (incisors down).
struct ArchChart: View {
    @Environment(AppModel.self) private var app
    var isSelected: (Tooth) -> Bool
    var isDimmed: (Tooth) -> Bool
    var onToggle: (Tooth) -> Void
    /// Optional fill colour per tooth (overrides the selection colours), e.g. accuracy in quiz statistics.
    var colorFor: ((Tooth) -> Color?)?

    private static let upper = positions(Dentition.upperRow)
    private static let lower = positions(Dentition.lowerRow)

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            // Chips are sized for a ~340 pt wide chart and scale with it (small phone, big iPad).
            let chipScale = min(max(w / 340, 0.6), 1.3)
            let rx = w * 0.44
            let ry = h * 0.39
            ZStack {
                caption(app.s.maxillaryArch).position(x: w / 2, y: h / 2 - 58)
                caption(app.s.mandibularArch).position(x: w / 2, y: h / 2 + 58)
                caption(app.s.rightSideShort).position(x: 10, y: h / 2)
                caption(app.s.leftSideShort).position(x: w - 10, y: h / 2)
                ForEach(Self.upper, id: \.0) { tooth, angle in
                    chip(tooth, chipScale).position(x: w / 2 + rx * cos(angle), y: h * 0.47 - ry * sin(angle))
                }
                ForEach(Self.lower, id: \.0) { tooth, angle in
                    chip(tooth, chipScale).position(x: w / 2 + rx * cos(angle), y: h * 0.53 + ry * sin(angle))
                }
            }
        }
        .aspectRatio(1.04, contentMode: .fit)
    }

    private func caption(_ text: String) -> some View {
        Text(text).font(.caption2.weight(.semibold)).tracking(0.8).foregroundStyle(MorfoColors.textFaint)
    }

    private func chip(_ tooth: Tooth, _ scale: CGFloat) -> some View {
        let size = Self.chipSize(tooth) * scale
        let selected = isSelected(tooth)
        let custom = colorFor?(tooth)
        let fill = custom ?? (selected ? MorfoColors.accent : MorfoColors.surface)
        return Button { onToggle(tooth) } label: {
            Text("\(tooth.fdi)")
                .font(.fdi(11 * scale))
                .foregroundStyle(selected || custom != nil ? MorfoColors.onAccent : MorfoColors.text)
                .frame(width: size, height: size)
                .background(fill, in: .rect(cornerRadius: size * 0.38))
                .overlay(RoundedRectangle(cornerRadius: size * 0.38)
                    .stroke(custom ?? (selected ? MorfoColors.accent : MorfoColors.separator), lineWidth: 1))
                .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .opacity(isDimmed(tooth) && !selected ? 0.28 : 1)
        .animation(.easeOut(duration: 0.15), value: selected)
        .accessibilityLabel("\(tooth.fdi), \(tooth.name(app.language))")
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    private static func chipSize(_ tooth: Tooth) -> CGFloat {
        switch tooth.toothClass {
        case .molar: 31
        case .premolar: 27
        case .canine: 26
        case .incisor: 25
        }
    }

    /// Spreads the 16 teeth of a row along a half-ellipse, spacing proportional to tooth width.
    private static func positions(_ row: [Tooth]) -> [(Tooth, CGFloat)] {
        let widths = row.map(chipSize)
        let total = widths.reduce(0, +)
        var acc: CGFloat = 0
        return row.enumerated().map { i, tooth in
            let mid = acc + widths[i] / 2
            acc += widths[i]
            // From the left end of the arch (angle π) over the front (π/2) to the right end (0).
            return (tooth, .pi * (1 - (0.02 + 0.96 * mid / total)))
        }
    }
}
