package app.morfo.ui.selector

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.morfo.dentition.Dentition
import app.morfo.dentition.Tooth
import app.morfo.dentition.ToothClass
import app.morfo.ui.theme.MorfoColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Permanent dentition chart in the clinician's view: patient's right on the left,
 * maxillary arch on top (incisors up), mandibular arch below (incisors down).
 */
@Composable
fun ArchChart(
    isSelected: (Tooth) -> Boolean,
    isDimmed: (Tooth) -> Boolean,
    onToggle: (Tooth) -> Unit,
    modifier: Modifier = Modifier,
    /** Optional fill colour per tooth (overrides the selection colours), e.g. accuracy in quiz statistics. */
    colorFor: ((Tooth) -> Color?)? = null,
) {
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(0.92f)) {
        val density = LocalDensity.current
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        // Chips are sized for a ~340 dp wide chart and scale with it (small landscape phone, big tablet).
        val chipScale = (maxWidth / 340.dp).coerceIn(0.6f, 1.3f)
        val upper = remember { archPositions(Dentition.upperRow) }
        val lower = remember { archPositions(Dentition.lowerRow) }

        val s = app.morfo.i18n.LocalStrings.current
        ArchCaption(s.maxillaryArch, Modifier.align(Alignment.Center).offset(y = (-58).dp))
        ArchCaption(s.mandibularArch, Modifier.align(Alignment.Center).offset(y = 58.dp))
        ArchCaption(s.rightSideShort, Modifier.align(Alignment.CenterStart).offset(x = 4.dp))
        ArchCaption(s.leftSideShort, Modifier.align(Alignment.CenterEnd).offset(x = (-4).dp))

        val rx = w * 0.44f
        val ry = h * 0.39f
        val cx = w / 2
        for ((tooth, angle) in upper) {
            val x = cx + rx * cos(angle)
            val y = h * 0.47f - ry * sin(angle)
            ToothChip(tooth, x, y, chipScale, isSelected(tooth), isDimmed(tooth), colorFor?.invoke(tooth)) { onToggle(tooth) }
        }
        for ((tooth, angle) in lower) {
            val x = cx + rx * cos(angle)
            val y = h * 0.53f + ry * sin(angle)
            ToothChip(tooth, x, y, chipScale, isSelected(tooth), isDimmed(tooth), colorFor?.invoke(tooth)) { onToggle(tooth) }
        }
    }
}

@Composable
private fun ArchCaption(text: String, modifier: Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelSmall, color = MorfoColors.TextFaint)
}

@Composable
private fun ToothChip(tooth: Tooth, x: Float, y: Float, scale: Float, selected: Boolean, dimmed: Boolean, custom: Color?, onClick: () -> Unit) {
    val size = chipSize(tooth) * scale
    val half = with(LocalDensity.current) { size.toPx() / 2 }
    val fill by animateColorAsState(custom ?: if (selected) MorfoColors.Accent else MorfoColors.Surface, label = "fill")
    val text by animateColorAsState(if (selected || custom != null) MorfoColors.OnAccent else MorfoColors.Text, label = "text")
    Box(
        Modifier
            .offset { IntOffset((x - half).roundToInt(), (y - half).roundToInt()) }
            .size(size)
            .alpha(if (dimmed && !selected) 0.28f else 1f)
            .background(fill, RoundedCornerShape(38))
            .border(1.dp, custom ?: if (selected) MorfoColors.Accent else MorfoColors.Outline, RoundedCornerShape(38))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(tooth.fdi.toString(), color = text, fontSize = 11.sp * scale, fontWeight = FontWeight.SemiBold)
    }
}

private fun chipSize(tooth: Tooth): Dp = when (tooth.toothClass) {
    ToothClass.MOLAR -> 31.dp
    ToothClass.PREMOLAR -> 27.dp
    ToothClass.CANINE -> 26.dp
    ToothClass.INCISOR -> 25.dp
}

/** Spreads the 16 teeth of a row along a half-ellipse, spacing proportional to tooth width. */
private fun archPositions(row: List<Tooth>): List<Pair<Tooth, Float>> {
    val widths = row.map { chipSize(it).value }
    val total = widths.sum()
    var acc = 0f
    return row.mapIndexed { i, tooth ->
        val mid = acc + widths[i] / 2
        acc += widths[i]
        // From the left end of the arch (angle π) over the front (π/2) to the right end (0).
        tooth to (PI * (1 - (0.02 + 0.96 * mid / total))).toFloat()
    }
}
