package com.example.project1.data.storage

import android.content.Context
import com.example.project1.util.AmbientSoundType
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class FocusTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
)

object FocusStorage {
    private const val PREFS = "focus_timer_prefs"
    private const val KEY_MINUTES = "saved_minutes"
    private const val KEY_SOUNDS = "saved_sounds"
    private const val KEY_STRICT = "saved_strict"
    private const val KEY_TASKS = "saved_tasks"

    fun getMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_MINUTES, 25)
    }

    fun saveMinutes(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_MINUTES, minutes).apply()
    }

    fun getSounds(context: Context): Set<AmbientSoundType> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getStringSet(KEY_SOUNDS, null) ?: return setOf(AmbientSoundType.RAIN)
        return raw.mapNotNull { name ->
            try { AmbientSoundType.valueOf(name) } catch (_: Exception) { null }
        }.toSet()
    }

    fun saveSounds(context: Context, sounds: Set<AmbientSoundType>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val names = sounds.map { it.name }.toSet()
        prefs.edit().putStringSet(KEY_SOUNDS, names).apply()
    }

    fun getStrictMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_STRICT, true)
    }

    fun saveStrictMode(context: Context, isStrict: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_STRICT, isStrict).apply()
    }

    fun getTasks(context: Context): List<FocusTask> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_TASKS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<FocusTask>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    FocusTask(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.getString("title"),
                        isCompleted = obj.optBoolean("isCompleted", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveTasks(context: Context, tasks: List<FocusTask>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (t in tasks) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("isCompleted", t.isCompleted)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_TASKS, arr.toString()).apply()
    }

    fun resetAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
