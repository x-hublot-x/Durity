package com.example.project1.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.project1.R
import com.example.project1.data.storage.AiTestManager
import com.example.project1.data.storage.DailyTaskStorage
import com.example.project1.ui.components.CoinIcon
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
    onNavigateToChat: (sessionTitle: String, firstMessage: String, taskLatex: String) -> Unit
) {
    val context = LocalContext.current

    val isTestPassed = AiTestManager.isTestCompleted
    // Читаем savedName напрямую — это mutableStateOf, Compose отследит изменения
    val savedName = AiTestManager.savedName
    val userName = if (savedName.isNotBlank())
        savedName.trim().split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: savedName.trim()
    else "Гость"
    val greeting = remember { getGreeting() }

    var coins by remember { mutableIntStateOf(DailyTaskStorage.getCoins(context)) }
    val isSolvedToday = remember { DailyTaskStorage.isSolvedToday(context) }
    val streak = remember { DailyTaskStorage.getStreak(context) }

    // Обновляем монеты при возврате на экран
    LaunchedEffect(Unit) {
        coins = DailyTaskStorage.getCoins(context)
    }

    var showRetestConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 100.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── Шапка: приветствие + баланс ───────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$greeting,",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = userName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Баланс монет
            CoinBalanceChip(coins = coins)
        }

        Spacer(modifier = Modifier.height(28.dp))

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

        // ── Панель 3: Математический справочник ───────────────────────────────
        MathReferenceCard()

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
                color = Color(0xFF1F1F2C),
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
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Результаты текущего теста будут заменены новыми. Настройки чат-бота обновятся.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.65f),
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
                            Text("Отмена", color = Color.White.copy(alpha = 0.55f))
                        }
                        Button(
                            onClick = {
                                showRetestConfirm = false
                                onShowTestDialog()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Перепройти", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ── Чип баланса монет ─────────────────────────────────────────────────────────

@Composable
fun CoinBalanceChip(coins: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A24))
            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
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

// ── Карточка теста личности ───────────────────────────────────────────────────

@Composable
fun PersonalityTestCard(
    isTestPassed: Boolean,
    onStartTest: () -> Unit,
    onRetest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1F1A2E), Color(0xFF1A1A30))
                    )
                )
                .border(
                    1.dp,
                    if (isTestPassed) Color(0xFF7C4DFF).copy(alpha = 0.5f)
                    else Color(0xFFFF5252).copy(alpha = 0.4f),
                    RoundedCornerShape(20.dp)
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
                            if (isTestPassed) Color(0xFF7C4DFF).copy(alpha = 0.2f)
                            else Color(0xFFFF5252).copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isTestPassed) "✨" else "🧠",
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Тест личности",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isTestPassed)
                            "Тест пройден. Профиль сохранён."
                        else
                            "Пройдите для персонализации ИИ",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Кнопка
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isTestPassed) Color(0xFF252535)
                            else Color(0xFFFF5252)
                        )
                        .clickable { if (isTestPassed) onRetest() else onStartTest() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isTestPassed) "Перепройти" else "Начать",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1A2E20), Color(0xFF1A2428))
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
                    Text(
                        text = if (isSolvedToday) "✅" else "∫",
                        fontSize = if (isSolvedToday) 22.sp else 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSolvedToday) Color(0xFF4CAF50) else Color(0xFF00BCD4)
                    )
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
                            color = Color.White
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
                            "Выполнено! Новая задача завтра в 3:00"
                        else
                            "Реши задачу и заработай монеты",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Стрелка
                Text(
                    text = "›",
                    fontSize = 26.sp,
                    color = Color.White.copy(alpha = 0.35f),
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

// ── Информационный баннер ─────────────────────────────────────────────────────

@Composable
fun InfoBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1A1A24))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("ℹ️", fontSize = 16.sp)
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.55f),
            lineHeight = 18.sp
        )
    }
}