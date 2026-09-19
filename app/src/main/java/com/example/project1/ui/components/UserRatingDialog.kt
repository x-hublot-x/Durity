package com.example.project1.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.project1.data.storage.UserRatingStorage
import com.example.project1.ui.theme.AppTheme
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

private data class RatingRule(
    val icon: ImageVector,
    val iconTint: Color,
    val title: String,
    val description: String
)

private val RATING_RULES = listOf(
    RatingRule(
        icon = Icons.Rounded.Bolt,
        iconTint = Color(0xFFFFB300),
        title = "Мат-Блиц",
        description = "+10 за победу, -10 при поражении или таймауте"
    ),
    RatingRule(
        icon = Icons.Rounded.TrackChanges,
        iconTint = Color(0xFF00BCD4),
        title = "Задача дня",
        description = "+15 за решение, -10 при пропуске без заморозки"
    ),
    RatingRule(
        icon = Icons.Rounded.Smartphone,
        iconTint = Color(0xFF2979FF),
        title = "Экранное время (00:00)",
        description = "Бонус за осознанность, штраф за скроллинг > 1 ч"
    ),
    RatingRule(
        icon = Icons.Rounded.Lock,
        iconTint = Color(0xFFE53935),
        title = "Блокировка Блица (-100)",
        description = "При -100 Блиц блокируется до исправления рейтинга"
    )
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun UserRatingDialog(
    rating: Int,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null
) {
    val colors = AppTheme.colors
    val currentColor = UserRatingStorage.getRatingColor(rating)
    val statusTitle = UserRatingStorage.getRatingStatusTitle(rating)
    val targetProgress = UserRatingStorage.getRatingProgress(rating).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 500),
        label = "rating_progress"
    )

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { RATING_RULES.size })
    val cardShape = RoundedCornerShape(22.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
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
                                Color(0xF51E2436),
                                Color(0xFA121524)
                            )
                        ),
                        shape = cardShape
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        ),
                        shape = cardShape
                    )
                    .clip(cardShape)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Шапка: заголовок и крестик ────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Репутация",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Число репутации (без рамок и обводок) ─────────────────────
                Text(
                    text = if (rating > 0) "+$rating" else "$rating",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = currentColor
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val statusIcon = when {
                        rating <= UserRatingStorage.MIN_RATING -> Icons.Rounded.Block
                        rating < 0 -> Icons.Rounded.WarningAmber
                        rating < 100 -> Icons.Rounded.CheckCircleOutline
                        rating < 150 -> Icons.Rounded.CheckCircleOutline
                        else -> Icons.Rounded.Diamond
                    }
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = currentColor.copy(alpha = 0.9f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = statusTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = currentColor.copy(alpha = 0.9f)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // ── Заполняемая шкала прогресса в новом стиле ─────────────────
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress.coerceIn(0.02f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            currentColor.copy(alpha = 0.75f),
                                            currentColor
                                        )
                                    )
                                )
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("-100", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFEF5350))
                        Text("0", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFB300))
                        Text("100", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFD54F))
                        Text("200+", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4CAF50))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Мелкое компактное окошко с правилами (листается влево/вправо) ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val rule = RATING_RULES[page]
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(rule.iconTint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = rule.icon,
                                    contentDescription = null,
                                    tint = rule.iconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = rule.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = rule.description,
                                    fontSize = 9.5.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    lineHeight = 12.5.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Индикатор точек для свайпа
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(RATING_RULES.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 5.dp else 3.5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) currentColor else Color.White.copy(alpha = 0.25f)
                                )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Кнопка закрытия ───────────────────────────────────────────
                val context = androidx.compose.ui.platform.LocalContext.current
                PrimaryGradientButton(
                    onClick = {
                        com.example.project1.util.VibrationUtil.vibrateTick(context)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Понятно",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
}



