import SwiftUI

/// Language, lighting, labels, rotation, layout and scale, as a standard iOS settings list.
struct ViewSettingsSheet: View {
    @Environment(AppModel.self) private var app
    @Environment(\.dismiss) private var dismiss
    var inWorkspace = false

    var body: some View {
        let ws = app.workspace
        let s = app.s
        let lang = app.language
        NavigationStack {
            List {
                Section(s.language) {
                    Picker(s.language, selection: Binding(get: { app.language }, set: app.updateLanguage)) {
                        ForEach(AppLanguage.allCases) { Text($0.label).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    .morfoRow()
                }

                Section {
                    Picker(s.lighting, selection: Binding(get: { app.lightingMode }, set: app.updateLightingMode)) {
                        ForEach(LightingMode.allCases) { Text($0.label(lang)).tag($0) }
                    }
                    .morfoRow()
                    Picker(s.labels, selection: Binding(get: { app.labelMode }, set: app.updateLabelMode)) {
                        ForEach(LabelMode.allCases.filter { lang == .pl || $0 != .fdiPl }) { Text($0.label(lang)).tag($0) }
                    }
                    .morfoRow()
                } header: {
                    Text(s.lighting)
                } footer: {
                    Text(app.lightingMode.description(lang))
                }

                Section(s.rotation) {
                    Toggle(isOn: Binding(get: { ws.syncRotation }, set: { ws.syncRotation = $0 })) {
                        Text(s.syncAllTeethTitle)
                        Text(s.syncAllTeethSubtitle)
                    }
                    .morfoRow()
                    Toggle(isOn: Binding(get: { ws.mirrorSync }, set: app.updateMirrorSync)) {
                        Text(s.mirrorContralateralTitle)
                        Text(s.mirrorContralateralSubtitle)
                    }
                    .morfoRow()
                }

                Section(s.layout) {
                    Picker(s.layout, selection: Binding(get: { ws.arrangement }, set: { ws.arrangement = $0; ws.autoArrange() })) {
                        ForEach(Arrangement.allCases.filter { $0 != .triangle || ws.visibleTeeth.count == 3 }) {
                            Text($0.label(lang)).tag($0)
                        }
                    }
                    .morfoRow()
                    VStack(alignment: .leading, spacing: 6) {
                        LabeledContent(s.spacing) {
                            Text("\(Int(ws.spacingMm.rounded())) mm").monospacedDigit()
                        }
                        Slider(value: Binding(get: { Double(ws.spacingMm) }, set: { app.updateSpacing(Float($0.rounded())) }), in: 1...20)
                    }
                    .morfoRow()
                }

                Section {
                    Picker(s.scale, selection: Binding(get: { ws.scaleMode }, set: app.updateScaleMode)) {
                        ForEach(ScaleMode.allCases) { Text($0.label(lang)).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    .morfoRow()
                } header: {
                    Text(s.scale)
                } footer: {
                    Text(ws.scaleMode.description(lang))
                }

                Section(s.controls) {
                    control("hand.point.up.left", s.controlRotate)
                    control("arrow.up.left.and.down.right.magnifyingglass", s.controlPanZoom)
                    control("hand.tap", s.controlDragTooth)
                    control("hand.tap.fill", s.controlTap)
                }
            }
            .morfoList()
            .tint(MorfoColors.accent)
            .navigationTitle(s.viewSettings)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(s.close, systemImage: "checkmark") { dismiss() }
                }
            }
        }
        .presentationDetents(inWorkspace ? [.medium, .large] : [.large])
        .presentationBackgroundInteraction(.enabled(upThrough: .medium))
    }

    private func control(_ symbol: String, _ text: String) -> some View {
        Label {
            Text(text).font(.subheadline).foregroundStyle(MorfoColors.textMuted)
        } icon: {
            Image(systemName: symbol).foregroundStyle(MorfoColors.accent)
        }
        .morfoRow()
    }
}

/// Facts and key morphological features of one placed tooth.
struct ToothInfoSheet: View {
    @Environment(AppModel.self) private var app
    @Environment(\.dismiss) private var dismiss
    let tooth: PlacedTooth

    var body: some View {
        let t = tooth.tooth
        let s = app.s
        let lang = app.language
        NavigationStack {
            List {
                Section {
                    HStack(alignment: .center, spacing: 14) {
                        Text("\(t.fdi)").font(.fdi(34, weight: .bold)).foregroundStyle(MorfoColors.accent)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(t.name(lang)).font(.headline)
                            if lang == .pl { Text(t.nameEn).font(.subheadline).foregroundStyle(MorfoColors.textMuted) }
                        }
                    }
                    .padding(.vertical, 4)
                    .morfoRow()
                    LabeledContent(s.classLabel, value: t.toothClass.label(lang)).morfoRow()
                    LabeledContent(s.archLabel, value: t.arch.label(lang)).morfoRow()
                    LabeledContent(s.sideLabel, value: t.side.label(lang)).morfoRow()
                    LabeledContent(s.avgLengthLabel) {
                        Text("\(t.averageLengthMm.formatted()) mm").monospacedDigit()
                    }
                    .morfoRow()
                    LabeledContent(s.modelLabel, value: t.isMirrored ? s.mirrorScanOf(t.sourceFdi) : s.scanOf(t.fdi)).morfoRow()
                }

                if t.variants.count > 1 {
                    Section {
                        Toggle(isOn: Binding(get: { tooth.variant == .carabelli },
                                             set: { app.workspace.setVariant(tooth.key, $0 ? .carabelli : .standard) })) {
                            Text(s.carabelliVariantTitle)
                            Text(s.carabelliVariantSubtitle)
                        }
                        .morfoRow()
                    }
                }

                Section {
                    ForEach(Array(Morphology.keyFeatures(t, language: lang).enumerated()), id: \.offset) { i, feature in
                        HStack(alignment: .firstTextBaseline, spacing: 12) {
                            Text("\(i + 1)").font(.fdi(13)).foregroundStyle(MorfoColors.accent).frame(width: 16)
                            Text(feature).font(.subheadline)
                        }
                        .morfoRow()
                    }
                } header: {
                    Text(s.morphologyTitle)
                } footer: {
                    Text(s.textbookSummaryFootnote)
                }
            }
            .morfoList()
            .tint(MorfoColors.accent)
            .navigationTitle(s.info)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(s.close, systemImage: "checkmark") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationBackgroundInteraction(.enabled(upThrough: .medium))
    }
}
