package app.morfo.ui.about

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.morfo.i18n.LocalStrings
import app.morfo.ui.theme.MorfoColors

/** Credits required by the CC-BY-4.0 licence of the models, plus notes on how they were prepared. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(MorfoColors.Background), contentAlignment = Alignment.TopCenter) {
    Column(
        Modifier
            .widthIn(max = 720.dp)
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back, tint = MorfoColors.Text)
            }
            Text(s.aboutTitle, style = MaterialTheme.typography.titleMedium)
        }
        Section(s.modelsSection)
        Body(s.modelsBody1)
        Body(s.modelsBody2)
        Section(s.prepSection)
        listOf(
            s.prep1,
            s.prep2,
            s.prep3,
            s.prep4,
            s.prep5,
            s.prep6,
        ).forEach { Body("•  $it") }
        Section(s.engineSection)
        Body(s.engineBody)
        Spacer(Modifier.height(24.dp))
    }
    }
}

@Composable
private fun Section(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MorfoColors.TextMuted)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Body(text: String) {
    Text(text, Modifier.padding(vertical = 3.dp), style = MaterialTheme.typography.bodyMedium)
}
