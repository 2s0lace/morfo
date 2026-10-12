package app.morfo.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.morfo.ui.theme.MorfoColors

private val KeyShape = RoundedCornerShape(12.dp)

/**
 * Typed answer: two digit boxes and a full 0–9 keypad. All digits are always offered,
 * so the keypad itself gives no hint about valid FDI numbers.
 */
@Composable
fun AnswerKeypad(
    input: String,
    error: String?,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onCheck: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(2) { i ->
                val ch = input.getOrNull(i)
                Box(
                    Modifier
                        .size(width = 56.dp, height = 64.dp)
                        .background(MorfoColors.Surface, KeyShape)
                        .border(1.5.dp, if (i == input.length) MorfoColors.Accent else MorfoColors.Outline, KeyShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(ch?.toString() ?: "", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Text(
            error ?: " ",
            Modifier.padding(vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (error != null) QuizColors.Bad else Color.Transparent,
        )
        for (row in listOf("12345", "67890")) {
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (d in row) Key(d.toString(), Modifier.weight(1f)) { onDigit(d) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Key("⌫", Modifier.weight(1f)) { onBackspace() }
            Key(app.morfo.i18n.LocalStrings.current.checkAnswer, Modifier.weight(2f), accent = input.length == 2) { onCheck() }
        }
    }
}

@Composable
private fun Key(label: String, modifier: Modifier, accent: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier
            .height(52.dp)
            .background(if (accent) MorfoColors.Accent else MorfoColors.SurfaceRaised, KeyShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = if (label.length == 1) 22.sp else 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (accent) MorfoColors.OnAccent else MorfoColors.Text,
        )
    }
}
