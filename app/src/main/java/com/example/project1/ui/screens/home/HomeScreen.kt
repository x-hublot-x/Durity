package com.example.project1.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.HourglassDisabled
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.project1.R
import com.example.project1.data.model.AppInfo
import com.example.project1.data.model.BlitzSessionState
import com.example.project1.ui.components.GradientIcon
import com.example.project1.ui.components.GradientText
import com.example.project1.ui.components.PrimaryGradientButton
import com.example.project1.data.storage.AiTestManager
import com.example.project1.data.storage.DailyTaskStorage
import com.example.project1.data.storage.MathBlitzStorage
import com.example.project1.data.storage.NotificationHistoryStorage
import com.example.project1.service.MathBlitzNotificationManager
import com.example.project1.ui.components.CoinIcon
import com.example.project1.ui.theme.AppTheme
import com.example.project1.BuildConfig
import com.example.project1.data.storage.DailySummaryData
import com.example.project1.data.storage.DailySummaryStorage
import com.example.project1.data.storage.UserRatingStorage
import com.example.project1.data.storage.WeeklyReportStorage
import com.example.project1.service.DailySummaryManager
import com.example.project1.service.WeeklyReportManager
import com.example.project1.ui.components.UserRatingDialog
import com.example.project1.util.VibrationUtil
import com.example.project1.util.formatMinutes
import com.example.project1.util.hapticClickable
import kotlinx.coroutines.delay
import dev.chrisbanes.haze.HazeState
import java.util.Calendar

// ── Утилита: имя пользователя из профиля личности ────────────────────────────

fun extractUserName(): String {
    val name = AiTestManager.savedName.trim()
    return if (name.isNotEmpty()) name.split(" ").firstOrNull() ?: name else "Гость"
}

// ── Приветствие по времени суток ─────────────────────────────────────────────

fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Доброе утро"
        in 12..17 -> "Добрый день"
        in 18..22 -> "Добрый вечер"
        else -> "Доброй ночи"
    }
}

