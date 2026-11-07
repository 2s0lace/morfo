import Foundation
import Observation

enum Route: Hashable {
    case compare, quizHome, quiz, quizStats, about
}

/// Small persisted view preferences.
struct AppPreferences {
    let defaults: UserDefaults

    func value<E: RawRepresentable>(_ key: String, default d: E) -> E where E.RawValue == String {
        defaults.string(forKey: key).flatMap(E.init) ?? d
    }

    func set<E: RawRepresentable>(_ key: String, _ v: E) where E.RawValue == String {
        defaults.set(v.rawValue, forKey: key)
    }
}

/// App-wide state and navigation; mirrors AppViewModel on Android.
@Observable
final class AppModel {
    @ObservationIgnored private let prefs: AppPreferences
    @ObservationIgnored private let quizStore: QuizStore
    @ObservationIgnored let meta: [String: ModelMeta]
    @ObservationIgnored let models = ModelRepository()

    let workspace: Workspace
    var path: [Route] = []

    /// Teeth chosen in the selector (FDI + variant).
    private(set) var selection: [ToothRef] = []

    private(set) var language: AppLanguage
    private(set) var labelMode: LabelMode
    private(set) var lightingMode: LightingMode

    let quizProgress: QuizProgress
    let quiz: QuizSession
    private(set) var quizSettings: QuizSettings

    var s: AppStrings { language.strings }

    init(defaults: UserDefaults = .standard) {
        prefs = AppPreferences(defaults: defaults)
        quizStore = QuizStore(defaults: defaults)
        meta = ModelMeta.loadAll()
        workspace = Workspace(meta: meta)
        language = prefs.value("language", default: Locale.current.language.languageCode?.identifier == "pl" ? .pl : .en)
        labelMode = prefs.value("labels", default: LabelMode.fdiPl)
        lightingMode = prefs.value("lighting", default: LightingMode.clinical)
        workspace.scaleMode = prefs.value("scale", default: ScaleMode.anatomical)
        workspace.spacingMm = (defaults.object(forKey: "spacing") as? NSNumber)?.floatValue ?? 4
        workspace.mirrorSync = defaults.object(forKey: "mirrorSync") as? Bool ?? true

        let progress = quizStore.loadProgress()
        quizProgress = progress
        let store = quizStore
        quiz = QuizSession(meta: meta, progress: progress, onProgressChanged: { store.saveProgress(progress) })
        quizSettings = quizStore.loadSettings()
        quiz.language = language
        if language == .en && labelMode == .fdiPl { labelMode = .fdiEn }
        #if DEBUG
        applyDebugLaunchArguments(defaults)
        #endif
    }

    #if DEBUG
    /// Debug builds only: `-debugTeeth 14,24 -debugRoute compare|quiz|quizHome|quizStats|about`
    /// opens a screen directly, for screenshots and testing without tapping through the app.
    private func applyDebugLaunchArguments(_ defaults: UserDefaults) {
        if let teeth = defaults.string(forKey: "debugTeeth") {
            select(teeth.split(separator: ",").compactMap { Int($0) }.map { ToothRef(fdi: $0) })
        }
        switch defaults.string(forKey: "debugRoute") {
        case "compare": openCompare()
        case "quizHome": openQuizHome()
        case "quiz": path = [.quizHome]; startQuiz()
        case "quizStats": path = [.quizHome, .quizStats]
        case "about": openAbout()
        default: break
        }
    }
    #endif

    // MARK: - Selection

    func isSelected(_ fdi: Int) -> Bool { selection.contains { $0.fdi == fdi } }

    func toggle(_ fdi: Int) {
        if isSelected(fdi) { selection.removeAll { $0.fdi == fdi } } else { selection.append(ToothRef(fdi: fdi)) }
    }

    func select(_ refs: [ToothRef]) {
        var seen = Set<ToothRef>()
        selection = refs.filter { seen.insert($0).inserted }
    }

    func addAll(_ fdis: [Int]) {
        for f in fdis where !isSelected(f) { selection.append(ToothRef(fdi: f)) }
    }

    func clearSelection() { selection = [] }

    // MARK: - Navigation

    func openQuick(_ q: QuickComparison) {
        select(q.teeth)
        openCompare()
    }

    func openCompare() {
        guard !selection.isEmpty else { return }
        workspace.setTeeth(selection)
        path.append(.compare)
    }

    /// Called when the comparison screen goes away: keep the selector in sync with what was on screen,
    /// unless the comparison was opened from the quiz.
    func compareClosed() {
        if path.last != .quiz { select(workspace.refs()) }
    }

    func openQuizHome() { path.append(.quizHome) }
    func openQuizStats() { path.append(.quizStats) }
    func openAbout() { path.append(.about) }

    func updateQuizSettings(_ s: QuizSettings) {
        quizSettings = s
        quizStore.saveSettings(s)
    }

    func startQuiz(onlyTeeth: [Int]? = nil) {
        var settings = quizSettings
        if onlyTeeth != nil { settings.mode = .round }
        quiz.start(settings, onlyTeeth: onlyTeeth)
        if path.last != .quiz { path.append(.quiz) }
    }

    /// Shows the tooth that was on screen next to the one typed, in the comparison workspace.
    func compareQuizAnswer(_ grade: Grade) {
        guard let shown = quiz.current else { return }
        workspace.setTeeth([shown, ToothRef(fdi: grade.answered.fdi)])
        path.append(.compare)
    }

    func resetQuizProgress() {
        quizProgress.reset()
        quizStore.saveProgress(quizProgress)
    }

    // MARK: - Settings

    func updateLanguage(_ lang: AppLanguage) {
        language = lang
        prefs.set("language", lang)
        quiz.language = lang
        if lang == .en && labelMode == .fdiPl { updateLabelMode(.fdiEn) }
        if lang == .pl && labelMode == .fdiEn { updateLabelMode(.fdiPl) }
    }

    func updateLabelMode(_ mode: LabelMode) { labelMode = mode; prefs.set("labels", mode) }
    func updateLightingMode(_ mode: LightingMode) { lightingMode = mode; prefs.set("lighting", mode) }
    func updateSpacing(_ mm: Float) { workspace.spacingMm = mm; prefs.defaults.set(mm, forKey: "spacing") }
    func updateMirrorSync(_ on: Bool) { workspace.mirrorSync = on; prefs.defaults.set(on, forKey: "mirrorSync") }

    func updateScaleMode(_ mode: ScaleMode) {
        workspace.scaleMode = mode
        prefs.set("scale", mode)
        workspace.requestFraming()
    }
}
