import SwiftUI

/// Quiz and flashcards: the tooth fills the screen; progress floats on top, and one island at the
/// bottom holds the keypad, then turns into the verdict after answering.
struct QuizScreen: View {
    @Environment(AppModel.self) private var app
    @State private var controller: ViewportController
    @State private var interacted = false

    init(app: AppModel) {
        _controller = State(initialValue: ViewportController(workspace: app.quiz.workspace, models: app.models))
    }

    var body: some View {
        Group {
            if app.quiz.phase == .finished {
                QuizResultsView()
            } else {
                session
            }
        }
        .background(MorfoColors.background)
    }

    private var session: some View {
        let quiz = app.quiz
        let answered = quiz.phase == .feedback
        return GeometryReader { geo in
            let landscape = geo.size.width > geo.size.height && geo.size.width >= 600
            let screenBottom = geo.frame(in: .global).maxY + geo.safeAreaInsets.bottom
            ZStack(alignment: landscape ? .trailing : .bottom) {
                viewport(answered: answered)
                    .padding(.trailing, landscape ? 400 : 0)
                AnswerIsland(answered: answered)
                    .frame(maxWidth: landscape ? 380 : 520)
                    .padding(.horizontal, 12)
                    .padding(.bottom, 6)
                    .onGeometryChange(for: CGFloat.self, of: { $0.frame(in: .global).minY }) { minY in
                        controller.rig.insets.bottom = landscape ? 0 : Float(screenBottom - minY)
                    }
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .onAppear {
            controller.lightingMode = app.lightingMode
            controller.uniformColor = app.quizSettings.uniformColor
        }
        .onChange(of: app.quizSettings.uniformColor) { controller.uniformColor = app.quizSettings.uniformColor }
        .onChange(of: answered, initial: true) {
            quiz.workspace.labelSpaceMm = answered ? 7 : 0
            quiz.workspace.requestFraming()
        }
        .focusable()
        .focusEffectDisabled()
        .onKeyPress { press in handleKey(press) }
    }

    private func viewport(answered: Bool) -> some View {
        let s = app.s
        // The name is revealed only after answering.
        let labels: LabelMode = answered ? (app.language == .en ? .fdiEn : .fdiPl) : .none
        return ZStack(alignment: .top) {
            Viewport3D(controller: controller, labelMode: labels, gestures: gestures).ignoresSafeArea()
            VStack(spacing: 8) {
                HStack(spacing: 10) {
                    CircleButton(systemImage: "chevron.left", label: s.back) { app.path.removeLast() }
                    ProgressIsland()
                    CircleButton(systemImage: "circle.lefthalf.filled", label: s.uniformColorTitle,
                                 active: app.quizSettings.uniformColor) {
                        var settings = app.quizSettings
                        settings.uniformColor.toggle()
                        app.updateQuizSettings(settings)
                    }
                }
                if !interacted {
                    Text(s.quizGestureHint)
                        .font(.footnote)
                        .foregroundStyle(MorfoColors.textMuted)
                        .transition(.opacity)
                }
            }
            .padding(.horizontal, 12)
            .padding(.top, 4)
            .animation(.easeOut(duration: 0.4), value: interacted)
            .onGeometryChange(for: CGFloat.self, of: { $0.frame(in: .global).maxY }) { controller.rig.insets.top = Float($0) }
        }
    }

    /// Quiz viewport: rotate, pan and zoom only; nothing to select or move.
    private var gestures: ViewportGestureHandler {
        let rig = controller.rig
        let ws = app.quiz.workspace
        return ViewportGestureHandler(
            onDoubleTap: { _ in ws.requestFraming() },
            onRotateDrag: { d in
                interacted = true
                ws.rotate(by: dragRotation(d, rig: rig))
            },
            onTransform: { pan, zoom, twist, centroid in
                interacted = true
                applyCameraTransform(rig: rig, workspace: ws, pan: pan, zoom: zoom, twist: twist, centroid: centroid)
            }
        )
    }

    /// Hardware keyboard (iPad, simulator): digits, backspace, return.
    private func handleKey(_ press: KeyPress) -> KeyPress.Result {
        let quiz = app.quiz
        switch press.key {
        case .return:
            if quiz.phase == .feedback { quiz.next() } else { quiz.check() }
            return .handled
        case .delete:
            quiz.backspace()
            return .handled
        default:
            if let c = press.characters.first, c.isASCII, c.isNumber {
                quiz.type(c)
                return .handled
            }
            return .ignored
        }
    }
}

/// Round: question n of total with a thin bar and the score. Flashcards: answers and streak.
private struct ProgressIsland: View {
    @Environment(AppModel.self) private var app

    var body: some View {
        let quiz = app.quiz
        Island(cornerRadius: 20, padding: 0) {
            HStack(spacing: 12) {
                switch quiz.settings.mode {
                case .round:
                    HStack(spacing: 0) {
                        Text("\(min(quiz.index + 1, quiz.total))").font(.fdi(15, weight: .bold))
                        Text("/\(quiz.total)").font(.fdi(15, weight: .regular)).foregroundStyle(MorfoColors.textMuted)
                    }
                    ProgressView(value: Double(quiz.results.count), total: Double(max(quiz.total, 1)))
                        .tint(MorfoColors.accent)
                    Label("\(quiz.correctCount)", systemImage: "checkmark")
                        .font(.subheadline.weight(.semibold).monospacedDigit())
                        .foregroundStyle(MorfoColors.good)
                case .flashcards:
                    Label(QuizMode.flashcards.label(app.language), systemImage: "rectangle.stack")
                        .font(.subheadline.weight(.semibold))
                    Spacer(minLength: 0)
                    Text("\(quiz.results.count)").font(.fdi(15)).foregroundStyle(MorfoColors.textMuted)
                    Label("\(quiz.streak)", systemImage: "flame.fill")
                        .font(.subheadline.weight(.semibold).monospacedDigit())
                        .foregroundStyle(quiz.streak > 0 ? MorfoColors.partial : MorfoColors.textFaint)
                }
            }
            .lineLimit(1)
            .padding(.horizontal, 16)
            .frame(height: 40)
        }
    }
}

/// Keypad while answering, verdict afterwards; the same glass island morphs between the two.
private struct AnswerIsland: View {
    @Environment(AppModel.self) private var app
    let answered: Bool

    var body: some View {
        let quiz = app.quiz
        GlassEffectContainer(spacing: 10) {
            VStack(spacing: 10) {
                if answered, let grade = quiz.lastGrade {
                    Island(cornerRadius: 28, padding: 16) { VerdictView(grade: grade) }
                        .transition(.blurReplace)
                    PresetIsland(workspace: quiz.workspace)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                } else {
                    Island(cornerRadius: 30, padding: 12) {
                        AnswerKeypad(input: quiz.input, error: quiz.inputError,
                                     onDigit: quiz.type, onBackspace: quiz.backspace, onCheck: quiz.check)
                    }
                    .transition(.blurReplace)
                }
            }
        }
        .animation(.smooth(duration: 0.35), value: answered)
    }
}

private struct VerdictView: View {
    @Environment(AppModel.self) private var app
    let grade: Grade

    var body: some View {
        let s = app.s
        let lang = app.language
        let quiz = app.quiz
        let wrongTooth = grade.answered != grade.expected
        let (color, symbol): (Color, String) = grade.correct
            ? (MorfoColors.good, "checkmark.circle.fill")
            : grade.archCorrect || grade.sideCorrect || grade.kindCorrect
                ? (MorfoColors.partial, "exclamationmark.circle.fill")
                : (MorfoColors.bad, "xmark.circle.fill")
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Image(systemName: symbol).foregroundStyle(color)
                Text(grade.summary(lang)).fixedSize(horizontal: false, vertical: true)
            }
            .font(.headline)

            HStack(spacing: 16) {
                criterion(s.checkArch, grade.archCorrect)
                criterion(s.checkSide, grade.sideCorrect)
                criterion(s.checkTooth, grade.kindCorrect)
            }

            VStack(alignment: .leading, spacing: 8) {
                answerRow(s.thisIs, grade.expected, emphasised: true)
                if wrongTooth { answerRow(s.entered, grade.answered, emphasised: false) }
            }

            HStack(spacing: 10) {
                if wrongTooth {
                    Button { app.compareQuizAnswer(grade) } label: {
                        Label(s.compare, systemImage: "square.split.2x1")
                            .frame(maxWidth: .infinity, minHeight: 34)
                    }
                    .buttonStyle(.glass)
                }
                Button { quiz.next() } label: {
                    Text(quiz.isLastQuestion ? s.results : s.next).font(.body.weight(.semibold))
                        .frame(maxWidth: .infinity, minHeight: 34)
                }
                .buttonStyle(.glassProminent)
                .tint(MorfoColors.accent)
                .foregroundStyle(MorfoColors.onAccent)
            }
            .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func criterion(_ label: String, _ ok: Bool) -> some View {
        Label(label, systemImage: ok ? "checkmark" : "xmark")
            .font(.subheadline.weight(.medium))
            .foregroundStyle(ok ? MorfoColors.good : MorfoColors.bad)
    }

    private func answerRow(_ caption: String, _ tooth: Tooth, emphasised: Bool) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 10) {
            Text(caption).font(.caption).foregroundStyle(MorfoColors.textMuted).frame(width: 64, alignment: .leading)
            Text("\(tooth.fdi)").font(.fdi(17, weight: .bold))
                .foregroundStyle(emphasised ? MorfoColors.accent : MorfoColors.textMuted)
            Text(tooth.name(app.language)).font(.subheadline)
                .foregroundStyle(emphasised ? MorfoColors.text : MorfoColors.textMuted)
                .lineLimit(2)
        }
    }
}

