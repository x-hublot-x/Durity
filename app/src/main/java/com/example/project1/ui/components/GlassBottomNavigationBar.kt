package com.example.project1.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import com.example.project1.ui.theme.AppTheme
import com.example.project1.ui.theme.BottomBarStyle
import com.example.project1.ui.theme.ThemeManager
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun GlassBottomNavigationBar(
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val colors = AppTheme.colors
    val currentStyle = ThemeManager.currentBottomBarStyle
    val isThemed = currentStyle != BottomBarStyle.DEFAULT

    val pillPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
    val itemWidthDp = 82.dp
    val padDp = 3.dp
    val offDp = itemWidthDp * pillPosition + padDp
    val wDp = itemWidthDp - padDp * 2

    // Внешний Box для позиционирования
    Box(modifier = modifier) {
        // Контейнер панели: капсула 340dp x 64dp
        Box(
            modifier = Modifier
                .width(340.dp)
                .height(64.dp)
                .clip(CircleShape)
        ) {
            // ── Слой 1: Фоновое наполнение (на ВСЮ площадь плашки 340x64 dp) ──
            if (!isThemed) {
                // Стандартный стиль: матовое стекло с эффектом размытия и водным градиентом
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (hazeState != null) {
                                Modifier.hazeChild(
                                    state = hazeState,
                                    shape = CircleShape,
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
                                    Color(0x4D283248),
                                    Color(0x30121526)
                                )
                            )
                        )
                )
            } else if (currentStyle.drawableRes != null) {
                // Тематический стиль в 1080p: заполняет 100% пространства плашки без черных полей
                Image(
                    painter = painterResource(id = currentStyle.drawableRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Если стиль анимированный — накладываем живую анимацию эффектов
                if (currentStyle.isAnimated) {
                    AnimatedBottomBarEffect(
                        style = currentStyle,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Затемнение заднего фона на 40% для глубокого контраста и четкости текста
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.40f))
                )
            }

            // ── Слой 2: Внешний спекулярный световой контур ровно по внешнему краю плашки ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (isThemed) 0.28f else 0.28f),
                                Color.White.copy(alpha = 0.06f)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // ── Слой 3: Скользящая пилюля и кнопки табов (ширина 328 dp, отступ по 6 dp с боков) ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Скользящая пилюля активной вкладки с эффектом лупы
                Box(
                    modifier = Modifier
                        .offset(x = offDp)
                        .width(wDp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                ) {
                    if (!isThemed) {
                        // Обычный стиль: акцентный градиент темы
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            colors.primary.copy(alpha = 0.45f),
                                            colors.secondary.copy(alpha = 0.45f)
                                        )
                                    )
                                )
                        )
                    } else {
                        // ── Тематический стиль: Эффект линзы-лупы (увеличение ~1.48x под пилюлей) ──
                        if (currentStyle.drawableRes != null) {
                            val scale = 1.48f

                            // Единый слой увеличенного фонового контента с точным позиционированием под линзой
                            Box(
                                modifier = Modifier
                                    .requiredSize(width = 340.dp, height = 64.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        transformOrigin = TransformOrigin(0f, 0f)
                                        translationX = (170.dp - (6.dp + offDp + wDp / 2) * scale).toPx()
                                        translationY = (32.dp - 32.dp * scale).toPx()
                                    }
                            ) {
                                Image(
                                    painter = painterResource(id = currentStyle.drawableRes),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (currentStyle.isAnimated) {
                                    AnimatedBottomBarEffect(
                                        style = currentStyle,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            // Мягкое контрастное затемнение под лупой для четкости текста
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.25f))
                            )

                            // Сферическое преломление лупы (радиальное затемнение по краям и блик в центре)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.16f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.32f)
                                            )
                                        )
                                    )
                            )
                        }

                        // Стеклянный градиент и спекулярный контур линзы
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.08f))
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.25f),
                                            Color.White.copy(alpha = 0.04f),
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.10f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.45f),
                                            Color.White.copy(alpha = 0.12f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Вкладки — 4 таба ровно по 82 dp
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavPillItem(
                        label = "Главная", index = 0, pillPosition = pillPosition, isThemed = isThemed,
                        onClick = { onTabSelected(0) }, modifier = Modifier.width(itemWidthDp)
                    ) { sel, col -> HomeIcon(isSelected = sel, activeColor = col) }
                    NavPillItem(
                        label = "Таймеры", index = 1, pillPosition = pillPosition, isThemed = isThemed,
                        onClick = { onTabSelected(1) }, modifier = Modifier.width(itemWidthDp)
                    ) { sel, col -> TimerIcon(isSelected = sel, activeColor = col) }
                    NavPillItem(
                        label = "Настройки", index = 2, pillPosition = pillPosition, isThemed = isThemed,
                        onClick = { onTabSelected(2) }, modifier = Modifier.width(itemWidthDp)
                    ) { sel, col -> SettingsIcon(isSelected = sel, activeColor = col) }
                    NavPillItem(
                        label = "Чат бот", index = 3, pillPosition = pillPosition, isThemed = isThemed,
                        onClick = { onTabSelected(3) }, modifier = Modifier.width(itemWidthDp)
                    ) { sel, col -> ChatIcon(isSelected = sel, activeColor = col) }
                }
            }
        }
    }
}

