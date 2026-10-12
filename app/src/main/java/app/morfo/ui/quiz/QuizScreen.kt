package app.morfo.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import app.morfo.AppViewModel
import app.morfo.i18n.AppLanguage
import app.morfo.i18n.AppStrings
import app.morfo.i18n.LocalLanguage
import app.morfo.i18n.LocalStrings
import app.morfo.math.Quat
import app.morfo.math.Vec3
import app.morfo.quiz.Grade
import app.morfo.quiz.QuizMode
import app.morfo.quiz.QuizSession
import app.morfo.render.ViewportRenderer
import app.morfo.ui.compare.Pill
import app.morfo.ui.compare.PresetRow
import app.morfo.ui.compare.ToothLabels
import app.morfo.ui.compare.Viewport3D
import app.morfo.ui.compare.ViewportGestureHandler
import app.morfo.ui.compare.rememberViewportRenderer
import app.morfo.ui.compare.viewportGestures
import app.morfo.ui.theme.MorfoColors
import app.morfo.workspace.LabelMode
import app.morfo.workspace.Workspace
import kotlin.math.PI

@Composable
fun QuizScreen(vm: AppViewModel) {
    val quiz = vm.quiz
    BackHandler(onBack = vm::openQuizHome)

    if (quiz.phase == QuizSession.Phase.FINISHED) {
        QuizResults(vm)
        return
    }

    val renderer = rememberViewportRenderer(vm.runtime, quiz.workspace)
    val uniform = vm.quizSettings.uniformColor
    LaunchedEffect(uniform) { renderer.uniformColor = uniform }
    val answered = quiz.phase == QuizSession.Phase.FEEDBACK
    LaunchedEffect(answered) {
        quiz.workspace.labelSpaceMm = if (answered) 7f else 0f
        quiz.workspace.requestFraming()
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MorfoColors.Background)) {
        // Landscape (phone or tablet): tooth on the left, answer panel on the right.
        val shortScreen = maxHeight < 480.dp
        if (maxWidth > maxHeight && maxWidth >= 600.dp) {
            Row(Modifier.fillMaxSize()) {
                QuizViewport(vm, renderer, uniform, answered, Modifier.weight(1f).fillMaxHeight(), bottomInset = false, showHint = !shortScreen)
                Column(
                    Modifier
                        .width(if (shortScreen) 340.dp else 420.dp)
                        .fillMaxHeight()
                        .background(MorfoColors.Surface)
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End))
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = if (shortScreen) 8.dp else 24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    AnswerPanel(vm, answered)
                }
            }
        } else {
            QuizViewport(vm, renderer, uniform, answered, Modifier.fillMaxSize(), bottomInset = true)
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .onGloballyPositioned { renderer.rig.insetBottomPx = it.size.height.toFloat() }
                    .fillMaxWidth()
                    .background(MorfoColors.Glass)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(top = 12.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Tablet in portrait: keep the keypad at a comfortable width.
                Column(Modifier.widthIn(max = 520.dp)) { AnswerPanel(vm, answered) }
            }
        }
    }
}

@Composable
private fun QuizViewport(
    vm: AppViewModel,
    renderer: ViewportRenderer,
    uniform: Boolean,
    answered: Boolean,
    modifier: Modifier,
    bottomInset: Boolean,
    showHint: Boolean = true,
) {
    val quiz = vm.quiz
    val s = LocalStrings.current
    val lang = LocalLanguage.current
    if (!bottomInset) renderer.rig.insetBottomPx = 0f
    Box(modifier) {
        Viewport3D(renderer, vm.lightingMode, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().viewportGestures(remember(renderer) { QuizGestures(quiz.workspace, renderer) }))
        // The name is revealed only after answering.
        if (answered) ToothLabels(quiz.workspace, renderer, if (lang == AppLanguage.EN) LabelMode.FDI_EN else LabelMode.FDI_PL)

        Column(
            Modifier
                .align(Alignment.TopCenter)
                // Measure before the inset paddings, so the status bar area counts as covered.
                .onGloballyPositioned { renderer.rig.insetTopPx = it.size.height.toFloat() }
                .fillMaxWidth()
                .background(MorfoColors.Glass)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                .padding(end = 12.dp, bottom = 6.dp),
        ) {
            QuizHeader(progressTitle(quiz, s), onBack = vm::openQuizHome) {
                Pill(s.uniformColorTitle, active = uniform) {
                    vm.updateQuizSettings(vm.quizSettings.copy(uniformColor = !uniform))
                }
            }
            if (showHint) {
                Text(
                    s.quizGestureHint,
                    Modifier.padding(start = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MorfoColors.TextFaint,
                )
            }
        }
    }
}

@Composable
private fun AnswerPanel(vm: AppViewModel, answered: Boolean) {
    val quiz = vm.quiz
    val grade = quiz.lastGrade
    if (answered && grade != null) {
        Feedback(grade, onCompare = { vm.compareQuizAnswer(grade) }, onNext = quiz::next, isLast = isLastQuestion(quiz))
        // Same side margins as the feedback above, so everything lines up.
        PresetRow(quiz.workspace, Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 14.dp))
    } else {
        AnswerKeypad(quiz.input, quiz.inputError, quiz::type, quiz::backspace, quiz::check)
    }
}

