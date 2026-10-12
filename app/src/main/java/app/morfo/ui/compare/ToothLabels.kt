package app.morfo.ui.compare

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.morfo.dentition.ModelVariant
import app.morfo.render.ViewportRenderer
import app.morfo.ui.theme.MorfoColors
import app.morfo.workspace.LabelMode
import app.morfo.workspace.Workspace
import kotlin.math.roundToInt

/** Small labels centred under each tooth; positions come from the renderer every frame. */
@Composable
fun ToothLabels(workspace: Workspace, renderer: ViewportRenderer, mode: LabelMode, modifier: Modifier = Modifier) {
    if (mode == LabelMode.NONE) return
    Box(modifier.fillMaxSize()) {
        for (t in workspace.teeth) {
            if (!t.visible) continue
            val anchor = renderer.labelAnchors[t.key] ?: continue
            val selected = t.key == workspace.selectedKey
            val name = when (mode) {
                LabelMode.FDI_EN -> t.tooth.nameEn
                LabelMode.FDI_PL -> t.tooth.namePl
                else -> null
            }
            Column(
                Modifier
                    .widthIn(max = 150.dp)
                    .layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(p.width, p.height) {
                            p.place((anchor.x - p.width / 2f).roundToInt(), (anchor.y + 6.dp.toPx()).roundToInt())
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val variant = if (t.variant == ModelVariant.CARABELLI) " · Carabelli" else ""
                Text(
                    "${t.tooth.fdi}$variant",
                    color = if (selected) MorfoColors.Accent else MorfoColors.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (name != null) {
                    Text(
                        name,
                        color = if (selected) MorfoColors.Accent.copy(alpha = 0.8f) else MorfoColors.TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
