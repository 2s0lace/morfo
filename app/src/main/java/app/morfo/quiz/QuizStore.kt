package app.morfo.quiz

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persists quiz settings and learning progress (small JSON blobs in SharedPreferences). */
class QuizStore(context: Context) {
    private val prefs = context.getSharedPreferences("morfo_quiz", Context.MODE_PRIVATE)

    fun loadSettings(): QuizSettings {
        val d = QuizSettings()
        return QuizSettings(
            mode = enumOr(prefs.getString("mode", null), d.mode),
            scope = enumOr(prefs.getString("scope", null), d.scope),
            difficulty = enumOr(prefs.getString("difficulty", null), d.difficulty),
            roundLength = prefs.getInt("roundLength", d.roundLength ?: 0).takeIf { it > 0 },
            uniformColor = prefs.getBoolean("uniformColor", d.uniformColor),
            ignoreSide = prefs.getBoolean("ignoreSide", d.ignoreSide),
        )
    }

    fun saveSettings(s: QuizSettings) {
        prefs.edit()
            .putString("mode", s.mode.name)
            .putString("scope", s.scope.name)
            .putString("difficulty", s.difficulty.name)
            .putInt("roundLength", s.roundLength ?: 0)
            .putBoolean("uniformColor", s.uniformColor)
            .putBoolean("ignoreSide", s.ignoreSide)
            .apply()
    }

    fun loadProgress(): QuizProgress {
        val p = QuizProgress()
        val raw = prefs.getString("progress", null) ?: return p
        runCatching {
            val o = JSONObject(raw)
            p.step = o.optLong("step")
            p.archErrors = o.optInt("archErrors")
            p.sideErrors = o.optInt("sideErrors")
            p.kindErrors = o.optInt("kindErrors")
            o.optJSONObject("stats")?.let { s ->
                s.keys().forEach { k ->
                    val a = s.getJSONArray(k)
                    p.stats[k.toInt()] = QuizProgress.ToothStats(a.getInt(0), a.getInt(1))
                }
            }
            o.optJSONObject("cards")?.let { c ->
                c.keys().forEach { k ->
                    val a = c.getJSONArray(k)
                    p.cards[k.toInt()] = QuizProgress.Card(a.getInt(0), a.getLong(1), a.getInt(2), a.getInt(3))
                }
            }
            o.optJSONArray("confusions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val a = arr.getJSONArray(i)
                    p.confusions[a.getInt(0) to a.getInt(1)] = a.getInt(2)
                }
            }
        }
        return p
    }

    fun saveProgress(p: QuizProgress) {
        val o = JSONObject()
            .put("step", p.step)
            .put("archErrors", p.archErrors)
            .put("sideErrors", p.sideErrors)
            .put("kindErrors", p.kindErrors)
            .put("stats", JSONObject().apply {
                p.stats.forEach { (fdi, s) -> put(fdi.toString(), JSONArray(listOf(s.attempts, s.correct))) }
            })
            .put("cards", JSONObject().apply {
                p.cards.forEach { (fdi, c) -> put(fdi.toString(), JSONArray(listOf(c.level, c.due, c.reviews, c.lapses))) }
            })
            .put("confusions", JSONArray().apply {
                p.confusions.forEach { (pair, n) -> put(JSONArray(listOf(pair.first, pair.second, n))) }
            })
        prefs.edit().putString("progress", o.toString()).apply()
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