// ── Главный экран ─────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    onNavigateToDailyTask: () -> Unit,
    onShowTestDialog: () -> Unit,
    onNavigateToChat: (sessionTitle: String, firstMessage: String, taskLatex: String) -> Unit,
    onNavigateToStats: () -> Unit = {},
    onOpenBlitz: () -> Unit = {},
    onOpenDailySummary: () -> Unit = {},
    onDiscussDailySummary: (DailySummaryData) -> Unit = {},
    appsWithTimers: List<AppInfo> = emptyList(),
    hazeState: HazeState? = null,
    onNotificationCenterOpenChanged: (Boolean) -> Unit = {},
    onMathReferenceOpenChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val colors = AppTheme.colors

    val isTestPassed = AiTestManager.isTestCompleted
    val savedName = AiTestManager.savedName
    val userName = if (savedName.isNotBlank())
        savedName.trim().split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: savedName.trim()
    else "Гость"
    val greeting = remember { getGreeting() }

    var coins by remember { mutableIntStateOf(DailyTaskStorage.getCoins(context)) }
    var userRating by remember { mutableIntStateOf(UserRatingStorage.getRating(context)) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showBlockedBlitzDialog by remember { mutableStateOf(false) }

    val isSolvedToday = remember { DailyTaskStorage.isSolvedToday(context) }
    val streak = remember { DailyTaskStorage.getStreak(context) }
    val totalUsageMinutes = remember(appsWithTimers) { appsWithTimers.sumOf { it.usedMinutesThisWeek } }
    var activeBlitzSession by remember { mutableStateOf<BlitzSessionState?>(null) }
    var blitzRemainingSeconds by remember { mutableLongStateOf(0L) }

    val isSummaryWindow = remember { DailySummaryManager.isSummaryWindowActive() }
    var dailySummary by remember { mutableStateOf<DailySummaryData?>(DailySummaryStorage.getLatestSummary(context)) }

    LaunchedEffect(Unit) {
        val summary = DailySummaryManager.checkAndGenerateDailySummary(context, BuildConfig.GEMINI_API_KEY)
        if (summary != null) {
            dailySummary = summary
        }
    }

    // Обновляем монеты, рейтинг и состояние блица в реальном времени каждую секунду
    LaunchedEffect(Unit) {
        while (true) {
            coins = DailyTaskStorage.getCoins(context)
            userRating = UserRatingStorage.getRating(context)
            val sess = MathBlitzStorage.getActiveSession(context)
            if (sess != null && !sess.isFinished) {
                val leftMs = sess.deadlineTime - System.currentTimeMillis()
                if (leftMs <= 0) {
                    val expired = sess.copy(isFinished = true, isWon = false)
                    MathBlitzStorage.saveActiveSession(context, expired)
                    activeBlitzSession = expired
                    blitzRemainingSeconds = 0L
                    val isAlreadyNotified = MathBlitzStorage.isTimeoutNotified(context, sess.id)
                    if (!isAlreadyNotified) {
                        MathBlitzStorage.setTimeoutNotified(context, sess.id)
                        DailySummaryStorage.recordBlitzLoss(context)
                        UserRatingStorage.addRating(context, -10)
                        userRating = UserRatingStorage.getRating(context)
                        MathBlitzNotificationManager.showTimeoutLossNotification(context, sess.config.betCoins)
                    }
                } else {
                    activeBlitzSession = sess
                    blitzRemainingSeconds = leftMs / 1000L
                }
            } else {
                activeBlitzSession = sess
                blitzRemainingSeconds = 0L
            }
            delay(1000L)
        }
    }

    var showRetestConfirm by remember { mutableStateOf(false) }
    var showNotificationCenter by remember { mutableStateOf(false) }
    var showWeeklyReportDialog by remember { mutableStateOf(false) }
    var hasUnreadNotifications by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Убеждаемся, что еженедельный отчет доступен
        if (WeeklyReportStorage.getLatestReport(context) == null) {
            WeeklyReportManager.generateOrUpdateReport(context)
        }
    }

    LaunchedEffect(showNotificationCenter) {
        onNotificationCenterOpenChanged(showNotificationCenter)
    }

    LaunchedEffect(Unit) {
        hasUnreadNotifications = NotificationHistoryStorage.hasUnread(context)
    }

    if (showRatingDialog) {
        UserRatingDialog(
            rating = userRating,
            onDismiss = { showRatingDialog = false }
        )
    }

    if (showBlockedBlitzDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedBlitzDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(22.dp)
                    )
                    Text("Доступ к блицу заблокирован", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Ваш рейтинг репутации достиг отметки -100. Участие в блицах временно заблокировано.\n\nВы можете восстановить репутацию, решая задачи дня и соблюдая лимиты скроллинга коротких видео.",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                PrimaryGradientButton(
                    onClick = {
                        showBlockedBlitzDialog = false
                        showRatingDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("Открыть рейтинг", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockedBlitzDialog = false }) {
                    Text("Закрыть", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 100.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── Шапка: приветствие + баланс + колокольчик уведомлений ─────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$greeting,",
                    fontSize = 15.sp,
                    color = colors.textSecondary
                )
                Text(
                    text = userName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // В конце дня (с 21:00 МСК) выводим кнопку перехода на отдельную вкладку итогов дня
                if (isSummaryWindow) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF7C4DFF).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f)),
                        modifier = Modifier.hapticClickable(context) { onOpenDailySummary() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bedtime,
                                contentDescription = null,
                                tint = Color(0xFFB388FF),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Итоги дня",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB388FF)
                            )
                        }
                    }
                }

                // Баланс монет (клик открывает шкалу рейтинга)
                CoinBalanceChip(
                    coins = coins,
                    onClick = {
                        userRating = UserRatingStorage.getRating(context)
                        showRatingDialog = true
                    }
                )

                // Колокольчик центра уведомлений с красной точкой
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.surfaceBorder, CircleShape)
                        .clickable {
                            VibrationUtil.vibrateTick(context)
                            showNotificationCenter = true
                            hasUnreadNotifications = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Уведомления",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    if (hasUnreadNotifications) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 2.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF0000))
                                .border(1.2.dp, colors.surfaceElevated, CircleShape)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Панель 1: Тест на личность ────────────────────────────────────────
        PersonalityTestCard(
            isTestPassed = isTestPassed,
            onStartTest = { onShowTestDialog() },
            onRetest = { showRetestConfirm = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Панель 2: Задача дня ──────────────────────────────────────────────
        DailyTaskCard(
            isSolvedToday = isSolvedToday,
            streak = streak,
            onNavigate = onNavigateToDailyTask
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Панель 3: Блиц по математике ─────────────────────────────────────
        val isBlitzBlocked = userRating <= UserRatingStorage.MIN_RATING
        MathBlitzCard(
            activeSession = activeBlitzSession,
            remainingSeconds = blitzRemainingSeconds,
            isBlocked = isBlitzBlocked,
            onClick = {
                if (isBlitzBlocked) {
                    showBlockedBlitzDialog = true
                    return@MathBlitzCard
                }
                if (activeBlitzSession != null && activeBlitzSession?.isFinished == true && !activeBlitzSession!!.isWon) {
                    MathBlitzStorage.clearSession(context)
                    activeBlitzSession = null
                }
                onOpenBlitz()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Панель 4: Статистика использования ────────────────────────────────
        UsageStatsCard(
            totalMinutes = totalUsageMinutes,
            appsCount = appsWithTimers.size,
            onNavigate = onNavigateToStats
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Панель 4: Математический справочник ───────────────────────────────
        MathReferenceCard(
            hazeState = hazeState,
            onSheetOpenChanged = onMathReferenceOpenChanged
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Мини-подсказка ────────────────────────────────────────────────────
        if (!isTestPassed) {
            InfoBanner(
                text = "Пройди тест личности, чтобы разблокировать чат-бота и получить персональные рекомендации."
            )
        }
    }

    // ── Диалог подтверждения перепрохождения теста ────────────────────────────
    if (showRetestConfirm) {
        Dialog(onDismissRequest = { showRetestConfirm = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Перепройти тест?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Результаты текущего теста будут заменены новыми. Настройки чат-бота обновятся.",
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { showRetestConfirm = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена", color = colors.textSecondary)
                        }
                        PrimaryGradientButton(
                            onClick = {
                                showRetestConfirm = false
                                onShowTestDialog()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text("Перепройти", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showNotificationCenter) {
        NotificationCenterBottomSheet(
            onDismiss = {
                showNotificationCenter = false
                hasUnreadNotifications = NotificationHistoryStorage.hasUnread(context)
            },
            onOpenDailySummary = onOpenDailySummary,
            onOpenDailyTask = onNavigateToDailyTask,
            onOpenBlitz = onOpenBlitz,
            onOpenWeeklyReport = { showWeeklyReportDialog = true },
            hazeState = hazeState
        )
    }

    if (showWeeklyReportDialog) {
        WeeklyDopamineReportDialog(
            onDismiss = { showWeeklyReportDialog = false }
        )
    }
}

// ── Карточка Блица по математике ─────────────────────────────────────────────

@Composable
fun MathBlitzCard(
    activeSession: BlitzSessionState?,
    remainingSeconds: Long,
    isBlocked: Boolean = false,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val session = activeSession
    val isBattleActive = session != null && !session.isFinished && remainingSeconds > 0
    val isTimedOut = session != null && session.isFinished && !session.isWon

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val isUrgent = remainingSeconds < 60 && isBattleActive

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .hapticClickable(context) { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        if (colors.isDark) {
                            when {
                                isBlocked -> listOf(Color(0xFF351212), Color(0xFF261010))
                                isBattleActive -> listOf(Color(0xFF351A10), Color(0xFF28181A))
                                isTimedOut -> listOf(Color(0xFF2A1515), Color(0xFF22161A))
                                else -> listOf(Color(0xFF261D15), Color(0xFF201A24))
                            }
                        } else {
                            when {
                                isBlocked -> listOf(Color(0xFFFFEBEE), Color(0xFFFFCDD2))
                                isBattleActive -> listOf(Color(0xFFFFF0EB), Color(0xFFFFE8E0))
                                isTimedOut -> listOf(Color(0xFFFFEBEE), Color(0xFFFDE8E8))
                                else -> listOf(Color(0xFFFFF9EC), Color(0xFFFFF4E6))
                            }
                        }
                    )
                )
                .border(
                    width = if (isBattleActive || isBlocked) 1.5.dp else 1.dp,
                    color = when {
                        isBlocked -> Color(0xFFE53935)
                        isUrgent -> Color(0xFFFF3D00)
                        isBattleActive -> Color(0xFFFF5722).copy(alpha = 0.8f)
                        isTimedOut -> Color(0xFFFF5252).copy(alpha = 0.55f)
                        else -> Color(0xFFFFB300).copy(alpha = 0.4f)
                    },
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Иконка
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                isBlocked -> Color(0xFFE53935).copy(alpha = 0.22f)
                                isBattleActive -> (if (isUrgent) Color(0xFFFF3D00) else Color(0xFFFF5722)).copy(alpha = 0.22f)
                                isTimedOut -> Color(0xFFFF5252).copy(alpha = 0.2f)
                                else -> Color(0xFFFFB300).copy(alpha = 0.18f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isBlocked -> Icons.Rounded.Block
                            isBattleActive -> if (isUrgent) Icons.Rounded.LocalFireDepartment else Icons.Rounded.FlashOn
                            isTimedOut -> Icons.Rounded.HourglassDisabled
                            else -> Icons.Rounded.Bolt
                        },
                        contentDescription = null,
                        tint = when {
                            isBlocked -> Color(0xFFE53935)
                            isBattleActive -> if (isUrgent) Color(0xFFFF3D00) else Color(0xFFFF5722)
                            isTimedOut -> Color(0xFFFF5252)
                            else -> Color(0xFFFFB300)
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = when {
                                isBlocked -> "Блиц заблокирован"
                                isBattleActive -> "Блиц-бой"
                                isTimedOut -> "Блиц проигран"
                                else -> "Блиц по математике"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isBlocked || isTimedOut -> Color(0xFFFF5252)
                                else -> colors.textPrimary
                            }
                        )

                        if (isBlocked) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE53935).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "РЕЙТИНГ -100",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE53935),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (isBattleActive) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = (if (isUrgent) Color(0xFFFF3D00) else Color(0xFFFF5722)).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    (if (isUrgent) Color(0xFFFF3D00) else Color(0xFFFF5722)).copy(alpha = 0.45f)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isUrgent) Icons.Rounded.LocalFireDepartment else Icons.Rounded.FlashOn,
                                        contentDescription = null,
                                        tint = if (isUrgent) Color(0xFFFF3D00) else Color(0xFFFF5722),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (isUrgent) "СРОЧНО" else "ИДЁТ БОЙ",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUrgent) Color(0xFFFF3D00) else Color(0xFFFF5722),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        } else if (isTimedOut) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFF5252).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "ВРЕМЯ ВЫШЛО",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    if (isBlocked) {
                        Text(
                            text = "Восстановите рейтинг решением задач дня и контролем скроллинга",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (session != null && isBattleActive) {
                        Text(
                            text = "Осталось: $timeFormatted • Задача ${session.currentTaskIndex + 1}/${session.tasks.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isUrgent) Color(0xFFFF3D00) else Color(0xFFFF7A00),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Ставка: ${session.config.betCoins} монет • Нажми, чтобы вернуться",
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (session != null && isTimedOut) {
                        Text(
                            text = "Ставка в ${session.config.betCoins} монет сгорела",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF5252),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Нажми, чтобы начать новый раунд",
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Реши задачи на время и умножь свои монеты!",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Стрелка перехода (в едином стиле с остальными плашками)
                Text(
                    text = "›",
                    fontSize = 26.sp,
                    color = if (isBattleActive) (if (isUrgent) Color(0xFFFF3D00) else Color(0xFFFF5722)) else colors.textTertiary,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

// ── Чип баланса монет ─────────────────────────────────────────────────────────

@Composable
fun CoinBalanceChip(
    coins: Int,
    onClick: () -> Unit = {}
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .hapticClickable(context) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CoinIcon(size = 20)
        Text(
            text = coins.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD700)
        )
    }
}

// ── Карточка статистики использования ─────────────────────────────────────────

@Composable
fun UsageStatsCard(
    totalMinutes: Int,
    appsCount: Int,
    onNavigate: () -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .hapticClickable(context) { onNavigate() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        if (colors.isDark) {
                            listOf(Color(0xFF182238), Color(0xFF161E30))
                        } else {
                            listOf(Color(0xFFEDF4FF), Color(0xFFE5EFFE))
                        }
                    )
                )
                .border(
                    1.dp,
                    Color(0xFF2979FF).copy(alpha = 0.45f),
                    RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF2979FF).copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = Color(0xFF2979FF),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Статистика за день",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val hours = totalMinutes / 60
                        val mins = totalMinutes % 60
                        val timeStr = if (hours > 0) "${hours}ч ${mins}м" else "${mins}м"
                        Text(
                            text = if (totalMinutes > 0) "В приложениях: $timeStr" else "Нет данных за день",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Стрелка перехода
                Text(
                    text = "›",
                    fontSize = 26.sp,
                    color = colors.textTertiary,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

// ── Карточка AI-теста личности ────────────────────────────────────────────────

@Composable
fun PersonalityTestCard(
    isTestPassed: Boolean,
    onStartTest: () -> Unit,
    onRetest: () -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .hapticClickable(context) { if (isTestPassed) onRetest() else onStartTest() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        if (colors.isDark) {
                            if (isTestPassed) listOf(Color(0xFF1E1430), Color(0xFF281838))
                            else listOf(Color(0xFF221430), Color(0xFF1E1428))
                        } else {
                            if (isTestPassed) listOf(Color(0xFFF3E8FF), Color(0xFFEDE0FF))
                            else listOf(Color(0xFFF7EEFF), Color(0xFFF2E6FF))
                        }
                    )
                )
                .border(
                    1.dp,
                    Color(0xFF9C27B0).copy(alpha = if (isTestPassed) 0.5f else 0.35f),
                    RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF9C27B0).copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isTestPassed) Icons.Rounded.Psychology else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = if (isTestPassed) "AI-психотип" else "Тест на AI-личность",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isTestPassed)
                                "Тест пройден. Профиль сохранён."
                            else
                                "Настрой характер AI-собеседника",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Кнопка
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isTestPassed) androidx.compose.ui.graphics.SolidColor(colors.surfaceElevated)
                            else colors.primaryBrush
                        )
                        .hapticClickable(context) { if (isTestPassed) onRetest() else onStartTest() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isTestPassed) "Перепройти" else "Начать",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isTestPassed) colors.textPrimary else Color.White
                    )
                }
            }
        }
    }
}

// ── Карточка задачи дня ───────────────────────────────────────────────────────

@Composable
fun DailyTaskCard(
    isSolvedToday: Boolean,
    streak: Int,
    onNavigate: () -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .hapticClickable(context) { onNavigate() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        if (colors.isDark) {
                            listOf(Color(0xFF1A2E20), Color(0xFF1A2428))
                        } else {
                            listOf(Color(0xFFEAF5EC), Color(0xFFE8F2F2))
                        }
                    )
                )
                .border(
                    1.dp,
                    if (isSolvedToday) Color(0xFF4CAF50).copy(alpha = 0.6f)
                    else Color(0xFF00BCD4).copy(alpha = 0.4f),
                    RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSolvedToday) Color(0xFF4CAF50).copy(alpha = 0.2f)
                            else Color(0xFF00BCD4).copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSolvedToday) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Text(
                            text = "∫",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00BCD4)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Задача дня",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        if (!isSolvedToday) {
                            // Бейдж "+ монетка"
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold
                                )
                                CoinIcon(size = 12)
                            }
                        }

                        // Огонёк со стриком (всегда виден, серый если неактивен)
                        val fireActive = streak > 0 && isSolvedToday
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (fireActive) Color(0xFFFF6D00).copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.fire1),
                                contentDescription = "Streak",
                                modifier = androidx.compose.ui.Modifier.size(13.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                colorFilter = if (fireActive) null
                                else {
                                    val cm = android.graphics.ColorMatrix().apply { setSaturation(0f) }
                                    val floats = cm.array
                                    androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                        androidx.compose.ui.graphics.ColorMatrix(floats)
                                    )
                                }
                            )
                            Text(
                                text = "$streak",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (fireActive) Color(0xFFFF9100) else Color.Gray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isSolvedToday)
                            "Выполнено! Новая задача завтра в 00:00"
                        else
                            "Реши задачу и заработай монеты",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Стрелка
                Text(
                    text = "›",
                    fontSize = 26.sp,
                    color = colors.textTertiary,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

// ── Информационный баннер ─────────────────────────────────────────────────────

@Composable
fun InfoBanner(text: String) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.surfaceBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = colors.textSecondary,
            lineHeight = 18.sp
        )
    }
}