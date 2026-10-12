package app.morfo.quiz

import app.morfo.dentition.Tooth
import app.morfo.i18n.AppLanguage

/** Result of comparing a typed FDI number with the tooth shown. */
data class Grade(
    val expected: Tooth,
    val answered: Tooth,
    val archCorrect: Boolean,
    val sideCorrect: Boolean,
    val kindCorrect: Boolean,
    val correct: Boolean,
) {
    /** One-line explanation in Polish, pointing at what exactly was wrong. */
    val summaryPl: String
        get() = when {
            correct && answered.fdi == expected.fdi -> "Dobrze!"
            correct -> "Dobry typ zęba (strona pominięta w tym trybie)"
            archCorrect && kindCorrect -> "Dobry typ zęba, ale zła strona"
            sideCorrect && kindCorrect -> "Dobry typ i strona, ale zły łuk"
            archCorrect && sideCorrect -> "Dobry łuk i strona, ale inny ząb"
            kindCorrect -> "Dobry ząb w kwadrancie, ale zły łuk i strona"
            archCorrect -> "Dobry łuk, ale inny ząb i strona"
            sideCorrect -> "Dobra strona, ale inny łuk i ząb"
            else -> "Niepoprawnie"
        }

    val summaryEn: String
        get() = when {
            correct && answered.fdi == expected.fdi -> "Correct!"
            correct -> "Correct tooth type (side ignored in this mode)"
            archCorrect && kindCorrect -> "Correct tooth type, but wrong side"
            sideCorrect && kindCorrect -> "Correct type and side, but wrong arch"
            archCorrect && sideCorrect -> "Correct arch and side, but wrong tooth"
            kindCorrect -> "Correct tooth in quadrant, but wrong arch and side"
            archCorrect -> "Correct arch, but wrong tooth and side"
            sideCorrect -> "Correct side, but wrong arch and tooth"
            else -> "Incorrect"
        }

    fun summary(lang: AppLanguage) = if (lang == AppLanguage.EN) summaryEn else summaryPl
}

object Grading {

    /** Parses exactly two digits into a permanent FDI number, or null if it is not one (e.g. "19", "50"). */
    fun parse(input: String): Tooth? {
        if (input.length != 2 || !input.all(Char::isDigit)) return null
        val q = input[0].digitToInt()
        val p = input[1].digitToInt()
        return if (q in 1..4 && p in 1..8) Tooth(q * 10 + p) else null
    }

    fun grade(expected: Tooth, answered: Tooth, ignoreSide: Boolean): Grade {
        val arch = expected.arch == answered.arch
        val side = expected.side == answered.side
        val kind = expected.kind == answered.kind
        return Grade(
            expected = expected,
            answered = answered,
            archCorrect = arch,
            sideCorrect = side,
            kindCorrect = kind,
            correct = arch && kind && (side || ignoreSide),
        )
    }
}
