package app.morfo.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Minimal clinical palette: graphite neutrals, one enamel-ivory accent. */
object MorfoColors {
    val Background = Color(0xFF111214)
    val Surface = Color(0xFF18191C)
    val SurfaceRaised = Color(0xFF202125)
    val Outline = Color(0xFF2E2F33)
    val Text = Color(0xFFECEAE6)
    val TextMuted = Color(0xFF8F8D89)
    val TextFaint = Color(0xFF5E5D5A)
    val Accent = Color(0xFFE9DFC8)
    val OnAccent = Color(0xFF16140F)
    val Glass = Color(0xEB141517)
}

private val scheme = darkColorScheme(
    primary = MorfoColors.Accent,
    onPrimary = MorfoColors.OnAccent,
    secondary = MorfoColors.Accent,
    onSecondary = MorfoColors.OnAccent,
    background = MorfoColors.Background,
    onBackground = MorfoColors.Text,
    surface = MorfoColors.Surface,
    onSurface = MorfoColors.Text,
    surfaceVariant = MorfoColors.SurfaceRaised,
    onSurfaceVariant = MorfoColors.TextMuted,
    surfaceContainer = MorfoColors.Surface,
    surfaceContainerHigh = MorfoColors.SurfaceRaised,
    surfaceContainerHighest = MorfoColors.SurfaceRaised,
    surfaceContainerLow = MorfoColors.Surface,
    outline = MorfoColors.Outline,
    outlineVariant = MorfoColors.Outline,
)

private val typography = Typography(
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
)

@Composable
fun MorfoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography) {
        CompositionLocalProvider(LocalContentColor provides MorfoColors.Text, content = content)
    }
}
