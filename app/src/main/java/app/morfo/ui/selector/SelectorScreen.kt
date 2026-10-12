package app.morfo.ui.selector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.morfo.AppViewModel
import app.morfo.dentition.Arch
import app.morfo.dentition.Dentition
import app.morfo.dentition.QuickComparisons
import app.morfo.dentition.Side
import app.morfo.dentition.Tooth
import app.morfo.dentition.ToothClass
import app.morfo.i18n.LocalLanguage
import app.morfo.i18n.LocalStrings
import app.morfo.ui.compare.Pill
import app.morfo.ui.compare.ViewSettingsSheet
import app.morfo.ui.theme.MorfoColors

/** Filter state shared by the chart (dimming) and the filter chips. */
private class ToothFilters {
    val classes = mutableStateListOf<ToothClass>()
    val arches = mutableStateListOf<Arch>()
    val sides = mutableStateListOf<Side>()
    val active get() = classes.isNotEmpty() || arches.isNotEmpty() || sides.isNotEmpty()
    fun matches(t: Tooth) = (classes.isEmpty() || t.toothClass in classes) &&
        (arches.isEmpty() || t.arch in arches) && (sides.isEmpty() || t.side in sides)
    fun clear() { classes.clear(); arches.clear(); sides.clear() }
}

@Composable
fun SelectorScreen(vm: AppViewModel) {
    val filters = remember { ToothFilters() }
    var showSettings by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize().background(MorfoColors.Background)) {
        // Landscape (phone or tablet): chart on the left, filters and shortcuts on the right.
        val landscape = maxWidth > maxHeight && maxWidth >= 600.dp
        if (landscape) {
            Row(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 104.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                // The whole chart fits the height (the header lives in the right column).
                val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val chartHeight = this@BoxWithConstraints.maxHeight - statusBar - 116.dp
                val chartWidth = (chartHeight * 0.92f).coerceIn(220.dp, 560.dp)
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Chart(vm, filters, Modifier.widthIn(max = chartWidth))
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Header(vm) { showSettings = true }
                    FiltersSection(vm, filters)
                    QuickSection(vm)
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 112.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Tablet in portrait: same single column, just not stretched.
                Column(Modifier.widthIn(max = 640.dp)) {
                    Header(vm) { showSettings = true }
                    Chart(vm, filters, Modifier)
                    FiltersSection(vm, filters)
                    QuickSection(vm)
                }
            }
        }

        BottomBar(vm, Modifier.align(Alignment.BottomCenter))
        if (showSettings) ViewSettingsSheet(vm, onDismiss = { showSettings = false })
    }
}

@Composable
private fun Header(vm: AppViewModel, onOpenSettings: () -> Unit) {
    val s = LocalStrings.current
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(s.appName, style = MaterialTheme.typography.headlineSmall)
            Text(s.selectTeethSubtitle, style = MaterialTheme.typography.bodyMedium, color = MorfoColors.TextMuted)
        }
        Pill(s.quiz, active = true, onClick = vm::openQuizHome)
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = s.settings, tint = MorfoColors.TextMuted)
        }
        IconButton(onClick = vm::openAbout) {
            Icon(Icons.Outlined.Info, contentDescription = s.about, tint = MorfoColors.TextMuted)
        }
    }
}

@Composable
private fun Chart(vm: AppViewModel, filters: ToothFilters, modifier: Modifier) {
    ArchChart(
        isSelected = { t -> vm.selection.any { it.fdi == t.fdi } },
        isDimmed = { t -> filters.active && !filters.matches(t) },
        onToggle = { vm.toggle(it.fdi) },
        modifier = modifier.padding(top = 8.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersSection(vm: AppViewModel, filters: ToothFilters) {
    val s = LocalStrings.current
    val lang = LocalLanguage.current
    SectionTitle(s.filters)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToothClass.entries.forEach { c -> Toggle(c.label(lang), c in filters.classes) { filters.classes.toggle(c) } }
        Arch.entries.forEach { a -> Toggle(a.label(lang), a in filters.arches) { filters.arches.toggle(a) } }
        Side.entries.forEach { side -> Toggle(side.label(lang), side in filters.sides) { filters.sides.toggle(side) } }
    }
    Row(Modifier.padding(top = 4.dp)) {
        TextButton(
            onClick = { vm.addAll(Dentition.permanent.filter(filters::matches).map { it.fdi }) },
            enabled = filters.active,
        ) { Text(s.selectMatching) }
        TextButton(onClick = filters::clear, enabled = filters.active) { Text(s.clearFilters) }
    }
}

@Composable
private fun QuickSection(vm: AppViewModel) {
    val s = LocalStrings.current
    val lang = LocalLanguage.current
    SectionTitle(s.quickComparisons)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickComparisons.all.forEach { q ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MorfoColors.Surface, RoundedCornerShape(14.dp))
                    .clickable { vm.openQuick(q) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(q.title(lang), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(
                    q.teeth.joinToString(" · ") { it.fdi.toString() + if (it.variant.suffix.isNotEmpty()) "C" else "" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MorfoColors.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun BottomBar(vm: AppViewModel, modifier: Modifier) {
    val s = LocalStrings.current
    val count = vm.selection.size
    Row(
        modifier
            .fillMaxWidth()
            .background(MorfoColors.Glass)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (count == 0) s.noneSelected else vm.selection.sortedBy { it.fdi }.joinToString(" · ") { it.fdi.toString() },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            if (count > 0) {
                Text(
                    s.clear,
                    Modifier.clickable { vm.clearSelection() }.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MorfoColors.TextMuted,
                )
            }
        }
        Button(
            onClick = vm::openCompare,
            enabled = count > 0,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = MorfoColors.SurfaceRaised,
                disabledContentColor = MorfoColors.TextFaint,
            ),
        ) {
            Text(if (count > 0) s.compareCount(count) else s.compare)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MorfoColors.TextMuted)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Toggle(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(10.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MorfoColors.Surface,
            labelColor = MorfoColors.Text,
            selectedContainerColor = MorfoColors.Accent,
            selectedLabelColor = MorfoColors.OnAccent,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MorfoColors.Outline,
            selectedBorderColor = MorfoColors.Accent,
        ),
    )
}

private fun <T> MutableList<T>.toggle(item: T) {
    if (!remove(item)) add(item)
}
