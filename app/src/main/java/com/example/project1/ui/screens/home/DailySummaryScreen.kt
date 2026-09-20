package com.example.project1.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.project1.data.storage.DailySummaryData
import com.example.project1.ui.components.CoinIcon
import com.example.project1.ui.theme.AppTheme
import com.example.project1.util.formatMinutes
import com.example.project1.util.hapticClickable

import com.example.project1.ui.wallpaper.AppBackgroundWallpaper

@Composable
fun DailySummaryScreen(
    summary: DailySummaryData,
    onBack: () -> Unit,
    onDiscussSummary: (DailySummaryData) -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val cardShape = RoundedCornerShape(20.dp)

    BackHandler {
        onBack()
    }

    AppBackgroundWallpaper {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 32.dp)
        ) {
            // ── Шапка: кнопка назад + заголовок ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.surfaceBorder, CircleShape)
                        .hapticClickable(context) { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Итоги дня",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Результаты за ${summary.dateKey}",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Карточка изменения репутации и наград ─────────────────────────
            Surface(
                shape = cardShape,
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (summary.ratingDelta >= 0) Color(0xFF4CAF50).copy(alpha = 0.35f) else Color(0xFFEF5350).copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Изменение репутации",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (summary.ratingDelta >= 0) "+${summary.ratingDelta}" else "${summary.ratingDelta}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = if (summary.ratingDelta >= 0) Color(0xFF4CAF50) else Color(0xFFEF5350)
                            )
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = if (summary.ratingDelta >= 0) Color(0xFF4CAF50) else Color(0xFFEF5350),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (summary.coinsAwarded > 0) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFD700).copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoinIcon(18)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "+${summary.coinsAwarded}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Карточка: Экранное время vs Шортсы ─────────────────────────────
            Surface(
                shape = cardShape,
                color = colors.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Smartphone,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Экранное время",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Всего в приложениях", fontSize = 11.5.sp, color = colors.textSecondary)
                            Text(
                                formatMinutes(summary.totalScreenMinutes),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Шортсы / Клипы", fontSize = 11.5.sp, color = colors.textSecondary)
                            Text(
                                formatMinutes(summary.scrollingMinutes),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.scrollingMinutes > 60) Color(0xFFEF5350) else Color(0xFFFFB300)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Прогресс-бар соотношения
                    val shortRatio = if (summary.totalScreenMinutes > 0) {
                        (summary.scrollingMinutes.toFloat() / summary.totalScreenMinutes).coerceIn(0f, 1f)
                    } else 0f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(shortRatio)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (summary.scrollingMinutes > 60) Color(0xFFEF5350) else Color(0xFFFFB300))
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Карточка: Задача дня и Блицы ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = cardShape,
                    color = colors.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TrackChanges,
                                contentDescription = null,
                                tint = Color(0xFFFF9100),
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Задача дня", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (summary.dailyTaskSolved) "Решена" else "Пропущена",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (summary.dailyTaskSolved) Color(0xFF4CAF50) else colors.textSecondary
                        )
                    }
                }

                Surface(
                    shape = cardShape,
                    color = colors.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFFFD600),
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Мат-Блиц", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${summary.blitzWins} побед • ${summary.blitzLosses} пор.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (summary.blitzWins > 0) Color(0xFF4CAF50) else colors.textSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Карточка: Совет наставника (ИИ — ровно 3 предложения) ──────────
            Surface(
                shape = cardShape,
                color = colors.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = null,
                            tint = Color(0xFFB388FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Совет наставника",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB388FF)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = summary.aiRecommendation,
                        fontSize = 13.5.sp,
                        color = Color.White.copy(alpha = 0.92f),
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Кнопка «Обсудить с наставником» ──────────────────────────────
            Button(
                onClick = { onDiscussSummary(summary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Обсудить итоги дня",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
