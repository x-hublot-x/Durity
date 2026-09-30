package com.example.project1.ui.screens.home

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.project1.data.storage.MonthlyDopamineReport
import com.example.project1.data.storage.MonthlyReportStorage
import com.example.project1.service.MonthlyReportManager
import com.example.project1.ui.components.CoinIcon
import com.example.project1.ui.components.GradientText
import com.example.project1.ui.theme.AppTheme
import com.example.project1.util.ReportImageExporter
import com.example.project1.util.VibrationUtil
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MonthlyDopamineReportDialog(
    report: MonthlyDopamineReport = MonthlyReportStorage.getLatestReport(LocalContext.current)
        ?: MonthlyReportManager.checkAndGenerateMonthlyReportIfNeeded(LocalContext.current, notify = false),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 6

    val goldColor = Color(0xFFFFD54F)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0814))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF1E142E), Color(0xFF110C1E), Color(0xFF07050E))
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(goldColor, colors.primary)),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Индикаторы шагов (Stories Progress Bars) ─────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    for (i in 0 until totalSteps) {
                        val isDone = i <= currentStep
                        val startFraction = i.toFloat() / totalSteps.toFloat()
                        val endFraction = (i + 1).toFloat() / totalSteps.toFloat()
                        val barBrush = if (isDone) {
                            val cStart = androidx.compose.ui.graphics.lerp(goldColor, colors.primary, startFraction)
                            val cEnd = androidx.compose.ui.graphics.lerp(goldColor, colors.primary, endFraction)
                            Brush.horizontalGradient(listOf(cStart, cEnd))
                        } else {
                            androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.15f))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(barBrush)
                        )
                    }
                }

                // ── Верхний бар: Даты + Кнопка закрытия ───────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(goldColor.copy(alpha = 0.25f), colors.primary.copy(alpha = 0.25f))
                                    )
                                )
                                .border(1.dp, goldColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "MONTHLY WRAPPED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = goldColor
                            )
                        }
                        Text(
                            text = report.monthTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary
                        )
                    }

                    IconButton(
                        onClick = {
                            VibrationUtil.vibrateTick(context)
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Содержимое текущего слайда с 3D Perspective Flip ────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Анимированные тематические фоны
                    when (currentStep) {
                        0 -> MonthlyOrbitBackground(modifier = Modifier.fillMaxSize())
                        1 -> MonthlyMathSymbolsBackground(modifier = Modifier.fillMaxSize())
                        2 -> MonthlyComplexPlaneBackground(modifier = Modifier.fillMaxSize())
                        3 -> MonthlyNeuralQuantumBackground(modifier = Modifier.fillMaxSize())
                        4 -> MonthlyDopamineAuraBackground(modifier = Modifier.fillMaxSize())
                        5 -> MonthlyCelebrationParticlesBackground(modifier = Modifier.fillMaxSize())
                    }

                    // 3D Perspective Slide Transition
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { it } +
                                        fadeIn(animationSpec = tween(280)) +
                                        scaleIn(initialScale = 0.88f, animationSpec = tween(320)))
                                    .togetherWith(
                                        slideOutHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { -it } +
                                                fadeOut(animationSpec = tween(240)) +
                                                scaleOut(targetScale = 0.88f, animationSpec = tween(320))
                                    )
                            } else {
                                (slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { -it } +
                                        fadeIn(animationSpec = tween(280)) +
                                        scaleIn(initialScale = 0.88f, animationSpec = tween(320)))
                                    .togetherWith(
                                        slideOutHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { it } +
                                                fadeOut(animationSpec = tween(240)) +
                                                scaleOut(targetScale = 0.88f, animationSpec = tween(320))
                                    )
                            }
                        },
                        modifier = Modifier.graphicsLayer {
                            cameraDistance = 16f * density
                        },
                        label = "3DMonthlyStoryTransition"
                    ) { step ->
                        when (step) {
                            0 -> SlideMonthlySavedTime(report)
                            1 -> SlideMonthlyIntelligence(report)
                            2 -> SlideMonthlyFavoriteBlitz(report)
                            3 -> SlideMonthlyAutonomy(report)
                            4 -> SlideMonthlyDisciplineAndRank(report)
                            5 -> SlideMonthlyShareCard(report, onShare = {
                                VibrationUtil.vibrateSuccess(context)
                                ReportImageExporter.shareMonthlyReportCard(
                                    context = context,
                                    report = report,
                                    primaryColorInt = colors.primary.toArgb(),
                                    secondaryColorInt = colors.secondary.toArgb()
                                )
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Нижняя панель навигации по шагам ─────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = {
                                VibrationUtil.vibrateTick(context)
                                currentStep--
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Назад", fontSize = 14.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(80.dp))
                    }

                    if (currentStep < totalSteps - 1) {
                        Box(
                            modifier = Modifier
                                .height(46.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(goldColor, colors.primary)))
                                .clickable {
                                    VibrationUtil.vibrateTick(context)
                                    currentStep++
                                }
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Дальше", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Вперед",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .height(46.dp)
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(goldColor, colors.primary)))
                                .clickable {
                                    VibrationUtil.vibrateSuccess(context)
                                    ReportImageExporter.shareMonthlyReportCard(
                                        context = context,
                                        report = report,
                                        primaryColorInt = colors.primary.toArgb(),
                                        secondaryColorInt = colors.secondary.toArgb()
                                    )
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Поделиться",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Поделиться картинкой",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Анимированные фоны для месяца ─────────────────────────────────────────────

@Composable
private fun MonthlyOrbitBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "MonthlyOrbit")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitAngle"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = size.width * 0.42f

        drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = 0.06f),
            radius = maxR,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f)
        )
        drawCircle(
            color = Color(0xFFAB47BC).copy(alpha = 0.08f),
            radius = maxR * 0.68f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f)
        )

        val rad1 = Math.toRadians(angle.toDouble())
        val p1x = cx + cos(rad1).toFloat() * maxR
        val p1y = cy + sin(rad1).toFloat() * maxR
        drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = 0.6f),
            radius = 4f,
            center = Offset(p1x, p1y)
        )

        val rad2 = Math.toRadians((-angle * 1.4).toDouble())
        val p2x = cx + cos(rad2).toFloat() * (maxR * 0.68f)
        val p2y = cy + sin(rad2).toFloat() * (maxR * 0.68f)
        drawCircle(
            color = Color(0xFF69F0AE).copy(alpha = 0.7f),
            radius = 3.5f,
            center = Offset(p2x, p2y)
        )
    }
}

