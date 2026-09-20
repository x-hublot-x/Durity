package com.example.project1.ui.screens.blitz

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project1.BuildConfig
import com.example.project1.ui.components.PrimaryGradientButton
import com.example.project1.api.checkAnswerWithAi
import com.example.project1.data.model.BlitzSessionState
import com.example.project1.data.repository.MathBlitzRepository
import com.example.project1.data.storage.DailyTaskStorage
import com.example.project1.data.storage.MathBlitzStorage
import com.example.project1.service.MathBlitzNotificationManager
import com.example.project1.ui.components.CoinIcon
import com.example.project1.ui.components.TaskLatexView
import com.example.project1.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object BlitzSoundEffects {
    private var staticTrack: AudioTrack? = null

    init {
        try {
            val sampleRate = 44100
            val durationMs = 45
            val numSamples = (sampleRate * durationMs) / 1000
            val pcm = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // Экспоненциальный спад для имитации резкого акустического щелчка механических часов
                val env = Math.exp(-t * 90.0)
                // Две гармоники: глухой щелчок шестеренки (950 Гц) и звонкий спуск (3400 Гц)
                val wave = 0.6 * Math.sin(2.0 * Math.PI * 950.0 * t) +
                           0.4 * Math.sin(2.0 * Math.PI * 3400.0 * t)
                pcm[i] = (wave * env * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            staticTrack = track
        } catch (_: Exception) {}
    }

    fun tick(context: Context, view: View?) {
        // 1. Звук механических часов через AudioTrack со звуковой дорожкой сонификации
        try {
            staticTrack?.let {
                it.stop()
                it.reloadStaticData()
                it.play()
            }
        } catch (_: Exception) {}

        // Fallback системный клик
        try {
            view?.playSoundEffect(SoundEffectConstants.CLICK)
        } catch (_: Exception) {}

        // 2. Тактильная микро-вибрация в такт секундам
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(22, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(22)
                }
            }
        } catch (_: Exception) {}
    }
}

