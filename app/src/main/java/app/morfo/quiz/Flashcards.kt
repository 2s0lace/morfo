package app.morfo.quiz

import kotlin.random.Random

/**
 * Anki-like spaced repetition, measured in answers instead of days (the session is endless):
 * a wrong answer brings the tooth back after a few cards, each correct answer pushes it further
 * away. Levels persist, so teeth already mastered stay rare in later sessions.
 */
class Flashcards(private val progress: QuizProgress, private val random: Random = Random.Default) {

    /** Picks the next FDI number from [scope], never repeating [previous] if avoidable. */
    fun next(scope: List<Int>, previous: Int?): Int {
        val candidates = scope.filter { it != previous }.ifEmpty { scope }
        val step = progress.step
        val due = candidates.filter { !progress.card(it).isNew && progress.card(it).due <= step }
        val fresh = candidates.filter { progress.card(it).isNew }

        // Introduce a new tooth every few cards, or whenever nothing is due for review.
        val introduceNew = fresh.isNotEmpty() && (due.isEmpty() || step % NEW_CARD_EVERY == 0L)
        return when {
            introduceNew -> fresh.random(random)
            due.isNotEmpty() -> due.minBy { progress.card(it).due }
            // Nothing due: keep going with whatever comes up soonest (endless mode).
            else -> candidates.minBy { progress.card(it).due }
        }
    }

    fun answer(fdi: Int, correct: Boolean) {
        progress.step++
        val card = progress.card(fdi)
        card.reviews++
        if (correct) {
            card.level = (card.level + 1).coerceAtMost(INTERVALS.lastIndex)
            card.due = progress.step + INTERVALS[card.level]
        } else {
            card.lapses++
            card.level = 0
            card.due = progress.step + RETRY_AFTER
        }
    }

    fun summary(scope: List<Int>): Summary {
        val cards = scope.map { progress.card(it) }
        return Summary(
            new = cards.count { it.isNew },
            due = cards.count { !it.isNew && it.due <= progress.step },
            mastered = cards.count { it.isMastered },
        )
    }

    data class Summary(val new: Int, val due: Int, val mastered: Int)

    companion object {
        /** Answers until the next review, per level. */
        val INTERVALS = longArrayOf(4, 8, 18, 40, 90, 200, 450)
        const val RETRY_AFTER = 3L
        const val NEW_CARD_EVERY = 3L
    }
}
