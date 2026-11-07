import Foundation

/// Persists quiz settings and learning progress (small JSON blobs in UserDefaults).
struct QuizStore {
    let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func loadSettings() -> QuizSettings {
        var s = QuizSettings()
        if let v = defaults.string(forKey: "quiz.mode").flatMap(QuizMode.init) { s.mode = v }
        if let v = defaults.string(forKey: "quiz.scope").flatMap(QuizScope.init) { s.scope = v }
        if let v = defaults.string(forKey: "quiz.difficulty").flatMap(Difficulty.init) { s.difficulty = v }
        if defaults.object(forKey: "quiz.roundLength") != nil {
            let n = defaults.integer(forKey: "quiz.roundLength")
            s.roundLength = n > 0 ? n : nil
        }
        s.uniformColor = defaults.bool(forKey: "quiz.uniformColor")
        s.ignoreSide = defaults.bool(forKey: "quiz.ignoreSide")
        return s
    }

    func saveSettings(_ s: QuizSettings) {
        defaults.set(s.mode.rawValue, forKey: "quiz.mode")
        defaults.set(s.scope.rawValue, forKey: "quiz.scope")
        defaults.set(s.difficulty.rawValue, forKey: "quiz.difficulty")
        defaults.set(s.roundLength ?? 0, forKey: "quiz.roundLength")
        defaults.set(s.uniformColor, forKey: "quiz.uniformColor")
        defaults.set(s.ignoreSide, forKey: "quiz.ignoreSide")
    }

    private struct Snapshot: Codable {
        var step = 0
        var archErrors = 0
        var sideErrors = 0
        var kindErrors = 0
        /// fdi → [attempts, correct]
        var stats: [String: [Int]] = [:]
        /// fdi → [level, due, reviews, lapses]
        var cards: [String: [Int]] = [:]
        /// [expected, answered, count]
        var confusions: [[Int]] = []
    }

    func loadProgress() -> QuizProgress {
        let p = QuizProgress()
        guard let data = defaults.data(forKey: "quiz.progress"),
              let o = try? JSONDecoder().decode(Snapshot.self, from: data) else { return p }
        p.step = o.step
        p.archErrors = o.archErrors
        p.sideErrors = o.sideErrors
        p.kindErrors = o.kindErrors
        for (k, a) in o.stats where a.count == 2 { if let fdi = Int(k) { p.stats[fdi] = .init(attempts: a[0], correct: a[1]) } }
        for (k, a) in o.cards where a.count == 4 {
            if let fdi = Int(k) { p.cards[fdi] = .init(level: a[0], due: a[1], reviews: a[2], lapses: a[3]) }
        }
        for a in o.confusions where a.count == 3 { p.confusions[.init(expected: a[0], answered: a[1])] = a[2] }
        return p
    }

    func saveProgress(_ p: QuizProgress) {
        let o = Snapshot(
            step: p.step, archErrors: p.archErrors, sideErrors: p.sideErrors, kindErrors: p.kindErrors,
            stats: Dictionary(uniqueKeysWithValues: p.stats.map { (String($0.key), [$0.value.attempts, $0.value.correct]) }),
            cards: Dictionary(uniqueKeysWithValues: p.cards.map { (String($0.key), [$0.value.level, $0.value.due, $0.value.reviews, $0.value.lapses]) }),
            confusions: p.confusions.map { [$0.key.expected, $0.key.answered, $0.value] }
        )
        if let data = try? JSONEncoder().encode(o) { defaults.set(data, forKey: "quiz.progress") }
    }
}
