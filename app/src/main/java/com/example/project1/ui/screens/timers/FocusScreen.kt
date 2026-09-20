package com.example.project1.ui.screens.timers

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.project1.data.storage.FocusStorage
import com.example.project1.data.storage.FocusTask
import com.example.project1.service.FocusSessionManager
import com.example.project1.ui.components.CoinIcon
import com.example.project1.ui.theme.AppTheme
import com.example.project1.util.AmbientSoundGenerator
import com.example.project1.util.AmbientSoundType
import com.example.project1.util.VibrationUtil

private data class SoundItemDef(
    val type: AmbientSoundType,
    val icon: ImageVector,
    val label: String,
    val activeColor: Color
)

@Composable
fun FocusScreen() {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val isActive = FocusSessionManager.isActive
    val isPaused = FocusSessionManager.isPaused
    val remainingSec = FocusSessionManager.remainingSeconds
    val totalSec = FocusSessionManager.totalSeconds
    val selectedSounds = FocusSessionManager.selectedSounds

    // Загрузка сохраненных настроек
    var selectedMinutesPreset by remember { mutableIntStateOf(FocusStorage.getMinutes(context)) }
    var chosenSounds by remember { mutableStateOf<Set<AmbientSoundType>>(FocusStorage.getSounds(context)) }
    var strictMode by remember { mutableStateOf(FocusStorage.getStrictMode(context)) }
    var tasks by remember { mutableStateOf<List<FocusTask>>(FocusStorage.getTasks(context)) }
    var newTaskText by remember { mutableStateOf("") }
    var showCustomTimeDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isActive, selectedSounds) {
        if (isActive) {
            chosenSounds = selectedSounds
        } else {
            chosenSounds = FocusStorage.getSounds(context)
        }
    }

    val progress = if (totalSec > 0) remainingSec.toFloat() / totalSec.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = if (isActive) progress else 1f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "FocusTimerProgress"
    )
    val minutes = remainingSec / 60
    val seconds = remainingSec % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val currentActiveSounds = if (isActive) selectedSounds else chosenSounds

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 0.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Круговой таймер / Прогресс сессии ────────────────────────────────
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidthPx = 14.dp.toPx()
                    val radius = (size.minDimension - strokeWidthPx) / 2f
                    val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)

                    // Фоновое кольцо (трек)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = radius,
                        center = centerOffset,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx)
                    )

                    // Плавный градиент для окружности таймера (переход между #FF6600 и #FF8000)
                    val gradientBrush = if (isPaused) {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFFF8000).copy(alpha = 0.65f),
                                Color(0xFFFF6600).copy(alpha = 0.65f)
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFFF6600),
                                Color(0xFFFF8000),
                                Color(0xFFFF6600)
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                        )
                    }

                    val sweepAngle = 360f * (if (isActive) animatedProgress else 1f)

                    // Мягкое свечение (Glow эффект)
                    if (sweepAngle > 0f) {
                        drawArc(
                            brush = gradientBrush,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(centerOffset.x - radius, centerOffset.y - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            alpha = 0.28f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidthPx + 6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    // Основная градиентная дуга
                    if (sweepAngle > 0f) {
                        drawArc(
                            brush = gradientBrush,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(centerOffset.x - radius, centerOffset.y - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidthPx,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isActive) {
                        Text(
                            text = timeFormatted,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isPaused) "Пауза" else "Фокусировка...",
                            fontSize = 13.sp,
                            color = if (isPaused) Color(0xFFFFB300) else colors.textSecondary
                        )
                    } else {
                        Text(
                            text = "$selectedMinutesPreset:00",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Готов к фокусу",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Награда за сессию
                    val reward = (if (isActive) totalSec / 60 else selectedMinutesPreset) * 2
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        CoinIcon(size = 14)
                        Text(
                            text = "+$reward",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFD54F)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Кнопки управления (Старт / Пауза / Стоп) ─────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isActive) {
                    // Кнопка СТАРТ
                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .weight(1f)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.primaryBrush)
                            .clickable {
                                VibrationUtil.vibrateTick(context)
                                FocusSessionManager.startSession(
                                    context = context,
                                    minutes = selectedMinutesPreset,
                                    sounds = chosenSounds,
                                    isStrict = strictMode
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Старт",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Начать фокус",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Кнопка Пауза / Продолжить
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .clickable {
                                VibrationUtil.vibrateTick(context)
                                if (isPaused) {
                                    FocusSessionManager.resumeSession()
                                } else {
                                    FocusSessionManager.pauseSession()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Возобновить" else "Пауза",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Кнопка Стоп
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFE53935).copy(alpha = 0.4f), CircleShape)
                            .clickable {
                                VibrationUtil.vibrateClick(context)
                                FocusSessionManager.stopSession(context, completed = false)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Завершить",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Пресеты времени (если не активен) ────────────────────────────────
            if (!isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Длительность сессии",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Своё время ⚙",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                VibrationUtil.vibrateTick(context)
                                showCustomTimeDialog = true
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val presets = listOf(15, 25, 45, 60, 90)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { mins ->
                        val isSelected = selectedMinutesPreset == mins
                        val presetBgBrush = if (isSelected) {
                            if (colors.isGradient) colors.primaryBrush
                            else Brush.linearGradient(listOf(colors.primary, colors.primary))
                        } else {
                            Brush.linearGradient(listOf(colors.surface, colors.surface))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(presetBgBrush)
                                .then(
                                    if (isSelected) Modifier.border(1.5.dp, colors.primaryBrush, RoundedCornerShape(12.dp))
                                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                )
                                .clickable {
                                    VibrationUtil.vibrateTick(context)
                                    selectedMinutesPreset = mins
                                    FocusStorage.saveMinutes(context, mins)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$mins м",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Фоновые звуки (Визуальная рамка-контейнер) ───────────────────────
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.85f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.surfaceBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    // Заголовок панели звуков
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Звуковая атмосфера",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Комбинируй звуки под настроение",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        if (currentActiveSounds.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.primary.copy(alpha = 0.2f))
                                    .border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Активно: ${currentActiveSounds.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val soundItems = listOf(
                        SoundItemDef(AmbientSoundType.NONE, Icons.AutoMirrored.Rounded.VolumeOff, "Тишина", Color(0xFF9E9E9E)),
                        SoundItemDef(AmbientSoundType.RAIN, Icons.Rounded.WaterDrop, "Дождь", Color(0xFF29B6F6)),
                        SoundItemDef(AmbientSoundType.CAMPFIRE, Icons.Rounded.LocalFireDepartment, "Костёр", Color(0xFFFF7043)),
                        SoundItemDef(AmbientSoundType.CRICKETS, Icons.Rounded.NightsStay, "Сверчки", Color(0xFFAB47BC)),
                        SoundItemDef(AmbientSoundType.BIRDS, Icons.Rounded.Park, "Птицы", Color(0xFF66BB6A)),
                        SoundItemDef(AmbientSoundType.WATER_STREAM, Icons.Rounded.Waves, "Ручей", Color(0xFF26A69A)),
                        SoundItemDef(AmbientSoundType.WINTER_STORM, Icons.Rounded.AcUnit, "Зимняя буря", Color(0xFF80D8FF)),
                        SoundItemDef(AmbientSoundType.WIND, Icons.Rounded.Air, "Ветер", Color(0xFF90A4AE)),
                        SoundItemDef(AmbientSoundType.THUNDERSTORM, Icons.Rounded.Bolt, "Гроза", Color(0xFFFFD54F)),
                        SoundItemDef(AmbientSoundType.COFFEE_SHOP, Icons.Rounded.LocalCafe, "Кофейня", Color(0xFF8D6E63))
                    )

                    val soundScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(soundScrollState)
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        soundItems.forEach { item ->
                            val soundType = item.type
                            val isSelected = if (soundType == AmbientSoundType.NONE) {
                                currentActiveSounds.isEmpty()
                            } else {
                                currentActiveSounds.contains(soundType)
                            }

                            val itemColor = item.activeColor
                            val chipBg = if (isSelected) {
                                itemColor.copy(alpha = 0.22f)
                            } else {
                                colors.surfaceElevated
                            }
                            val borderModifier = if (isSelected) {
                                Modifier.border(1.5.dp, itemColor, RoundedCornerShape(16.dp))
                            } else {
                                Modifier.border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .width(78.dp)
                                    .height(76.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(chipBg)
                                    .then(borderModifier)
                                    .clickable {
                                        VibrationUtil.vibrateTick(context)
                                        if (isActive) {
                                            FocusSessionManager.toggleSound(context, soundType)
                                            chosenSounds = FocusSessionManager.selectedSounds
                                        } else {
                                            val newSet = if (soundType == AmbientSoundType.NONE) {
                                                emptySet()
                                            } else {
                                                if (chosenSounds.contains(soundType)) {
                                                    chosenSounds - soundType
                                                } else {
                                                    chosenSounds + soundType
                                                }
                                            }
                                            chosenSounds = newSet
                                            FocusStorage.saveSounds(context, newSet)
                                        }
                                    }
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    AnimatedSoundIcon(
                                        soundType = soundType,
                                        icon = item.icon,
                                        isSelected = isSelected,
                                        activeColor = itemColor,
                                        inactiveColor = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = item.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else colors.textSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Опция строгого режима (Анти-отвлечение) ───────────────────────────
            if (!isActive) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.85f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.surfaceBorder, RoundedCornerShape(20.dp))
                        .clickable {
                            VibrationUtil.vibrateTick(context)
                            strictMode = !strictMode
                            FocusStorage.saveStrictMode(context, strictMode)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = if (strictMode) Color(0xFF69F0AE) else colors.textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Анти-отвлечение",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Блокировать приложения во время фокуса",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        val switchTrackBrush = if (strictMode) {
                            if (colors.isGradient) colors.primaryBrush
                            else Brush.linearGradient(listOf(colors.primary, colors.primary))
                        } else {
                            Brush.linearGradient(listOf(colors.surfaceElevated, colors.surfaceElevated))
                        }

                        val thumbOffset by animateDpAsState(
                            targetValue = if (strictMode) 20.dp else 0.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "SwitchThumbOffset"
                        )

                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(switchTrackBrush)
                                .padding(2.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(x = thumbOffset)
                                    .size(20.dp)
                                    .shadow(2.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Чек-лист задач сессии (To-Do List) ───────────────────────────────
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.85f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.surfaceBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Checklist,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Задачи на сессию",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }

                        if (tasks.isNotEmpty()) {
                            val doneCount = tasks.count { it.isCompleted }
                            Text(
                                text = "$doneCount / ${tasks.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (doneCount == tasks.size) Color(0xFF69F0AE) else colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Поле добавления задачи
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceElevated)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTaskText,
                            onValueChange = { newTaskText = it },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { state ->
                                    if (state.isFocused) {
                                        scope.launch {
                                            delay(200)
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }
                                    }
                                },
                            placeholder = {
                                Text("Например: Решить вариант 3...", fontSize = 13.sp, color = colors.textSecondary)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (newTaskText.isNotBlank()) {
                                    VibrationUtil.vibrateTick(context)
                                    val updated = tasks + FocusTask(title = newTaskText.trim())
                                    tasks = updated
                                    FocusStorage.saveTasks(context, updated)
                                    newTaskText = ""
                                }
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            )

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (newTaskText.isNotBlank()) colors.primary else colors.surfaceBorder)
                                .clickable(enabled = newTaskText.isNotBlank()) {
                                    VibrationUtil.vibrateTick(context)
                                    val updated = tasks + FocusTask(title = newTaskText.trim())
                                    tasks = updated
                                    FocusStorage.saveTasks(context, updated)
                                    newTaskText = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Добавить",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Список задач
                    if (tasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            tasks.forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .clickable {
                                            VibrationUtil.vibrateTick(context)
                                            val updated = tasks.map {
                                                if (it.id == task.id) it.copy(isCompleted = !it.isCompleted) else it
                                            }
                                            tasks = updated
                                            FocusStorage.saveTasks(context, updated)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (task.isCompleted) Color(0xFF69F0AE) else Color.Transparent)
                                            .border(
                                                1.5.dp,
                                                if (task.isCompleted) Color(0xFF69F0AE) else colors.textSecondary,
                                                RoundedCornerShape(6.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (task.isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = task.title,
                                        fontSize = 13.sp,
                                        color = if (task.isCompleted) colors.textSecondary else Color.White,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = {
                                            VibrationUtil.vibrateTick(context)
                                            val updated = tasks.filter { it.id != task.id }
                                            tasks = updated
                                            FocusStorage.saveTasks(context, updated)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Удалить",
                                            tint = colors.textSecondary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Отступ снизу для комфортной прокрутки
            Spacer(modifier = Modifier.height(130.dp))
        }

        // ── Круглая кнопка сброса настроек таймера (справа снизу) ────────────
        AnimatedVisibility(
            visible = !isActive,
            enter = scaleIn(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
            exit = scaleOut(animationSpec = tween(220)) + fadeOut(animationSpec = tween(220)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .shadow(8.dp, CircleShape)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.primaryBrush)
                    .clickable {
                        VibrationUtil.vibrateTick(context)
                        showResetConfirmDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = "Сброс настроек таймера",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    // ── Диалог подтверждения сброса настроек ─────────────────────────────────
    if (showResetConfirmDialog) {
        Dialog(onDismissRequest = { showResetConfirmDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFE53935).copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Сбросить настройки?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Длительность, выбранные звуки, режим анти-отвлечения и задачи будут сброшены к значениям по умолчанию.",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Отмена
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.surfaceElevated)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                .clickable {
                                    VibrationUtil.vibrateTick(context)
                                    showResetConfirmDialog = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Отмена",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                        }

                        // Сбросить
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFE53935), Color(0xFFD32F2F))
                                    )
                                )
                                .clickable {
                                    VibrationUtil.vibrateSuccess(context)
                                    FocusStorage.resetAll(context)
                                    selectedMinutesPreset = 25
                                    chosenSounds = setOf(AmbientSoundType.RAIN)
                                    strictMode = true
                                    tasks = emptyList()
                                    showResetConfirmDialog = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Сбросить",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Диалог настройки своего времени ──────────────────────────────────────
    if (showCustomTimeDialog) {
        var tempMinutes by remember { mutableIntStateOf(selectedMinutesPreset) }

        Dialog(onDismissRequest = { showCustomTimeDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timelapse,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Своя длительность",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "$tempMinutes мин",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Регуляторы + / -
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                VibrationUtil.vibrateTick(context)
                                tempMinutes = (tempMinutes - 5).coerceAtLeast(1)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                        ) {
                            Text(text = "-5", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }

                        IconButton(
                            onClick = {
                                VibrationUtil.vibrateTick(context)
                                tempMinutes = (tempMinutes - 1).coerceAtLeast(1)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "-1", tint = colors.textPrimary)
                        }

                        IconButton(
                            onClick = {
                                VibrationUtil.vibrateTick(context)
                                tempMinutes = (tempMinutes + 1).coerceAtMost(180)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "+1", tint = colors.textPrimary)
                        }

                        IconButton(
                            onClick = {
                                VibrationUtil.vibrateTick(context)
                                tempMinutes = (tempMinutes + 5).coerceAtMost(180)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                        ) {
                            Text(text = "+5", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Slider(
                        value = tempMinutes.toFloat(),
                        onValueChange = { tempMinutes = it.toInt() },
                        valueRange = 1f..180f,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = colors.surfaceElevated
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCustomTimeDialog = false },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена", color = colors.textSecondary)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.primaryBrush)
                                .clickable {
                                    VibrationUtil.vibrateTick(context)
                                    selectedMinutesPreset = tempMinutes
                                    FocusStorage.saveMinutes(context, tempMinutes)
                                    showCustomTimeDialog = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Применить", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedSoundIcon(
    soundType: AmbientSoundType,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    inactiveColor: Color
) {
    val tint = if (isSelected) activeColor else inactiveColor

    if (!isSelected) {
        Icon(
            imageVector = icon,
            contentDescription = soundType.title,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SoundIconAnim")

    when (soundType) {
        AmbientSoundType.RAIN -> {
            val offsetY by infiniteTransition.animateFloat(
                initialValue = -2.5f,
                targetValue = 2.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(450, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "RainDrop"
            )
            Icon(
                imageVector = icon,
                contentDescription = soundType.title,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { translationY = offsetY }
            )
        }
        AmbientSoundType.CAMPFIRE -> {
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.88f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(380, easing = FastOutLinearInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "FireScale"
            )
            val rotation by infiniteTransition.animateFloat(
                initialValue = -6f,
                targetValue = 6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(280, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "FireRot"
            )
            Icon(
                imageVector = icon,
                contentDescription = soundType.title,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    }
            )
        }
        AmbientSoundType.CRICKETS -> {
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.92f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "CricketsBreathe"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.7f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "CricketsGlow"
            )
            Icon(
                imageVector = icon,
                contentDescription = soundType.title,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            )
        }
        AmbientSoundType.BIRDS -> {
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "BirdsHop"
            )
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.92f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "BirdsFlutter"
            )
            Icon(
                imageVector = icon,
                contentDescription = soundType.title,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        translationY = offsetY
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
        AmbientSoundType.WATER_STREAM -> {
            val offsetX by infiniteTransition.animateFloat(
                initialValue = -3f,
                targetValue = 3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "WaterWave"
            )
            val rot by infiniteTransition.animateFloat(
                initialValue = -4f,
                targetValue = 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "WaterTilt"
            )
            Icon(
                imageVector = icon,
                contentDescription = soundType.title,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        translationX = offsetX
                        rotationZ = rot
                    }
            )
        }
        AmbientSoundType.WINTER_STORM -> {
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "SnowSpin"
            )
            Icon(
                imageVector = icon,
                contentDescription = soundType.title,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
        AmbientSoundType.WIND -> {
            val rot by infiniteTransition.animateFloat(
                initialValue = -12f,
                targetValue = 12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "WindSway"
            )
            val offsetX by infiniteTransition.animateFloat(
                initialValue = -3f,
                targetValue = 3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "WindBreeze"
            )
            Icon(
                imageVector = icon,
                contentDescription = soundType.title,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        rotationZ = rot
                        translationX = offsetX
                    }
            )
        }
        AmbientSoundType.THUNDERSTORM -> {
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.22f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ThunderPulse"
            )
            Icon(
                imageVector = icon,
                contentDescription = soundType.title,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
        AmbientSoundType.COFFEE_SHOP -> {
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -3.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "CoffeeSteam"
            )
            Icon(
                imageVector = icon,
                contentDescription = soundType.title,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { translationY = offsetY }
            )
        }
        AmbientSoundType.NONE -> {
            Icon(
                imageVector = icon,
                contentDescription = soundType.title,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
