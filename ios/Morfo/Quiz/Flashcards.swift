/// Anki-like spaced repetition, measured in answers instead of days (the session is endless):
/// a wrong answer brings the tooth back after a few cards, each correct answer pushes it further
/// away. Levels persist, so teeth already mastered stay rare in later sessions.
struct Flashcards {
    /// Answers until the next review, per level.
    static let intervals = [4, 8, 18, 40, 90, 200, 450]
    static let retryAfter = 3
    static let newCardEvery = 3

    struct Summary: Equatable {
        let new: Int
        let due: Int
        let mastered: Int
    }

    let progress: QuizProgress
    var random: AnyRandomGenerator

    init(progress: QuizProgress, random: some RandomNumberGenerator = SystemRandomNumberGenerator()) {
        self.progress = progress
        self.random = AnyRandomGenerator(random)
    }

    /// Picks the next FDI number from `scope`, never repeating `previous` if avoidable.
    mutating func next(scope: [Int], previous: Int?) -> Int {
        let others = scope.filter { $0 != previous }
        let candidates = others.isEmpty ? scope : others
        let step = progress.step
        let due = candidates.filter { !progress.card($0).isNew && progress.card($0).due <= step }
        let fresh = candidates.filter { progress.card($0).isNew }

        // Introduce a new tooth every few cards, or whenever nothing is due for review.
        let introduceNew = !fresh.isEmpty && (due.isEmpty || step % Self.newCardEvery == 0)
        if introduceNew { return fresh.randomElement(using: &random)! }
        if let soonest = due.min(by: { progress.card($0).due < progress.card($1).due }) { return soonest }
        // Nothing due: keep going with whatever comes up soonest (endless mode).
        return candidates.min { progress.card($0).due < progress.card($1).due }!
    }

    func answer(_ fdi: Int, correct: Bool) {
        progress.step += 1
        var card = progress.card(fdi)
        card.reviews += 1
        if correct {
            card.level = min(card.level + 1, Self.intervals.count - 1)
            card.due = progress.step + Self.intervals[card.level]
        } else {
            card.lapses += 1
            card.level = 0
            card.due = progress.step + Self.retryAfter
        }
        progress.cards[fdi] = card
    }

    func summary(scope: [Int]) -> Summary {
        let cards = scope.map(progress.card)
        return Summary(
            new: cards.filter(\.isNew).count,
            due: cards.filter { !$0.isNew && $0.due <= progress.step }.count,
            mastered: cards.filter(\.isMastered).count
        )
    }
}