@Composable
private fun MonthlyMathSymbolsBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "MathSymbols")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "MathTime"
    )

    val symbols = remember {
        listOf("∫", "∑", "∂", "∇", "lim", "e^x", "π", "√", "dx", "∞", "λ", "θ")
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val paint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#B388FF")
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }

        for (i in symbols.indices) {
            val startX = (w * 0.1f) + ((i * 73) % (w * 0.8f))
            val baseProgress = (time + i * 0.083f) % 1f
            val curY = h * (1f - baseProgress)
            val curX = startX + sin(baseProgress * 2 * Math.PI + i).toFloat() * 25f
            val alpha = (sin(baseProgress * Math.PI) * 0.22f).toFloat()

            paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            drawContext.canvas.nativeCanvas.drawText(symbols[i], curX, curY, paint)
        }
    }
}

@Composable
private fun MonthlyComplexPlaneBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ComplexPlane")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.width * 0.38f

        drawLine(
            color = Color.White.copy(alpha = 0.07f),
            start = Offset(cx - r - 20f, cy),
            end = Offset(cx + r + 20f, cy),
            strokeWidth = 1.5f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.07f),
            start = Offset(cx, cy - r - 20f),
            end = Offset(cx, cy + r + 20f),
            strokeWidth = 1.5f
        )

        drawCircle(
            color = Color(0xFFFF4081).copy(alpha = 0.08f),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f)
        )

        val contour = Path()
        val numPoints = 80
        for (i in 0..numPoints) {
            val t = (i.toFloat() / numPoints) * 2 * Math.PI
            val dr = r * (0.85f + 0.15f * sin(3 * t + phase).toFloat())
            val px = cx + dr * cos(t).toFloat()
            val py = cy + dr * sin(t).toFloat()
            if (i == 0) contour.moveTo(px, py) else contour.lineTo(px, py)
        }
        contour.close()
        drawPath(contour, Color(0xFFFF4081).copy(alpha = 0.18f), style = Stroke(width = 2f))
    }
}

@Composable
private fun MonthlyNeuralQuantumBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "NeuralQuantum")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val nodes = listOf(
            Offset(cx - 90f, cy - 60f),
            Offset(cx + 80f, cy - 80f),
            Offset(cx - 100f, cy + 70f),
            Offset(cx + 90f, cy + 60f),
            Offset(cx, cy),
            Offset(cx - 140f, cy),
            Offset(cx + 140f, cy - 20f)
        )

        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val d = (nodes[i] - nodes[j]).getDistance()
                if (d < 220f) {
                    val edgeAlpha = (1f - d / 220f) * 0.15f
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = edgeAlpha),
                        start = nodes[i],
                        end = nodes[j],
                        strokeWidth = 1.5f
                    )
                }
            }
        }

        nodes.forEachIndexed { idx, node ->
            val p = (pulse + idx * 0.14f) % 1f
            val nodeAlpha = (0.2f + 0.5f * sin(p * Math.PI).toFloat()).coerceIn(0f, 1f)
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = nodeAlpha),
                radius = 3.5f,
                center = node
            )
        }
    }
}

@Composable
private fun MonthlyDopamineAuraBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "DopamineAura")
    val wave by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Wave"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseR = size.width * 0.35f

        drawCircle(
            color = Color(0xFFFFCA28).copy(alpha = 0.05f * wave),
            radius = baseR * wave,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFF69F0AE).copy(alpha = 0.08f),
            radius = baseR * 0.75f,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )
    }
}

