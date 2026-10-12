package app.morfo.ui.compare

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.morfo.dentition.ModelVariant
import app.morfo.dentition.Tooth
import app.morfo.i18n.AppLanguage
import app.morfo.i18n.LocalLanguage
import app.morfo.i18n.LocalStrings
import app.morfo.ui.theme.MorfoColors
import app.morfo.workspace.SurfacePreset
import app.morfo.workspace.Workspace

private val PillShape = RoundedCornerShape(10.dp)

@Composable
fun CompareTopBar(
    workspace: Workspace,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    /** Landscape phone: everything in one row to leave height for the teeth. */
    compact: Boolean = false,
) {
    val s = LocalStrings.current
    Column(
        modifier
            .fillMaxWidth()
            .background(MorfoColors.Glass)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back, tint = MorfoColors.Text)
            }
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (t in workspace.teeth) {
                    val selected = t.key == workspace.selectedKey
                    Pill(
                        text = t.tooth.fdi.toString() + if (t.variant == ModelVariant.CARABELLI) "C" else "",
                        active = selected,
                        faded = !t.visible,
                        strike = !t.visible,
                    ) {
                        if (!t.visible) workspace.setVisible(t.key, true)
                        workspace.toggleSelect(t.key)
                    }
                }
            }
            if (compact) {
                Row(Modifier.padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { LayoutPills(workspace, short = true) }
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = s.viewSettings, tint = MorfoColors.Text)
            }
        }
        if (!compact) {
            Row(
                Modifier.padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { LayoutPills(workspace, short = false) }
        }
    }
}

@Composable
private fun LayoutPills(workspace: Workspace, short: Boolean) {
    val s = LocalStrings.current
    val sync = workspace.syncRotation
    Pill(
        when {
            short -> s.syncShort
            sync -> s.syncOn
            else -> s.syncOff
        },
        active = sync,
    ) { workspace.syncRotation = !sync }
    Pill(if (short) s.auto else s.autoLayout, active = workspace.autoLayout) { workspace.autoArrange() }
}

@Composable
fun SelectedToothBar(
    workspace: Workspace,
    onInfo: (Long) -> Unit,
    onVariant: (Long, ModelVariant) -> Unit,
    /** Landscape phone: a single row without the full name. */
    compact: Boolean = false,
) {
    val t = workspace.selected
    val s = LocalStrings.current
    val lang = LocalLanguage.current
    AnimatedVisibility(t != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        if (t == null) return@AnimatedVisibility
        Column(
            Modifier
                .fillMaxWidth()
                .background(MorfoColors.Glass)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(horizontal = 16.dp)
                .padding(top = if (compact) 6.dp else 12.dp, bottom = 4.dp),
        ) {
            if (!compact) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${t.tooth.fdi}", style = MaterialTheme.typography.titleMedium, color = MorfoColors.Accent)
                    Text(
                        "  ${t.tooth.name(lang)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MorfoColors.TextMuted,
                        maxLines = 1,
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = if (compact) 4.dp else 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (compact) {
                    Text("${t.tooth.fdi}", Modifier.padding(end = 6.dp), style = MaterialTheme.typography.titleMedium, color = MorfoColors.Accent)
                }
                Pill(s.info) { onInfo(t.key) }
                Pill(s.resetRotation) { workspace.resetOrientation(t.key) }
                Pill(s.resetPosition) { workspace.resetPosition(t.key) }
                Pill(s.hide) { workspace.setVisible(t.key, false) }
                Pill(s.duplicate) { workspace.duplicate(t.key) }
                if (t.tooth.variants.size > 1) {
                    val next = if (t.variant == ModelVariant.STANDARD) ModelVariant.CARABELLI else ModelVariant.STANDARD
                    Pill(s.carabelliCusp, active = t.variant == ModelVariant.CARABELLI) { onVariant(t.key, next) }
                }
                Pill(s.delete) { workspace.remove(t.key) }
            }
        }
    }
}

@Composable
fun PresetBar(workspace: Workspace) {
    val teeth = workspace.rotationTargets().map { it.tooth }
    Box(
        Modifier
            .fillMaxWidth()
            .background(MorfoColors.Glass)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.Center,
    ) {
        PresetRow(workspace, Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp))
    }
}

/** The five preset buttons in one row, without background or insets (for use inside other panels). */
@Composable
fun PresetRow(workspace: Workspace, modifier: Modifier = Modifier) {
    val teeth = workspace.rotationTargets().map { it.tooth }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        for (preset in SurfacePreset.entries) {
            Column(
                Modifier
                    .weight(1f)
                    // Fixed height: a label that shrinks to fit must not make its button shorter.
                    .height(52.dp)
                    .background(MorfoColors.SurfaceRaised, PillShape)
                    .clickable(enabled = teeth.isNotEmpty()) { workspace.applyPreset(preset) }
                    .padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { PresetLabel(preset, teeth) }
        }
    }
}

/** Primary term (shrinks to fit narrow buttons) with the secondary term underneath. */
@Composable
private fun PresetLabel(preset: SurfacePreset, teeth: List<Tooth>) {
    val lang = LocalLanguage.current
    if (lang == AppLanguage.EN) {
        BasicText(
            preset.labelEnTitle(teeth),
            maxLines = 1,
            style = TextStyle(color = MorfoColors.Text, fontWeight = FontWeight.Medium),
            autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = 12.sp),
        )
    } else {
        BasicText(
            preset.labelPl(teeth),
            maxLines = 1,
            style = TextStyle(color = MorfoColors.Text, fontWeight = FontWeight.Medium),
            autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = 12.sp),
        )
        BasicText(
            preset.labelEn(teeth),
            maxLines = 1,
            style = TextStyle(color = MorfoColors.TextFaint, letterSpacing = 0.6.sp),
            autoSize = TextAutoSize.StepBased(minFontSize = 5.sp, maxFontSize = 8.sp),
        )
    }
}

/**
 * Vertical preset buttons along the right edge (landscape phone). The column spans the full screen
 * height and the five buttons share it evenly, so they always fit without scrolling.
 */
@Composable
fun PresetColumn(workspace: Workspace, modifier: Modifier = Modifier) {
    val teeth = workspace.rotationTargets().map { it.tooth }
    Column(
        modifier
            .fillMaxHeight()
            .background(MorfoColors.Glass)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Vertical))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        for (preset in SurfacePreset.entries) {
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(max = 64.dp)
                    .width(104.dp)
                    .background(MorfoColors.SurfaceRaised, PillShape)
                    .clickable(enabled = teeth.isNotEmpty()) { workspace.applyPreset(preset) }
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { PresetLabel(preset, teeth) }
        }
    }
}

@Composable
fun Pill(
    text: String,
    active: Boolean = false,
    faded: Boolean = false,
    strike: Boolean = false,
    onClick: () -> Unit,
) {
    Text(
        text,
        modifier = Modifier
            .alpha(if (faded) 0.45f else 1f)
            .background(if (active) MorfoColors.Accent else MorfoColors.SurfaceRaised, PillShape)
            .border(1.dp, if (active) MorfoColors.Accent else Color.Transparent, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = if (active) MorfoColors.OnAccent else MorfoColors.Text,
        style = MaterialTheme.typography.labelMedium,
        textDecoration = if (strike) TextDecoration.LineThrough else null,
        maxLines = 1,
    )
}
