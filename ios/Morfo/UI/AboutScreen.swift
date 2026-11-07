import SwiftUI

/// Credits required by the CC-BY-4.0 licence of the models, plus notes on how they were prepared.
struct AboutScreen: View {
    @Environment(AppModel.self) private var app

    var body: some View {
        let s = app.s
        List {
            Section(s.modelsSection) {
                Text(s.modelsBody1).morfoRow()
                Text(s.modelsBody2).foregroundStyle(MorfoColors.textMuted).morfoRow()
                Link(destination: URL(string: "https://sketchfab.com/DundeeDental")!) {
                    Label("sketchfab.com/DundeeDental", systemImage: "arrow.up.right.square")
                }
                .morfoRow()
            }
            Section(s.prepSection) {
                ForEach(Array([s.prep1, s.prep2, s.prep3, s.prep4, s.prep5, s.prep6].enumerated()), id: \.offset) { i, text in
                    HStack(alignment: .firstTextBaseline, spacing: 12) {
                        Text("\(i + 1)").font(.fdi(13)).foregroundStyle(MorfoColors.accent).frame(width: 16)
                        Text(text)
                    }
                    .morfoRow()
                }
            }
            Section(s.engineSection) {
                Text(s.engineBody).morfoRow()
                Link(destination: URL(string: "https://github.com/2s0lace/morfo")!) {
                    Label("github.com/2s0lace/morfo", systemImage: "arrow.up.right.square")
                }
                .morfoRow()
            }
        }
        .font(.subheadline)
        .tint(MorfoColors.accent)
        .morfoList()
        .navigationTitle(s.aboutTitle)
        .navigationBarTitleDisplayMode(.inline)
    }
}
