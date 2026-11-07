import Observation

/// Long-term learning data: per-tooth accuracy, typical confusions and flashcard scheduling state.
@Observable
final class QuizProgress {
    static let newLevel = -1
    static let masteredLevel = 3

    struct ToothStats: Equatable {
        var attempts = 0
        var correct = 0
        var accuracy: Double? { attempts == 0 ? nil : Double(correct) / Double(attempts) }
    }

    /// Flashcard state for one FDI number. `level` < 0 means never seen. `due` is in answer steps.
    struct Card: Equatable {
        var level = QuizProgress.newLevel
        var due = 0
        var reviews = 0
        var lapses = 0
        var isNew: Bool { level < 0 }
        var isMastered: Bool { level >= QuizProgress.masteredLevel }
    }

    struct Confusion: Hashable {
        let expected: Int
        let answered: Int
    }

    var stats: [Int: ToothStats] = [:]
    /// Wrong answers only.
    var confusions: [Confusion: Int] = [:]
    var cards: [Int: Card] = [:]

    var archErrors = 0
    var sideErrors = 0
    var kindErrors = 0

    /// Total flashcard answers; the flashcard clock.
    var step = 0

    func statsFor(_ fdi: Int) -> ToothStats { stats[fdi] ?? ToothStats() }
    func card(_ fdi: Int) -> Card { cards[fdi] ?? Card() }

    func record(_ grade: Grade) {
        let fdi = grade.expected.fdi
        stats[fdi, default: ToothStats()].attempts += 1
        if grade.correct {
            stats[fdi, default: ToothStats()].correct += 1
            return
        }
        confusions[Confusion(expected: fdi, answered: grade.answered.fdi), default: 0] += 1
        if !grade.archCorrect { archErrors += 1 }
        if !grade.sideCorrect { sideErrors += 1 }
        if !grade.kindCorrect { kindErrors += 1 }
    }

    var totalAttempts: Int { stats.values.reduce(0) { $0 + $1.attempts } }
    var totalCorrect: Int { stats.values.reduce(0) { $0 + $1.correct } }

    func topConfusions(_ n: Int) -> [(Confusion, Int)] {
        confusions.sorted { $0.value != $1.value ? $0.value > $1.value : ($0.key.expected, $0.key.answered) < ($1.key.expected, $1.key.answered) }
            .prefix(n).map { ($0.key, $0.value) }
    }

    func reset() {
        stats = [:]; confusions = [:]; cards = [:]
        archErrors = 0; sideErrors = 0; kindErrors = 0; step = 0
    }
}
