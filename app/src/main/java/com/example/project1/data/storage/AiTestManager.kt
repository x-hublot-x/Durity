package com.example.project1.data.storage

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AiTestManager {
    private const val PREFS_NAME = "ai_test_prefs"
    private const val KEY_COMPLETED = "ai_test_completed"
    private const val KEY_PERSONALITY = "ai_personality"
    private const val KEY_RECOMMENDATIONS = "ai_recommendations"
    private const val KEY_NAME = "ai_user_name"

    var isTestCompleted by mutableStateOf(false)
        private set

    var savedPersonality by mutableStateOf("")
        private set

    var savedName by mutableStateOf("")
        private set

    var savedRecommendations by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isTestCompleted = prefs.getBoolean(KEY_COMPLETED, false)
        savedPersonality = prefs.getString(KEY_PERSONALITY, "") ?: ""
        val loadedName = prefs.getString(KEY_NAME, "") ?: ""
        savedName = if (loadedName.isNotBlank()) loadedName else {
            // Пытаемся извлечь имя из personality (формат "Личность: ИМЯ (возраст лет)")
            val personality = prefs.getString(KEY_PERSONALITY, "") ?: ""
            val nameFromPersonality = Regex("Личность:\\s*([^,(]+)").find(personality)
                ?.groupValues?.getOrNull(1)?.trim() ?: ""
            nameFromPersonality
        }

        val recsString = prefs.getString(KEY_RECOMMENDATIONS, "") ?: ""
        if (recsString.isNotBlank()) {
            savedRecommendations = recsString.split(";").mapNotNull {
                val parts = it.split("=")
                if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
            }.toMap()
        }
    }

    fun saveResults(context: Context, personality: String, recs: Map<String, Int>, name: String = "") {
        isTestCompleted = true
        savedPersonality = personality
        savedRecommendations = recs
        savedName = name

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val recsString = recs.map { "${it.key}=${it.value}" }.joinToString(";")
        prefs.edit()
            .putBoolean(KEY_COMPLETED, true)
            .putString(KEY_PERSONALITY, personality)
            .putString(KEY_RECOMMENDATIONS, recsString)
            .putString(KEY_NAME, name)
            .apply()
    }

    fun resetTest(context: Context) {
        isTestCompleted = false
        savedPersonality = ""
        savedName = ""
        savedRecommendations = emptyMap()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
