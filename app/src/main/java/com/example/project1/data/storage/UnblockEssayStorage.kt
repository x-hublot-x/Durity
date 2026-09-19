package com.example.project1.data.storage

import android.content.Context
import org.json.JSONArray

object UnblockEssayStorage {
    private const val PREFS_NAME = "unblock_essay_prefs"
    private const val KEY_PAST_ESSAYS = "past_essays_history"
    private const val KEY_LOCKOUT_PREFIX = "lockout_until_"

    fun getPastEssays(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PAST_ESSAYS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveEssay(context: Context, essay: String) {
        val past = getPastEssays(context).toMutableList()
        past.add(0, essay.trim())
        // Храним последние 20 сочинений
        val trimmed = past.take(20)
        val jsonArray = JSONArray()
        trimmed.forEach { jsonArray.put(it) }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_PAST_ESSAYS, jsonArray.toString())
            .apply()
    }

    fun setLockout(context: Context, packageName: String, durationMs: Long = 3600_000L) {
        val until = System.currentTimeMillis() + durationMs
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LOCKOUT_PREFIX + packageName, until)
            .apply()
    }

    fun getLockoutRemainingMinutes(context: Context, packageName: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val until = prefs.getLong(KEY_LOCKOUT_PREFIX + packageName, 0L)
        val diff = until - System.currentTimeMillis()
        return if (diff > 0) {
            val mins = (diff / 60_000L).toInt() + 1
            mins
        } else {
            0
        }
    }

    fun isLocked(context: Context, packageName: String): Boolean {
        return getLockoutRemainingMinutes(context, packageName) > 0
    }
}