/// Typed answer: two digit slots and a full 0–9 keypad. All digits are always offered,
/// so the keypad itself gives no hint about valid FDI numbers.
struct AnswerKeypad: View {
    @Environment(AppModel.self) private var app
    let input: String
    let error: String?
    let onDigit: (Character) -> Void
    let onBackspace: () -> Void
    let onCheck: () -> Void

    var body: some View {
        let chars = Array(input)
        VStack(spacing: 10) {
            HStack(spacing: 14) {
                ForEach(0..<2, id: \.self) { i in
                    VStack(spacing: 6) {
                        Text(i < chars.count ? String(chars[i]) : " ")
                            .font(.fdi(40, weight: .semibold))
                            .contentTransition(.numericText())
                        Capsule()
                            .fill(i == input.count ? MorfoColors.accent : MorfoColors.separator)
                            .frame(width: 34, height: 3)
                    }
                }
            }
            .animation(.snappy(duration: 0.2), value: input)
            .padding(.top, 4)

            Text(error ?? " ")
                .font(.footnote)
                .foregroundStyle(error != nil ? MorfoColors.bad : .clear)
                .lineLimit(1)
                .minimumScaleFactor(0.7)

            Grid(horizontalSpacing: 4, verticalSpacing: 4) {
                ForEach(["12345", "67890"], id: \.self) { row in
                    GridRow {
                        ForEach(Array(row), id: \.self) { d in
                            Button { onDigit(d) } label: {
                                Text(String(d)).font(.system(size: 26, weight: .regular).monospacedDigit())
                                    .frame(maxWidth: .infinity, minHeight: 50)
                                    .contentShape(.rect)
                            }
                            .buttonStyle(PressHighlightStyle(cornerRadius: 16))
                            .foregroundStyle(MorfoColors.text)
                        }
                    }
                }
                GridRow {
                    Button(action: onBackspace) {
                        Image(systemName: "delete.left").font(.system(size: 20))
                            .frame(maxWidth: .infinity, minHeight: 50)
                            .contentShape(.rect)
                    }
                    .buttonStyle(PressHighlightStyle(cornerRadius: 16))
                    .foregroundStyle(MorfoColors.textMuted)
                    .accessibilityLabel("Backspace")
                    .gridCellColumns(2)
                    Button(action: onCheck) {
                        Text(app.s.checkAnswer).font(.body.weight(.semibold))
                            .frame(maxWidth: .infinity, minHeight: 34)
                    }
                    .buttonStyle(.glassProminent)
                    .tint(input.count == 2 ? MorfoColors.accent : MorfoColors.surfaceRaised)
                    .foregroundStyle(input.count == 2 ? MorfoColors.onAccent : MorfoColors.textMuted)
                    .gridCellColumns(3)
                }
            }
        }
    }
}

