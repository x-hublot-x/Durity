package com.example.project1.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var totalWidthPx by remember { mutableStateOf(0) }
    val itemWidthPx = if (totalWidthPx > 0) totalWidthPx / 4f else 0f   // 4 вкладки
    val pillPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction

    // Внешний Box только для позиционирования — без фона
    Box(modifier = modifier) {
        // ── Слой 1: размытие фона под панелью ─────────────────────────────
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .graphicsLayer {
                        renderEffect = android.graphics.RenderEffect
                            .createBlurEffect(80f, 80f, android.graphics.Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                    .background(Color.Transparent)
            )
        }

        // ── Слой 2: сама панель поверх ────────────────────────────────────
        Box(
            modifier = Modifier
                .height(64.dp)
                .clip(CircleShape)
                .background(Color(0xCC1A1A24))
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .onSizeChanged { totalWidthPx = it.width },
            contentAlignment = Alignment.CenterStart
        ) {
            // Скользящая пилюля
            if (totalWidthPx > 0) {
                val padPx   = with(density) { 4.dp.toPx() }
                val offPx   = itemWidthPx * pillPosition + padPx
                val wPx     = itemWidthPx - padPx * 2f
                Box(
                    modifier = Modifier
                        .offset { IntOffset(offPx.toInt(), 0) }
                        .width(with(density) { wPx.toDp() })
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color(0x88FF5252), Color(0x88E53935))
                            )
                        )
                )
            }

            // Вкладки — фиксированная ширина каждого таба, панель сжата по содержимому
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavPillItem(
                    label = "Главная", index = 0, pillPosition = pillPosition,
                    onClick = { onTabSelected(0) }, modifier = Modifier.width(82.dp)
                ) { sel -> HomeIcon(isSelected = sel) }
                NavPillItem(
                    label = "Таймеры", index = 1, pillPosition = pillPosition,
                    onClick = { onTabSelected(1) }, modifier = Modifier.width(82.dp)
                ) { sel -> TimerIcon(isSelected = sel) }
                NavPillItem(
                    label = "Статистика", index = 2, pillPosition = pillPosition,
                    onClick = { onTabSelected(2) }, modifier = Modifier.width(82.dp)
                ) { sel -> StatsIcon(isSelected = sel) }
                NavPillItem(
                    label = "Чат бот", index = 3, pillPosition = pillPosition,
                    onClick = { onTabSelected(3) }, modifier = Modifier.width(82.dp)
                ) { sel -> ChatIcon(isSelected = sel) }
            }
        }
    }
}

@Composable
private fun NavPillItem(
    label: String,
    index: Int,
    pillPosition: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (Boolean) -> Unit
) {
    val closeness = (1f - kotlin.math.abs(pillPosition - index)).coerceIn(0f, 1f)
    val isActive  = closeness > 0.5f
    val textColor = lerp(Color.White.copy(alpha = 0.55f), Color.White, closeness)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        icon(isActive)
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
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFF5252) else Color.White.copy(alpha = 0.6f),
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
fun HomeIcon(isSelected: Boolean) {
    val activeColor = Color(0xFFFF5252)
    val inactiveColor = Color.White.copy(alpha = 0.8f)

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
fun TimerIcon(isSelected: Boolean) {
    val activeColor = Color(0xFFFF5252)
    val inactiveColor = Color.White.copy(alpha = 0.8f)

    Canvas(modifier = Modifier.size(20.dp)) {
        val centerPoint = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 1.dp.toPx()

        if (isSelected) {
            drawCircle(color = activeColor, radius = radius, center = centerPoint, style = Fill)
            drawLine(
                color = Color.White,
                start = centerPoint,
                end = Offset(centerPoint.x, centerPoint.y - radius * 0.5f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
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
fun StatsIcon(isSelected: Boolean) {
    val activeColor = Color(0xFFFF5252)
    val inactiveColor = Color.White.copy(alpha = 0.8f)

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
fun ChatIcon(isSelected: Boolean) {
    val activeColor = Color(0xFFFF5252)
    val inactiveColor = Color.White.copy(alpha = 0.8f)

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
