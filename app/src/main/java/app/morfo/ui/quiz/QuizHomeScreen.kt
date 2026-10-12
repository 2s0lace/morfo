package app.morfo.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.morfo.AppViewModel
import app.morfo.i18n.LocalLanguage
import app.morfo.i18n.LocalStrings
import app.morfo.quiz.Difficulty
import app.morfo.quiz.Flashcards
import app.morfo.quiz.QuizMode
import app.morfo.quiz.QuizScope
import app.morfo.ui.compare.Pill
import app.morfo.ui.theme.MorfoColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizHomeScreen(vm: AppViewModel) {
    val s = vm.quizSettings
    val str = LocalStrings.current
    val lang = LocalLanguage.current
    BackHandler(onBack = vm::goHome)

    Box(Modifier.fillMaxSize().background(MorfoColors.Background), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 104.dp),
        ) {
            QuizHeader(str.quiz, onBack = vm::goHome) {
                Pill(str.stats, onClick = vm::openQuizStats)
            }
            Text(
                str.quizSubtitle,
                Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MorfoColors.TextMuted,
            )

            QuizSection(str.mode)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuizMode.entries.forEach { m ->
                    ChoiceCard(m.label(lang), m.description(lang), s.mode == m, Modifier.weight(1f)) {
                        vm.updateQuizSettings(s.copy(mode = m))
                    }
                }
            }

            if (s.mode == QuizMode.ROUND) {
                QuizSection(str.questionCount)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(10, 20, null).forEach { n ->
                        Pill(n?.toString() ?: str.fullScope(s.scope.teeth().size), active = s.roundLength == n) {
                            vm.updateQuizSettings(s.copy(roundLength = n))
                        }
                    }
                }
            } else {
                val summary = Flashcards(vm.quizProgress).summary(s.scope.teeth())
                QuizSection(str.yourFlashcards(s.scope.label(lang).lowercase()))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat(str.newCards, summary.new.toString(), Modifier.weight(1f))
                    Stat(str.dueCards, summary.due.toString(), Modifier.weight(1f))
                    Stat(str.masteredCards, summary.mastered.toString(), Modifier.weight(1f))
                }
            }

            QuizSection(str.scope)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                QuizScope.entries.forEach { sc ->
                    Pill(sc.label(lang), active = s.scope == sc) { vm.updateQuizSettings(s.copy(scope = sc)) }
                }
            }

            QuizSection(str.difficulty)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { d ->
                    ChoiceCard(d.label(lang), d.description(lang), s.difficulty == d, Modifier.weight(1f)) {
                        vm.updateQuizSettings(s.copy(difficulty = d))
                    }
                }
            }

            QuizSection(str.options)
            ToggleRow(
                str.uniformColorTitle,
                str.uniformColorSubtitle,
                s.uniformColor,
            ) { vm.updateQuizSettings(s.copy(uniformColor = it)) }
            ToggleRow(
                str.ignoreSideTitle,
                str.ignoreSideSubtitle,
                s.ignoreSide,
            ) { vm.updateQuizSettings(s.copy(ignoreSide = it)) }
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MorfoColors.Glass)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Button(
                onClick = { vm.startQuiz() },
                modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text(if (s.mode == QuizMode.ROUND) str.startRound else str.startFlashcards)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.background(MorfoColors.Surface, RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MorfoColors.TextMuted)
    }
}
