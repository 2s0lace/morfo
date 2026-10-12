package app.morfo

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import app.morfo.ui.about.AboutScreen
import app.morfo.ui.compare.CompareScreen
import app.morfo.ui.quiz.QuizHomeScreen
import app.morfo.ui.quiz.QuizScreen
import app.morfo.ui.quiz.QuizStatsScreen
import app.morfo.ui.selector.SelectorScreen
import app.morfo.ui.theme.MorfoTheme

import androidx.compose.runtime.CompositionLocalProvider
import app.morfo.i18n.LocalLanguage
import app.morfo.i18n.LocalStrings
import app.morfo.i18n.getStrings

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        applyDebugOrientation(intent)
        setContent {
            MorfoTheme {
                CompositionLocalProvider(
                    LocalLanguage provides vm.appLanguage,
                    LocalStrings provides getStrings(vm.appLanguage),
                ) {
                    Crossfade(vm.screen, label = "screen") { screen ->
                        when (screen) {
                            Screen.SELECTOR -> SelectorScreen(vm)
                            Screen.COMPARE -> CompareScreen(vm)
                            Screen.ABOUT -> AboutScreen(onBack = vm::closeAbout)
                            Screen.QUIZ_HOME -> QuizHomeScreen(vm)
                            Screen.QUIZ -> QuizScreen(vm)
                            Screen.QUIZ_STATS -> QuizStatsScreen(vm)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyDebugOrientation(intent)
    }

    /**
     * Debug builds only: `adb shell am start -n app.morfo/.MainActivity --es orientation landscape|portrait|auto`
     * forces an orientation for testing without touching the device's rotation settings.
     */
    private fun applyDebugOrientation(intent: Intent?) {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        requestedOrientation = when (intent?.getStringExtra("orientation")) {
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "auto" -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            else -> return
        }
    }
}
