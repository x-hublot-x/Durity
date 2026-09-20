package com.example.project1.api

import android.content.Context
import com.example.project1.BuildConfig
import com.example.project1.data.storage.AiTestManager
import com.example.project1.data.storage.UnblockEssayStorage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class EssayValidationResult(
    val isValid: Boolean,
    val feedback: String,
    val wordCount: Int
)

object AiEssayValidator {

    fun countWords(text: String): Int {
        return text.trim().split(Regex("[\\s\\p{Punct}&&[^-]]+")).filter { it.isNotBlank() }.size
    }

    private fun checkDuplicateSimilarity(newText: String, pastEssays: List<String>): Boolean {
        if (pastEssays.isEmpty()) return false
        val newWords = newText.lowercase().split(Regex("\\s+")).filter { it.length > 2 }.toSet()
        if (newWords.isEmpty()) return false

        for (past in pastEssays) {
            val pastWords = past.lowercase().split(Regex("\\s+")).filter { it.length > 2 }.toSet()
            if (pastWords.isEmpty()) continue
            val intersection = newWords.intersect(pastWords).size
            val union = newWords.union(pastWords).size
            val similarity = if (union > 0) intersection.toFloat() / union.toFloat() else 0f
            if (similarity > 0.65f) {
                return true
            }
        }
        return false
    }

    suspend fun validateEssay(
        context: Context,
        appName: String,
        essayText: String
    ): EssayValidationResult = withContext(Dispatchers.IO) {
        val wordCount = countWords(essayText)

        if (wordCount < 100) {
            return@withContext EssayValidationResult(
                isValid = false,
                feedback = "В сочинении должно быть не менее 100 слов. Сейчас написано: $wordCount. Пожалуйста, раскройте мысль глубже.",
                wordCount = wordCount
            )
        }

        val pastEssays = UnblockEssayStorage.getPastEssays(context)
        if (checkDuplicateSimilarity(essayText, pastEssays)) {
            return@withContext EssayValidationResult(
                isValid = false,
                feedback = "ИИ обнаружил, что этот текст почти идентичен вашему предыдущему сочинению. Копирование и шаблоны запрещены — напишите искреннее новое сочинение.",
                wordCount = wordCount
            )
        }

        val personality = AiTestManager.savedPersonality.ifBlank { "Пользователь, стремящийся к осознанному использованию времени." }
        val userName = AiTestManager.savedName.ifBlank { "Пользователь" }

        val prompt = """
Ты — строгий и мудрый персональный ИИ-наставник по цифровой осознанности и тайм-менеджменту.
Пользователь ($userName) исчерпал лимит времени в приложении "$appName" и пытается его разблокировать.
Для разблокировки он обязан написать искреннее осмысленное сочинение объемом не менее 100 слов на тему:
"Что полезного я извлек из данного приложения и зачем мне дальше в нем сидеть?"

ПРОФИЛЬ ЛИЧНОСТИ И СКЛОННОСТЕЙ ПОЛЬЗОВАТЕЛЯ:
$personality

СОЧИНЕНИЕ ПОЛЬЗОВАТЕЛЯ (слов: $wordCount):
\"\"\"$essayText\"\"\"

ТВОЯ ЗАДАЧА:
1. Оценить искренность, глубину рефлексии и обоснованность ответа.
2. Проверить, действительно ли пользователь извлек пользу и есть ли веская, конкретная причина продолжать сидеть в приложении "$appName", учитывая его психологический профиль.
3. Категорически отклонять (valid = false):
   - Бессмысленный набор слов, тавтологии, "воду" и бессодержательные отговорки.
   - Сарказм, насмешки над правилами или формальные шаблонные отписки.
   - Очевидные попытки просто набить счетчик 100 слов повторяющимися фразами.
4. Одобрять (valid = true) только в случае, если текст честный, конструктивный, осознанный и показывает реальное понимание своих действий.
5. Если отказываешь (valid = false), дай честное, четкое и конструктивное объяснение (на русском языке), почему текст не принят и над чем пользователю нужно подумать перед следующей попыткой.

ОТВЕТЬ ИСКЛЮЧИТЕЛЬНО В ФОРМАТЕ ЧИСТОГО JSON БЕЗ Markdown разметки:
{
  "valid": true или false,
  "feedback": "Пояснение для пользователя"
}
""".trimIndent()

        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-3.5-flash-lite",
                apiKey = BuildConfig.GEMINI_API_KEY
            )
            val response: GenerateContentResponse = generativeModel.generateContent(prompt)
            val rawText = response.text ?: ""
            val jsonStr = rawText.replace("```json", "").replace("```", "").trim()
            val json = JSONObject(jsonStr)
            val valid = json.optBoolean("valid", false)
            val feedback = json.optString("feedback", "Не удалось проанализировать сочинение.")

            if (valid) {
                UnblockEssayStorage.saveEssay(context, essayText)
            }

            EssayValidationResult(
                isValid = valid,
                feedback = feedback,
                wordCount = wordCount
            )
        } catch (e: Exception) {
            // Если возникла сетевая ошибка
            EssayValidationResult(
                isValid = false,
                feedback = "Ошибка связи с ИИ при проверке: ${e.localizedMessage ?: "проверьте интернет"}. Попробуйте отправить снова.",
                wordCount = wordCount
            )
        }
    }
}
