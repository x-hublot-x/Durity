package com.example.project1.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.TrackChanges
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
import com.example.project1.data.storage.AppNotificationItem
import com.example.project1.data.storage.NotificationHistoryStorage
import com.example.project1.ui.components.GradientIcon
import com.example.project1.ui.components.GradientText
import com.example.project1.ui.theme.AppTheme
import com.example.project1.util.VibrationUtil
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterBottomSheet(
    onDismiss: () -> Unit,
    onOpenDailySummary: (() -> Unit)? = null,
    onOpenDailyTask: (() -> Unit)? = null,
    onOpenBlitz: (() -> Unit)? = null,
    onOpenWeeklyReport: (() -> Unit)? = null,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = AppTheme.colors
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var notifications by remember {
        mutableStateOf(NotificationHistoryStorage.getNotifications(context))
    }

    LaunchedEffect(Unit) {
        NotificationHistoryStorage.markAllAsRead(context)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        shape = sheetShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.58f)
                .then(
                    if (hazeState != null) {
                        Modifier.hazeChild(
                            state = hazeState,
                            shape = sheetShape,
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
                            Color(0x52283248),
                            Color(0x35121526)
                        )
                    ),
                    shape = sheetShape
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    ),
                    shape = sheetShape
                )
                .clip(sheetShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 24.dp)
            ) {
                // Драг-хэндл
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, bottom = 4.dp)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                )

                // ── Верхняя строка: Заголовок + кнопки Очистить и Закрыть ──────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GradientIcon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Центр уведомлений",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (notifications.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                VibrationUtil.vibrateTick(context)
                                NotificationHistoryStorage.clearAll(context)
                                notifications = emptyList()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Очистить всё",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(
                        onClick = {
                            VibrationUtil.vibrateTick(context)
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsOff,
                            contentDescription = null,
                            tint = colors.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Уведомлений пока нет",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Здесь будут сохраняться итоги дня, задачи, блицы и предупреждения о таймерах.",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        NotificationItemCard(
                            item = item,
                            onClick = {
                                when (item.type) {
                                    "summary" -> {
                                        onDismiss()
                                        onOpenDailySummary?.invoke()
                                    }
                                    "task" -> {
                                        onDismiss()
                                        onOpenDailyTask?.invoke()
                                    }
                                    "blitz" -> {
                                        onDismiss()
                                        onOpenBlitz?.invoke()
                                    }
                                    "weekly_report" -> {
                                        onDismiss()
                                        onOpenWeeklyReport?.invoke()
                                    }
                                    else -> {}
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun NotificationItemCard(
    item: AppNotificationItem,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val context = LocalContext.current

    val (iconVector, iconTint, iconBg) = when (item.type) {
        "weekly_report" -> Triple(Icons.Rounded.Insights, Color(0xFFFFD54F), Color(0xFF9C27B0).copy(alpha = 0.35f))
        "summary" -> Triple(Icons.Rounded.Bedtime, Color(0xFF9D4EDD), Color(0xFF7C4DFF).copy(alpha = 0.2f))
        "task"    -> Triple(Icons.Rounded.TrackChanges, Color(0xFFFF9100), Color(0xFFFF9100).copy(alpha = 0.2f))
        "blitz"   -> Triple(Icons.Rounded.Bolt, Color(0xFFFFD600), Color(0xFFFFD600).copy(alpha = 0.2f))
        "timer"   -> Triple(Icons.Rounded.HourglassBottom, Color(0xFFFF5252), Color(0xFFFF5252).copy(alpha = 0.2f))
        "streak"  -> Triple(Icons.Rounded.AcUnit, Color(0xFF00B0FF), Color(0xFF00B0FF).copy(alpha = 0.2f))
        else      -> Triple(Icons.Rounded.Notifications, colors.primary, colors.primary.copy(alpha = 0.2f))
    }

    val timeFormatted = remember(item.timestamp) {
        val date = Date(item.timestamp)
        val now = System.currentTimeMillis()
        val diffMins = (now - item.timestamp) / (60 * 1000L)
        when {
            diffMins < 1 -> "Только что"
            diffMins < 60 -> "$diffMins мин назад"
            diffMins < 24 * 60 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            else -> SimpleDateFormat("dd MMM, HH:mm", Locale("ru")).format(date)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                VibrationUtil.vibrateTick(context)
                onClick()
            },
        color = Color(0x351E2438),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timeFormatted,
                        fontSize = 11.sp,
                        color = colors.textTertiary
                    )
                }

                if (item.message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.message,
                        fontSize = 12.5.sp,
                        color = colors.textSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
