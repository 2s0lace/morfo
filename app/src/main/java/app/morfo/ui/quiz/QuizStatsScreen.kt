package app.morfo.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.morfo.AppViewModel
import app.morfo.dentition.Dentition
import app.morfo.dentition.Tooth
import app.morfo.i18n.AppLanguage
import app.morfo.i18n.AppStrings
import app.morfo.i18n.LocalLanguage
import app.morfo.i18n.LocalStrings
import app.morfo.quiz.Flashcards
import app.morfo.quiz.QuizProgress
import app.morfo.ui.compare.Pill
import app.morfo.ui.selector.ArchChart
import app.morfo.ui.theme.MorfoColors

@Composable
fun QuizStatsScreen(vm: AppViewModel) {
    BackHandler(onBack = vm::openQuizHome)
    val s = LocalStrings.current
    val lang = LocalLanguage.current
    var confirmReset by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) } // re-read progress after a reset
    var tapped by remember { mutableStateOf<Tooth?>(null) }
    val p = vm.quizProgress

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MorfoColors.Background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.TopCenter,
    ) {
        val landscape = maxWidth > maxHeight && maxWidth >= 600.dp
        key(revision) {
            if (landscape) {
                // Chart on the left sized to the height, numbers on the right: no giant chart to scroll past.
                Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Header(vm, s) { confirmReset = true }
                    if (p.totalAttempts == 0) {
                        EmptyState(s)
                    } else {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            // Size the chart from the space actually left in this column (legend included).
                            BoxWithConstraints(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                val chartWidth = ((maxHeight - 44.dp) * 0.92f).coerceIn(200.dp, 560.dp)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AccuracyChart(p, Modifier.widthIn(max = chartWidth)) { tapped = it }
                                    Legend()
                                }
                            }
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                                Tiles(p, s)
                                TappedTooth(p, tapped, s, lang)
                                ErrorTypes(p, s)
                                Confusions(p, s, lang)
                            }
                        }
                    }
                }
            } else {
                Column(
                    Modifier
                        .widthIn(max = MaxContentWidth)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    Header(vm, s) { confirmReset = true }
                    if (p.totalAttempts == 0) {
                        EmptyState(s)
                    } else {
                        Tiles(p, s)
                        QuizSection(s.accuracyOnTeeth)
                        AccuracyChart(p, Modifier) { tapped = it }
                        Legend()
                        TappedTooth(p, tapped, s, lang)
                        ErrorTypes(p, s)
                        Confusions(p, s, lang)
                    }
                    Box(Modifier.padding(bottom = 24.dp))
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(s.confirmResetTitle) },
            text = { Text(s.confirmResetMessage) },
            confirmButton = {
                TextButton(onClick = { vm.resetQuizProgress(); confirmReset = false; tapped = null; revision++ }) { Text(s.reset) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(s.cancel) } },
            containerColor = MorfoColors.Surface,
        )
    }
}

@Composable
private fun Header(vm: AppViewModel, s: AppStrings, onReset: () -> Unit) {
    QuizHeader(s.stats, onBack = vm::openQuizHome) { Pill(s.reset, onClick = onReset) }
}

@Composable
private fun EmptyState(s: AppStrings) {
    Text(
        s.emptyStats,
        Modifier.padding(16.dp),
        color = MorfoColors.TextMuted,
    )
}

@Composable
private fun Tiles(p: QuizProgress, s: AppStrings) {
    val total = p.totalAttempts
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile(s.answersLabel, total.toString(), Modifier.weight(1f))
        Tile(s.correctLabel, "${p.totalCorrect * 100 / total}%", Modifier.weight(1f))
        val cards = Flashcards(p).summary(Dentition.permanent.map { it.fdi })
        Tile(s.masteredLabel, "${cards.mastered}/32", Modifier.weight(1f))
    }
}

@Composable
private fun AccuracyChart(p: QuizProgress, modifier: Modifier, onTap: (Tooth) -> Unit) {
    ArchChart(
        isSelected = { false },
        isDimmed = { p.statsFor(it.fdi).attempts == 0 },
        onToggle = onTap,
        colorFor = { accuracyColor(p.stats[it.fdi]?.accuracy) },
        modifier = modifier,
    )
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 6.dp)) {
        LegendItem(QuizColors.Good, "≥ 80%")
        LegendItem(QuizColors.Partial, "50–79%")
        LegendItem(QuizColors.Bad, "< 50%")
    }
}

@Composable
private fun TappedTooth(p: QuizProgress, tapped: Tooth?, s: AppStrings, lang: AppLanguage) {
    val t = tapped ?: return
    val st = p.statsFor(t.fdi)
    Text(
        s.accuracyForTooth(t.fdi, t.name(lang), st.correct, st.attempts),
        Modifier.padding(top = 12.dp),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun ErrorTypes(p: QuizProgress, s: AppStrings) {
    QuizSection(s.errorTypes)
    val wrong = (p.totalAttempts - p.totalCorrect).coerceAtLeast(1)
    ErrorRow(s.wrongArch, p.archErrors, wrong, s)
    ErrorRow(s.wrongSide, p.sideErrors, wrong, s)
    ErrorRow(s.wrongTooth, p.kindErrors, wrong, s)
}

@Composable
private fun Confusions(p: QuizProgress, s: AppStrings, lang: AppLanguage) {
    QuizSection(s.mostFrequentConfusions)
    val top = p.topConfusions(6)
    if (top.isEmpty()) Text(s.noConfusions, color = MorfoColors.TextMuted)
    top.forEach { (pair, n) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${pair.first} → ${pair.second}", Modifier.weight(0.3f), style = MaterialTheme.typography.titleSmall)
            Text(
                s.shownEntered(Tooth(pair.first).name(lang).lowercase(), pair.second),
                Modifier.weight(0.6f),
                style = MaterialTheme.typography.bodySmall,
                color = MorfoColors.TextMuted,
            )
            Text("×$n", Modifier.weight(0.1f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(MorfoColors.Surface, RoundedCornerShape(12.dp)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MorfoColors.TextMuted)
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
        Text(" $label", style = MaterialTheme.typography.bodySmall, color = MorfoColors.TextMuted)
    }
}

@Composable
private fun ErrorRow(label: String, count: Int, wrong: Int, s: AppStrings) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(s.errorPercent(count, count * 100 / wrong), style = MaterialTheme.typography.bodySmall, color = MorfoColors.TextMuted)
    }
}