@Composable
private fun MonthlyCelebrationParticlesBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "Celebration")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CelebrationTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val particleColors = listOf(
            Color(0xFFFFD54F),
            Color(0xFF69F0AE),
            Color(0xFFB388FF),
            Color(0xFFFF4081),
            Color(0xFF40C4FF)
        )

        for (i in 0..24) {
            val color = particleColors[i % particleColors.size].copy(alpha = 0.35f)
            val px = (i * 37f) % w
            val progress = (time + i * 0.04f) % 1f
            val py = h * (1f - progress)
            val rad = 4f + (i % 3) * 2f
            drawCircle(
                color = color,
                radius = rad,
                center = Offset(px + sin(progress * 6f) * 15f, py)
            )
        }
    }
}

// ── Слайды истории месяца ─────────────────────────────────────────────────────

@Composable
private fun SlideMonthlySavedTime(report: MonthlyDopamineReport) {
    val goldColor = Color(0xFFFFD54F)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(goldColor.copy(alpha = 0.15f))
                .border(2.dp, goldColor.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.HourglassBottom,
                contentDescription = null,
                tint = goldColor,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(text = "За прошедший месяц ты вернул", fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f))

        Text(
            text = "${report.savedHours} часов",
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF69F0AE)
        )

        Text(text = "чистой осознанной жизни", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))

        Spacer(modifier = Modifier.height(22.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, goldColor.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = goldColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Главный спасённый рубеж:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = goldColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${report.topSavedApp}. Это равносильно прочтению 10+ книг или 40 спортивным тренировкам!",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SlideMonthlyIntelligence(report: MonthlyDopamineReport) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(Color(0xFF7C4DFF).copy(alpha = 0.2f))
                .border(2.dp, Color(0xFFB388FF).copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Psychology,
                contentDescription = null,
                tint = Color(0xFFB388FF),
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(text = "Интеллектуальный прогресс", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF261D38)),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0xFFAB47BC).copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFFCE93D8), modifier = Modifier.size(15.dp))
                        Text(text = "Задачи месяца", fontSize = 12.sp, color = Color(0xFFCE93D8))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "${report.tasksSolved}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF261D38)),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Bolt, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(15.dp))
                        Text(text = "Блиц-победы", fontSize = 12.sp, color = Color(0xFFCE93D8))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "${report.blitzWins}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Твой мозг переключился с пассивного скроллинга на непрерывную прокачку нейронных связей.",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SlideMonthlyFavoriteBlitz(report: MonthlyDopamineReport) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(Color(0xFFE91E63).copy(alpha = 0.2f))
                .border(2.dp, Color(0xFFFF4081).copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFFF4081),
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(text = "Любимая тема месяца", fontSize = 16.sp, color = Color.White.copy(alpha = 0.75f))

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = report.favoriteBlitzTopic,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFFD54F)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF261A33)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFF4081).copy(alpha = 0.35f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                    Text(text = "${report.blitzWins} победных раундов", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = report.favoriteBlitzComment,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SlideMonthlyAutonomy(report: MonthlyDopamineReport) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Lightbulb,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(text = "Автономность мышления", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13232E)),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.SelfImprovement, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(15.dp))
                        Text(text = "Без подсказок", fontSize = 12.sp, color = Color(0xFF80D8FF))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "${report.tasksSolvedWithoutHints}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF261D38)),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0xFFAB47BC).copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.TipsAndUpdates, contentDescription = null, tint = Color(0xFFCE93D8), modifier = Modifier.size(15.dp))
                        Text(text = "С помощью ИИ", fontSize = 12.sp, color = Color(0xFFCE93D8))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "${report.tasksSolvedWithHints}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.25f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = report.aiAssistanceComment,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SlideMonthlyDisciplineAndRank(report: MonthlyDopamineReport) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFCA28).copy(alpha = 0.2f))
                .border(2.dp, Color(0xFFFFD54F).copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Stars,
                contentDescription = null,
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(text = "Дофаминовый статус", fontSize = 16.sp, color = Color.White.copy(alpha = 0.75f))

        Text(
            text = "ТОП ${100 - report.percentile}% пользователей",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF69F0AE)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF261E38)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFFCA28).copy(alpha = 0.35f), RoundedCornerShape(18.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CoinIcon(size = 20)
                        Text(text = "+${report.coinsEarned}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                    }
                    Text(text = "Монет заработано", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }
                Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.White.copy(alpha = 0.1f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${report.tasksSolved + report.blitzWins}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                    Text(text = "Победных активностей", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun SlideMonthlyShareCard(report: MonthlyDopamineReport, onShare: () -> Unit) {
    val goldColor = Color(0xFFFFD54F)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(goldColor.copy(alpha = 0.2f))
                .border(2.dp, goldColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = goldColor,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = report.monthlyTrophy,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = goldColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Превью карточки отчета (16:9 Landscape Card)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF160E26)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, goldColor.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "DURITY MONTHLY WRAPPED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = goldColor)
                    Text(text = "+${report.savedHours} ч спасено", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF69F0AE))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Задачи: ${report.tasksSolved} решено", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    Text(text = "Блицы: ${report.blitzWins} побед", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    Text(text = "ТОП ${100 - report.percentile}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF69F0AE))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "«${report.aiVerdict}»",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = 15.sp,
                    maxLines = 3
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onShare,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, goldColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = goldColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Поделиться 16:9 карточкой", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
