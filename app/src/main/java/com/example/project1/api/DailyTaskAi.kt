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

val OFFLINE_DAILY_TASKS = listOf(
    // Интегралы
    DailyIntegralTask(
        id = "off_1",
        type = "Найти интеграл",
        latexStatement = "\$\$I = \\int x e^x \\, dx\$\$",
        correctAnswer = "e^x(x - 1) + C",
        description = "Вычисли неопределенный интеграл: $\\int x e^x \\, dx$"
    ),
    DailyIntegralTask(
        id = "off_2",
        type = "Найти интеграл",
        latexStatement = "\$\$I = \\int \\frac{dx}{x^2 + 9}\$\$",
        correctAnswer = "\\frac{1}{3} \\arctan(\\frac{x}{3}) + C",
        description = "Вычисли интеграл вида dx/(x^2 + a^2)"
    ),
    DailyIntegralTask(
        id = "off_3",
        type = "Найти интеграл",
        latexStatement = "\$\$I = \\int x \\cos(x) \\, dx\$\$",
        correctAnswer = "x \\sin(x) + \\cos(x) + C",
        description = "Вычисли интеграл методом интегрирования по частям"
    ),

    // Производные
    DailyIntegralTask(
        id = "off_4",
        type = "Найти производную",
        latexStatement = "\$\$f(x) = \\ln(\\sin x) + x^3 e^{2x}\$\$",
        correctAnswer = "\\cot(x) + 3x^2 e^{2x} + 2x^3 e^{2x}",
        description = "Найди производную сложной функции f'(x)"
    ),
    DailyIntegralTask(
        id = "off_5",
        type = "Найти производную",
        latexStatement = "\$\$f(x) = \\frac{\\arcsin(x)}{\\sqrt{1 - x^2}}\$\$",
        correctAnswer = "\\frac{1}{1 - x^2} + \\frac{x \\arcsin(x)}{(1 - x^2)^{3/2}}",
        description = "Найди производную дроби f'(x)"
    ),

    // Пределы
    DailyIntegralTask(
        id = "off_6",
        type = "Найти предел",
        latexStatement = "\$\$\\lim_{x \\to 0} \\frac{\\sin(5x)}{\\ln(1 + 2x)}\$\$",
        correctAnswer = "5/2",
        description = "Вычисли предел, используя эквивалентные бесконечно малые"
    ),
    DailyIntegralTask(
        id = "off_7",
        type = "Найти предел",
        latexStatement = "\$\$\\lim_{x \\to \\infty} \\left(1 + \\frac{3}{x}\\right)^{2x}\$\$",
        correctAnswer = "e^6",
        description = "Вычисли второй замечательный предел"
    ),

    // Дифференциальные уравнения
    DailyIntegralTask(
        id = "off_8",
        type = "Решить ДУ",
        latexStatement = "\$\$y' - \\frac{2y}{x} = x^3\$\$",
        correctAnswer = "y = \\frac{x^4}{2} + C x^2",
        description = "Реши линейное дифференциальное уравнение первого порядка"
    ),
    DailyIntegralTask(
        id = "off_9",
        type = "Решить ДУ",
        latexStatement = "\$\$y'' + 4y = 0, \\quad y(0) = 1, \\ y'(0) = 0\$\$",
        correctAnswer = "y = \\cos(2x)",
        description = "Реши задачу Коши для линейного однородного ДУ второго порядка"
    ),

    // Операции с матрицами
    DailyIntegralTask(
        id = "off_10",
        type = "Операции с матрицами",
        latexStatement = "\$\$\\det \\begin{pmatrix} 3 & -2 \\\\ 4 & 5 \\end{pmatrix}\$\$",
        correctAnswer = "23",
        description = "Вычисли определитель матрицы второго порядка: 3*5 - (-2)*4"
    ),
    DailyIntegralTask(
        id = "off_11",
        type = "Операции с матрицами",
        latexStatement = "\$\$\\det \\begin{pmatrix} 1 & 2 & 3 \\\\ 0 & 4 & 5 \\\\ 0 & 0 & 6 \\end{pmatrix}\$\$",
        correctAnswer = "24",
        description = "Найди определитель верхнетреугольной матрицы 3x3 (произведение элементов главной диагонали)"
    ),
    DailyIntegralTask(
        id = "off_12",
        type = "Операции с матрицами",
        latexStatement = "\$\$M_{12} \\text{ для матрицы } A = \\begin{pmatrix} 1 & 4 & 2 \\\\ 3 & 0 & -1 \\\\ 2 & 5 & 1 \\end{pmatrix}\$\$",
        correctAnswer = "5",
        description = "Найди минор M12 (определитель матрицы при вычеркивании 1 строки и 2 столбца: det [[3,-1],[2,1]] = 3 - (-2) = 5)"
    ),
    DailyIntegralTask(
        id = "off_13",
        type = "Операции с матрицами",
        latexStatement = "\$$\\begin{pmatrix} 1 & 2 \\\\ 3 & 4 \\end{pmatrix} \\cdot \\begin{pmatrix} 2 & 0 \\\\ 1 & 3 \\end{pmatrix}\$\$",
        correctAnswer = "\\begin{pmatrix} 4 & 6 \\\\ 10 & 12 \\end{pmatrix}",
        description = "Вычисли произведение двух матриц 2x2"
    ),

    // Контурное интегрирование (ТФКП)
    DailyIntegralTask(
        id = "off_14",
        type = "Контурное интегрирование",
        latexStatement = "\$$\\oint_{|z|=2} \\frac{dz}{z - 1}\$\$",
        correctAnswer = "2\\pi i",
        description = "Вычисли интеграл по окружности радиуса 2 с центром в 0 по интегральной формуле Коши (внутри полюс z = 1)"
    ),
    DailyIntegralTask(
        id = "off_15",
        type = "Контурное интегрирование",
        latexStatement = "\$$\\oint_{|z|=1} e^{z^2} \\, dz\$\$",
        correctAnswer = "0",
        description = "Вычисли интеграл от целой голоморфной функции по замкнутому контуру (теорема Коши)"
    ),
    DailyIntegralTask(
        id = "off_16",
        type = "Контурное интегрирование",
        latexStatement = "\$$\\oint_{|z|=3} \\frac{\\cos z}{z - \\pi} \\, dz\$\$",
        correctAnswer = "0",
        description = "Определи значение интеграла (точка z = pi лежит вне круга радиуса 3, по теореме Коши интеграл равен 0)"
    ),
    DailyIntegralTask(
        id = "off_17",
        type = "Контурное интегрирование",
        latexStatement = "\$$\\oint_{|z|=2} \\frac{e^z}{(z - 1)^2} \\, dz\$\$",
        correctAnswer = "2\\pi i e",
        description = "Интеграл Коши для производной: 2*pi*i * f'(1), где f(z) = e^z, поэтому ответ 2*pi*i*e"
    ),

    // Производная комплексной функции (ТФКП)
    DailyIntegralTask(
        id = "off_18",
        type = "Производная комплексной функции",
        latexStatement = "\$\$f(z) = z^3 - 2z + i, \\quad \\text{найти } f'(1 + i)\$\$",
        correctAnswer = "4 + 6i",
        description = "Найди производную комплексного полинома f'(z) = 3z^2 - 2 и вычисли в точке z = 1 + i"
    ),
    DailyIntegralTask(
        id = "off_19",
        type = "Производная комплексной функции",
        latexStatement = "\$\$u(x,y) = x^2 - y^2 + 2x, \\quad \\text{найди } \\frac{\\partial v}{\\partial y} \\text{ из условий Коши-Римана}\$\$",
        correctAnswer = "2x + 2",
        description = "По условию Коши-Римана: du/dx = dv/dy, вычисли частную производную du/dx"
    ),
    DailyIntegralTask(
        id = "off_20",
        type = "Производная комплексной функции",
        latexStatement = "\$\$f(z) = e^{2z}, \\quad \\text{найти } f'(z)\$\$",
        correctAnswer = "2e^{2z}",
        description = "Найди производную комплексной экспоненты f'(z) по правилу дифференцирования"
    )
)

