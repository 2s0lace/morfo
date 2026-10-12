package app.morfo.quiz

import app.morfo.dentition.Tooth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuizLogicTest {

    @Test
    fun parseAcceptsOnlyPermanentFdiNumbers() {
        assertEquals(14, Grading.parse("14")?.fdi)
        assertEquals(48, Grading.parse("48")?.fdi)
        assertNull(Grading.parse("19"))
        assertNull(Grading.parse("50"))
        assertNull(Grading.parse("4"))
        assertNull(Grading.parse("144"))
    }

    @Test
    fun gradingExplainsWhichPartIsWrong() {
        val wrongSide = Grading.grade(Tooth(24), Tooth(14), ignoreSide = false)
        assertFalse(wrongSide.correct)
        assertTrue(wrongSide.archCorrect && wrongSide.kindCorrect && !wrongSide.sideCorrect)
        assertEquals("Dobry typ zęba, ale zła strona", wrongSide.summaryPl)

        val wrongArch = Grading.grade(Tooth(34), Tooth(24), ignoreSide = false)
        assertTrue(!wrongArch.archCorrect && wrongArch.sideCorrect && wrongArch.kindCorrect)

        val wrongKind = Grading.grade(Tooth(15), Tooth(14), ignoreSide = false)
        assertTrue(wrongKind.archCorrect && wrongKind.sideCorrect && !wrongKind.kindCorrect)

        assertTrue(Grading.grade(Tooth(24), Tooth(14), ignoreSide = true).correct)
        assertFalse(Grading.grade(Tooth(24), Tooth(34), ignoreSide = true).correct)
    }

    @Test
    fun progressCountsErrorsAndConfusions() {
        val p = QuizProgress()
        p.record(Grading.grade(Tooth(24), Tooth(14), false))
        p.record(Grading.grade(Tooth(24), Tooth(24), false))
        assertEquals(2, p.statsFor(24).attempts)
        assertEquals(1, p.statsFor(24).correct)
        assertEquals(1, p.sideErrors)
        assertEquals(0, p.archErrors)
        assertEquals(1, p.confusions[24 to 14])
    }

    @Test
    fun flashcardsBringMistakesBackSoonAndMasteredLater() {
        val p = QuizProgress()
        val cards = Flashcards(p, Random(1))
        val scope = listOf(11, 12, 13, 14, 15, 16)

        cards.answer(14, correct = false)
        val wrongDue = p.card(14).due
        cards.answer(15, correct = true)
        val rightDue = p.card(15).due
        assertTrue("a mistake returns sooner than a correct answer", wrongDue < rightDue)

        // After enough other answers the missed tooth is due and gets picked before new ones catch up.
        repeat(5) { cards.answer(11, correct = true) }
        val picks = (1..3).map { cards.next(scope, previous = null) }
        assertTrue(14 in picks || p.card(14).due > p.step)

        // Repeated correct answers push the interval out.
        repeat(4) { cards.answer(16, correct = true) }
        assertTrue(p.card(16).due - p.step >= Flashcards.INTERVALS[3])
        assertTrue(p.card(16).isMastered)
    }

    @Test
    fun flashcardsAvoidImmediateRepeat() {
        val p = QuizProgress()
        val cards = Flashcards(p, Random(7))
        val scope = listOf(21, 22)
        var prev: Int? = null
        repeat(20) {
            val n = cards.next(scope, prev)
            if (prev != null) assertTrue(n != prev)
            cards.answer(n, correct = it % 3 != 0)
            prev = n
        }
    }
}
