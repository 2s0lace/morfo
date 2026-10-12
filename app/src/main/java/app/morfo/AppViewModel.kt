package app.morfo

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.morfo.dentition.ModelVariant
import app.morfo.dentition.QuickComparison
import app.morfo.dentition.ToothRef
import app.morfo.i18n.AppLanguage
import app.morfo.model.ModelMeta
import app.morfo.quiz.Grade
import app.morfo.quiz.QuizMode
import app.morfo.quiz.QuizSession
import app.morfo.quiz.QuizSettings
import app.morfo.quiz.QuizStore
import app.morfo.render.FilamentRuntime
import app.morfo.workspace.LabelMode
import app.morfo.workspace.LightingMode
import app.morfo.workspace.ScaleMode
import app.morfo.workspace.Workspace

enum class Screen { SELECTOR, COMPARE, ABOUT, QUIZ_HOME, QUIZ, QUIZ_STATS }

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = AppPreferences(app)
    private val quizStore = QuizStore(app)
    private val meta = ModelMeta.loadAll(app.assets)

    val runtime = FilamentRuntime(app, viewModelScope)
    val workspace = Workspace(meta).apply {
        spacingMm = prefs.spacingMm
        mirrorSync = prefs.mirrorSync
        scaleMode = prefs.scaleMode
    }

    var screen by mutableStateOf(Screen.SELECTOR)
        private set

    /** Teeth chosen in the selector (FDI + variant). */
    val selection = mutableStateListOf<ToothRef>()

    var appLanguage by mutableStateOf(prefs.appLanguage)
        private set
    var labelMode by mutableStateOf(prefs.labelMode)
        private set
    var lightingMode by mutableStateOf(prefs.lightingMode)
        private set

    // ---- quiz ------------------------------------------------------------------------------

    val quizProgress = quizStore.loadProgress()
    val quiz = QuizSession(meta, quizProgress, onProgressChanged = { quizStore.saveProgress(quizProgress) }).apply {
        language = prefs.appLanguage
    }
    var quizSettings by mutableStateOf(quizStore.loadSettings())
        private set

    /** Where the comparison screen returns to (the quiz can open it to compare a mistake). */
    private var compareReturn = Screen.SELECTOR

    fun openQuizHome() { screen = Screen.QUIZ_HOME }
    fun openQuizStats() { screen = Screen.QUIZ_STATS }

    fun updateQuizSettings(s: QuizSettings) {
        quizSettings = s
        quizStore.saveSettings(s)
    }

    fun startQuiz(onlyTeeth: List<Int>? = null) {
        val settings = if (onlyTeeth != null) quizSettings.copy(mode = QuizMode.ROUND) else quizSettings
        quiz.start(settings, onlyTeeth)
        screen = Screen.QUIZ
    }

    /** Shows the tooth that was on screen next to the one typed, in the comparison workspace. */
    fun compareQuizAnswer(grade: Grade) {
        val shown = quiz.current ?: return
        workspace.setTeeth(listOf(shown, ToothRef(grade.answered.fdi)))
        compareReturn = Screen.QUIZ
        screen = Screen.COMPARE
    }

    fun resetQuizProgress() {
        quizProgress.reset()
        quizStore.saveProgress(quizProgress)
    }

    fun toggle(fdi: Int) {
        val existing = selection.filter { it.fdi == fdi }
        if (existing.isEmpty()) selection += ToothRef(fdi) else selection.removeAll(existing)
    }

    fun select(refs: Collection<ToothRef>) {
        selection.clear()
        selection.addAll(refs.distinct())
    }

    fun addAll(fdis: Collection<Int>) {
        fdis.filter { f -> selection.none { it.fdi == f } }.forEach { selection += ToothRef(it) }
    }

    fun clearSelection() = selection.clear()

    fun openQuick(q: QuickComparison) {
        select(q.teeth)
        openCompare()
    }

    fun openCompare() {
        if (selection.isEmpty()) return
        compareReturn = Screen.SELECTOR
        workspace.setTeeth(selection.toList())
        screen = Screen.COMPARE
    }

    /** Back from the workspace: keep the selector in sync with what is on screen. */
    fun backToSelector() {
        if (compareReturn == Screen.QUIZ) {
            compareReturn = Screen.SELECTOR
            screen = Screen.QUIZ
            return
        }
        select(workspace.refs())
        screen = Screen.SELECTOR
    }

    fun openAbout() { screen = Screen.ABOUT }
    fun closeAbout() { screen = Screen.SELECTOR }
    fun goHome() { screen = Screen.SELECTOR }

    fun setVariant(key: Long, variant: ModelVariant) = workspace.setVariant(key, variant)

    fun updateLanguage(lang: AppLanguage) {
        appLanguage = lang
        prefs.appLanguage = lang
        quiz.language = lang
        if (lang == AppLanguage.EN && labelMode == LabelMode.FDI_PL) {
            updateLabelMode(LabelMode.FDI_EN)
        } else if (lang == AppLanguage.PL && labelMode == LabelMode.FDI_EN) {
            updateLabelMode(LabelMode.FDI_PL)
        }
    }
    fun updateLabelMode(mode: LabelMode) { labelMode = mode; prefs.labelMode = mode }
    fun updateLightingMode(mode: LightingMode) { lightingMode = mode; prefs.lightingMode = mode }
    fun updateSpacing(mm: Float) { workspace.spacingMm = mm; prefs.spacingMm = mm }
    fun updateMirrorSync(on: Boolean) { workspace.mirrorSync = on; prefs.mirrorSync = on }
    fun updateScaleMode(mode: ScaleMode) { workspace.scaleMode = mode; prefs.scaleMode = mode; workspace.requestFraming() }

    override fun onCleared() {
        runtime.destroy()
    }
}
