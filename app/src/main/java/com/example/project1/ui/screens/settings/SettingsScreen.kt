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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project1.R
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

                        Spacer(modifier = Modifier.height(18.dp))

                        // Заголовок Линии 1
                        Text(
                            text = "Классические",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AccentTheme.row1.forEach { accent ->
                                val isSelected = currentAccent == accent
                                ColorCircleItem(
                                    accent = accent,
                                    isSelected = isSelected,
                                    onClick = { ThemeManager.setAccent(context, accent) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Заголовок Линии 2
                        Text(
                            text = "Альтернативные",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AccentTheme.row2.forEach { accent ->
                                val isSelected = currentAccent == accent
                                ColorCircleItem(
                                    accent = accent,
                                    isSelected = isSelected,
                                    onClick = { ThemeManager.setAccent(context, accent) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Заголовок Линии 3
                        Text(
                            text = "Градиентные дуэты",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AccentTheme.row3.forEach { accent ->
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
                val animPrimary by animateColorAsState(
                    targetValue = colors.primary,
                    animationSpec = tween(350),
                    label = "previewPrimary"
                )
                val animSecondary by animateColorAsState(
                    targetValue = colors.secondary,
                    animationSpec = tween(350),
                    label = "previewSecondary"
                )
                val animSubtle by animateColorAsState(
                    targetValue = colors.primarySubtle,
                    animationSpec = tween(350),
                    label = "previewSubtle"
                )

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
                            text = "Так выглядят элементы и иконка в выбранном цвете",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // ── Блок предпросмотра иконки приложения ─────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.surfaceElevated.copy(alpha = 0.7f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Реальная иконка приложения
                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(animPrimary, animSecondary)
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.35f),
                                                    Color.White.copy(alpha = 0.08f)
                                                )
                                            ),
                                            RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.mipmap.ic_launcher_adaptive_fore),
                                        contentDescription = "Иконка приложения",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Иконка приложения",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Автоматически изменится на рабочем столе при выходе",
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Пример основной кнопки (с плавным градиентом)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(animPrimary, animSecondary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Пример основной кнопки",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Пример прогресс-бара (с плавным градиентом)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.68f)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(animPrimary, animSecondary)
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Пример карточки с градиентным текстом и бейджем
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(animSubtle)
                                .border(
                                    1.2.dp,
                                    Brush.horizontalGradient(
                                        listOf(
                                            animPrimary.copy(alpha = 0.5f),
                                            animSecondary.copy(alpha = 0.5f)
                                        )
                                    ),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Акцентный градиентный стиль",
                                    style = TextStyle(
                                        brush = Brush.horizontalGradient(
                                            listOf(animPrimary, animSecondary)
                                        ),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(animPrimary, animSecondary)
                                            )
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
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