@Composable
private fun NavPillItem(
    label: String,
    index: Int,
    pillPosition: Float,
    isThemed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (Boolean, Color) -> Unit
) {
    val closeness = (1f - kotlin.math.abs(pillPosition - index)).coerceIn(0f, 1f)
    val isActive  = closeness > 0.5f
    val activeColor = if (isThemed) Color.White else AppTheme.accent
    val inactiveColor = if (isThemed) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.55f)
    val textColor = lerp(inactiveColor, activeColor, closeness)
    val contentScale = if (isThemed) (1.0f + 0.08f * closeness) else 1.0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .graphicsLayer {
                scaleX = contentScale
                scaleY = contentScale
            }
    ) {
        icon(isActive, activeColor)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
fun GlassNavItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    iconContent: @Composable (Boolean) -> Unit
) {
    val colors = AppTheme.colors
    val inactiveColor = if (colors.isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF6B7280)
    val textColor by animateColorAsState(
        targetValue = if (isSelected) colors.primary else inactiveColor,
        animationSpec = tween(durationMillis = 200),
        label = "textColor"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        iconContent(isSelected)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
fun HomeIcon(isSelected: Boolean, activeColor: Color = AppTheme.accent) {
    val inactiveColor = if (AppTheme.colors.isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF6B7280)

    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height

        val roofPath = Path().apply {
            moveTo(w * 0.12f, h * 0.44f)
            lineTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.88f, h * 0.44f)
        }

        if (isSelected) {
            drawPath(
                path = roofPath,
                color = activeColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            drawRoundRect(
                color = activeColor,
                topLeft = Offset(w * 0.22f, h * 0.46f),
                size = Size(w * 0.56f, h * 0.42f),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                style = Fill
            )
        } else {
            drawPath(
                path = roofPath,
                color = inactiveColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(w * 0.22f, h * 0.46f),
                size = Size(w * 0.56f, h * 0.42f),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                style = Stroke(width = 1.8.dp.toPx())
            )
        }
    }
}

@Composable
fun TimerIcon(isSelected: Boolean, activeColor: Color = AppTheme.accent) {
    val inactiveColor = if (AppTheme.colors.isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF6B7280)

    Canvas(modifier = Modifier.size(20.dp)) {
        val centerPoint = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 1.dp.toPx()

        if (isSelected) {
            drawCircle(color = activeColor, radius = radius, center = centerPoint, style = Fill)
            val handsColor = if (activeColor == Color.White) Color(0xFF101424) else Color.White
            drawLine(
                color = handsColor,
                start = centerPoint,
                end = Offset(centerPoint.x, centerPoint.y - radius * 0.5f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = handsColor,
                start = centerPoint,
                end = Offset(centerPoint.x + radius * 0.4f, centerPoint.y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        } else {
            drawCircle(color = inactiveColor, radius = radius, center = centerPoint, style = Stroke(width = 2.dp.toPx()))
            drawLine(
                color = inactiveColor,
                start = centerPoint,
                end = Offset(centerPoint.x, centerPoint.y - radius * 0.5f),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = inactiveColor,
                start = centerPoint,
                end = Offset(centerPoint.x + radius * 0.4f, centerPoint.y),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun SettingsIcon(isSelected: Boolean, activeColor: Color = AppTheme.accent) {
    val inactiveColor = if (AppTheme.colors.isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF6B7280)
    val color = if (isSelected) activeColor else inactiveColor
    val holeBg = if (activeColor == Color.White) Color(0xFF101424) else Color(0xFF1E2232)

    Canvas(modifier = Modifier.size(20.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val rOut = size.minDimension * 0.42f
        val rIn = size.minDimension * 0.32f
        val rHole = size.minDimension * 0.15f
        val teeth = 6

        val gearPath = Path()
        for (i in 0 until teeth) {
            val baseAngle = (i * 360f / teeth) * (Math.PI / 180.0).toFloat()
            val a1 = baseAngle - 0.22f
            val a2 = baseAngle + 0.22f

            val p1 = Offset(center.x + rIn * kotlin.math.cos(a1 - 0.12f), center.y + rIn * kotlin.math.sin(a1 - 0.12f))
            val p2 = Offset(center.x + rOut * kotlin.math.cos(a1), center.y + rOut * kotlin.math.sin(a1))
            val p3 = Offset(center.x + rOut * kotlin.math.cos(a2), center.y + rOut * kotlin.math.sin(a2))
            val p4 = Offset(center.x + rIn * kotlin.math.cos(a2 + 0.12f), center.y + rIn * kotlin.math.sin(a2 + 0.12f))

            if (i == 0) gearPath.moveTo(p1.x, p1.y) else gearPath.lineTo(p1.x, p1.y)
            gearPath.lineTo(p2.x, p2.y)
            gearPath.lineTo(p3.x, p3.y)
            gearPath.lineTo(p4.x, p4.y)
        }
        gearPath.close()

        if (isSelected) {
            drawPath(path = gearPath, color = color, style = Fill)
            drawCircle(color = holeBg, radius = rHole, center = center, style = Fill)
        } else {
            drawPath(
                path = gearPath,
                color = color,
                style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
            )
            drawCircle(color = color, radius = rHole, center = center, style = Stroke(width = 1.6.dp.toPx()))
        }
    }
}

@Composable
fun StatsIcon(isSelected: Boolean, activeColor: Color = AppTheme.accent) {
    val inactiveColor = if (AppTheme.colors.isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF6B7280)

    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val color = if (isSelected) activeColor else inactiveColor

        val barWidth = w * 0.22f
        val corner = CornerRadius(2.dp.toPx(), 2.dp.toPx())

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.05f, h * 0.45f),
            size = Size(barWidth, h * 0.55f),
            cornerRadius = corner
        )

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.39f, h * 0.15f),
            size = Size(barWidth, h * 0.85f),
            cornerRadius = corner
        )

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.73f, h * 0.65f),
            size = Size(barWidth, h * 0.35f),
            cornerRadius = corner
        )
    }
}

@Composable
fun ChatIcon(isSelected: Boolean, activeColor: Color = AppTheme.accent) {
    val inactiveColor = if (AppTheme.colors.isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF6B7280)

    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val color = if (isSelected) activeColor else inactiveColor

        val path = Path().apply {
            moveTo(w * 0.1f, h * 0.2f)
            lineTo(w * 0.9f, h * 0.2f)
            quadraticBezierTo(w, h * 0.2f, w, h * 0.35f)
            lineTo(w, h * 0.65f)
            quadraticBezierTo(w, h * 0.8f, w * 0.9f, h * 0.8f)
            lineTo(w * 0.35f, h * 0.8f)
            lineTo(w * 0.15f, h * 0.95f)
            lineTo(w * 0.15f, h * 0.8f)
            quadraticBezierTo(0f, h * 0.8f, 0f, h * 0.65f)
            lineTo(0f, h * 0.35f)
            quadraticBezierTo(0f, h * 0.2f, w * 0.1f, h * 0.2f)
            close()
        }

        if (isSelected) {
            drawPath(path = path, color = color, style = Fill)
        } else {
            drawPath(path = path, color = color, style = Stroke(width = 1.8.dp.toPx()))
        }
    }
}

/**
 * Полноценный компонент предпросмотра стиля нижней панели (1:1 со всеми элементами навигации)
 */
@Composable
fun BottomBarPreview(
    style: BottomBarStyle,
    modifier: Modifier = Modifier,
    selectedTab: Int = 0
) {
    val colors = AppTheme.colors
    val isThemed = style != BottomBarStyle.DEFAULT

    var previewTab by remember { mutableIntStateOf(selectedTab) }
    LaunchedEffect(selectedTab) {
        previewTab = selectedTab
    }

    val animatedPillPos by animateFloatAsState(
        targetValue = previewTab.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "PreviewPillPosition"
    )

    val itemWidthDp = 82.dp
    val padDp = 3.dp
    val offDp = itemWidthDp * animatedPillPos + padDp
    val wDp = itemWidthDp - padDp * 2

    Box(
        modifier = modifier
            .width(340.dp)
            .height(64.dp)
            .clip(CircleShape)
    ) {
        // Слой 1: Фоновое наполнение
        if (!isThemed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0x4D283248), Color(0x30121526))
                        )
                    )
            )
        } else if (style.drawableRes != null) {
            Image(
                painter = painterResource(id = style.drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (style.isAnimated) {
                AnimatedBottomBarEffect(
                    style = style,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.40f))
            )
        }

        // Слой 2: Внешний спекулярный контур
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Слой 3: Пилюля и кнопки табов
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = offDp)
                    .width(wDp)
                    .fillMaxHeight()
                    .clip(CircleShape)
            ) {
                if (!isThemed) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        colors.primary.copy(alpha = 0.45f),
                                        colors.secondary.copy(alpha = 0.45f)
                                    )
                                )
                            )
                    )
                } else {
                    // ── Тематический стиль: Эффект линзы-лупы (увеличение ~1.48x под пилюлей) ──
                    if (style.drawableRes != null) {
                        val scale = 1.48f

                        // Единый слой увеличенного фонового контента с точным позиционированием под линзой
                        Box(
                            modifier = Modifier
                                .requiredSize(width = 340.dp, height = 64.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    transformOrigin = TransformOrigin(0f, 0f)
                                    translationX = (170.dp - (6.dp + offDp + wDp / 2) * scale).toPx()
                                    translationY = (32.dp - 32.dp * scale).toPx()
                                }
                        ) {
                            Image(
                                painter = painterResource(id = style.drawableRes),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (style.isAnimated) {
                                AnimatedBottomBarEffect(
                                    style = style,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Мягкое затемнение под лупой для четкости текста
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f))
                        )

                        // Сферическое преломление лупы (радиальное затемнение по краям и блик в центре)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.16f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.32f)
                                        )
                                    )
                                )
                        )
                    }

                    // Стеклянный градиент и спекулярный контур линзы
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.08f))
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.04f),
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.10f)
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.12f)
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                }
            }

            // Табы с возможностью интерактивного переключения в превью
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavPillItem(
                    label = "Главная", index = 0, pillPosition = animatedPillPos, isThemed = isThemed,
                    onClick = { previewTab = 0 }, modifier = Modifier.width(itemWidthDp)
                ) { sel, col -> HomeIcon(isSelected = sel, activeColor = col) }
                NavPillItem(
                    label = "Таймеры", index = 1, pillPosition = animatedPillPos, isThemed = isThemed,
                    onClick = { previewTab = 1 }, modifier = Modifier.width(itemWidthDp)
                ) { sel, col -> TimerIcon(isSelected = sel, activeColor = col) }
                NavPillItem(
                    label = "Настройки", index = 2, pillPosition = animatedPillPos, isThemed = isThemed,
                    onClick = { previewTab = 2 }, modifier = Modifier.width(itemWidthDp)
                ) { sel, col -> SettingsIcon(isSelected = sel, activeColor = col) }
                NavPillItem(
                    label = "Чат бот", index = 3, pillPosition = animatedPillPos, isThemed = isThemed,
                    onClick = { previewTab = 3 }, modifier = Modifier.width(itemWidthDp)
                ) { sel, col -> ChatIcon(isSelected = sel, activeColor = col) }
            }
        }
    }
}