@Composable
fun MathBlitzScreen(
    initialSession: BlitzSessionState,
    onClose: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val colors = AppTheme.colors

    var session by remember { mutableStateOf(initialSession) }
    var remainingMs by remember {
        mutableLongStateOf(maxOf(0L, session.deadlineTime - System.currentTimeMillis()))
    }

    var answerText by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var hintText by remember { mutableStateOf<String?>(null) }
    var isWrongAnswer by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }

    // Матричный ввод произвольного размера (1x1 .. 4x4)
    var isMatrixInputMode by remember { mutableStateOf(false) }
    var matrixRows by remember { mutableIntStateOf(2) }
    var matrixCols by remember { mutableIntStateOf(2) }
    var matrixGrid by remember { mutableStateOf(List(4) { List(4) { "" } }) }

    fun updateMatrixCell(r: Int, c: Int, text: String) {
        val newGrid = matrixGrid.mapIndexed { ri, row ->
            if (ri == r) row.mapIndexed { ci, cell -> if (ci == c) text else cell }
            else row
        }
        matrixGrid = newGrid

        val hasAny = newGrid.take(matrixRows).any { row -> row.take(matrixCols).any { it.isNotBlank() } }
        if (hasAny) {
            answerText = newGrid.take(matrixRows).joinToString("; ") { row ->
                row.take(matrixCols).joinToString(" ") { it.trim().ifBlank { "0" } }
            }
        } else {
            answerText = ""
        }
        isWrongAnswer = false
        hintText = null
    }

    fun setMatrixDimensions(rows: Int, cols: Int) {
        matrixRows = rows.coerceIn(1, 4)
        matrixCols = cols.coerceIn(1, 4)
        val hasAny = matrixGrid.take(matrixRows).any { row -> row.take(matrixCols).any { it.isNotBlank() } }
        if (hasAny) {
            answerText = matrixGrid.take(matrixRows).joinToString("; ") { row ->
                row.take(matrixCols).joinToString(" ") { it.trim().ifBlank { "0" } }
            }
        }
    }

    // Сброс и авто-переключение режима при переходе к задаче
    LaunchedEffect(session.currentTaskIndex) {
        answerText = ""
        matrixGrid = List(4) { List(4) { "" } }
        isWrongAnswer = false
        hintText = null

        val currentTask = session.tasks.getOrNull(session.currentTaskIndex)
        val rawExp = currentTask?.correctAnswer ?: ""
        val isScalarOperation = currentTask != null && (
            currentTask.latexStatement.contains("\\det", ignoreCase = true) ||
            currentTask.latexStatement.contains("det ", ignoreCase = true) ||
            currentTask.latexStatement.contains("vmatrix", ignoreCase = true) ||
            currentTask.latexStatement.contains("\\operatorname{tr}", ignoreCase = true) ||
            currentTask.latexStatement.contains("tr(", ignoreCase = true) ||
            currentTask.latexStatement.contains("tr ", ignoreCase = true)
        )
        val expectsMatrix = currentTask != null && !isScalarOperation && (
            rawExp.contains(";") ||
            rawExp.contains("\\\\") ||
            rawExp.contains("[[")
        )
        isMatrixInputMode = expectsMatrix

        // Автоопределение размера матрицы из условия/ответа
        if (expectsMatrix) {
            val rowsSplit = rawExp.replace("[", "").replace("]", "").replace("pmatrix", "")
                .split(Regex("""(?:\s*;\s*|\s*\\\\\s*|\n+)"""))
                .filter { it.isNotBlank() }
            if (rowsSplit.isNotEmpty()) {
                val r = rowsSplit.size.coerceIn(1, 4)
                val c = rowsSplit.first().trim().split(Regex("""[\s,&]+""")).filter { it.isNotBlank() }.size.coerceIn(1, 4)
                matrixRows = r
                matrixCols = c
            } else {
                matrixRows = 2
                matrixCols = 2
            }
        }
    }

    // Таймер обратного отсчета в реальном времени со звуком и микро-вибрацией на последних 30 секундах
    LaunchedEffect(session.deadlineTime, session.isFinished) {
        var lastTickedSecond = -1L
        while (!session.isFinished) {
            val now = System.currentTimeMillis()
            val left = session.deadlineTime - now
            if (left <= 0) {
                remainingMs = 0L
                val finishedSession = session.copy(isFinished = true, isWon = false)
                session = finishedSession
                com.example.project1.data.storage.DailySummaryStorage.recordBlitzLoss(context)
                com.example.project1.data.storage.UserRatingStorage.addRating(context, -10)
                MathBlitzStorage.saveActiveSession(context, finishedSession)
                MathBlitzNotificationManager.cancelActiveNotification(context)
                MathBlitzNotificationManager.cancelTimeoutAlarm(context)
                break
            } else {
                remainingMs = left
                val currentSecond = left / 1000L
                if (left in 1..30_000L && currentSecond != lastTickedSecond) {
                    lastTickedSecond = currentSecond
                    BlitzSoundEffects.tick(context, view)
                }
            }
            delay(200)
        }
    }

    // Обработка кнопки Назад
    BackHandler(enabled = !session.isFinished) {
        showExitConfirm = true
    }

    fun submitAnswer() {
        val currentTask = session.tasks.getOrNull(session.currentTaskIndex) ?: return
        if (answerText.isBlank() || isChecking) return

        com.example.project1.util.VibrationUtil.vibrateClick(context)
        isChecking = true
        coroutineScope.launch {
            val (isCorrect, hint) = MathBlitzRepository.checkBlitzAnswer(
                task = currentTask,
                userAnswer = answerText,
                apiKey = BuildConfig.GEMINI_API_KEY
            )
            isChecking = false

            if (isCorrect) {
                com.example.project1.util.VibrationUtil.vibrateClick(context)
                isWrongAnswer = false
                hintText = null
                answerText = ""

                val nextIndex = session.currentTaskIndex + 1
                if (nextIndex >= session.tasks.size) {
                    // Победа! Все задачи решены вовремя
                    val wonSession = session.copy(
                        currentTaskIndex = nextIndex,
                        isFinished = true,
                        isWon = true
                    )
                    session = wonSession
                    DailyTaskStorage.addCoins(context, wonSession.potentialWinCoins)
                    com.example.project1.data.storage.DailySummaryStorage.recordBlitzWin(context)
                    com.example.project1.data.storage.UserRatingStorage.addRating(context, 10)
                    MathBlitzStorage.saveActiveSession(context, wonSession)
                    MathBlitzNotificationManager.cancelActiveNotification(context)
                    MathBlitzNotificationManager.cancelTimeoutAlarm(context)
                } else {
                    // Переход к следующей задаче
                    val updatedSession = session.copy(currentTaskIndex = nextIndex)
                    session = updatedSession
                    MathBlitzStorage.saveActiveSession(context, updatedSession)
                }
            } else {
                com.example.project1.util.VibrationUtil.vibrateHeavy(context)
                isWrongAnswer = true
                hintText = hint
            }
        }
    }

    val minutesLeft = (remainingMs / 1000) / 60
    val secondsLeft = (remainingMs / 1000) % 60
    val isUrgent = remainingMs < 30_000L && !session.isFinished

    com.example.project1.ui.wallpaper.AppBackgroundWallpaper {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // ─── Верхний бар (Таймер строго по центру) ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Кнопка закрытия слева
                IconButton(
                    onClick = {
                        com.example.project1.util.VibrationUtil.vibrateTick(context)
                        if (session.isFinished) {
                            MathBlitzStorage.clearSession(context)
                            onFinish()
                        } else {
                            showExitConfirm = true
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Выйти",
                        tint = colors.textSecondary
                    )
                }

                // Таймер строго по центру экрана
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isUrgent) Color(0xFFFF3D00).copy(alpha = 0.2f) else colors.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isUrgent) Color(0xFFFF3D00) else colors.surfaceBorder
                    ),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isUrgent) Icons.Rounded.LocalFireDepartment else Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = if (isUrgent) Color(0xFFFF3D00) else colors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = String.format("%02d:%02d", minutesLeft, secondsLeft),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUrgent) Color(0xFFFF3D00) else colors.textPrimary
                        )
                    }
                }

                // Куш справа
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+${session.potentialWinCoins}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(Modifier.width(4.dp))
                    CoinIcon(14)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ─── Прогресс по задачам ────────────────────────────────────────────────
            if (!session.isFinished) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    session.tasks.indices.forEach { index ->
                        val isDone = index < session.currentTaskIndex
                        val isCurrent = index == session.currentTaskIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    when {
                                        isDone -> Color(0xFF4CAF50)
                                        isCurrent -> colors.primary
                                        else -> colors.surfaceBorder
                                    }
                                )
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Задача ${session.currentTaskIndex + 1} из ${session.tasks.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = session.config.category.title,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ─── Основное тело: либо задача, либо финал ───────────────────────────
            if (session.isFinished) {
                // Экран результатов
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (session.isWon) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "БЛИЦ ПРОЙДЕН!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Ты решил все задачи вовремя и увеличил свои монеты!",
                                fontSize = 14.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(20.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "+${session.potentialWinCoins}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    CoinIcon(24)
                                }
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.HourglassBottom,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "ВРЕМЯ ВЫШЛО!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5252)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "К сожалению, ты не успел решить все задачи до истечения времени.",
                                fontSize = 14.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Ставка в ${session.config.betCoins} монет сгорела.",
                                fontSize = 13.sp,
                                color = Color(0xFFFF5252),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.height(32.dp))

                        PrimaryGradientButton(
                            onClick = {
                                MathBlitzStorage.clearSession(context)
                                onFinish()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = if (session.isWon) "Забрать награду" else "Завершить",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                // Карточка текущей задачи
                val currentTask = session.tasks.getOrNull(session.currentTaskIndex)
                if (currentTask != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Условие задачи:",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                                Spacer(Modifier.height(12.dp))

                                // Рендеринг формулы
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.background)
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TaskLatexView(currentTask.latexStatement)
                                }

                                Spacer(Modifier.height(20.dp))

                                // Поле ввода ответа
                                val isMatrixTask = currentTask.category.contains("Матриц", ignoreCase = true)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ваш ответ:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )

                                    if (isMatrixTask) {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(colors.surface)
                                                .border(1.dp, colors.surfaceBorder, RoundedCornerShape(8.dp))
                                                .padding(2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (!isMatrixInputMode) colors.primaryBrush else androidx.compose.ui.graphics.SolidColor(Color.Transparent))
                                                    .clickable {
                                                        com.example.project1.util.VibrationUtil.vibrateTick(context)
                                                        isMatrixInputMode = false
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Число",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (!isMatrixInputMode) Color.White else colors.textSecondary
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isMatrixInputMode) colors.primaryBrush else androidx.compose.ui.graphics.SolidColor(Color.Transparent))
                                                    .clickable {
                                                        com.example.project1.util.VibrationUtil.vibrateTick(context)
                                                        isMatrixInputMode = true
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Матрица (${matrixRows}×${matrixCols})",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isMatrixInputMode) Color.White else colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))

                                if (isMatrixInputMode) {
                                    // Переключатель размеров матрицы
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(2 to 2, 3 to 3, 2 to 3, 3 to 2, 1 to 3, 3 to 1, 1 to 2, 2 to 1).forEach { (r, c) ->
                                            val isSelected = matrixRows == r && matrixCols == c
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) colors.primary else colors.surface)
                                                    .border(1.dp, if (isSelected) colors.primary else colors.surfaceBorder, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        com.example.project1.util.VibrationUtil.vibrateTick(context)
                                                        setMatrixDimensions(r, c)
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "${r}×${c}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else colors.textSecondary
                                                )
                                            }
                                        }
                                    }

                                    val cellWidth = when (matrixCols) {
                                        1 -> 110.dp
                                        2 -> 76.dp
                                        3 -> 56.dp
                                        else -> 44.dp
                                    }

                                    // Динамическая матричная сетка с визуальными скобками
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = colors.background,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isWrongAnswer) Color(0xFFFF5252) else colors.surfaceBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "[",
                                                fontSize = (32 + matrixRows * 14).sp,
                                                fontWeight = FontWeight.ExtraLight,
                                                color = colors.primary
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                for (r in 0 until matrixRows) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        for (c in 0 until matrixCols) {
                                                            val cellVal = matrixGrid.getOrNull(r)?.getOrNull(c) ?: ""
                                                            val placeholder = "a${r + 1}${c + 1}"
                                                            MatrixCell(
                                                                value = cellVal,
                                                                onValueChange = { updateMatrixCell(r, c, it) },
                                                                placeholder = placeholder,
                                                                cellWidth = cellWidth
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "]",
                                                fontSize = (32 + matrixRows * 14).sp,
                                                fontWeight = FontWeight.ExtraLight,
                                                color = colors.primary
                                            )
                                        }
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = answerText,
                                        onValueChange = {
                                            answerText = it
                                            isWrongAnswer = false
                                            hintText = null
                                        },
                                        placeholder = { Text("Например: 5, -2, 1/2, e-1, -i", color = colors.textSecondary.copy(alpha = 0.5f)) },
                                        isError = isWrongAnswer,
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.primary,
                                            unfocusedBorderColor = colors.surfaceBorder,
                                            errorBorderColor = Color(0xFFFF5252),
                                            focusedTextColor = colors.textPrimary,
                                            unfocusedTextColor = colors.textPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Подсказка при ошибке
                                if (hintText != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFFF5252).copy(alpha = 0.12f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Lightbulb,
                                                contentDescription = null,
                                                tint = Color(0xFFFF8A80),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = hintText!!,
                                                fontSize = 12.sp,
                                                color = Color(0xFFFF8A80),
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Кнопка отправки
                            PrimaryGradientButton(
                                onClick = { submitAnswer() },
                                enabled = answerText.isNotBlank() && !isChecking,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                if (isChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Проверяем ответ...", color = Color.White)
                                } else {
                                    Text(
                                        text = if (session.currentTaskIndex == session.tasks.size - 1)
                                            "Завершить блиц" else "Ответить и продолжить",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Диалог подтверждения выхода в меню
        if (showExitConfirm) {
            AlertDialog(
                onDismissRequest = { showExitConfirm = false },
                title = {
                    Text("Выйти в меню?", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                },
                text = {
                    Text(
                        text = "Блиц продолжится в фоне, но таймер НЕ остановится! Вы сможете вернуться из главного меню в любой момент, пока не выйдет время.",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExitConfirm = false
                            onClose()
                        }
                    ) {
                        Text("Свернуть", color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitConfirm = false }) {
                        Text("Остаться", color = colors.textSecondary)
                    }
                },
                containerColor = colors.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun MatrixCell(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    cellWidth: androidx.compose.ui.unit.Dp = 76.dp
) {
    val colors = AppTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 11.sp,
                color = colors.textSecondary.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = colors.textPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.surfaceBorder,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary
        ),
        modifier = Modifier
            .width(cellWidth)
            .height(52.dp)
    )
}

