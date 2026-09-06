package com.example.project1.ui.screens.daily

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.project1.ui.components.CoinIcon
import com.example.project1.ui.theme.AppTheme
import java.util.Calendar
import java.util.TimeZone

/**
 * Диалог-календарь:
 * - Подсвечивает решённые задачи зелёным цветом
 * - Подсвечивает активную стрик-сессию оранжевым цветом (даты соединены между собой лентой)
 * - Подсвечивает заморозки голубым цветом (тоже соединены в цепочку)
 * - Позволяет купить заморозку стрика за 79 монет (с выбором количества)
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
    onDismiss: () -> Unit
) {
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
            set(year, month, dom, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.get(Calendar.YEAR).toLong() * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }

    fun prevDayCode(year: Int, month: Int, dom: Int): Long {
        val c = Calendar.getInstance(msk).apply {
            set(year, month, dom, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, -1)
        }
        return c.get(Calendar.YEAR).toLong() * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }

    fun nextDayCode(year: Int, month: Int, dom: Int): Long {
        val c = Calendar.getInstance(msk).apply {
            set(year, month, dom, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, 1)
        }
        return c.get(Calendar.YEAR).toLong() * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }

    fun prevMonth() {
        if (displayMonth == 0) { displayMonth = 11; displayYear-- }
        else displayMonth--
    }
    fun nextMonth() {
        if (isCurrentMonth) return
        if (displayMonth == 11) { displayMonth = 0; displayYear++ }
        else displayMonth++
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        modifier = Modifier.border(1.dp, colors.surfaceBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { prevMonth() }) {
                    Text("‹", color = colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Light)
                }
                Text(
                    text = "${monthNames[displayMonth]} $displayYear",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                IconButton(
                    onClick = { nextMonth() },
                    enabled = !isCurrentMonth
                ) {
                    Text(
                        "›",
                        color = if (isCurrentMonth) colors.textTertiary else colors.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

                // Сетка дней
                val totalCells = startDow + daysInMonth
                val rows = (totalCells + 6) / 7
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

                                // Соединения активной цепочки по горизонтали
                                val prevCode = prevDayCode(displayYear, displayMonth, dom)
                                val nextCode = nextDayCode(displayYear, displayMonth, dom)
                                val connectLeft = isActiveStreak && col > 0 && activeStreakDays.contains(prevCode)
                                val connectRight = isActiveStreak && col < 6 && activeStreakDays.contains(nextCode)

                                val streakConnectColor = Color(0xFFFF6D00).copy(alpha = 0.38f)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Соединительная лента позади кружков
                                    if (isActiveStreak) {
                                        Row(modifier = Modifier.fillMaxSize()) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .padding(vertical = 6.dp)
                                                    .background(
                                                        if (connectLeft) streakConnectColor else Color.Transparent
                                                    )
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .padding(vertical = 6.dp)
                                                    .background(
                                                        if (connectRight) streakConnectColor else Color.Transparent
                                                    )
                                            )
                                        }
                                    }

                                    // Кружок дня
                                    val circleBg = when {
                                        isActiveStreak && isFrozen -> Color(0xFF00B0FF)
                                        isActiveStreak && isSolved -> Color(0xFFFF6D00)
                                        !isActiveStreak && isSolved -> Color(0xFF388E3C)
                                        !isActiveStreak && isFrozen -> Color(0xFF00B0FF).copy(alpha = 0.7f)
                                        else -> Color.Transparent
                                    }

                                    val circleBorder = if (isToday && !isSolved && !isFrozen) {
                                        BorderStroke(1.5.dp, colors.primary)
                                    } else null

                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(circleBg)
                                            .then(
                                                if (circleBorder != null) Modifier.border(circleBorder, CircleShape)
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            isFrozen -> {
                                                Text("❄️", fontSize = 14.sp)
                                            }
                                            isActiveStreak && isSolved -> {
                                                Text("🔥", fontSize = 14.sp)
                                            }
                                            !isActiveStreak && isSolved -> {
                                                // Прошлые решённые дни — белое число на зелёном фоне
                                                Text(
                                                    text = "$dom",
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            else -> {
                                                Text(
                                                    text = "$dom",
                                                    color = when {
                                                        isFuture -> colors.textTertiary
                                                        isToday  -> colors.primary
                                                        else     -> colors.textSecondary
                                                    },
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

                Spacer(modifier = Modifier.height(10.dp))

                // Легенда
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = Color(0xFFFF6D00), icon = "🔥", label = "Стрик")
                    LegendItem(color = Color(0xFF00B0FF), icon = "❄️", label = "Заморозка")
                    LegendItem(color = Color(0xFF388E3C), icon = null, label = "Решено")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Подпись стрейка
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (streak > 0) colors.primarySubtle
                            else colors.surfaceElevated
                        )
                        .padding(vertical = 10.dp, horizontal = 16.dp)
                ) {
                    Text("🔥", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (streak > 0) "$streak ${streakDaysLabel(streak)} подряд"
                        else "Начни streak сегодня!",
                        color = if (streak > 0) colors.primary else colors.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Блок покупки заморозки стрика
                val totalCost = buyCount * 79
                val canAfford = coins >= totalCost

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                    border = BorderStroke(1.dp, colors.surfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("❄️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Заморозка стрика",
                                    color = Color(0xFF40C4FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Text(
                                text = "В наличии: $freezeCount",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Спасает стрик, если за весь день не была решена задача (до 3:00 МСК). Засчитает +1 к серии!",
                            color = colors.textSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Счётчик количества
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surface)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { if (buyCount > 1) buyCount-- },
                                    modifier = Modifier.size(32.dp),
                                    enabled = buyCount > 1
                                ) {
                                    Text(
                                        "−",
                                        color = if (buyCount > 1) colors.textPrimary else colors.textTertiary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "$buyCount",
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { if (buyCount < 99) buyCount++ },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text(
                                        "+",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Кнопка покупки
                            Button(
                                onClick = {
                                    val now = System.currentTimeMillis()
                                    if (now - lastBuyTime >= 1000L) {
                                        showConfirmDialog = true
                                    }
                                },
                                enabled = canAfford,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    disabledContainerColor = colors.surfaceBorder
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (canAfford) "Купить ($totalCost" else "Нужно $totalCost",
                                        color = if (canAfford) Color.White else colors.textTertiary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    CoinIcon(size = 14)
                                    if (canAfford) {
                                        Text(
                                            text = ")",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = "Баланс: $coins ",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                            CoinIcon(size = 13)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Закрыть",
                    color = colors.textSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )

    if (showConfirmDialog) {
        val totalCost = buyCount * 79
        val freezeWordText = freezeWord(buyCount)
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = colors.surface,
            modifier = Modifier.border(1.dp, colors.surfaceBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("❄️", fontSize = 20.sp)
                    Text(
                        text = "Покупка заморозки",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Купить $buyCount $freezeWordText за $totalCost",
                            color = colors.textPrimary,
                            fontSize = 14.sp
                        )
                        CoinIcon(size = 15)
                        Text(
                            text = "?",
                            color = colors.textPrimary,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "Заморозка автоматически спасёт ваш стрик в случае пропуска 1 дня.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - lastBuyTime >= 1000L) {
                            lastBuyTime = now
                            showConfirmDialog = false
                            onBuyFreezes(buyCount)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Купить", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Отмена", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun LegendItem(color: Color, icon: String?, label: String) {
    val colors = AppTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Text(icon, fontSize = 9.sp)
            }
        }
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Склонение слова «день» под число */
fun streakDaysLabel(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..19 -> "дней"
        mod10 == 1       -> "день"
        mod10 in 2..4    -> "дня"
        else             -> "дней"
    }
}

/** Склонение слова «заморозка» под число */
fun freezeWord(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..19 -> "заморозок"
        mod10 == 1       -> "заморозку"
        mod10 in 2..4    -> "заморозки"
        else             -> "заморозок"
    }
}
