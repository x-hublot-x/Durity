package com.example.project1.ui.screens.blitz

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.SquareFoot
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project1.ui.components.PrimaryGradientButton
import com.example.project1.ui.components.GradientText
import com.example.project1.BuildConfig
import com.example.project1.data.model.BlitzCategory
import com.example.project1.data.model.BlitzConfig
import com.example.project1.data.model.BlitzTask
import com.example.project1.data.repository.MathBlitzRepository
import com.example.project1.data.storage.UserRatingStorage
import com.example.project1.service.MathBlitzNotificationManager
import com.example.project1.ui.components.CoinIcon
import com.example.project1.ui.components.UserRatingDialog
import com.example.project1.ui.theme.AppTheme
import com.example.project1.util.VibrationUtil
import com.example.project1.util.hapticClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

enum class BlitzPreset(
    val title: String,
    val icon: ImageVector,
    val durationMinutes: Int,
    val taskCount: Int,
    val subtitle: String
) {
    SPRINT("Спринт", Icons.Rounded.Bolt, 2, 3, "2 мин • 3 задачи"),
    CLASSIC("Классика", Icons.Rounded.TrackChanges, 3, 4, "3 мин • 4 задачи"),
    MARATHON("Марафон", Icons.Rounded.Psychology, 5, 5, "5 мин • 5 задач"),
    CUSTOM("Своё", Icons.Rounded.Tune, 3, 3, "Настрой сам")
}

