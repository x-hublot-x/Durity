package com.example.project1.api

import android.content.Context
import com.example.project1.data.model.CheckResponse
import com.example.project1.data.model.CheckResult
import com.example.project1.data.model.DailyIntegralTask
import com.example.project1.data.storage.DailyTaskStorage
import com.example.project1.service.DailyTaskNotificationManager
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

suspend fun generateDailyTaskWithAi(context: Context, apiKey: String): DailyIntegralTask = withContext(Dispatchers.IO) {
    DailyTaskStorage.getSavedTask(context)?.let { return@withContext it }

    val streak = DailyTaskStorage.getStreak(context)
    val history = DailyTaskStorage.getPreviousTasks(context)
    val types = listOf("Найти производную", "Найти интеграл", "Найти предел", "Решить ДУ")
    val chosenType = types.random()

    val prompt = """
Ты эксперт по высшей математике. Сгенерируй ОДНУ математическую задачу дня.

Параметры генерации:
- Тип задачи: $chosenType
- Текущий streak пользователя (дней подряд): $streak.
- Принцип сложности: Чем больше streak, тем сложнее задача. 
- ВАЖНО: Не повторяй задачи из этого списка ранее сгенерированных: ${history.joinToString("; ")}

Формат поля latexStatement должен содержать ТОЛЬКО математическое условие без лишнего текста и слов. Оно должно быть обернуто в двойные знаки доллара:
- Если производная, пиши: ${'$'}${'$'}f(x) = ...${'$'}${'$'}
- Если интеграл, пиши: ${'$'}${'$'}I = \int ... dx${'$'}${'$'}
- Если предел, пиши: ${'$'}${'$'}\lim_{x \to ...} ...${'$'}${'$'}
- Если ДУ, пиши что-то на примере: $${'$'}y' - \frac{y}{x} = x^2 \sin x$$ (если есть начальные условия, разделяй их через \\, например: $${'$'}y'' + y = 0 \\ y(0)=1$${'$'})

Верни СТРОГО JSON без markdown-обёрток:
{
  "type": "$chosenType",
  "latexStatement": "...",
  "correctAnswer": "Математически точный эталон ответа",
  "description": "Текстовое описание задачи для проверки нейросетью"
}
    """.trimIndent()

    try {
        val model = GenerativeModel(modelName = "gemini-3.5-flash-lite", apiKey = apiKey)
        val response = model.generateContent(prompt)
        val raw = response.text?.trim() ?: ""
        val cleaned = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val safeJson = cleaned.replace(Regex("""(?<!\\)\\to(?![a-zA-Z])"""), "\\\\to")
        val json = JSONObject(safeJson)
        val newTask = DailyIntegralTask(
            id = System.currentTimeMillis().toString().takeLast(4),
            type = json.optString("type", chosenType),
            latexStatement = json.getString("latexStatement").replace("\to", "\\to "),
            correctAnswer = json.getString("correctAnswer"),
            description = json.getString("description")
        )

        DailyTaskStorage.saveCurrentTask(context, newTask)
        DailyTaskNotificationManager.scheduleDailyNotification(context)
        newTask
    } catch (e: Exception) {
        val fallbackTask = DailyIntegralTask(
            id = "00",
            type = "Найти интеграл",
            latexStatement = "\$\$I = \\int x e^x \\, dx\$\$",
            correctAnswer = "e^x(x - 1) + C",
            description = "Вычисли интеграл: $\\int x e^x \\, dx$"
        )
        DailyTaskStorage.saveCurrentTask(context, fallbackTask)
        fallbackTask
    }
}

suspend fun checkAnswerWithAi(
    task: DailyIntegralTask,
    userAnswer: String,
    apiKey: String
): CheckResponse = withContext(Dispatchers.IO) {
    try {
        val model = GenerativeModel(
            modelName = "gemini-3.5-flash-lite",
            apiKey = apiKey
        )
        val prompt = """
Ты строгий преподаватель высшей математики. Проверь ответ студента.

Тип задачи: ${task.type}
Условие задачи: ${task.description}
Эталон ответа: ${task.correctAnswer}
Ответ студента: $userAnswer

Оцени ответ по одной из трёх категорий:
1. CORRECT — ответ верный (математически эквивалентен эталону, константа интегрирования может быть опущена или записана как +C)
2. CLOSE — ответ почти верный (небольшая арефметическая ошибка, перепутан знак или забыта константа)
3. WRONG — ответ абсолютно неверный

Ответь СТРОГО в формате JSON (без markdown):
{
  "result": "CORRECT" | "CLOSE" | "WRONG",
  "comment": "Короткий комментарий на русском (1-2 предложения). Если CLOSE — укажи на ошибку. Если WRONG — дай наводящую подсказку, не давай готовый ответ."
}
        """.trimIndent()

        val response = model.generateContent(prompt)
        val raw = response.text?.trim() ?: return@withContext CheckResponse(
            CheckResult.WRONG, "Не удалось получить ответ от ИИ."
        )

        val cleaned = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = JSONObject(cleaned)
        val resultStr = json.optString("result", "WRONG")
        val comment = json.optString("comment", "")

        val result = when (resultStr) {
            "CORRECT" -> CheckResult.CORRECT
            "CLOSE" -> CheckResult.CLOSE
            else -> CheckResult.WRONG
        }
        CheckResponse(result, comment)
    } catch (e: Exception) {
        CheckResponse(CheckResult.WRONG, "Ошибка проверки: ${e.localizedMessage}")
    }
}

suspend fun getHintForTask(task: DailyIntegralTask, apiKey: String): String = withContext(Dispatchers.IO) {
    try {
        val model = GenerativeModel(modelName = "gemini-3.5-flash-lite", apiKey = apiKey)
        val prompt = """
Ты опытный репетитор по высшей математике. Дай ПОДСКАЗКУ к задаче — ключевую мысль, которая направит ученика к правильному решению.

Тип задачи: ${task.type}
Условие: ${task.description}
Формула: ${task.latexStatement}

Требования к подсказке:
- НЕ давай готовый ответ и не показывай полное решение
- Укажи только КЛЮЧЕВУЮ ИДЕЮ или метод (например: "применить метод интегрирования по частям", "воспользоваться формулой тригонометрического тождества sin²x + cos²x = 1", "разложить на элементарные дроби" и т.д.)
- 1-2 предложения, чётко и по делу
- Отвечай по-русски
        """.trimIndent()

        val response = model.generateContent(prompt)
        response.text?.trim() ?: "Подумай, какой стандартный метод подходит для этого типа задачи."
    } catch (e: Exception) {
        "Подумай, какой стандартный метод подходит для этого типа задачи."
    }
}