suspend fun generateDailyTaskWithAi(context: Context, apiKey: String): DailyIntegralTask = withContext(Dispatchers.IO) {
    DailyTaskStorage.getSavedTask(context)?.let { return@withContext it }

    val streak = DailyTaskStorage.getStreak(context)
    val history = DailyTaskStorage.getPreviousTasks(context)
    val types = listOf(
        "Найти производную",
        "Найти интеграл",
        "Найти предел",
        "Решить ДУ",
        "Операции с матрицами",
        "Контурное интегрирование",
        "Производная комплексной функции"
    )
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
- Если ДУ, пиши: ${'$'}${'$'}y' - \frac{y}{x} = x^2 \sin x${'$'}${'$'} (если есть начальные условия, разделяй через \\, например: ${'$'}${'$'}y'' + y = 0 \\ y(0)=1${'$'}${'$'})
- Если операции с матрицами, пиши задачу на вычисление определителя 2x2 или 3x3, минора или умножения матриц: например ${'$'}${'$'}\det \begin{pmatrix} 2 & 3 \\ 1 & 4 \end{pmatrix}${'$'}${'$'}
- Если контурное интегрирование, пиши задачу на замкнутый контур с простыми вычетами или интегральной формулой Коши: например ${'$'}${'$'}\oint_{|z|=2} \frac{e^z}{z-1} \, dz${'$'}${'$'}
- Если производная комплексной функции, пиши задачу на нахождение производной f'(z) или через условия Коши-Римана: например ${'$'}${'$'}f(z) = z^3 - 3iz, \ f'(1+i)${'$'}${'$'}

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
        val historyStatements = history.toSet()
        val candidate = OFFLINE_DAILY_TASKS.filter { it.latexStatement !in historyStatements }.ifEmpty { OFFLINE_DAILY_TASKS }.random()
        val fallbackTask = candidate.copy(id = System.currentTimeMillis().toString().takeLast(4))
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
  "comment": "Короткий комментарий на русском (1-2 предложения). Если упоминаешь формулы, оборачивай их в LaTeX ${'$'}...${'$'}. Если CLOSE — укажи на ошибку. Если WRONG — дай наводящую подсказку, не давай готовый ответ."
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
- Укажи только КЛЮЧЕВУЮ ИДЕЮ или метод (например: "применить метод интегрирования по частям", "воспользоваться формулой ${'$'}\\sin^2(x) + \\cos^2(x) = 1${'$'}", "разложить на элементарные дроби" и т.д.)
- Все математические формулы и переменные оборачивай в стандартный LaTeX: inline в ${'$'}...${'$'} (например ${'$'}u = x^2${'$'}, ${'$'}\\det(A) = ad - bc${'$'}, ${'$'}\\int u \\, dv${'$'})
- 1-2 предложения, чётко и по делу
- Отвечай по-русски
        """.trimIndent()

        val response = model.generateContent(prompt)
        response.text?.trim() ?: "Подумай, какой стандартный метод подходит для этого типа задачи."
    } catch (e: Exception) {
        "Подумай, какой стандартный метод подходит для этого типа задачи."
    }
}
