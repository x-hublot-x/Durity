package com.example.project1.data.storage

import android.content.Context
import com.example.project1.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiApiKeyManager {
    private const val PREFS = "gemini_api_key_prefs"
    private const val KEY_CUSTOM_KEY = "custom_gemini_api_key"

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val customKey = prefs.getString(KEY_CUSTOM_KEY, null)?.trim()
        if (!customKey.isNullOrEmpty()) {
            return customKey
        }
        return BuildConfig.GEMINI_API_KEY
    }

    fun getCustomKey(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_KEY, null)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun isCustomKeySet(context: Context): Boolean {
        return !getCustomKey(context).isNullOrBlank()
    }

    fun saveCustomKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val clean = key.trim()
        if (clean.isEmpty()) {
            prefs.edit().remove(KEY_CUSTOM_KEY).commit()
        } else {
            prefs.edit().putString(KEY_CUSTOM_KEY, clean).commit()
        }
    }

    fun resetToDefault(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CUSTOM_KEY).commit()
    }

    fun getMaskedKey(context: Context): String {
        val key = getApiKey(context)
        return maskKey(key)
    }

    fun maskKey(key: String): String {
        val clean = key.trim()
        if (clean.length <= 8) return "••••••••"
        val prefix = clean.take(6)
        val suffix = clean.takeLast(4)
        return "$prefix••••••••$suffix"
    }

    suspend fun testApiKey(key: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanKey = key.trim()
            if (cleanKey.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Ключ не может быть пустым"))
            }
            val model = GenerativeModel(
                modelName = "gemini-3.5-flash-lite",
                apiKey = cleanKey
            )
            val response = model.generateContent("Ответь одним словом: 'OK'")
            val text = response.text?.trim()
            if (!text.isNullOrEmpty()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Пустой ответ от Gemini API"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
