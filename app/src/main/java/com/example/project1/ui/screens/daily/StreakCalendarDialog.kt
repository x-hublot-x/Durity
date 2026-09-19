package com.example.project1.ui.screens.daily

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project1.data.model.DailyIntegralTask
import com.example.project1.data.storage.DailyTaskStorage
import com.example.project1.ui.components.CoinIcon
import com.example.project1.ui.components.PrimaryGradientButton
import com.example.project1.ui.components.TaskLatexView
import com.example.project1.ui.theme.AppTheme
import com.example.project1.util.cleanLatexInText
import com.example.project1.util.latexToUnicode
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import java.util.Calendar
import java.util.TimeZone

data class PressedDayTaskInfo(
    val dayCode: Long,
    val dayOfMonth: Int,
    val monthName: String,
    val task: DailyIntegralTask
)

/**
 * Панель календаря:
 * - Стиль матового стекла как у стандартной нижней панели:
 *   hazeChild размытие фона, градиент Color(0x4D283248) -> Color(0x30121526),
 *   световой контур border Color.White 0.28f -> 0.06f.
 */
@Composable
fun StreakCalendarDialog(
    streak: Int,
    coins: Int,
    freezeCount: Int,
    solvedDays: Set<Long>,
    frozenDays: Set<Long>,
    activeStreakDays: Set<Long>,
    onBuyFreezes: (count: Int) -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = AppTheme.colors
    val msk = TimeZone.getTimeZone("Europe/Moscow")
    val realToday = Calendar.getInstance(msk)
    val realTodayYear  = realToday.get(Calendar.YEAR)
    val realTodayMonth = realToday.get(Calendar.MONTH)
    val realTodayDom   = realToday.get(Calendar.DAY_OF_MONTH)

    var displayYear  by remember { mutableIntStateOf(realTodayYear) }
    var displayMonth by remember { mutableIntStateOf(realTodayMonth) }

    var buyCount by remember { mutableIntStateOf(1) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var lastBuyTime by remember { mutableLongStateOf(0L) }
    var pressedDayInfo by remember { mutableStateOf<PressedDayTaskInfo?>(null) }

    val monthNames = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )

    val firstDayOfMonth = Calendar.getInstance(msk).apply {
        set(displayYear, displayMonth, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val daysInMonth = firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startDow = ((firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7)

    val isCurrentMonth = displayYear == realTodayYear && displayMonth == realTodayMonth

    fun dayCode(year: Int, month: Int, dom: Int): Long {
        val c = Calendar.getInstance(msk).apply {
            set(year, month, dom, 12, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        return c.get(Calendar.YEAR).toLong() * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }
    fun prevDayCode(year: Int, month: Int, dom: Int): Long {
        val c = Calendar.getInstance(msk).apply {
            set(year, month, dom, 12, 0, 0); set(Calendar.MILLISECOND, 0); add(Calendar.DAY_OF_MONTH, -1)
        }
        return c.get(Calendar.YEAR).toLong() * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }
    fun nextDayCode(year: Int, month: Int, dom: Int): Long {
        val c = Calendar.getInstance(msk).apply {
            set(year, month, dom, 12, 0, 0); set(Calendar.MILLISECOND, 0); add(Calendar.DAY_OF_MONTH, 1)
        }
        return c.get(Calendar.YEAR).toLong() * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }
    fun prevMonth() { if (displayMonth == 0) { displayMonth = 11; displayYear-- } else displayMonth-- }
    fun nextMonth() { if (isCurrentMonth) return; if (displayMonth == 11) { displayMonth = 0; displayYear++ } else displayMonth++ }

    val cardShape = RoundedCornerShape(26.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 24.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .then(
                if (hazeState != null) {
                    Modifier.hazeChild(
                        state = hazeState,
                        shape = cardShape,
                        style = HazeDefaults.style(
                            backgroundColor = Color(0x18141828),
                            tint = Color(0x30141828),
                            blurRadius = 32.dp,
                            noiseFactor = 0.04f
                        )
                    )
                } else Modifier
            )
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x4D283248),
                        Color(0x30121526)
                    )
                ),
                shape = cardShape
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.06f)
                    )
                ),
                shape = cardShape
            )
            .clip(cardShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Заголовок с навигацией по месяцам
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    com.example.project1.util.VibrationUtil.vibrateTick(context)
                    prevMonth()
                }) {
                    Text("‹", color = colors.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Light)
                }
                Text(
                    text = "${monthNames[displayMonth]} $displayYear",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = {
                    com.example.project1.util.VibrationUtil.vibrateTick(context)
                    nextMonth()
                }, enabled = !isCurrentMonth) {
                    Text(
                        "›",
                        color = if (isCurrentMonth) colors.textTertiary else colors.textPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            // Разделитель
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Заголовки дней недели
            val dowLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
            Row(modifier = Modifier.fillMaxWidth()) {
                dowLabels.forEach { label ->
                    Text(
                        text = label,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Сетка дней с непрерывным отслеживанием ведения пальца
            val totalCells = startDow + daysInMonth
            val rows = (totalCells + 6) / 7
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(displayYear, displayMonth, startDow, daysInMonth, rows) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val trackingId = down.id
                            fun updateForOffset(offset: androidx.compose.ui.geometry.Offset) {
                                if (size.width <= 0 || size.height <= 0 || rows <= 0) return
                                val colWidth = size.width.toFloat() / 7f
                                val rowHeight = size.height.toFloat() / rows.toFloat()
                                val col = (offset.x / colWidth).toInt().coerceIn(0, 6)
                                val row = (offset.y / rowHeight).toInt().coerceIn(0, rows - 1)
                                val cell = row * 7 + col
                                val dom = cell - startDow + 1
                                if (dom in 1..daysInMonth) {
                                    val code = dayCode(displayYear, displayMonth, dom)
                                    if (pressedDayInfo?.dayCode != code) {
                                        val task = DailyTaskStorage.getTaskForDay(context, code)
                                        if (task != null) {
                                            com.example.project1.util.VibrationUtil.vibrateTick(context)
                                            pressedDayInfo = PressedDayTaskInfo(
                                                dayCode = code,
                                                dayOfMonth = dom,
                                                monthName = monthNames[displayMonth],
                                                task = task
                                            )
                                        } else {
                                            pressedDayInfo = null
                                        }
                                    }
                                } else {
                                    pressedDayInfo = null
                                }
                            }

                            updateForOffset(down.position)

                            do {
                                val event = awaitPointerEvent()
                                val trackingChange = event.changes.firstOrNull { it.id == trackingId }
                                if (trackingChange != null && trackingChange.pressed) {
                                    updateForOffset(trackingChange.position)
                                }
                            } while (event.changes.any { it.id == trackingId && it.pressed })

                            pressedDayInfo = null
                        }
                    }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (col in 0 until 7) {
                                val cell = row * 7 + col
                                val dom  = cell - startDow + 1
                                if (dom < 1 || dom > daysInMonth) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight())
                                } else {
                                    val code = dayCode(displayYear, displayMonth, dom)
                                    val isSolved = solvedDays.contains(code)
                                    val isFrozen = frozenDays.contains(code)
                                    val isActiveStreak = activeStreakDays.contains(code)
                                    val isToday = isCurrentMonth && dom == realTodayDom
                                    val isFuture = isCurrentMonth && dom > realTodayDom
                                    val prevCode = prevDayCode(displayYear, displayMonth, dom)
                                    val nextCode = nextDayCode(displayYear, displayMonth, dom)
                                    val connectLeft = isActiveStreak && col > 0 && activeStreakDays.contains(prevCode)
                                    val connectRight = isActiveStreak && col < 6 && activeStreakDays.contains(nextCode)
                                    val streakConnectColor = Color(0xFFFF6D00).copy(alpha = 0.38f)

                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isActiveStreak) {
                                            Row(modifier = Modifier.fillMaxSize()) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .padding(vertical = 6.dp)
                                                        .background(if (connectLeft) streakConnectColor else Color.Transparent)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .padding(vertical = 6.dp)
                                                        .background(if (connectRight) streakConnectColor else Color.Transparent)
                                                )
                                            }
                                        }
                                        val circleBg = when {
                                            isActiveStreak && isFrozen  -> Color(0xFF00B0FF)
                                            isActiveStreak && isSolved  -> Color(0xFFFF6D00)
                                            !isActiveStreak && isSolved -> Color(0xFF388E3C)
                                            !isActiveStreak && isFrozen -> Color(0xFF00B0FF).copy(alpha = 0.7f)
                                            else -> Color.Transparent
                                        }
                                        val circleBorder = if (isToday && !isSolved && !isFrozen) BorderStroke(1.5.dp, colors.primary) else null
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(circleBg)
                                                .then(if (circleBorder != null) Modifier.border(circleBorder, CircleShape) else Modifier),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when {
                                                isFrozen -> Icon(
                                                    imageVector = Icons.Rounded.AcUnit,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                isActiveStreak && isSolved -> Icon(
                                                    imageVector = Icons.Rounded.Whatshot,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                !isActiveStreak && isSolved -> Text("$dom", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                else -> Text(
                                                    "$dom",
                                                    color = when { isFuture -> colors.textTertiary; isToday -> colors.primary; else -> colors.textSecondary },
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Легенда
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = Color(0xFFFF6D00), iconVector = Icons.Rounded.Whatshot, label = "Стрик")
                LegendItem(color = Color(0xFF00B0FF), iconVector = Icons.Rounded.AcUnit, label = "Заморозка")
                LegendItem(color = Color(0xFF388E3C), iconVector = Icons.Rounded.Check, label = "Решено")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Стрик-строка
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF6D00).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFFF6D00).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(vertical = 10.dp, horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Whatshot,
                    contentDescription = null,
                    tint = Color(0xFFFF9100),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (streak > 0) "$streak ${streakDaysLabel(streak)} подряд" else "Начни streak сегодня!",
                    color = if (streak > 0) Color(0xFFFF9100) else colors.textSecondary,
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Блок покупки заморозки (полупрозрачная плашка)
            val totalCost = buyCount * 79
            val canAfford = coins >= totalCost
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x35101424))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AcUnit,
                                contentDescription = null,
                                tint = Color(0xFF40C4FF),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Заморозка стрика", color = Color(0xFF40C4FF), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text(text = "В наличии: $freezeCount", color = colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Спасает стрик, если за весь день не была решена задача (до 00:00 МСК). Засчитает +1 к серии!",
                        color = colors.textSecondary, fontSize = 11.5.sp, lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(onClick = {
                                com.example.project1.util.VibrationUtil.vibrateTick(context)
                                if (buyCount > 1) buyCount--
                            }, modifier = Modifier.size(32.dp), enabled = buyCount > 1) {
                                Text("−", color = if (buyCount > 1) colors.textPrimary else colors.textTertiary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "$buyCount", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = {
                                com.example.project1.util.VibrationUtil.vibrateTick(context)
                                if (buyCount < 99) buyCount++
                            }, modifier = Modifier.size(32.dp)) {
                                Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        PrimaryGradientButton(
                            onClick = {
                                com.example.project1.util.VibrationUtil.vibrateClick(context)
                                val now = System.currentTimeMillis()
                                if (now - lastBuyTime >= 1000L) showConfirmDialog = true
                            },
                            enabled = canAfford,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (canAfford) "Купить ($totalCost" else "Нужно $totalCost",
                                    color = if (canAfford) Color.White else colors.textTertiary,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                CoinIcon(size = 14)
                                if (canAfford) Text(text = ")", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.End)) {
                        Text(text = "Баланс: $coins ", color = colors.textSecondary, fontSize = 12.sp)
                        CoinIcon(size = 13)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Кнопка Закрыть
            TextButton(
                onClick = {
                    com.example.project1.util.VibrationUtil.vibrateTick(context)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(vertical = 4.dp)
            ) {
                Text("Закрыть", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Всплывающее превью задачи дня при зажатии / ведении пальца по дате
        AnimatedVisibility(
            visible = pressedDayInfo != null,
            enter = fadeIn(tween(140)) + scaleIn(tween(140), initialScale = 0.94f),
            exit = fadeOut(tween(90)) + scaleOut(tween(90), targetScale = 0.94f),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 14.dp)
        ) {
            pressedDayInfo?.let { info ->
                val popupShape = RoundedCornerShape(22.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xF6232B3E),
                                    Color(0xFA121524)
                                )
                            ),
                            shape = popupShape
                        )
                        .border(
                            width = 1.3.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.10f)
                                )
                            ),
                            shape = popupShape
                        )
                        .clip(popupShape)
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    AnimatedContent(
                        targetState = info,
                        transitionSpec = {
                            fadeIn(tween(120)) togetherWith fadeOut(tween(80))
                        },
                        label = "dayTaskPopupAnim"
                    ) { targetInfo ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Заголовок даты и типа задачи
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarToday,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9100),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${targetInfo.dayOfMonth} ${targetInfo.monthName}",
                                    color = Color(0xFFFF9100),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = " • ",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = targetInfo.task.type,
                                    color = Color(0xFF40C4FF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Формула / математическое условие в формате LaTeX
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.07f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TaskLatexView(latex = targetInfo.task.latexStatement)
                            }

                            // Описание задачи
                            val rawMath = targetInfo.task.latexStatement
                                .removePrefix("$$").removeSuffix("$$")
                                .removePrefix("$").removeSuffix("$")
                                .trim()
                            val desc = cleanLatexInText(targetInfo.task.description).trim()
                            if (desc.isNotBlank() && desc != rawMath) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = desc,
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Диалог подтверждения покупки заморозки
    if (showConfirmDialog) {
        val totalCost = buyCount * 79
        val freezeWordText = freezeWord(buyCount)
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = colors.surface,
            modifier = Modifier.border(1.dp, colors.surfaceBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.AcUnit,
                        contentDescription = null,
                        tint = Color(0xFF40C4FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(text = "Покупка заморозки", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Купить $buyCount $freezeWordText за $totalCost", color = colors.textPrimary, fontSize = 14.sp)
                        CoinIcon(size = 15)
                        Text(text = "?", color = colors.textPrimary, fontSize = 14.sp)
                    }
                    Text(
                        text = "Заморозка автоматически спасёт ваш стрик в случае пропуска 1 дня.",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                PrimaryGradientButton(
                    onClick = {
                        com.example.project1.util.VibrationUtil.vibrateClick(context)
                        val now = System.currentTimeMillis()
                        if (now - lastBuyTime >= 1000L) { lastBuyTime = now; showConfirmDialog = false; onBuyFreezes(buyCount) }
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("Купить", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    com.example.project1.util.VibrationUtil.vibrateTick(context)
                    showConfirmDialog = false
                }) { Text("Отмена", color = colors.textSecondary) }
            }
        )
    }
}

@Composable
private fun LegendItem(color: Color, iconVector: androidx.compose.ui.graphics.vector.ImageVector?, label: String) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            if (iconVector != null) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        Text(text = label, color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

fun streakDaysLabel(n: Int): String {
    val mod100 = n % 100; val mod10 = n % 10
    return when { mod100 in 11..19 -> "дней"; mod10 == 1 -> "день"; mod10 in 2..4 -> "дня"; else -> "дней" }
}

fun freezeWord(n: Int): String {
    val mod100 = n % 100; val mod10 = n % 10
    return when { mod100 in 11..19 -> "заморозок"; mod10 == 1 -> "заморозку"; mod10 in 2..4 -> "заморозки"; else -> "заморозок" }
}