private fun progressTitle(quiz: QuizSession, s: AppStrings): String = when (quiz.settings.mode) {
    QuizMode.ROUND -> s.questionProgress(quiz.index + 1, quiz.total, quiz.correctCount)
    QuizMode.FLASHCARDS -> s.flashcardsProgress(quiz.results.size, quiz.streak)
}

private fun isLastQuestion(quiz: QuizSession) = quiz.settings.mode == QuizMode.ROUND && quiz.index + 1 >= quiz.total

@Composable
private fun Feedback(grade: Grade, onCompare: () -> Unit, onNext: () -> Unit, isLast: Boolean) {
    val s = LocalStrings.current
    val lang = LocalLanguage.current
    val color = when {
        grade.correct -> QuizColors.Good
        grade.archCorrect || grade.sideCorrect || grade.kindCorrect -> QuizColors.Partial
        else -> QuizColors.Bad
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(grade.summary(lang), style = MaterialTheme.typography.titleMedium, color = color)
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Check(s.checkArch, grade.archCorrect)
            Check(s.checkSide, grade.sideCorrect)
            Check(s.checkTooth, grade.kindCorrect)
        }
        Text("${s.thisIs} ${grade.expected.fdi} · ${grade.expected.name(lang)}", style = MaterialTheme.typography.bodyMedium)
        if (grade.answered.fdi != grade.expected.fdi) {
            Text(
                "${s.entered} ${grade.answered.fdi} · ${grade.answered.name(lang)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MorfoColors.TextMuted,
            )
        }
        // Two equal, single-line buttons.
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (grade.answered.fdi != grade.expected.fdi) {
                OutlinedButton(
                    onClick = onCompare,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(s.compare, maxLines = 1)
                }
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text(if (isLast) s.results else s.next, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Check(label: String, ok: Boolean) {
    Text(
        (if (ok) "✓ " else "✗ ") + label,
        Modifier
            .background((if (ok) QuizColors.Good else QuizColors.Bad).copy(alpha = 0.18f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelMedium,
        color = if (ok) QuizColors.Good else QuizColors.Bad,
    )
}

@Composable
private fun QuizResults(vm: AppViewModel) {
    val s = LocalStrings.current
    val lang = LocalLanguage.current
    val quiz = vm.quiz
    val total = quiz.results.size
    val correct = quiz.correctCount
    val mistakes = quiz.mistakes()
    Box(Modifier.fillMaxSize().background(MorfoColors.Background), contentAlignment = Alignment.TopCenter) {
    Column(
        Modifier
            .widthIn(max = MaxContentWidth)
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        QuizHeader(s.roundResults, onBack = vm::openQuizHome)
        Text(
            "$correct / $total",
            Modifier.padding(start = 4.dp, top = 12.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = accuracyColor(if (total == 0) null else correct.toFloat() / total) ?: MorfoColors.Text,
        )
        Text(
            if (total == 0) "" else s.correctPercentage(correct * 100 / total),
            Modifier.padding(start = 4.dp),
            color = MorfoColors.TextMuted,
        )
        QuizSection(s.mistakes)
        val wrong = quiz.results.filter { !it.correct }
        if (wrong.isEmpty()) Text(s.noMistakes, Modifier.padding(start = 4.dp))
        wrong.forEach { g ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp, horizontal = 4.dp)) {
                Text("${g.expected.fdi}", Modifier.padding(end = 10.dp), style = MaterialTheme.typography.titleSmall, color = MorfoColors.Accent)
                Column {
                    Text(g.expected.name(lang), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        s.enteredMistake(g.answered.fdi, g.summary(lang).lowercase()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MorfoColors.TextMuted,
                    )
                }
            }
        }
        Column(Modifier.padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (mistakes.isNotEmpty()) {
                Button(onClick = { vm.startQuiz(onlyTeeth = mistakes) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(s.repeatMistakes(mistakes.size))
                }
            }
            OutlinedButton(onClick = { vm.startQuiz() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text(s.newRound)
            }
            OutlinedButton(onClick = vm::openQuizStats, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text(s.stats)
            }
        }
    }
    }
}

/** Quiz viewport: rotate, pan and zoom only; nothing to select or move. */
private class QuizGestures(private val workspace: Workspace, private val renderer: ViewportRenderer) : ViewportGestureHandler {
    override fun onTap(position: Offset) = Unit
    override fun onDoubleTap(position: Offset) = workspace.requestFraming()
    override fun onRotateDrag(delta: Offset) {
        val k = (PI / renderer.rig.width).toFloat() * 1.1f
        workspace.rotateBy(Quat.axisAngle(Vec3.Y, delta.x * k) * Quat.axisAngle(Vec3.X, delta.y * k))
    }
    override fun onMoveStart(position: Offset) = Unit
    override fun onMoveDrag(delta: Offset) = Unit
    override fun onMoveEnd() = Unit
    override fun onTransform(pan: Offset, zoom: Float, twistRadians: Float, centroid: Offset) {
        renderer.rig.pan(pan.x, pan.y)
        if (zoom != 1f) renderer.rig.zoom(zoom, centroid)
        if (twistRadians != 0f) workspace.rotateBy(Quat.axisAngle(Vec3.Z, -twistRadians))
    }
}
