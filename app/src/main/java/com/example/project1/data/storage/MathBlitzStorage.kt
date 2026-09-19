package com.example.project1.data.storage

import android.content.Context
import com.example.project1.data.model.BlitzCategory
import com.example.project1.data.model.BlitzConfig
import com.example.project1.data.model.BlitzSessionState
import com.example.project1.data.model.BlitzTask
import org.json.JSONArray
import org.json.JSONObject

object MathBlitzStorage {
    private const val PREFS_NAME = "math_blitz_prefs"
    private const val KEY_SESSION = "active_session_json"

    fun getActiveSession(context: Context): BlitzSessionState? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return try {
            val json = JSONObject(raw)
            val cfgJson = json.getJSONObject("config")
            val config = BlitzConfig(
                category = BlitzCategory.valueOf(cfgJson.getString("category")),
                difficulty = cfgJson.getInt("difficulty"),
                durationMinutes = cfgJson.getInt("durationMinutes"),
                taskCount = cfgJson.getInt("taskCount"),
                betCoins = cfgJson.getInt("betCoins")
            )

            val tasksArr = json.getJSONArray("tasks")
            val tasks = (0 until tasksArr.length()).map { i ->
                val tObj = tasksArr.getJSONObject(i)
                BlitzTask(
                    id = tObj.getString("id"),
                    category = tObj.getString("category"),
                    latexStatement = tObj.getString("latexStatement"),
                    correctAnswer = tObj.getString("correctAnswer"),
                    hint = tObj.optString("hint", "")
                )
            }

            BlitzSessionState(
                id = json.getString("id"),
                config = config,
                startTime = json.getLong("startTime"),
                deadlineTime = json.getLong("deadlineTime"),
                potentialWinCoins = json.getInt("potentialWinCoins"),
                tasks = tasks,
                currentTaskIndex = json.getInt("currentTaskIndex"),
                isFinished = json.getBoolean("isFinished"),
                isWon = json.getBoolean("isWon")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveActiveSession(context: Context, state: BlitzSessionState?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (state == null) {
            prefs.edit().remove(KEY_SESSION).apply()
            return
        }

        val json = JSONObject().apply {
            put("id", state.id)
            put("startTime", state.startTime)
            put("deadlineTime", state.deadlineTime)
            put("potentialWinCoins", state.potentialWinCoins)
            put("currentTaskIndex", state.currentTaskIndex)
            put("isFinished", state.isFinished)
            put("isWon", state.isWon)

            val cfgObj = JSONObject().apply {
                put("category", state.config.category.name)
                put("difficulty", state.config.difficulty)
                put("durationMinutes", state.config.durationMinutes)
                put("taskCount", state.config.taskCount)
                put("betCoins", state.config.betCoins)
            }
            put("config", cfgObj)

            val tasksArr = JSONArray()
            state.tasks.forEach { t ->
                tasksArr.put(JSONObject().apply {
                    put("id", t.id)
                    put("category", t.category)
                    put("latexStatement", t.latexStatement)
                    put("correctAnswer", t.correctAnswer)
                    put("hint", t.hint)
                })
            }
            put("tasks", tasksArr)
        }

        prefs.edit().putString(KEY_SESSION, json.toString()).apply()
    }

    fun isTimeoutNotified(context: Context, sessionId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("timeout_notified_$sessionId", false)
    }

    fun setTimeoutNotified(context: Context, sessionId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("timeout_notified_$sessionId", true).apply()
    }

    fun clearSession(context: Context) {
        saveActiveSession(context, null)
    }
}
