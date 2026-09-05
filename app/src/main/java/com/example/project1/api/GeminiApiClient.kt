package com.example.project1.api

import com.example.project1.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("unused", "RedundantSuppression")
class GeminiApiClient {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun sendMessage(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val response: GenerateContentResponse = generativeModel.generateContent(prompt)
            response.text ?: "Пустой ответ"
        } catch (e: Exception) {
            "Ошибка API: ${e.localizedMessage}"
        }
    }
}
