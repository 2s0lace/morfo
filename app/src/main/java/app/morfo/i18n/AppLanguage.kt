package app.morfo.i18n

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val label: String) {
    PL("pl", "Polski"),
    EN("en", "English"),
}

val LocalLanguage = staticCompositionLocalOf { AppLanguage.PL }