private struct QuizResultsView: View {
    @Environment(AppModel.self) private var app

    var body: some View {
        let s = app.s
        let lang = app.language
        let quiz = app.quiz
        let total = quiz.results.count
        let correct = quiz.correctCount
        let fraction = total == 0 ? 0 : Double(correct) / Double(total)
        let mistakes = quiz.mistakes()
        let wrong = quiz.results.filter { !$0.correct }
        List {
            Section {
                HStack(spacing: 20) {
                    ZStack {
                        Circle().stroke(MorfoColors.separator, lineWidth: 8)
                        Circle().trim(from: 0, to: fraction)
                            .stroke(MorfoColors.accuracy(fraction) ?? MorfoColors.accent, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                            .rotationEffect(.degrees(-90))
                        Text("\(Int((fraction * 100).rounded()))%").font(.fdi(18, weight: .bold))
                    }
                    .frame(width: 84, height: 84)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("\(correct) / \(total)").font(.fdi(28, weight: .bold))
                        if total > 0 { Text(s.correctPercentage(correct * 100 / total)).font(.subheadline).foregroundStyle(MorfoColors.textMuted) }
                    }
                }
                .padding(.vertical, 8)
                .morfoRow()
            }

            Section(s.mistakes) {
                if wrong.isEmpty {
                    Text(s.noMistakes).morfoRow()
                }
                ForEach(Array(wrong.enumerated()), id: \.offset) { _, g in
                    HStack(alignment: .firstTextBaseline, spacing: 12) {
                        Text("\(g.expected.fdi)").font(.fdi(17, weight: .bold)).foregroundStyle(MorfoColors.accent)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(g.expected.name(lang)).font(.subheadline)
                            Text(s.enteredMistake(g.answered.fdi, g.summary(lang).lowercased()))
                                .font(.caption).foregroundStyle(MorfoColors.textMuted)
                        }
                    }
                    .morfoRow()
                }
            }

            Section {
                if !mistakes.isEmpty {
                    Button { app.startQuiz(onlyTeeth: mistakes) } label: {
                        Label(s.repeatMistakes(mistakes.count), systemImage: "arrow.uturn.backward")
                    }
                    .morfoRow()
                }
                Button { app.startQuiz() } label: { Label(s.newRound, systemImage: "play") }.morfoRow()
                Button { app.openQuizStats() } label: { Label(s.stats, systemImage: "chart.bar") }.morfoRow()
            }
            .tint(MorfoColors.accent)
        }
        .morfoList()
        .navigationTitle(s.roundResults)
        .toolbar(.visible, for: .navigationBar)
    }
}
