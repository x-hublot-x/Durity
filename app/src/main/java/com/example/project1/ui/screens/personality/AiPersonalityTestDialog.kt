package com.example.project1.ui.screens.personality

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.project1.BuildConfig
import com.example.project1.data.model.AppInfo
import com.example.project1.data.storage.AiTestManager
import com.example.project1.ui.components.MinimalRedLoadingSpinner
import com.example.project1.util.formatMinutes
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun QuestionBlock(
    question: String,
    subtext: String,
    options: List<String>,
    selectedOptions: List<String>,
    onOptionToggled: (String) -> Unit,
    detailLabel: String,
    detailValue: String,
    onDetailChange: (String) -> Unit
) {
    Column {
        Text(
            text = question,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtext,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        options.forEach { option ->
            val isSelected = selectedOptions.contains(option)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOptionToggled(option) }
                    .padding(vertical = 6.dp)
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onOptionToggled(option) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFFF5252),
                        uncheckedColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = option, color = Color.White, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = detailValue,
            onValueChange = onDetailChange,
            label = { Text(detailLabel, color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFF5252),
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

enum class AiTestStage {
    INTRO,
    QUESTIONS,
    LOADING,
    VERDICT
}

@Composable
fun AiPersonalityTestDialog(
    top5Apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onApplyRecommendations: (Map<String, Int>) -> Unit,
    onResetTest: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by remember {
        mutableStateOf(if (AiTestManager.isTestCompleted) AiTestStage.VERDICT else AiTestStage.INTRO)
    }

    // Состояние для управления всплывающим диалогом сброса
    var showResetConfirmation by remember { mutableStateOf(false) }

    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 5

    val selectedReasons = remember { mutableStateListOf<String>() }
    var reasonDetails by remember { mutableStateOf("") }

    val selectedWaste = remember { mutableStateListOf<String>() }
    var wasteDetails by remember { mutableStateOf("") }

    var vitalAppsDetails by remember { mutableStateOf("") }

    var selectedStyle by remember { mutableStateOf("⚖️ Рациональный аналитик") }

    var userName by remember { mutableStateOf("") }
    var userAge by remember { mutableStateOf("") }

    var generatedPersonality by remember { mutableStateOf(AiTestManager.savedPersonality) }
    var generatedRecs by remember { mutableStateOf(AiTestManager.savedRecommendations) }
    var errorMessage by remember { mutableStateOf("") }

    val isStepValid = when (currentStep) {
        0 -> selectedReasons.isNotEmpty()
        1 -> selectedWaste.isNotEmpty()
        2 -> vitalAppsDetails.isNotBlank()
        3 -> true
        4 -> userName.isNotBlank() && userAge.isNotBlank()
        else -> false
    }

    // Вызов Gemini API при переходе в режим загрузки
    LaunchedEffect(stage) {
        if (stage == AiTestStage.LOADING) {
            coroutineScope.launch {
                try {
                    val generativeModel = GenerativeModel(
                        modelName = "gemini-3.5-flash",
                        apiKey = BuildConfig.GEMINI_API_KEY
                    )

                    val appsUsagePrompt = top5Apps.joinToString("\n") { app ->
                        "- Приложение: ${app.name} (Package: ${app.packageName}), Использование за последний день: ${app.usedMinutesThisWeek} минут (~${app.usedMinutesThisWeek * 4} минут за месяц)."
                    }

                    val prompt = """
                        Ты — эксперт по цифровому балансу и продуктивности. Проанализируй данные психологического теста пользователя и его реальную статистику экранного времени за месяц (рассчитанную на основе последнего дня).

                        Информация о пользователе:
                        - Имя: $userName, Возраст: $userAge
                        - Для чего нужно свободное время: ${selectedReasons.joinToString()} (Детали: $reasonDetails)
                        - На что время уходит впустую (главные триггеры): ${selectedWaste.joinToString()} (Детали: $wasteDetails)
                        - Важные рабочие приложения: $vitalAppsDetails
                        - Предпочитаемый стиль ограничений и тональности общения: $selectedStyle

                        Статистика топовых приложений пользователя:
                        $appsUsagePrompt

                        ЗАДАЧА:
                        1. Сформируй краткую психологическую характеристику пользователя и объясни логику выставляемых лимитов в соответствии с выбранным стилем общения ($selectedStyle).
                        2. Назначь рекомендуемый лимит времени НА ДЕНЬ (в минутах) для КАЖДОГО из перечисленных приложений (package name), основываясь на их полезности для пользователя и выбранном стиле урезания лимитов.

                        ОТВЕТ ДОЛЖЕН БЫТЬ СТРОГО В ФОРМАТЕ JSON без дополнительных символов форматирования кода markdown:
                        {
                          "personality": "Текст характеристики и вердикта...",
                          "limits": {
                            "package.name.1": 120,
                            "package.name.2": 60
                          }
                        }
                    """.trimIndent()

                    val response = withContext(Dispatchers.IO) {
                        generativeModel.generateContent(prompt).text ?: ""
                    }

                    // Чистим JSON от возможных маркдаун тэгов ```json ... ```
                    val cleanJson = response.replace("```json", "").replace("```", "").trim()
                    val jsonObject = JSONObject(cleanJson)

                    val personalityText = jsonObject.optString("personality", "Характеристика сформирована.")
                    val limitsJson = jsonObject.getJSONObject("limits")

                    val calculatedRecs = mutableMapOf<String, Int>()
                    val keys = limitsJson.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        calculatedRecs[key] = limitsJson.getInt(key)
                    }

                    generatedPersonality = personalityText
                    generatedRecs = calculatedRecs

                    AiTestManager.saveResults(context, generatedPersonality, generatedRecs, userName)
                    stage = AiTestStage.VERDICT
                } catch (e: Exception) {
                    // Резервный фоллбэк, если сбой сети или ошибки API
                    errorMessage = "Не удалось связаться с ИИ: ${e.localizedMessage}. Применены базовые расчёты."

                    val fallbackRecs = mutableMapOf<String, Int>()
                    top5Apps.forEach { app ->
                        fallbackRecs[app.packageName] = (app.usedMinutesThisWeek * 0.7f).toInt().coerceAtLeast(15)
                    }
                    generatedPersonality = "Личность: $userName ($userAge лет).\nСформирован базовый баланс лимитов на основе вашего профиля."
                    generatedRecs = fallbackRecs
                    AiTestManager.saveResults(context, generatedPersonality, generatedRecs, userName)
                    stage = AiTestStage.VERDICT
                }
            }
        }
    }

    // Всплывающее окно подтверждения
    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = {
                Text(
                    text = "Перепройти тест?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Текущие результаты анализа и выставленные рекомендации ИИ будут сброшены. Вы уверены?",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmation = false
                        AiTestManager.resetTest(context)
                        onResetTest()
                        currentStep = 0
                        stage = AiTestStage.INTRO
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Да, сбросить", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmation = false }
                ) {
                    Text("Отмена", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF252533),
            shape = RoundedCornerShape(16.dp)
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1F1F2C),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                when (stage) {
                    AiTestStage.INTRO -> {
                        Text(
                            text = "Персонализация",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ИИ проанализирует ваши ответы и месячную статистику экранного времени, после чего составит индивидуальный план лимитов.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                currentStep = 0
                                stage = AiTestStage.QUESTIONS
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Сформировать личность", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    AiTestStage.QUESTIONS -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Тест персонализации",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${currentStep + 1} / $totalSteps",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF5252)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            when (currentStep) {
                                0 -> {
                                    QuestionBlock(
                                        question = "Для чего тебе больше всего нужно свободное время?",
                                        subtext = "Этот вопрос помогает ИИ определить ваши приоритетные сферы жизни и осознать ценность освобождаемого времени для личной мотивации.",
                                        options = listOf(
                                            "📚 Учёба, подготовка к экзаменам / курсам",
                                            "💼 Работа, проекты, карьера",
                                            "🧘 Ментальное здоровье, отдых без экранов"
                                        ),
                                        selectedOptions = selectedReasons,
                                        onOptionToggled = { opt ->
                                            if (selectedReasons.contains(opt)) selectedReasons.remove(opt)
                                            else selectedReasons.add(opt)
                                        },
                                        detailLabel = "Распишите подробнее цели (опционально)",
                                        detailValue = reasonDetails,
                                        onDetailChange = { reasonDetails = it }
                                    )
                                }

                                1 -> {
                                    QuestionBlock(
                                        question = "На что уходит больше всего времени впустую?",
                                        subtext = "Помогает зафиксировать главные триггеры прокрастинации и подсветить вам «черные дыры» внимания.",
                                        options = listOf(
                                            "🎬 Короткие видео (Shorts, Reels, TikTok)",
                                            "💬 Бесконечные переписки и каналы в мессенджерах",
                                            "🎮 Игры",
                                            "🌐 Бессмысленный сёрфинг в браузере"
                                        ),
                                        selectedOptions = selectedWaste,
                                        onOptionToggled = { opt ->
                                            if (selectedWaste.contains(opt)) selectedWaste.remove(opt)
                                            else selectedWaste.add(opt)
                                        },
                                        detailLabel = "Есть идеи почему затягивает? (опционально)",
                                        detailValue = wasteDetails,
                                        onDetailChange = { wasteDetails = it }
                                    )
                                }

                                2 -> {
                                    val top5Summary = if (top5Apps.isEmpty()) "Список пуст" else top5Apps.take(5).joinToString(", ") { "${it.name} (${formatMinutes(it.usedMinutesThisWeek)}/ден)" }
                                    Column {
                                        Text(
                                            text = "Есть ли приложения из топа, которые жизненно необходимы для дела?",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Топ за день: $top5Summary",
                                            fontSize = 12.sp,
                                            color = Color(0xFFFF8A8A)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Позволяет разграничить рабочий инструментарий от развлекательного, избегая случайной блокировки нужных сервисов.",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        OutlinedTextField(
                                            value = vitalAppsDetails,
                                            onValueChange = { vitalAppsDetails = it },
                                            label = { Text("Укажите важные приложения или 'Нет'", color = Color.Gray) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = Color(0xFFFF5252),
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                            )
                                        )
                                    }
                                }

                                3 -> {
                                    Column {
                                        Text(
                                            text = "Как ИИ должен общаться с тобой и резать лимиты?",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Формирует тональность поддержки и уровень строгости ограничений под ваш характер.",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        val styles = listOf(
                                            "🐣 Мягкий наставник: «Урезаем плавно (на 10-20%), подбадриваем и не давим.»",
                                            "⚖️ Рациональный аналитик: «Честная статистика, среднее урезание, нейтральный тон.»",
                                            "🦾 Строгий четкий мужик: «Жесткое урезание, прямая критика прокрастинации, без сюсюканий, без буллинга и прямых оскорблений»"
                                        )

                                        styles.forEach { styleText ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedStyle = styleText }
                                                    .padding(vertical = 6.dp)
                                            ) {
                                                RadioButton(
                                                    selected = (selectedStyle == styleText),
                                                    onClick = { selectedStyle = styleText },
                                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF5252))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(styleText, color = Color.White, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }

                                4 -> {
                                    Column {
                                        Text(
                                            text = "Финальное завершение настройки ИИ",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF5252)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Как вас называть?", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                            OutlinedTextField(
                                                value = userName,
                                                onValueChange = { userName = it },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedBorderColor = Color(0xFFFF5252)
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Какой у вас возраст?", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                            OutlinedTextField(
                                                value = userAge,
                                                onValueChange = { userAge = it },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedBorderColor = Color(0xFFFF5252)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!isStepValid) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (currentStep == 0 || currentStep == 1) "* Выберите хотя бы один вариант" else "* Заполните все поля",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (currentStep > 0) currentStep-- },
                                enabled = currentStep > 0,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White,
                                    disabledContentColor = Color.Gray.copy(alpha = 0.3f)
                                )
                            ) {
                                Text("← Назад")
                            }

                            if (currentStep < totalSteps - 1) {
                                Button(
                                    onClick = { currentStep++ },
                                    enabled = isStepValid,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF5252),
                                        disabledContainerColor = Color(0xFF332222)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Далее →", color = if (isStepValid) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { stage = AiTestStage.LOADING },
                                    enabled = isStepValid,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF5252),
                                        disabledContainerColor = Color(0xFF332222)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Завершить тест", color = if (isStepValid) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    AiTestStage.LOADING -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Обработка данных через Gemini ИИ...",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            MinimalRedLoadingSpinner()
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "ИИ рассчитывает баланс лимитов на основе статистики за месяц...",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    AiTestStage.VERDICT -> {
                        // Если имя не было сохранено — показываем поле ввода
                        var verdictName by remember { mutableStateOf(AiTestManager.savedName) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (AiTestManager.savedName.isBlank()) {
                                OutlinedTextField(
                                    value = verdictName,
                                    onValueChange = { verdictName = it },
                                    label = { Text("Как вас называть?", color = Color.Gray, fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFFF5252),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                                if (verdictName.isNotBlank()) {
                                    androidx.compose.runtime.LaunchedEffect(verdictName) {
                                        AiTestManager.saveResults(context, generatedPersonality, generatedRecs, verdictName)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Text(
                                text = "Мнение ИИ",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = generatedPersonality,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )

                            if (errorMessage.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF8A8A)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Рекомендуемое время на день:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF5252)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                generatedRecs.toList().forEach { (pkg, mins) ->
                                    val app = top5Apps.find { it.packageName == pkg }
                                    val name = app?.name ?: if (pkg == "com.google.android.youtube.shorts") "YouTube Shorts" else pkg
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF252533))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Text(formatMinutes(mins), color = Color(0xFFFF5252), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        AiTestManager.resetTest(context)
                                        onResetTest()
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Заново",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        onApplyRecommendations(generatedRecs)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Применить",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Отмена",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