@Composable
fun MathBlitzSetupScreen(
    userCoins: Int,
    onBack: () -> Unit,
    onStartBlitz: (BlitzConfig, List<BlitzTask>) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    var selectedCategory by remember { mutableStateOf(BlitzCategory.MATRICES) }
    var selectedDifficulty by remember { mutableIntStateOf(1) } // 1, 2, 3
    var selectedPreset by remember { mutableStateOf(BlitzPreset.CLASSIC) }

    var isCountingDown by remember { mutableStateOf(false) }
    var countdownStep by remember { mutableIntStateOf(3) }

    var customDuration by remember { mutableIntStateOf(3) }
    var customTaskCount by remember { mutableIntStateOf(3) }

    val durationMinutes = if (selectedPreset == BlitzPreset.CUSTOM) customDuration else selectedPreset.durationMinutes
    val taskCount = if (selectedPreset == BlitzPreset.CUSTOM) customTaskCount else selectedPreset.taskCount

    var betCoins by remember {
        val initial = when {
            userCoins >= 50 -> 25
            userCoins >= 10 -> 10
            else -> 0
        }
        mutableIntStateOf(initial)
    }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    val isBlitzBlocked = remember { UserRatingStorage.isBlitzBlocked(context) }

    val potentialWin = remember(betCoins, selectedDifficulty, durationMinutes, taskCount) {
        MathBlitzRepository.calculatePotentialWin(betCoins, selectedDifficulty, durationMinutes, taskCount)
    }

    val multiplier = remember(selectedDifficulty, durationMinutes, taskCount) {
        MathBlitzRepository.calculateRewardMultiplier(selectedDifficulty, durationMinutes, taskCount)
    }

    LaunchedEffect(isCountingDown) {
        if (isCountingDown) {
            val config = BlitzConfig(
                category = selectedCategory,
                difficulty = selectedDifficulty,
                durationMinutes = durationMinutes,
                taskCount = taskCount,
                betCoins = betCoins
            )
            // Предзагрузка задач в фоне прямо во время обратного отсчета 3-2-1!
            val preloadedTasksDeferred = async(Dispatchers.IO) {
                MathBlitzRepository.generateBlitzTasks(config, BuildConfig.GEMINI_API_KEY)
            }

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            fun playTick() {
                try {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                } catch (_: Exception) {}
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(30)
                    }
                } catch (_: Exception) {}
            }

            // 3
            countdownStep = 3
            playTick()
            delay(850)

            // 2
            countdownStep = 2
            playTick()
            delay(850)

            // 1
            countdownStep = 1
            playTick()
            delay(850)

            // СТАРТ!
            countdownStep = 0
            try {
                view.playSoundEffect(SoundEffectConstants.CLICK)
            } catch (_: Exception) {}
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 40, 60), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(80)
                }
            } catch (_: Exception) {}
            delay(550)

            val tasks = preloadedTasksDeferred.await()
            onStartBlitz(config, tasks)
            isCountingDown = false
        }
    }

    val colors = AppTheme.colors

    BackHandler {
        onBack()
    }

    com.example.project1.ui.wallpaper.AppBackgroundWallpaper {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Шапка: кнопка назад + баланс монет ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        VibrationUtil.vibrateTick(context)
                        onBack()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f)),
                    modifier = Modifier.hapticClickable(context) {
                        showRatingDialog = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CoinIcon(18)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$userCoins",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Заголовок ─────────────────────────────────────────────────────
            Text(
                text = "Блиц по математике",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary
            )
            Text(
                text = "Реши задачи на время и умножь свою ставку",
                fontSize = 13.sp,
                color = colors.textSecondary
            )

            Spacer(Modifier.height(24.dp))

            // ── 1. Раздел математики (Горизонтальный скролл) ──────────────────
            GradientText(
                text = "1. РАЗДЕЛ МАТЕМАТИКИ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BlitzCategory.values().forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Card(
                        modifier = Modifier
                            .width(132.dp)
                            .height(86.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .hapticClickable(context) { selectedCategory = cat },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) colors.primary.copy(alpha = 0.18f) else colors.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            brush = if (isSelected) colors.primaryBrush else androidx.compose.ui.graphics.SolidColor(colors.surfaceBorder)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier.size(26.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                when (cat) {
                                    BlitzCategory.MATRICES -> Icon(
                                        imageVector = Icons.Rounded.Calculate,
                                        contentDescription = null,
                                        tint = if (isSelected) colors.primary else colors.textSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    BlitzCategory.DERIVATIVES -> Icon(
                                        imageVector = Icons.Rounded.ShowChart,
                                        contentDescription = null,
                                        tint = if (isSelected) colors.primary else colors.textSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    BlitzCategory.INTEGRALS -> Text(
                                        text = "∫",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colors.primary else colors.textSecondary
                                    )
                                    BlitzCategory.LIMITS -> Icon(
                                        imageVector = Icons.Rounded.TrackChanges,
                                        contentDescription = null,
                                        tint = if (isSelected) colors.primary else colors.textSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    BlitzCategory.COMPLEX -> Text(
                                        text = "ℂ",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colors.primary else colors.textSecondary
                                    )
                                    BlitzCategory.TRIGONOMETRY -> Icon(
                                        imageVector = Icons.Rounded.SquareFoot,
                                        contentDescription = null,
                                        tint = if (isSelected) colors.primary else colors.textSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Text(
                                text = cat.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) colors.primary else colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── 2. Уровень сложности (3 крупные карточки) ─────────────────────
            GradientText(
                text = "2. УРОВЕНЬ СЛОЖНОСТИ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(1, "Новичок", "Устный счет"),
                    Triple(2, "Студент", "Универ"),
                    Triple(3, "Профессор", "Хардкор")
                ).forEach { (diff, title, desc) ->
                    val isSelected = selectedDifficulty == diff
                    val cardColor = when (diff) {
                        1 -> Color(0xFF4CAF50)
                        2 -> Color(0xFFFFB300)
                        else -> Color(0xFFFF5252)
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .hapticClickable(context) { selectedDifficulty = diff },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) cardColor.copy(alpha = 0.16f) else colors.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) cardColor else colors.surfaceBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(cardColor)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) cardColor else colors.textPrimary
                            )
                            Text(
                                text = desc,
                                fontSize = 10.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── 3. Формат блица (Пресеты) ─────────────────────────────────────
            GradientText(
                text = "3. ФОРМАТ РАУНДА",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BlitzPreset.values().forEach { preset ->
                    val isSelected = selectedPreset == preset
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .hapticClickable(context) { selectedPreset = preset },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) colors.primary.copy(alpha = 0.16f) else colors.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            brush = if (isSelected) colors.primaryBrush else androidx.compose.ui.graphics.SolidColor(colors.surfaceBorder)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = preset.icon,
                                contentDescription = null,
                                tint = if (isSelected) colors.primary else colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = preset.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) colors.primary else colors.textPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = if (preset == BlitzPreset.CUSTOM) "Ручной" else "${preset.durationMinutes}м • ${preset.taskCount}з",
                                fontSize = 9.sp,
                                color = colors.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Кастомная настройка если выбран пресет "Своё"
            AnimatedVisibility(visible = selectedPreset == BlitzPreset.CUSTOM) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Время раунда:", fontSize = 12.sp, color = colors.textSecondary)
                            GradientText(
                                text = "$customDuration мин",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = customDuration.toFloat(),
                            onValueChange = { customDuration = it.toInt() },
                            valueRange = 1f..7f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                        )

                        val taskCountLabel = when {
                            customTaskCount % 10 == 1 && customTaskCount % 100 != 11 -> "$customTaskCount задача"
                            customTaskCount % 10 in 2..4 && customTaskCount % 100 !in 12..14 -> "$customTaskCount задачи"
                            else -> "$customTaskCount задач"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Количество задач:", fontSize = 12.sp, color = colors.textSecondary)
                            GradientText(
                                text = taskCountLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = customTaskCount.toFloat(),
                            onValueChange = { customTaskCount = it.toInt() },
                            valueRange = 2f..20f,
                            steps = 17,
                            colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── 4. Ставка в монетах ───────────────────────────────────────────
            GradientText(
                text = "4. СТАВКА",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Сумма ставки:", fontSize = 13.sp, color = colors.textSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$betCoins",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.textPrimary
                            )
                            Spacer(Modifier.width(6.dp))
                            CoinIcon(20)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 25, 50, 100).forEach { preset ->
                            val isSel = betCoins == preset
                            val enabled = userCoins >= preset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) colors.primaryBrush else SolidColor(colors.background))
                                    .hapticClickable(context = context, enabled = enabled) { betCoins = preset }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$preset",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!enabled) colors.textSecondary.copy(alpha = 0.3f)
                                    else if (isSel) Color.White
                                    else colors.textPrimary
                                )
                            }
                        }

                        // Кнопка "Всё"
                        val isAll = betCoins == userCoins && userCoins >= 10
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAll) colors.primaryBrush else SolidColor(colors.background))
                                .hapticClickable(context = context, enabled = userCoins >= 10) { betCoins = userCoins }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAll) {
                                Text(
                                    text = "Всё",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                GradientText(
                                    text = "Всё",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (userCoins < 10) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Для начала блица нужно минимум 10 монет. Реши задачу дня, чтобы пополнить баланс!",
                            fontSize = 11.sp,
                            color = Color(0xFFFF5252),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Итоговая карточка выигрыша ────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50).copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Множитель: ${String.format("%.2fx", multiplier)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Возможный выигрыш:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "+$potentialWin",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4CAF50)
                            )
                            Spacer(Modifier.width(6.dp))
                            CoinIcon(22)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            if (isBlitzBlocked) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFEF5350).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Блиц заблокирован (рейтинг ≤ -100). Решайте задачи дня и соблюдайте лимиты экранного времени, чтобы поднять рейтинг!",
                            fontSize = 12.sp,
                            color = Color(0xFFFF8A80),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // ── Кнопка Старта ─────────────────────────────────────────────────
            PrimaryGradientButton(
                onClick = { showConfirmDialog = true },
                enabled = !isBlitzBlocked && betCoins in 10..userCoins,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = if (isBlitzBlocked) "Блиц заблокирован" else "Начать блиц (${betCoins} монет)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Полноэкранный обратный отсчет перед стартом блица (3 -> 2 -> 1 -> СТАРТ!)
        AnimatedVisibility(
            visible = isCountingDown,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(250))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.84f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.94f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .scale(pulseScale)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        colors.primary.copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .clip(CircleShape)
                                .background(colors.surface.copy(alpha = 0.92f))
                                .border(3.5.dp, colors.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = countdownStep,
                                transitionSpec = {
                                    (scaleIn(initialScale = 1.5f, animationSpec = tween(240)) + fadeIn(tween(180)))
                                        .togetherWith(scaleOut(targetScale = 0.6f, animationSpec = tween(180)) + fadeOut(tween(150)))
                                },
                                contentAlignment = Alignment.Center,
                                label = "countdown"
                            ) { step ->
                                if (step > 0) {
                                    Text(
                                        text = "$step",
                                        fontSize = 64.sp,
                                        fontWeight = FontWeight.Black,
                                        color = colors.primary
                                    )
                                } else {
                                    Text(
                                        text = "СТАРТ!",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF4CAF50),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    Text(
                        text = if (countdownStep > 0) "Приготовься к решению..." else "Погнали!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }

    // Диалог подтверждения старта
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Готов к блицу?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Ставка $betCoins монет будет списана с баланса немедленно.",
                        fontSize = 13.sp,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "• Таймер ($durationMinutes мин) продолжит тикать в реальном времени, даже если выйти из приложения!\n• Запущенный блиц нельзя отменить.\n• Реши все $taskCount задач вовремя, чтобы забрать +$potentialWin монет.",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                PrimaryGradientButton(
                    onClick = {
                        showConfirmDialog = false
                        (context as? Activity)?.let { MathBlitzNotificationManager.requestNotificationPermission(it) }
                        countdownStep = 3
                        isCountingDown = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("В бой!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Отмена", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showRatingDialog) {
        UserRatingDialog(
            rating = UserRatingStorage.getRating(context),
            onDismiss = { showRatingDialog = false }
        )
    }
}
