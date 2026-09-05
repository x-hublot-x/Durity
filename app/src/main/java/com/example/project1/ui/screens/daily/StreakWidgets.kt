package com.example.project1.ui.screens.daily

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*

enum class StreakAnimPhase {
    IDLE,        // Обычное состояние
    EXPANDING,   // Панелька расширяется
    PLAYING_VIDEO, // Играет mp4-анимация огня
    SHOWING_FIRE,  // Показывается обычный оранжевый огонёк
    SHRINKING    // Панелька возвращается на место
}

@Composable
fun StreakVideoPlayer(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Asset("fire1.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = true
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}

@Composable
fun StreakCounter(
    displayedStreak: Int,
    showNew: Boolean,
    color: Color
) {
    // Сдвиг: старое число уходит вверх, новое приходит снизу
    val oldOffsetY by animateFloatAsState(
        targetValue = if (showNew) -20f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "oldOffset"
    )
    val newOffsetY by animateFloatAsState(
        targetValue = if (showNew) 0f else 20f,
        animationSpec = tween(durationMillis = 300),
        label = "newOffset"
    )
    val oldAlpha by animateFloatAsState(
        targetValue = if (showNew) 0f else 1f,
        animationSpec = tween(durationMillis = 250),
        label = "oldAlpha"
    )
    val newAlpha by animateFloatAsState(
        targetValue = if (showNew) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "newAlpha"
    )

    Box(
        modifier = Modifier
            .height(22.dp)
            .width(IntrinsicSize.Max),
        contentAlignment = Alignment.Center
    ) {
        // Старое число — уходит вверх
        Text(
            text = "${if (showNew) displayedStreak - 1 else displayedStreak}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color.copy(alpha = oldAlpha),
            modifier = Modifier.offset(y = oldOffsetY.dp)
        )
        // Новое число — приходит снизу
        Text(
            text = "$displayedStreak",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color.copy(alpha = newAlpha),
            modifier = Modifier.offset(y = newOffsetY.dp)
        )
    }
}
