import SwiftUI

struct QuizStatsScreen: View {
    @Environment(AppModel.self) private var app
    @State private var confirmReset = false
    @State private var tapped: Tooth?

    var body: some View {
        let s = app.s
        let p = app.quizProgress
        Group {
            if p.totalAttempts == 0 {
                ContentUnavailableView(s.stats, systemImage: "chart.bar.xaxis", description: Text(s.emptyStats))
            } else {
                List {
                    Section {
                        let mastered = Flashcards(progress: p).summary(scope: Dentition.permanent.map(\.fdi)).mastered
                        MetricStrip(metrics: [
                            (s.answersLabel, "\(p.totalAttempts)"),
                            (s.correctLabel, "\(p.totalCorrect * 100 / max(p.totalAttempts, 1))%"),
                            (s.masteredLabel, "\(mastered)/32"),
                        ])
                        .morfoRow()
                    }

                    Section {
                        VStack(spacing: 10) {
                            ArchChart(
                                isSelected: { $0 == tapped },
                                isDimmed: { p.statsFor($0.fdi).attempts == 0 },
                                onToggle: { tapped = $0 == tapped ? nil : $0 },
                                colorFor: { MorfoColors.accuracy(p.stats[$0.fdi]?.accuracy) }
                            )
                            .frame(maxWidth: 420)
                            legend
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .morfoRow()
                        if let t = tapped {
                            let st = p.statsFor(t.fdi)
                            LabeledContent {
                                Text("\(st.correct)/\(st.attempts)").monospacedDigit()
                            } label: {
                                HStack(spacing: 10) {
                                    Text("\(t.fdi)").font(.fdi(17, weight: .bold)).foregroundStyle(MorfoColors.accent)
                                    Text(t.name(app.language)).font(.subheadline)
                                }
                            }
                            .morfoRow()
                        }
                    } header: {
                        Text(s.accuracyOnTeeth)
                    }

                    Section(s.errorTypes) {
                        let wrong = max(p.totalAttempts - p.totalCorrect, 1)
                        errorRow(s.wrongArch, p.archErrors, wrong)
                        errorRow(s.wrongSide, p.sideErrors, wrong)
                        errorRow(s.wrongTooth, p.kindErrors, wrong)
                    }

                    Section(s.mostFrequentConfusions) {
                        let top = p.topConfusions(6)
                        if top.isEmpty { Text(s.noConfusions).foregroundStyle(MorfoColors.textMuted).morfoRow() }
                        ForEach(top, id: \.0) { pair, n in
                            HStack(spacing: 12) {
                                HStack(spacing: 6) {
                                    Text("\(pair.expected)").foregroundStyle(MorfoColors.accent)
                                    Image(systemName: "arrow.right").font(.caption).foregroundStyle(MorfoColors.textFaint)
                                    Text("\(pair.answered)")
                                }
                                .font(.fdi(16))
                                Text(Tooth(pair.expected).name(app.language))
                                    .font(.caption).foregroundStyle(MorfoColors.textMuted).lineLimit(2)
                                Spacer(minLength: 0)
                                Text("×\(n)").font(.subheadline.weight(.semibold).monospacedDigit())
                            }
                            .morfoRow()
                        }
                    }
                }
                .morfoList()
            }
        }
        .background(MorfoColors.background)
        .navigationTitle(s.stats)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(s.reset, systemImage: "trash", role: .destructive) { confirmReset = true }
                    .disabled(p.totalAttempts == 0)
            }
        }
        .alert(s.confirmResetTitle, isPresented: $confirmReset) {
            Button(s.reset, role: .destructive) {
                app.resetQuizProgress()
                tapped = nil
            }
            Button(s.cancel, role: .cancel) {}
        } message: {
            Text(s.confirmResetMessage)
        }
    }

    private var legend: some View {
        HStack(spacing: 14) {
            legendItem(MorfoColors.good, "≥ 80%")
            legendItem(MorfoColors.partial, "50–79%")
            legendItem(MorfoColors.bad, "< 50%")
        }
    }

    private func legendItem(_ color: Color, _ label: String) -> some View {
        HStack(spacing: 5) {
            Circle().fill(color).frame(width: 8, height: 8)
            Text(label).font(.caption.monospacedDigit()).foregroundStyle(MorfoColors.textMuted)
        }
    }

    private func errorRow(_ label: String, _ count: Int, _ wrong: Int) -> some View {
        LabeledContent(label) {
            Text(app.s.errorPercent(count, count * 100 / wrong)).monospacedDigit()
        }
        .morfoRow()
    }
}
