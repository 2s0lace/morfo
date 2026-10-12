package app.morfo.quiz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.morfo.dentition.Tooth
import app.morfo.dentition.ToothRef
import app.morfo.i18n.AppLanguage
import app.morfo.i18n.getStrings
import app.morfo.math.randomRotation
import app.morfo.model.ModelMeta
import app.morfo.workspace.SurfacePreset
import app.morfo.workspace.Workspace
import kotlin.random.Random

/**
 * One quiz run: shows a single tooth, takes a typed FDI number, grades it and moves on.
 * The tooth lives in its own [Workspace], so the comparison screen is untouched.
 */
class QuizSession(
    meta: Map<String, ModelMeta>,
    val progress: QuizProgress,
    private val onProgressChanged: () -> Unit,
    private val random: Random = Random.Default,
) {
    enum class Phase { ANSWERING, FEEDBACK, FINISHED }

    val workspace = Workspace(meta).apply { labelSpaceMm = 7f }
    private val flashcards = Flashcards(progress, random)

    var language: AppLanguage = AppLanguage.PL
    var settings by mutableStateOf(QuizSettings())
        private set
    var phase by mutableStateOf(Phase.ANSWERING)
        private set
    var input by mutableStateOf("")
        private set
    var inputError by mutableStateOf<String?>(null)
        private set
    var current by mutableStateOf<ToothRef?>(null)
        private set
    var lastGrade by mutableStateOf<Grade?>(null)
        private set

    /** Round mode: position and length. Flashcards: answers in this session. */
    var index by mutableIntStateOf(0)
        private set
    var total by mutableIntStateOf(0)
        private set
    var streak by mutableIntStateOf(0)
        private set

    /** Grades of this session (round results / flashcard history). */
    val results = mutableStateListOf<Grade>()

    private var queue: List<Int> = emptyList()

    val correctCount get() = results.count { it.correct }
    val scope get() = settings.scope.teeth()

    fun start(newSettings: QuizSettings, onlyTeeth: List<Int>? = null) {
        settings = newSettings
        results.clear()
        streak = 0
        index = 0
        when (newSettings.mode) {
            QuizMode.ROUND -> {
                val pool = onlyTeeth ?: newSettings.scope.teeth()
                val length = if (onlyTeeth != null) pool.size else newSettings.roundLength ?: pool.size
                queue = buildQueue(pool, length)
                total = queue.size
                show(queue[0])
            }
            QuizMode.FLASHCARDS -> {
                total = 0
                show(flashcards.next(scope, previous = null))
            }
        }
    }

    fun type(digit: Char) {
        if (phase != Phase.ANSWERING || input.length >= 2) return
        input += digit
        inputError = null
    }

    fun backspace() {
        if (phase != Phase.ANSWERING) return
        input = input.dropLast(1)
        inputError = null
    }

    fun check() {
        if (phase != Phase.ANSWERING) return
        val shown = current?.tooth ?: return
        val answered = Grading.parse(input)
        if (answered == null) {
            val s = getStrings(language)
            inputError = if (input.length < 2) s.enterTwoDigits else s.notPermanentFdi(input)
            return
        }
        val grade = Grading.grade(shown, answered, settings.ignoreSide)
        lastGrade = grade
        results += grade
        streak = if (grade.correct) streak + 1 else 0
        progress.record(grade)
        if (settings.mode == QuizMode.FLASHCARDS) flashcards.answer(shown.fdi, grade.correct)
        onProgressChanged()
        phase = Phase.FEEDBACK
        workspace.requestFraming() // the feedback panel is taller than the keypad
    }

    fun next() {
        if (phase != Phase.FEEDBACK) return
        when (settings.mode) {
            QuizMode.ROUND -> {
                index++
                if (index >= queue.size) {
                    phase = Phase.FINISHED
                    return
                }
                show(queue[index])
            }
            QuizMode.FLASHCARDS -> {
                index++
                show(flashcards.next(scope, previous = current?.fdi))
            }
        }
    }

    /** Teeth answered wrongly in this session (for "repeat mistakes"). */
    fun mistakes(): List<Int> = results.filter { !it.correct }.map { it.expected.fdi }.distinct()

    fun flashcardSummary() = flashcards.summary(scope)

    /** Turns the tooth to its buccal view after answering, to study it. */
    fun showFacial() = workspace.applyPreset(SurfacePreset.FACIAL)

    private fun show(fdi: Int) {
        val tooth = Tooth(fdi)
        // First molars have two specimens (with / without Carabelli cusp); either may appear.
        val ref = ToothRef(fdi, tooth.variants.random(random))
        workspace.setTeeth(emptyList())
        workspace.setTeeth(listOf(ref))
        val placed = workspace.teeth.first()
        val rotation = when (settings.difficulty) {
            Difficulty.EXAM -> randomRotation(random)
            Difficulty.EASY -> SurfacePreset.FACIAL.orientation(tooth)
        }
        workspace.setRotation(placed.key, rotation)
        workspace.requestFraming()
        current = ref
        input = ""
        inputError = null
        lastGrade = null
        phase = Phase.ANSWERING
    }

    /** Shuffled teeth, cycling the pool if the round is longer, without the same tooth twice in a row. */
    private fun buildQueue(pool: List<Int>, length: Int): List<Int> {
        if (pool.isEmpty()) return emptyList()
        val out = ArrayList<Int>(length)
        while (out.size < length) {
            val batch = pool.shuffled(random).toMutableList()
            if (out.isNotEmpty() && batch.size > 1 && batch.first() == out.last()) batch.add(batch.removeAt(0))
            out += batch
        }
        return out.take(length)
    }
}
