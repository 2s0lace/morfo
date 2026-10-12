package app.morfo.quiz

import app.morfo.dentition.Arch
import app.morfo.dentition.Dentition
import app.morfo.dentition.ToothClass
import app.morfo.i18n.AppLanguage

enum class QuizMode(val labelPl: String, val descPl: String, val labelEn: String, val descEn: String) {
    ROUND("Runda", "Określona liczba pytań, na końcu wynik i lista błędów", "Round", "Fixed number of questions, score and mistake list at the end"),
    FLASHCARDS("Fiszki", "Bez końca. Zęby, które mylisz, wracają częściej, a opanowane coraz rzadziej (jak w Anki)", "Flashcards", "Endless. Teeth you confuse return more often, mastered teeth less often (spaced repetition)");

    val description get() = descPl
    fun label(lang: AppLanguage) = if (lang == AppLanguage.EN) labelEn else labelPl
    fun description(lang: AppLanguage) = if (lang == AppLanguage.EN) descEn else descPl
}

enum class Difficulty(val labelPl: String, val descPl: String, val labelEn: String, val descEn: String) {
    EXAM("Egzamin", "Losowa orientacja, trzeba samemu ustalić łuk, stronę i ząb", "Exam", "Random orientation, determine arch, side and tooth yourself"),
    EASY("Łatwy", "Pozycja anatomiczna, powierzchnią przedsionkową do Ciebie", "Easy", "Anatomical position, facial surface facing you");

    val description get() = descPl
    fun label(lang: AppLanguage) = if (lang == AppLanguage.EN) labelEn else labelPl
    fun description(lang: AppLanguage) = if (lang == AppLanguage.EN) descEn else descPl
}

/** Which teeth can appear. */
enum class QuizScope(val labelPl: String, val labelEn: String) {
    ALL("Wszystkie", "All"),
    MAXILLARY("Szczęka", "Maxillary"),
    MANDIBULAR("Żuchwa", "Mandibular"),
    INCISORS("Siekacze", "Incisors"),
    CANINES("Kły", "Canines"),
    PREMOLARS("Przedtrzonowce", "Premolars"),
    MOLARS("Trzonowce", "Molars"),
    ;

    fun label(lang: AppLanguage) = if (lang == AppLanguage.EN) labelEn else labelPl

    fun teeth(): List<Int> = Dentition.permanent.filter {
        when (this) {
            ALL -> true
            MAXILLARY -> it.arch == Arch.MAXILLARY
            MANDIBULAR -> it.arch == Arch.MANDIBULAR
            INCISORS -> it.toothClass == ToothClass.INCISOR
            CANINES -> it.toothClass == ToothClass.CANINE
            PREMOLARS -> it.toothClass == ToothClass.PREMOLAR
            MOLARS -> it.toothClass == ToothClass.MOLAR
        }
    }.map { it.fdi }
}

data class QuizSettings(
    val mode: QuizMode = QuizMode.ROUND,
    val scope: QuizScope = QuizScope.ALL,
    val difficulty: Difficulty = Difficulty.EXAM,
    /** Round length; null = every tooth in scope once. */
    val roundLength: Int? = 20,
    /** Replace the photo texture with a plain enamel colour, so only the shape can be used. */
    val uniformColor: Boolean = false,
    /** Accept the right tooth type (arch + position) even with the wrong side. */
    val ignoreSide: Boolean = false,
)
