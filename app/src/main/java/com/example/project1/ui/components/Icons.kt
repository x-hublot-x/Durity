package com.example.project1.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

@Composable
fun AutoAddIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier
                .size(18.dp)
                .offset(x = 3.dp, y = 3.dp)
        )
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Автодобавление",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SamsungAiStarsIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFFFF5252)
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height

        fun draw4PointStar(center: Offset, maxRadius: Float) {
            val path = Path().apply {
                moveTo(center.x, center.y - maxRadius)
                quadraticBezierTo(center.x, center.y, center.x + maxRadius, center.y)
                quadraticBezierTo(center.x, center.y, center.x, center.y + maxRadius)
                quadraticBezierTo(center.x, center.y, center.x - maxRadius, center.y)
                quadraticBezierTo(center.x, center.y, center.x, center.y - maxRadius)
                close()
            }
            drawPath(path, color = tint)
        }

        draw4PointStar(Offset(w * 0.4f, h * 0.5f), w * 0.35f)
        draw4PointStar(Offset(w * 0.8f, h * 0.25f), w * 0.18f)
        draw4PointStar(Offset(w * 0.75f, h * 0.75f), w * 0.15f)
    }
}

@Composable
fun MinimalRedLoadingSpinner(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_transition")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    Canvas(modifier = modifier.size(48.dp)) {
        val strokeWidth = 4.dp.toPx()
        drawArc(
            color = Color(0xFFFF5252),
            startAngle = angle,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun CoinIcon(size: Int = 18) {
    val piColor = Color(0xFF7A5200)
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(0xFFFFD700))
            .border(1.dp, Color(0xFFFFA000), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val text = "π"
            val paint = android.graphics.Paint().apply {
                color = piColor.toArgb()
                textSize = this@Canvas.size.height * 0.65f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val bounds = android.graphics.Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            val x = this.size.width / 2f
            val y = (this.size.height / 2f) + (bounds.height() / 2f) - bounds.bottom
            drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText(text, x, y, paint) }
        }
    }
}
