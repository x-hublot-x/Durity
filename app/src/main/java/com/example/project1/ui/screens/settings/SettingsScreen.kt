package com.example.project1.ui.screens.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project1.ui.theme.AccentTheme
import com.example.project1.ui.theme.AppTheme
import com.example.project1.ui.theme.BottomBarStyle
import com.example.project1.ui.theme.ThemeManager

@Composable
fun SettingsScreen(
    onBottomBarStyleOpenChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val currentAccent = ThemeManager.currentAccent
    val currentBarStyle = ThemeManager.currentBottomBarStyle
    val colors = AppTheme.colors

    var showBottomBarThemesScreen by remember { mutableStateOf(false) }

    DisposableEffect(showBottomBarThemesScreen) {
        onBottomBarStyleOpenChanged(showBottomBarThemesScreen)
        onDispose {
            if (showBottomBarThemesScreen) {
                onBottomBarStyleOpenChanged(false)
            }
        }
    }

    AnimatedContent(
        targetState = showBottomBarThemesScreen,
        transitionSpec = {
            if (targetState) {
                // Переход на экран тем: въезжает справа
                (slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(250)))
                    .togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 3 },
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(200))
                    )
            } else {
                // Возврат в настройки: уезжает вправо
                (slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth / 3 },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(250)))
                    .togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(200))
                    )
            }
        },
        label = "SettingsSubScreenTransition"
    ) { isSubScreenOpen ->
        if (isSubScreenOpen) {
            BottomBarStyleScreen(
                onBack = { showBottomBarThemesScreen = false }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 120.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Настройки",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Меню выбора цвета интерфейса ──────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Цвет интерфейса",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Акцентный цвет кнопок, индикаторов и рамок",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Ряд иконок цветов
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AccentTheme.entries.forEach { accent ->
                                val isSelected = currentAccent == accent
                                ColorCircleItem(
                                    accent = accent,
                                    isSelected = isSelected,
                                    onClick = { ThemeManager.setAccent(context, accent) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Переход на отдельный экран выбора стиля нижней панели ─────────
                Card(
                    onClick = { showBottomBarThemesScreen = true },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Превью текущего стиля
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.surfaceElevated)
                            ) {
                                if (currentBarStyle.drawableRes != null) {
                                    Image(
                                        painter = painterResource(id = currentBarStyle.drawableRes),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.35f))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF28344A), Color(0xFF141828))
                                                )
                                            )
                                    )
                                }
                                // Мини-капля в центре
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.Center)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.22f))
                                        .border(0.8.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                                )
                            }

                            Column {
                                Text(
                                    text = "Стиль нижней панели",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentBarStyle.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.primary
                                    )
                                    Text(
                                        text = " • ${BottomBarStyle.entries.size} тем",
                                        fontSize = 13.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Выбрать стиль",
                            tint = colors.textSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(180f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Предпросмотр интерфейса ───────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Предпросмотр",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Так выглядят элементы в выбранном цвете",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Пример основной кнопки
                        Button(
                            onClick = { },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Пример основной кнопки",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Пример прогресс-бара
                        LinearProgressIndicator(
                            progress = { 0.68f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = colors.primary,
                            trackColor = colors.surfaceElevated
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Пример карточки с обводкой
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.primarySubtle)
                                .border(1.dp, colors.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Акцентная плашка",
                                    color = colors.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.primary)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Активно",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
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

@Composable
private fun ColorCircleItem(
    accent: AccentTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(2.5.dp, colors.textPrimary, CircleShape)
                } else {
                    Modifier
                }
            )
            .padding(if (isSelected) 3.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(accent.primary, accent.secondary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
