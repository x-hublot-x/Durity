package com.example.project1.ui.screens.daily

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project1.BuildConfig
import com.example.project1.R
import com.example.project1.api.checkAnswerWithAi
import com.example.project1.api.generateDailyTaskWithAi
import com.example.project1.api.getHintForTask
import com.example.project1.data.model.CheckResponse
import com.example.project1.data.model.CheckResult
import com.example.project1.data.model.DailyIntegralTask
import com.example.project1.data.storage.AiTestManager
import com.example.project1.data.storage.DailyTaskStorage
import com.example.project1.ui.components.CoinIcon
import com.example.project1.ui.components.TaskLatexView
import com.example.project1.ui.screens.home.MathReferenceFullScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun DailyTaskScreen(
    onNavigateToChat: (sessionTitle: String, firstMessage: String, taskLatex: String) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // Защита от поиска обводкой (Circle to Search), скриншотов и захвата экрана в задаче дня
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val scope = rememberCoroutineScope()

    var task by remember { mutableStateOf<DailyIntegralTask?>(null) }
    var coins by remember { mutableIntStateOf(DailyTaskStorage.getCoins(context)) }
    var streak by remember { mutableIntStateOf(DailyTaskStorage.getStreak(context)) }
    var isSolved by remember { mutableStateOf(DailyTaskStorage.isSolvedToday(context)) }

    var answerText by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf<CheckResponse?>(null) }
    var showCoinAnimation by remember { mutableStateOf(false) }

    // ── Подсказка ──────────────────────────────────────────────────────────────
    var hintText by remember { mutableStateOf("") }
    var isLoadingHint by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var showMathReference by remember { mutableStateOf(false) }

    // ── Календарь стрейка ─────────────────────────────────────────────────────
    var showStreakCalendar by remember { mutableStateOf(false) }

    BackHandler {
        if (showMathReference) {
            showMathReference = false
        } else if (showStreakCalendar) {
            showStreakCalendar = false
        } else {
            onBack()
        }
    }
    var freezeCount by remember { mutableIntStateOf(DailyTaskStorage.getFreezes(context)) }
    var solvedDays by remember { mutableStateOf(DailyTaskStorage.getSolvedDays(context)) }
    var frozenDays by remember { mutableStateOf(DailyTaskStorage.getFrozenDays(context)) }
    var activeStreakDays by remember { mutableStateOf(DailyTaskStorage.getActiveStreakDays(context)) }

    // ── Состояния анимации огонька ────────────────────────────────────────────
    // Фазы: IDLE -> EXPANDING -> PLAYING_VIDEO -> SHOWING_FIRE -> SHRINKING -> IDLE
    var streakAnimPhase by remember { mutableStateOf(StreakAnimPhase.IDLE) }
    var displayedStreak by remember { mutableIntStateOf(DailyTaskStorage.getStreak(context)) }
    var showNewStreakNumber by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        task = generateDailyTaskWithAi(context, BuildConfig.GEMINI_API_KEY)
    }

    LaunchedEffect(showCoinAnimation) {
        if (showCoinAnimation) {
            delay(2000)
            showCoinAnimation = false
        }
    }

    // ── Запуск анимации при получении правильного ответа ─────────────────────
    LaunchedEffect(streakAnimPhase) {
        when (streakAnimPhase) {
            StreakAnimPhase.EXPANDING -> {
                delay(300) // Время расширения
                streakAnimPhase = StreakAnimPhase.PLAYING_VIDEO
            }
            StreakAnimPhase.PLAYING_VIDEO -> {
                delay(2500) // Длительность видео
                // Обновляем displayedStreak на новое значение и запускаем перелистывание
                displayedStreak = streak
                showNewStreakNumber = true
                streakAnimPhase = StreakAnimPhase.SHOWING_FIRE
            }
            StreakAnimPhase.SHOWING_FIRE -> {
                delay(600)
                streakAnimPhase = StreakAnimPhase.SHRINKING
            }
            StreakAnimPhase.SHRINKING -> {
                delay(400)
                streakAnimPhase = StreakAnimPhase.IDLE
            }
            else -> {}
        }
    }

    // ── Диалог-календарь ──────────────────────────────────────────────────────
    if (showStreakCalendar) {
        StreakCalendarDialog(
            streak = streak,
            coins = coins,
            freezeCount = freezeCount,
            solvedDays = solvedDays,
            frozenDays = frozenDays,
            activeStreakDays = activeStreakDays,
            onBuyFreezes = { count ->
                val success = DailyTaskStorage.buyFreezes(context, count)
                if (success) {
                    coins = DailyTaskStorage.getCoins(context)
                    freezeCount = DailyTaskStorage.getFreezes(context)
                    streak = DailyTaskStorage.getStreak(context)
                    solvedDays = DailyTaskStorage.getSolvedDays(context)
                    frozenDays = DailyTaskStorage.getFrozenDays(context)
                    activeStreakDays = DailyTaskStorage.getActiveStreakDays(context)
                }
            },
            onDismiss = { showStreakCalendar = false }
        )
    }

    // ── Справочник ────────────────────────────────────────────────────────────
    if (showMathReference) {
        MathReferenceFullScreen(onDismiss = { showMathReference = false })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 100.dp)
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Задача дня",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Обновляется каждый день в 3:00 МСК",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }

                // ── Значок Огонька (Streak) с анимацией ──────────────────
                val fireActive = streak > 0 && isSolved
                val isAnimating = streakAnimPhase != StreakAnimPhase.IDLE

                val streakColor by animateColorAsState(
                    targetValue = if (fireActive || isAnimating) Color(0xFFFF9100) else Color.Gray,
                    animationSpec = tween(300),
                    label = "streakColor"
                )

                val badgeScale by animateFloatAsState(
                    targetValue = when (streakAnimPhase) {
                        StreakAnimPhase.EXPANDING, StreakAnimPhase.PLAYING_VIDEO, StreakAnimPhase.SHOWING_FIRE -> 2.0f
                        StreakAnimPhase.SHRINKING -> 1.4f
                        else -> 1f
                    },
                    animationSpec = tween(durationMillis = when (streakAnimPhase) {
                        StreakAnimPhase.EXPANDING -> 400
                        StreakAnimPhase.SHRINKING -> 500
                        else -> 250
                    }),
                    label = "badgeScale"
                )

                val bgColor by animateColorAsState(
                    targetValue = if (fireActive || isAnimating) Color(0xFF251A14) else Color(0xFF1E1E24),
                    animationSpec = tween(300),
                    label = "bgColor"
                )

                // Смещение влево при увеличении, чтобы плашка не выходила за экран
                val badgeOffsetX by animateFloatAsState(
                    targetValue = when (streakAnimPhase) {
                        StreakAnimPhase.EXPANDING, StreakAnimPhase.PLAYING_VIDEO, StreakAnimPhase.SHOWING_FIRE -> -40f
                        StreakAnimPhase.SHRINKING -> -16f
                        else -> 0f
                    },
                    animationSpec = tween(durationMillis = when (streakAnimPhase) {
                        StreakAnimPhase.EXPANDING -> 400
                        StreakAnimPhase.SHRINKING -> 500
                        else -> 250
                    }),
                    label = "badgeOffsetX"
                )

                Box(
                    modifier = Modifier
                        .scale(badgeScale)
                        .offset(x = badgeOffsetX.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .border(1.dp, streakColor, RoundedCornerShape(12.dp))
                            .clickable(enabled = streakAnimPhase == StreakAnimPhase.IDLE) {
                                coins = DailyTaskStorage.getCoins(context)
                                freezeCount = DailyTaskStorage.getFreezes(context)
                                streak = DailyTaskStorage.getStreak(context)
                                solvedDays = DailyTaskStorage.getSolvedDays(context)
                                frozenDays = DailyTaskStorage.getFrozenDays(context)
                                activeStreakDays = DailyTaskStorage.getActiveStreakDays(context)
                                showStreakCalendar = true
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        // ── Иконка огня / видео ───────────────────────────────
                        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                            when (streakAnimPhase) {
                                StreakAnimPhase.PLAYING_VIDEO -> {
                                    // Воспроизведение mp4-анимации
                                    StreakVideoPlayer(modifier = Modifier.size(20.dp))
                                }
                                else -> {
                                    // Обычный/серый огонёк
                                    Image(
                                        painter = painterResource(id = R.drawable.fire1),
                                        contentDescription = "Streak",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit,
                                        colorFilter = if (fireActive || isAnimating) null
                                        else {
                                            val cm = android.graphics.ColorMatrix().apply { setSaturation(0f) }
                                            androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                                androidx.compose.ui.graphics.ColorMatrix(cm.array)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // ── Счётчик дней с перелистыванием ───────────────────
                        StreakCounter(
                            displayedStreak = displayedStreak,
                            showNew = showNewStreakNumber,
                            color = streakColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (task == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFF5252))
                }
            } else {
                val currentTask = task!!
                // ── Карточка задачи ──────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentTask.type.uppercase(),
                                fontSize = 12.sp,
                                color = Color(0xFFFF5252),
                                fontWeight = FontWeight.Bold
                            )
                            // Монетка — награда за задачу
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 13.sp,
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold
                                )
                                CoinIcon(size = 14)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TaskLatexView(latex = currentTask.latexStatement)

                        if (isSolved) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "Задача решена! Приходи завтра за новой.",
                                    fontSize = 14.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // ── Когда задача решена: действия пользователя ─────────────
                if (isSolved) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showMathReference = true },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color(0xFF7C4DFF).copy(alpha = 0.6f)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text("📖", fontSize = 15.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("Справочник", color = Color(0xFF7C4DFF), fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val uName = AiTestManager.savedName.trim()
                                    .split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "друг"
                                val title = "Похожие задачи — ${currentTask.type}"
                                val firstMsg = buildString {
                                    appendLine("Привет, $uName! 👋 Давай потренируемся на похожих задачах.")
                                    appendLine()
                                    appendLine("Вот исходная задача дня:")
                                    appendLine(currentTask.latexStatement)
                                    appendLine()
                                    appendLine("Пожалуйста, сгенерируй **3 похожих задачи** по теме «${currentTask.type}» — аналогичного уровня сложности. Выведи их пронумерованным списком.")
                                    appendLine()
                                    append("После каждой задачи оставь место для моего решения. Как только я пришлю решение — проверь его и дай обратную связь. Начнём? 🚀")
                                }
                                onNavigateToChat(title, firstMsg, currentTask.latexStatement)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color(0xFF4CAF50).copy(alpha = 0.8f)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🔁", fontSize = 15.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("Сгенерировать похожие", color = Color(0xFF4CAF50), fontSize = 13.sp)
                        }
                    }
                }

                // ── Форма ввода ответа ──────────────────────────────────────
                if (!isSolved) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Ваш ответ",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = answerText,
                        onValueChange = {
                            answerText = it
                            checkResult = null
                        },
                        placeholder = {
                            Text(
                                text = "Введите ответ...",
                                color = Color.White.copy(alpha = 0.35f),
                                fontSize = 14.sp
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF5252),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedContainerColor = Color(0xFF1A1A24),
                            unfocusedContainerColor = Color(0xFF1A1A24)
                        ),
                        maxLines = 3,
                        enabled = !isChecking
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (answerText.isBlank()) return@Button
                            scope.launch {
                                isChecking = true
                                val resp = checkAnswerWithAi(
                                    task = currentTask,
                                    userAnswer = answerText,
                                    apiKey = BuildConfig.GEMINI_API_KEY
                                )
                                checkResult = resp
                                if (resp.result == CheckResult.CORRECT) {
                                    DailyTaskStorage.markSolvedToday(context)
                                    DailyTaskStorage.addCoins(context, 100)
                                    coins = DailyTaskStorage.getCoins(context)
                                    val newStreak = DailyTaskStorage.getStreak(context)
                                    solvedDays = DailyTaskStorage.getSolvedDays(context)
                                    frozenDays = DailyTaskStorage.getFrozenDays(context)
                                    activeStreakDays = DailyTaskStorage.getActiveStreakDays(context)
                                    isSolved = true
                                    showCoinAnimation = true
                                    // Запуск анимации огонька
                                    showNewStreakNumber = false
                                    displayedStreak = streak // Показываем старое число до перелистывания
                                    streakAnimPhase = StreakAnimPhase.EXPANDING
                                    // Обновляем streak после анимации числа
                                    streak = newStreak
                                }
                                isChecking = false
                            }
                        },
                        enabled = answerText.isNotBlank() && !isChecking,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Проверить ответ", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // ── Кнопки: Справочник / Подсказка / Похожие задачи ─────
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Справочник
                        OutlinedButton(
                            onClick = { showMathReference = true },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color(0xFF7C4DFF).copy(alpha = 0.6f)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📖", fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text("Справочник", color = Color(0xFF7C4DFF), fontSize = 12.sp)
                        }

                        // Подсказка
                        OutlinedButton(
                            onClick = {
                                if (!isLoadingHint && hintText.isEmpty()) {
                                    scope.launch {
                                        isLoadingHint = true
                                        hintText = getHintForTask(currentTask, BuildConfig.GEMINI_API_KEY)
                                        isLoadingHint = false
                                        showHint = true
                                    }
                                } else {
                                    showHint = !showHint
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color(0xFFFFB300).copy(alpha = 0.6f)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isLoadingHint) {
                                CircularProgressIndicator(
                                    color = Color(0xFFFFB300),
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("💡", fontSize = 14.sp)
                            }
                            Spacer(Modifier.width(4.dp))
                            Text("Подсказка", color = Color(0xFFFFB300), fontSize = 12.sp)
                        }

                        // Похожие задачи
                        OutlinedButton(
                            onClick = {
                                val uName = AiTestManager.savedName.trim()
                                    .split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "друг"
                                val title = "Похожие задачи — ${currentTask.type}"
                                val firstMsg = buildString {
                                    appendLine("Привет, $uName! 👋 Давай потренируемся на похожих задачах.")
                                    appendLine()
                                    appendLine("Вот исходная задача дня:")
                                    appendLine(currentTask.latexStatement)
                                    appendLine()
                                    appendLine("Пожалуйста, сгенерируй **3 похожих задачи** по теме «${currentTask.type}» — аналогичного уровня сложности. Выведи их пронумерованным списком.")
                                    appendLine()
                                    append("После каждой задачи оставь место для моего решения. Как только я пришлю решение — проверь его и дай обратную связь. Начнём? 🚀")
                                }
                                onNavigateToChat(title, firstMsg, currentTask.latexStatement)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color(0xFF4CAF50).copy(alpha = 0.6f)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🔁", fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text("Похожие", color = Color(0xFF4CAF50), fontSize = 12.sp)
                        }
                    }

                    // ── Карточка подсказки ───────────────────────────────────
                    if (showHint && hintText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2410))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("💡", fontSize = 18.sp)
                                    Text(
                                        text = "Подсказка",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB300)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = hintText,
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    lineHeight = 20.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        val title = "Помощь с задачей дня"
                                        val hintBotMsg = "💡 **Подсказка к задаче**\n\n$hintText\n\n---\nЗадача: ${currentTask.latexStatement}\n\nЗадавай вопросы — помогу разобраться пошагово, без готового ответа."
                                        onNavigateToChat(title, hintBotMsg, currentTask.latexStatement)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp, Color.White.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("💬 Обсудить задачу", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    // ── Результат проверки ───────────────────────────────────
                    checkResult?.let { result ->
                        Spacer(modifier = Modifier.height(16.dp))

                        val (bgColor, borderColor, emoji) = when (result.result) {
                            CheckResult.CORRECT -> Triple(Color(0xFF1A2E1A), Color(0xFF4CAF50), "✅")
                            CheckResult.CLOSE -> Triple(Color(0xFF2E2A1A), Color(0xFFFFB300), "⚠️")
                            CheckResult.WRONG -> Triple(Color(0xFF2E1A1A), Color(0xFFFF5252), "❌")
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(emoji, fontSize = 18.sp)
                                    Text(
                                        text = when (result.result) {
                                            CheckResult.CORRECT -> "Верно!"
                                            CheckResult.CLOSE -> "Почти!"
                                            CheckResult.WRONG -> "Попробуй ещё"
                                        },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = borderColor
                                    )
                                }

                                if (result.comment.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = result.comment,
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        lineHeight = 20.sp
                                    )
                                }

                                if (result.result != CheckResult.CORRECT) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedButton(
                                        onClick = {
                                            val uName = AiTestManager.savedName.trim()
                                                .split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "друг"
                                            val title = "Помощь с задачей дня"
                                            val firstMsg = "Привет, $uName! 👋\n\nДавай вместе разберем эту задачу по теме \"${currentTask.type}\". Я не буду сразу давать готовый ответ, а помогу шагами.\n\n${currentTask.latexStatement}"

                                            onNavigateToChat(title, firstMsg, currentTask.latexStatement)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("💬 Обсудить задачу", color = Color.White, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showCoinAnimation) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A1A))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CoinIcon(size = 24)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+100 монет начислено!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 8.dp, start = 4.dp)
                .size(40.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
