package app.morfo.quiz

/** Long-term learning data: per-tooth accuracy, typical confusions and flashcard scheduling state. */
class QuizProgress {

    class ToothStats(var attempts: Int = 0, var correct: Int = 0) {
        val accuracy: Float? get() = if (attempts == 0) null else correct.toFloat() / attempts
    }

    /** Flashcard state for one FDI number. [level] < 0 means never seen. [due] is in answer steps. */
    class Card(var level: Int = NEW, var due: Long = 0, var reviews: Int = 0, var lapses: Int = 0) {
        val isNew get() = level < 0
        val isMastered get() = level >= MASTERED_LEVEL
    }

    val stats = HashMap<Int, ToothStats>()
    /** (expected, answered) → count, only for wrong answers. */
    val confusions = HashMap<Pair<Int, Int>, Int>()
    val cards = HashMap<Int, Card>()

    var archErrors = 0
    var sideErrors = 0
    var kindErrors = 0

    /** Total flashcard answers; the flashcard clock. */
    var step = 0L

    fun statsFor(fdi: Int) = stats.getOrPut(fdi) { ToothStats() }
    fun card(fdi: Int) = cards.getOrPut(fdi) { Card() }

    fun record(grade: Grade) {
        val s = statsFor(grade.expected.fdi)
        s.attempts++
        if (grade.correct) {
            s.correct++
            return
        }
        confusions.merge(grade.expected.fdi to grade.answered.fdi, 1, Int::plus)
        if (!grade.archCorrect) archErrors++
        if (!grade.sideCorrect) sideErrors++
        if (!grade.kindCorrect) kindErrors++
    }

    val totalAttempts get() = stats.values.sumOf { it.attempts }
    val totalCorrect get() = stats.values.sumOf { it.correct }

    fun topConfusions(n: Int) = confusions.entries.sortedByDescending { it.value }.take(n)

    fun reset() {
        stats.clear(); confusions.clear(); cards.clear()
        archErrors = 0; sideErrors = 0; kindErrors = 0; step = 0
    }

    companion object {
        const val NEW = -1
        const val MASTERED_LEVEL = 3
    }
}
