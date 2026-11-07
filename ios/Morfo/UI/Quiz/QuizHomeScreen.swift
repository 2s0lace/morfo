import SwiftUI

struct QuizHomeScreen: View {
    @Environment(AppModel.self) private var app

    var body: some View {
        let settings = app.quizSettings
        let s = app.s
        let lang = app.language
        List {
            Section {
                Text(s.quizSubtitle).font(.subheadline).foregroundStyle(MorfoColors.textMuted)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
            }

            Section {
                Picker(s.mode, selection: binding(\.mode)) {
                    ForEach(QuizMode.allCases) { Text($0.label(lang)).tag($0) }
                }
                .pickerStyle(.segmented)
                .morfoRow()
                if settings.mode == .round {
                    Picker(s.questionCount, selection: binding(\.roundLength)) {
                        Text("10").tag(Int?.some(10))
                        Text("20").tag(Int?.some(20))
                        Text(s.fullScope(settings.scope.teeth().count)).tag(Int?.none)
                    }
                    .morfoRow()
                } else {
                    let summary = Flashcards(progress: app.quizProgress).summary(scope: settings.scope.teeth())
                    MetricStrip(metrics: [
                        (s.newCards, "\(summary.new)"), (s.dueCards, "\(summary.due)"), (s.masteredCards, "\(summary.mastered)"),
                    ])
                    .morfoRow()
                }
                Picker(s.scope, selection: binding(\.scope)) {
                    ForEach(QuizScope.allCases) { Text($0.label(lang)).tag($0) }
                }
                .morfoRow()
            } header: {
                Text(s.mode)
            } footer: {
                Text(settings.mode.description(lang))
            }

            Section {
                Picker(s.difficulty, selection: binding(\.difficulty)) {
                    ForEach(Difficulty.allCases) { Text($0.label(lang)).tag($0) }
                }
                .pickerStyle(.segmented)
                .morfoRow()
            } header: {
                Text(s.difficulty)
            } footer: {
                Text(settings.difficulty.description(lang))
            }

            Section(s.options) {
                Toggle(isOn: binding(\.uniformColor)) {
                    Text(s.uniformColorTitle)
                    Text(s.uniformColorSubtitle)
                }
                .morfoRow()
                Toggle(isOn: binding(\.ignoreSide)) {
                    Text(s.ignoreSideTitle)
                    Text(s.ignoreSideSubtitle)
                }
                .morfoRow()
            }
        }
        .morfoList()
        .tint(MorfoColors.accent)
        .navigationTitle(s.quiz)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(s.stats, systemImage: "chart.bar.xaxis") { app.openQuizStats() }
            }
        }
        .safeAreaInset(edge: .bottom) {
            Button { app.startQuiz() } label: {
                Label(settings.mode == .round ? s.startRound : s.startFlashcards, systemImage: "play.fill")
                    .font(.body.weight(.semibold))
                    .frame(maxWidth: .infinity, minHeight: 36)
            }
            .buttonStyle(.glassProminent)
            .tint(MorfoColors.accent)
            .foregroundStyle(MorfoColors.onAccent)
            .frame(maxWidth: maxContentWidth)
            .padding(.horizontal, 20)
            .padding(.bottom, 6)
        }
    }

    private func binding<V>(_ key: WritableKeyPath<QuizSettings, V>) -> Binding<V> {
        Binding(
            get: { app.quizSettings[keyPath: key] },
            set: { value in
                var s = app.quizSettings
                s[keyPath: key] = value
                app.updateQuizSettings(s)
            }
        )
    }
}
