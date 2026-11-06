package com.feelime.ime.engine

import android.content.Context
import org.json.JSONObject

/**
 * 本机联想习惯：记下「上一个上屏词 → 这次接着打的词」。
 * 下次联想时这些词排在语料表前面。只留在本机，不上传。
 * 最多 80 个前词、每个 4 个后继，避免偏好文件无限长大。
 */
class AssocHabit(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("feelime_assoc_habit", Context.MODE_PRIVATE)

    fun note(previous: String, next: String) {
        val key = anchor(previous) ?: return
        val word = next.trim()
        if (word.isEmpty() || word.length > 8) return
        if (word.any { it !in '\u4E00'..'\u9FFF' }) return
        val root = read()
        val list = root.optJSONArray(key)?.let { array ->
            (0 until array.length()).map { array.optString(it) }.filter { it.isNotEmpty() }
        }?.toMutableList() ?: mutableListOf()
        list.remove(word)
        list.add(0, word)
        while (list.size > 4) list.removeAt(list.lastIndex)
        root.put(key, org.json.JSONArray(list))
        trim(root)
        prefs.edit().putString(KEY, root.toString()).apply()
    }

    fun promote(committed: String, base: List<String>): List<String> {
        val key = anchor(committed) ?: return base
        val learned = read().optJSONArray(key)?.let { array ->
            (0 until array.length()).map { array.optString(it) }.filter { it.isNotEmpty() }
        }.orEmpty()
        if (learned.isEmpty()) return base
        val rest = base.filter { it !in learned }
        return (learned + rest).distinct().take(8)
    }

    private fun anchor(text: String): String? {
        val han = text.trim().filter { it in '\u4E00'..'\u9FFF' }
        if (han.isEmpty()) return null
        return han.takeLast(4)
    }

    private fun read(): JSONObject = runCatching {
        JSONObject(prefs.getString(KEY, "{}") ?: "{}")
    }.getOrDefault(JSONObject())

    private fun trim(root: JSONObject) {
        val keys = root.keys().asSequence().toList()
        if (keys.size <= 80) return
        keys.take(keys.size - 80).forEach { root.remove(it) }
    }

    private companion object {
        const val KEY = "pairs"
    }
}
