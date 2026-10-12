package app.morfo.ui.compare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.morfo.AppViewModel
import app.morfo.dentition.ModelVariant
import app.morfo.dentition.Morphology
import app.morfo.i18n.AppLanguage
import app.morfo.i18n.LocalLanguage
import app.morfo.i18n.LocalStrings
import app.morfo.ui.theme.MorfoColors
import app.morfo.workspace.Arrangement as Layout
import app.morfo.workspace.LabelMode
import app.morfo.workspace.LightingMode
import app.morfo.workspace.PlacedTooth
import app.morfo.workspace.ScaleMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewSettingsSheet(vm: AppViewModel, onDismiss: () -> Unit, onSidePanelWidth: (Float) -> Unit = {}) {
    val ws = vm.workspace
    val s = LocalStrings.current
    val lang = LocalLanguage.current

    AdaptiveSheet(onDismiss, onSidePanelWidth) {
        Heading(s.language)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AppLanguage.entries.forEach { l ->
                Pill(l.label, active = vm.appLanguage == l) { vm.updateLanguage(l) }
            }
        }

        Heading(s.lighting)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LightingMode.entries.forEach { m -> Pill(m.label(lang), active = vm.lightingMode == m) { vm.updateLightingMode(m) } }
        }
        Text(vm.lightingMode.description(lang), Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MorfoColors.TextMuted)

        Heading(s.labels)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LabelMode.entries.filter { lang == AppLanguage.PL || it != LabelMode.FDI_PL }.forEach { m ->
                Pill(m.label(lang), active = vm.labelMode == m) { vm.updateLabelMode(m) }
            }
        }

        Heading(s.rotation)
        SwitchRow(s.syncAllTeethTitle, s.syncAllTeethSubtitle, ws.syncRotation) { ws.syncRotation = it }
        SwitchRow(
            s.mirrorContralateralTitle,
            s.mirrorContralateralSubtitle,
            ws.mirrorSync,
        ) { vm.updateMirrorSync(it) }

        Heading(s.layout)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Layout.entries.filter { it != Layout.TRIANGLE || ws.visibleTeeth.size == 3 }.forEach { a ->
                Pill(a.label(lang), active = ws.arrangement == a) { ws.arrangement = a; ws.autoArrange() }
            }
        }
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(s.spacing, Modifier.padding(end = 12.dp), style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = ws.spacingMm,
                onValueChange = { vm.updateSpacing(it) },
                valueRange = 1f..20f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = MorfoColors.Accent, activeTrackColor = MorfoColors.Accent),
            )
            Text("${ws.spacingMm.roundToInt()} mm", Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodySmall, color = MorfoColors.TextMuted)
        }

        Heading(s.scale)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ScaleMode.entries.forEach { m -> Pill(m.label(lang), active = ws.scaleMode == m) { vm.updateScaleMode(m) } }
        }
        Text(ws.scaleMode.description(lang), Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MorfoColors.TextMuted)

        Heading(s.controls)
        listOf(
            s.controlRotate,
            s.controlPanZoom,
            s.controlDragTooth,
            s.controlTap,
        ).forEach { Text("•  $it", Modifier.padding(vertical = 2.dp), style = MaterialTheme.typography.bodySmall, color = MorfoColors.TextMuted) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToothInfoSheet(tooth: PlacedTooth, onVariant: (ModelVariant) -> Unit, onDismiss: () -> Unit, onSidePanelWidth: (Float) -> Unit = {}) {
    val t = tooth.tooth
    val s = LocalStrings.current
    val lang = LocalLanguage.current

    AdaptiveSheet(onDismiss, onSidePanelWidth) {
        Text("${t.fdi} — ${t.name(lang)}", style = MaterialTheme.typography.titleMedium)
        if (lang == AppLanguage.PL) {
            Text(t.nameEn, style = MaterialTheme.typography.bodySmall, color = MorfoColors.TextMuted)
        }
        Spacer(Modifier.height(16.dp))
        Fact(s.classLabel, t.toothClass.label(lang))
        Fact(s.archLabel, t.arch.label(lang))
        Fact(s.sideLabel, t.side.label(lang))
        Fact(s.avgLengthLabel, "${t.averageLengthMm} ${s.crownPlusRoot}")
        Fact(s.modelLabel, if (t.isMirrored) s.mirrorScanOf(t.sourceFdi) else s.scanOf(t.fdi))
        if (t.variants.size > 1) {
            SwitchRow(s.carabelliVariantTitle, s.carabelliVariantSubtitle, tooth.variant == ModelVariant.CARABELLI) {
                onVariant(if (it) ModelVariant.CARABELLI else ModelVariant.STANDARD)
            }
        }
        Heading(s.morphologyTitle)
        Morphology.keyFeatures(t, lang).forEach {
            Row(Modifier.padding(vertical = 3.dp)) {
                Text("•", Modifier.padding(end = 8.dp), color = MorfoColors.Accent)
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(
            s.textbookSummaryFootnote,
            Modifier.padding(top = 14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MorfoColors.TextFaint,
        )
    }
}

@Composable
private fun Heading(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MorfoColors.TextMuted)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Fact(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, Modifier.weight(0.4f), style = MaterialTheme.typography.bodyMedium, color = MorfoColors.TextMuted)
        Text(value, Modifier.weight(0.6f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
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
