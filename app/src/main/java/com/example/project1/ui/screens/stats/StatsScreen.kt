package com.example.project1.ui.screens.stats

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.project1.data.model.AppInfo
import com.example.project1.ui.theme.AppTheme
import com.example.project1.util.formatMinutes

@Composable
fun StatsScreen(
    appsWithTimers: List<AppInfo>,
    onBack: (() -> Unit)? = null
) {
    val totalMinutes = appsWithTimers.sumOf { it.usedMinutesThisWeek }
    val colors = AppTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = "Статистика",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Использование за текущий день цикла",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formatMinutes(totalMinutes),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Сброс статистики каждый день в 3:00 МСК",
                    fontSize = 11.sp,
                    color = colors.textTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Детализация по таймерам",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (appsWithTimers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Список таймеров пуст",
                    color = colors.textSecondary,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(appsWithTimers) { app ->
                    StatItemCard(app = app)
                }
            }
        }
    }
}

@Composable
fun StatItemCard(app: AppInfo) {
    val colors = AppTheme.colors
    val progress = if (app.timeLimitMinutes > 0) {
        (app.usedMinutesThisWeek.toFloat() / app.timeLimitMinutes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bitmap = remember(app.icon) { app.icon.toBitmap(128, 128).asImageBitmap() }
        Image(
            bitmap = bitmap,
            contentDescription = app.name,
            modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = app.name,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${formatMinutes(app.usedMinutesThisWeek)} / ${formatMinutes(app.timeLimitMinutes)}",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (progress >= 1f) Color(0xFF80D8FF) else colors.primary,
                trackColor = colors.surfaceElevated
            )
        }
    }
}
