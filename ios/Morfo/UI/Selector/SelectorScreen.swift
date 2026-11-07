import SwiftUI

/// Filter state shared by the chart (dimming) and the filter chips.
private struct ToothFilters {
    var classes: Set<ToothClass> = []
    var arches: Set<Arch> = []
    var sides: Set<Side> = []

    var active: Bool { !classes.isEmpty || !arches.isEmpty || !sides.isEmpty }

    func matches(_ t: Tooth) -> Bool {
        (classes.isEmpty || classes.contains(t.toothClass)) && (arches.isEmpty || arches.contains(t.arch))
            && (sides.isEmpty || sides.contains(t.side))
    }
}

struct SelectorScreen: View {
    @Environment(AppModel.self) private var app
    @State private var filters = ToothFilters()
    @State private var showSettings = false

    var body: some View {
        GeometryReader { geo in
            // Landscape (phone or iPad): chart on the left, filters and shortcuts on the right.
            if geo.size.width > geo.size.height && geo.size.width >= 600 {
                HStack(alignment: .top, spacing: 32) {
                    chart.frame(maxWidth: min(geo.size.height * 0.92, 560)).frame(maxWidth: .infinity, maxHeight: .infinity)
                    ScrollView {
                        VStack(alignment: .leading, spacing: 0) {
                            filtersSection
                            quickSection
                        }
                        .padding(.bottom, 24)
                    }
                    .frame(maxWidth: .infinity)
                }
                .padding(.horizontal, 24)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        Text(app.s.selectTeethSubtitle).font(.subheadline).foregroundStyle(MorfoColors.textMuted)
                        chart.padding(.top, 4)
                        filtersSection
                        quickSection
                    }
                    .frame(maxWidth: 640)
                    .padding(.horizontal, 20)
                    .padding(.bottom, 24)
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .background(MorfoColors.background)
        .navigationTitle(app.s.appName)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(app.s.quiz) { app.openQuizHome() }
            }
            ToolbarSpacer(.fixed, placement: .topBarTrailing)
            ToolbarItemGroup(placement: .topBarTrailing) {
                Button(app.s.settings, systemImage: "gearshape") { showSettings = true }
                Button(app.s.about, systemImage: "info.circle") { app.openAbout() }
            }
        }
        .safeAreaInset(edge: .bottom) { bottomBar }
        .sheet(isPresented: $showSettings) { ViewSettingsSheet() }
    }

    private var chart: some View {
        ArchChart(
            isSelected: { app.isSelected($0.fdi) },
            isDimmed: { filters.active && !filters.matches($0) },
            onToggle: { app.toggle($0.fdi) }
        )
    }

    private var filtersSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionHeader(app.s.filters)
            FlowLayout(spacing: 8) {
                ForEach(ToothClass.allCases) { c in
                    Chip(text: c.label(app.language), active: filters.classes.contains(c)) { filters.classes.toggle(c) }
                }
                ForEach(Arch.allCases) { a in
                    Chip(text: a.label(app.language), active: filters.arches.contains(a)) { filters.arches.toggle(a) }
                }
                ForEach(Side.allCases) { side in
                    Chip(text: side.label(app.language), active: filters.sides.contains(side)) { filters.sides.toggle(side) }
                }
            }
            if filters.active {
                HStack(spacing: 20) {
                    Button(app.s.selectMatching, systemImage: "checkmark.circle") {
                        app.addAll(Dentition.permanent.filter(filters.matches).map(\.fdi))
                    }
                    Button(app.s.clearFilters, systemImage: "xmark.circle") { filters = ToothFilters() }
                }
                .font(.subheadline.weight(.medium))
                .tint(MorfoColors.accent)
                .transition(.opacity)
            }
        }
        .animation(.easeOut(duration: 0.15), value: filters.active)
    }

    private var quickSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionHeader(app.s.quickComparisons)
            VStack(spacing: 0) {
                ForEach(Array(QuickComparisons.all.enumerated()), id: \.element.id) { i, q in
                    if i > 0 { Rectangle().fill(MorfoColors.separator).frame(height: 1).padding(.leading, 16) }
                    Button { app.openQuick(q) } label: {
                        HStack(spacing: 12) {
                            Text(q.title(app.language)).font(.subheadline).multilineTextAlignment(.leading)
                            Spacer(minLength: 8)
                            Text(q.teeth.map { "\($0.fdi)" + ($0.variant == .carabelli ? "C" : "") }.joined(separator: " "))
                                .font(.fdi(13, weight: .medium))
                                .foregroundStyle(MorfoColors.textMuted)
                            Image(systemName: "chevron.right").font(.caption.weight(.semibold)).foregroundStyle(MorfoColors.textFaint)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 13)
                        .contentShape(.rect)
                    }
                    .buttonStyle(PressHighlightStyle(cornerRadius: 0))
                }
            }
            .background(MorfoColors.surface, in: .rect(cornerRadius: 22))
            .clipShape(.rect(cornerRadius: 22))
        }
    }

    private func sectionHeader(_ text: String) -> some View {
        Text(text).font(.footnote.weight(.semibold)).foregroundStyle(MorfoColors.textMuted)
            .textCase(.uppercase)
            .padding(.top, 26)
            .padding(.leading, 4)
    }

    private var bottomBar: some View {
        let count = app.selection.count
        return Island(cornerRadius: 30, padding: 8) {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 1) {
                    if count == 0 {
                        Text(app.s.noneSelected).font(.subheadline).foregroundStyle(MorfoColors.textMuted)
                    } else {
                        Text(app.selection.map(\.fdi).sorted().map(String.init).joined(separator: " "))
                            .font(.fdi(15)).lineLimit(1)
                        Button(app.s.clear) { app.clearSelection() }
                            .font(.caption).foregroundStyle(MorfoColors.textMuted)
                    }
                }
                .padding(.leading, 12)
                .frame(maxWidth: .infinity, alignment: .leading)
                Button { app.openCompare() } label: {
                    Label(count > 0 ? app.s.compareCount(count) : app.s.compare, systemImage: "cube.transparent")
                        .font(.body.weight(.semibold))
                        .padding(.horizontal, 6)
                        .frame(minHeight: 36)
                }
                .buttonStyle(.glassProminent)
                .tint(count > 0 ? MorfoColors.accent : MorfoColors.surfaceRaised)
                .foregroundStyle(count > 0 ? MorfoColors.onAccent : MorfoColors.textFaint)
                .disabled(count == 0)
            }
        }
        .frame(maxWidth: 560)
        .padding(.horizontal, 12)
        .padding(.bottom, 4)
        .animation(.snappy(duration: 0.2), value: count)
    }
}

extension Set {
    mutating func toggle(_ item: Element) {
        if contains(item) { remove(item) } else { insert(item) }
    }
}
