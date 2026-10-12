package app.morfo

import android.content.Context
import app.morfo.i18n.AppLanguage
import app.morfo.workspace.LabelMode
import app.morfo.workspace.LightingMode
import app.morfo.workspace.ScaleMode

/** Small persisted view preferences. */
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("morfo", Context.MODE_PRIVATE)

    var appLanguage: AppLanguage
        get() = enumOr(prefs.getString("language", null), AppLanguage.PL)
        set(v) = prefs.edit().putString("language", v.name).apply()

    var labelMode: LabelMode
        get() = enumOr(prefs.getString("labels", null), LabelMode.FDI_PL)
        set(v) = prefs.edit().putString("labels", v.name).apply()

    var lightingMode: LightingMode
        get() = enumOr(prefs.getString("lighting", null), LightingMode.CLINICAL)
        set(v) = prefs.edit().putString("lighting", v.name).apply()

    var scaleMode: ScaleMode
        get() = enumOr(prefs.getString("scale", null), ScaleMode.ANATOMICAL)
        set(v) = prefs.edit().putString("scale", v.name).apply()

    var spacingMm: Float
        get() = prefs.getFloat("spacing", 4f)
        set(v) = prefs.edit().putFloat("spacing", v).apply()

    var mirrorSync: Boolean
        get() = prefs.getBoolean("mirrorSync", true)
        set(v) = prefs.edit().putBoolean("mirrorSync", v).apply()

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
