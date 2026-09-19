package com.example.project1.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project1.data.storage.DailySummaryData
import com.example.project1.ui.components.CoinIcon
import com.example.project1.ui.theme.AppTheme
import com.example.project1.util.hapticClickable

@Composable
fun DailySummaryCard(
    summary: DailySummaryData,
    onDiscussSummary: () -> Unit
) {
    val colors = AppTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(
            width = 1.5.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color(0xFF7C4DFF),
                    Color(0xFF00E676),
                    Color(0xFFFFD54F)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // ── Шапка ─────────────────────────────────────────────────────────
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
                        imageVector = Icons.Rounded.Bedtime,
                        contentDescription = null,
                        tint = Color(0xFF7C4DFF),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "ИТОГИ ДНЯ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color(0xFF7C4DFF)
                        )
                        Text(
                            text = "Активно с 00:00 до 03:00 МСК",
                            fontSize = 10.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                // Дельта рейтинга
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (summary.ratingDelta >= 0) Color(0xFF00E676).copy(alpha = 0.15f)
                            else Color(0xFFE53935).copy(alpha = 0.15f)
                        )
                        .border(
                            1.dp,
                            if (summary.ratingDelta >= 0) Color(0xFF00E676).copy(alpha = 0.4f)
                            else Color(0xFFE53935).copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (summary.ratingDelta >= 0) "+${summary.ratingDelta}" else "${summary.ratingDelta}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.ratingDelta >= 0) Color(0xFF00E676) else Color(0xFFE53935)
                        )
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = if (summary.ratingDelta >= 0) Color(0xFF00E676) else Color(0xFFE53935),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Метрики дня ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    modifier = Modifier.weight(1f),
                    title = "Скроллинг",
                    value = "${summary.scrollingMinutes} мин",
                    sub = "Всего: ${summary.totalScreenMinutes} мин",
                    accentColor = if (summary.scrollingMinutes > 60) Color(0xFFE53935) else Color(0xFF00E676)
                )

                MetricChip(
                    modifier = Modifier.weight(1f),
                    title = "Задача дня",
                    value = if (summary.dailyTaskSolved) "Решена" else "Пропущена",
                    sub = if (summary.coinsAwarded > 0) "+${summary.coinsAwarded} монет" else "Без бонуса",
                    accentColor = if (summary.dailyTaskSolved) Color(0xFF00E676) else colors.textSecondary
                )

                MetricChip(
                    modifier = Modifier.weight(1f),
                    title = "Блицы",
                    value = "${summary.blitzWins}W / ${summary.blitzLosses}L",
                    sub = "Раундов: ${summary.blitzWins + summary.blitzLosses}",
                    accentColor = Color(0xFFFFD54F)
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Блок рекомендации ИИ ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.background),
                border = BorderStroke(1.dp, colors.surfaceBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "Рекомендация ИИ наставника",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = summary.aiRecommendation,
                        fontSize = 12.sp,
                        color = colors.textPrimary,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Кнопка «Обсудить итоги дня» ──────────────────────────────────
            Button(
                onClick = onDiscussSummary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .hapticClickable(context) { onDiscussSummary() },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C4DFF)
                )
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    sub: String,
    accentColor: Color
) {
    val colors = AppTheme.colors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.background),
        border = BorderStroke(1.dp, colors.surfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 9.sp, color = colors.textSecondary)
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(text = sub, fontSize = 8.sp, color = colors.textSecondary, maxLines = 1)
        }
    }
}