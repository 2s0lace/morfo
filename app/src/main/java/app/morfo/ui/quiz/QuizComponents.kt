package app.morfo.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.morfo.ui.theme.MorfoColors

/** Content width limit so screens stay readable on a tablet. */
val MaxContentWidth = 720.dp

/** Muted status colours, kept close to the neutral palette. */
object QuizColors {
    val Good = Color(0xFF7FB38A)
    val Partial = Color(0xFFD9B26A)
    val Bad = Color(0xFFD77A6E)
}

@Composable
fun QuizHeader(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = app.morfo.i18n.LocalStrings.current.back, tint = MorfoColors.Text)
        }
        Text(title, Modifier.weight(1f).padding(end = 12.dp), style = MaterialTheme.typography.titleMedium, maxLines = 1)
        // The back icon sits 12 dp inside its touch target; mirror that on the right for symmetry.
        Box(Modifier.padding(end = 12.dp)) { trailing() }
    }
}

@Composable
fun QuizSection(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MorfoColors.TextMuted)
    Spacer(Modifier.height(10.dp))
}

/** A large selectable card with a title and a short description. */
@Composable
fun ChoiceCard(title: String, description: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .background(if (selected) MorfoColors.SurfaceRaised else MorfoColors.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, if (selected) MorfoColors.Accent else MorfoColors.Outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = if (selected) MorfoColors.Accent else MorfoColors.Text)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MorfoColors.TextMuted)
    }
}

@Composable
fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MorfoColors.TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MorfoColors.Accent, checkedThumbColor = MorfoColors.OnAccent),
        )
    }
}

/** Colour for an accuracy value: none / weak / medium / good. */
fun accuracyColor(accuracy: Float?): Color? = when {
    accuracy == null -> null
    accuracy >= 0.8f -> QuizColors.Good
    accuracy >= 0.5f -> QuizColors.Partial
    else -> QuizColors.Bad
}
