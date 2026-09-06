package com.example.project1.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project1.data.storage.DailyTaskStorage
import com.example.project1.ui.components.AnimatedBottomBarEffect
import com.example.project1.ui.components.BottomBarPreview
import com.example.project1.ui.components.CoinIcon
import com.example.project1.ui.theme.AppTheme
import com.example.project1.ui.theme.BottomBarStyle
import com.example.project1.ui.theme.ThemeManager

@Composable
fun BottomBarStyleScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val currentBarStyle = ThemeManager.currentBottomBarStyle
    val colors = AppTheme.colors
    val allStyles = BottomBarStyle.entries

    var previewStyle by remember { mutableStateOf(currentBarStyle) }
    var styleToBuy by remember { mutableStateOf<BottomBarStyle?>(null) }
    var userCoins by remember { mutableIntStateOf(DailyTaskStorage.getCoins(context)) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Верхняя панель: Навигация + баланс монет
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Стиль нижней панели",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${allStyles.size} вариантов оформления",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }

                // Чип баланса монет
                Row(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.surfaceBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CoinIcon(size = 16)
                    Text(
                        text = "$userCoins",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
            }

            // Сетка всех стилей
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 220.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allStyles) { style ->
                    val isUnlocked = ThemeManager.isStyleUnlocked(style)
                    val isSelected = currentBarStyle == style
                    val isPreviewed = previewStyle == style

                    BottomBarStyleItemCard(
                        style = style,
                        isSelected = isSelected,
                        isPreviewed = isPreviewed,
                        isUnlocked = isUnlocked,
                        onClick = {
                            if (previewStyle == style) {
                                // Повторное нажатие
                                if (isUnlocked) {
                                    ThemeManager.setBottomBarStyle(context, style)
                                } else {
                                    styleToBuy = style
                                }
                            } else {
                                // Первое нажатие — только предпросмотр
                                previewStyle = style
                            }
                        }
                    )
                }
            }
        }

        // Закреплённая нижняя панель живого предпросмотра
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(colors.surfaceElevated)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(colors.primary.copy(alpha = 0.45f), colors.surfaceBorder)
                    ),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Шапка предпросмотра
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Предпросмотр:", fontSize = 12.sp, color = colors.textSecondary)
                        Text(
                            previewStyle.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (previewStyle.isAnimated) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xCC7C4DFF))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("GIF", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    val isPreviewUnlocked = ThemeManager.isStyleUnlocked(previewStyle)
                    if (previewStyle == currentBarStyle) {
                        Text("✓ Активен", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
                    } else if (isPreviewUnlocked) {
                        Text("Куплено", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CoinIcon(size = 14)
                            Text(
                                "${previewStyle.price}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Полноценный бар превью 1:1
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BottomBarPreview(
                        style = previewStyle,
                        selectedTab = 2
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Кнопка действия
                val isPreviewUnlocked = ThemeManager.isStyleUnlocked(previewStyle)
                if (previewStyle == currentBarStyle) {
                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Text("✓ Стиль уже выбран", color = colors.textSecondary, fontSize = 14.sp)
                    }
                } else if (isPreviewUnlocked) {
                    Button(
                        onClick = {
                            ThemeManager.setBottomBarStyle(context, previewStyle)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Text("Применить стиль", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            styleToBuy = previewStyle
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (userCoins >= previewStyle.price) colors.primary else colors.surface
                        ),
                        border = if (userCoins < previewStyle.price) BorderStroke(1.dp, colors.surfaceBorder) else null,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Купить за ${previewStyle.price}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            CoinIcon(size = 16)
                        }
                    }
                }
            }
        }
    }

    // Диалог подтверждения покупки
    if (styleToBuy != null) {
        val style = styleToBuy!!
        val canAfford = userCoins >= style.price

        AlertDialog(
            onDismissRequest = { styleToBuy = null },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(1.dp, colors.surfaceBorder, RoundedCornerShape(24.dp)),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🛍️", fontSize = 20.sp)
                    Text(
                        "Покупка стиля",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Превью стиля в карточке
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (style.drawableRes != null) {
                            Image(
                                painter = painterResource(id = style.drawableRes),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (style.isAnimated) {
                                AnimatedBottomBarEffect(style = style, modifier = Modifier.fillMaxSize())
                            }
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
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
                        Text(
                            style.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Стоимость
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Стоимость:", color = colors.textSecondary, fontSize = 14.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "${style.price}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFFFD700)
                            )
                            CoinIcon(size = 16)
                        }
                    }

                    // Баланс
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ваш баланс:", color = colors.textSecondary, fontSize = 14.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "$userCoins",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.textPrimary
                            )
                            CoinIcon(size = 15)
                        }
                    }

                    HorizontalDivider(color = colors.surfaceBorder)

                    if (canAfford) {
                        Text(
                            "После покупки у вас останется: ${userCoins - style.price}. Стиль будет разблокирован навсегда!",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    } else {
                        Text(
                            "Недостаточно средств! Не хватает: ${style.price - userCoins}. Решайте ежедневные интегралы, чтобы заработать ещё.",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = ThemeManager.unlockStyle(context, style)
                        if (success) {
                            ThemeManager.setBottomBarStyle(context, style)
                            previewStyle = style
                            userCoins = DailyTaskStorage.getCoins(context)
                            styleToBuy = null
                        }
                    },
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Купить и применить", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { styleToBuy = null }) {
                    Text("Отмена", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
fun BottomBarStyleItemCard(
    style: BottomBarStyle,
    isSelected: Boolean,
    isPreviewed: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected || isPreviewed) colors.surfaceElevated else colors.surface
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, colors.primary)
        } else if (isPreviewed) {
            BorderStroke(1.5.dp, colors.primary.copy(alpha = 0.6f))
        } else {
            BorderStroke(1.dp, colors.surfaceBorder)
        },
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Миниатюра панели
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (style.drawableRes != null) {
                    Image(
                        painter = painterResource(id = style.drawableRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    // Живая анимация на карточке превью
                    if (style.isAnimated) {
                        AnimatedBottomBarEffect(
                            style = style,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                    // Затемнение для контраста на 40%
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.40f))
                    )
                } else {
                    // Стандартный стиль: водный стеклянный градиент
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF28344A),
                                        Color(0xFF141828)
                                    )
                                )
                            )
                    )
                }

                // Бейдж GIF для анимированных тем
                if (style.isAnimated) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xCC7C4DFF))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "GIF",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Бейдж статуса справа вверху
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else if (!isUnlocked) {
                    // Бейдж замка с ценником
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xDD12121A))
                            .border(0.8.dp, Color(0x40FFD700), RoundedCornerShape(8.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "${style.price}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                // Имитация миниатюрной стеклянной пилюли активной вкладки
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .height(24.dp)
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(
                            if (style == BottomBarStyle.DEFAULT) {
                                colors.primary.copy(alpha = 0.45f)
                            } else {
                                Color.White.copy(alpha = 0.16f)
                            }
                        )
                        .border(
                            0.8.dp,
                            if (style == BottomBarStyle.DEFAULT) colors.primary else Color.White.copy(alpha = 0.32f),
                            CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = style.title,
                fontSize = 14.sp,
                fontWeight = if (isSelected || isPreviewed) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) colors.primary else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Text(
                        text = "✓ Активен",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                } else if (isUnlocked) {
                    Text(
                        text = "Куплено",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        CoinIcon(size = 12)
                        Text(
                            text = "${style.price}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }
        }
    }
}
