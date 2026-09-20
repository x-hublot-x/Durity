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
import com.example.project1.data.storage.WeeklyDopamineReport
import com.example.project1.data.storage.WeeklyReportStorage
import com.example.project1.ui.components.CoinIcon
import com.example.project1.ui.components.GradientText
import com.example.project1.ui.theme.AppTheme
import com.example.project1.util.ReportImageExporter
import com.example.project1.util.VibrationUtil
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WeeklyDopamineReportDialog(
    report: WeeklyDopamineReport = WeeklyReportStorage.getLatestReport(LocalContext.current)
        ?: WeeklyDopamineReport(
            dateRange = "За 7 дней",
            savedHours = 14.2f,
            screenTimeHours = 18.5f,
            tasksSolved = 5,
            blitzWins = 7,
            coinsEarned = 2150,
            percentile = 89,
            topSavedApp = "YouTube Shorts",
            aiVerdict = "Отличная неделя! Ты предотвратил дофаминовые срывы и укрепил серию фокуса.",
            favoriteBlitzTopic = "ТФКП",
            favoriteBlitzComment = "Выбор истинных математических эстетов! Комплексный анализ и контуры покорились тебе.",
            tasksSolvedWithoutHints = 4,
            tasksSolvedWithHints = 1,
            aiAssistanceComment = "Чистый разум: 4 из 5 задач решены без единой подсказки от ИИ. Твой мозг работает как квантовый процессор!"
        ),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 6

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
                            listOf(Color(0xFF191326), Color(0xFF0F0C1C), Color(0xFF07050E))
                        )
                    )
                    .border(1.5.dp, colors.primaryBrush, RoundedCornerShape(28.dp))
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
                            if (colors.isGradient) {
                                val cStart = androidx.compose.ui.graphics.lerp(colors.primary, colors.secondary, startFraction)
                                val cEnd = androidx.compose.ui.graphics.lerp(colors.primary, colors.secondary, endFraction)
                                Brush.horizontalGradient(listOf(cStart, cEnd))
                            } else {
                                androidx.compose.ui.graphics.SolidColor(colors.primary)
                            }
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
                                    if (colors.isGradient) Brush.horizontalGradient(listOf(colors.primary.copy(alpha = 0.25f), colors.secondary.copy(alpha = 0.25f)))
                                    else androidx.compose.ui.graphics.SolidColor(colors.primary.copy(alpha = 0.25f))
                                )
                                .border(1.dp, colors.primaryBrush, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "DURITY WRAPPED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        }
                        Text(
                            text = report.dateRange,
                            fontSize = 12.sp,
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
                    // Анимированный тематический фон
                    when (currentStep) {
                        0 -> BooksFlippingBackground(modifier = Modifier.fillMaxSize())
                        1 -> MathSymbolsBackground(modifier = Modifier.fillMaxSize())
                        2 -> ComplexPlaneBackground(modifier = Modifier.fillMaxSize())
                        3 -> NeuralQuantumBackground(modifier = Modifier.fillMaxSize())
                        4 -> DopamineAuraBackground(modifier = Modifier.fillMaxSize())
                        5 -> CelebrationParticlesBackground(modifier = Modifier.fillMaxSize())
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
                        label = "3DStoryTransition"
                    ) { step ->
                        when (step) {
                            0 -> SlideSavedTime(report)
                            1 -> SlideIntelligence(report)
                            2 -> SlideFavoriteBlitz(report)
                            3 -> SlideAiAssistance(report)
                            4 -> SlideDiscipline(report)
                            5 -> SlideShareCard(report, onShare = {
                                VibrationUtil.vibrateSuccess(context)
                                ReportImageExporter.shareReportCard(
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
                                .background(colors.primaryBrush)
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
                                .background(colors.primaryBrush)
                                .clickable {
                                    VibrationUtil.vibrateSuccess(context)
                                    ReportImageExporter.shareReportCard(
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
                                    text = "Поделиться",
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

// ── Анимированные фоны ────────────────────────────────────────────────────────

@Composable
private fun BooksFlippingBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "BookFlipping")
    val pageBend by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PageBend"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FloatParticles"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val bookWidth = w * 0.7f
        val bookHeight = h * 0.35f
        val left = cx - bookWidth / 2f
        val right = cx + bookWidth / 2f
        val top = cy - bookHeight / 2f
        val bottom = cy + bookHeight / 2f

        val strokeColor = Color(0xFF69F0AE).copy(alpha = 0.08f)
        val activeColor = Color(0xFF69F0AE).copy(alpha = 0.16f)

        val leftCover = Path().apply {
            moveTo(cx, top + 20f)
            quadraticBezierTo(cx - bookWidth * 0.25f, top - 10f, left, top + 10f)
            lineTo(left, bottom)
            quadraticBezierTo(cx - bookWidth * 0.25f, bottom - 20f, cx, bottom + 10f)
            close()
        }
        drawPath(leftCover, strokeColor, style = Stroke(width = 2.5f))

        val rightCover = Path().apply {
            moveTo(cx, top + 20f)
            quadraticBezierTo(cx + bookWidth * 0.25f, top - 10f, right, top + 10f)
            lineTo(right, bottom)
            quadraticBezierTo(cx + bookWidth * 0.25f, bottom - 20f, cx, bottom + 10f)
            close()
        }
        drawPath(rightCover, strokeColor, style = Stroke(width = 2.5f))

        val turningPage = Path().apply {
            moveTo(cx, top + 20f)
            val tipX = cx + (bookWidth * 0.42f) * pageBend
            val tipY = top + 15f - kotlin.math.abs(pageBend) * 20f
            quadraticBezierTo(cx + (tipX - cx) * 0.5f, top - 30f, tipX, tipY)
            lineTo(tipX, bottom - 10f)
            quadraticBezierTo(cx + (tipX - cx) * 0.5f, bottom - 15f, cx, bottom + 10f)
            close()
        }
        drawPath(turningPage, activeColor, style = Stroke(width = 2f))

        for (i in 0..12) {
            val px = (cx + (i * 47) % (w * 0.8f) - w * 0.4f)
            val progress = (floatOffset + i * 0.08f) % 1f
            val py = h * (1f - progress)
            val alpha = sin(progress * Math.PI).toFloat() * 0.2f
            drawCircle(
                color = Color(0xFF69F0AE).copy(alpha = alpha),
                radius = 2.5f + (i % 3),
                center = Offset(px, py)
            )
        }
    }
}

@Composable
private fun MathSymbolsBackground(modifier: Modifier = Modifier) {
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
        listOf("π", "∑", "√", "∫", "Δ", "∞", "×", "÷", "λ", "+", "f(x)", "e²", "θ", "Ω")
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 32f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        symbols.forEachIndexed { index, symbol ->
            val baseX = ((index * 61 + 30) % w.toInt()).toFloat()
            val speed = 0.5f + (index % 4) * 0.25f
            val progress = (time * speed + index * 0.12f) % 1f
            val posY = h * (1f - progress)
            val driftX = baseX + sin(progress * 6.28f + index) * 20f

            val alpha = (sin(progress * Math.PI).toFloat() * 0.18f).coerceIn(0.04f, 0.22f)
            paint.color = android.graphics.Color.argb((alpha * 255).toInt(), 179, 136, 255)
            drawContext.canvas.nativeCanvas.drawText(symbol, driftX, posY, paint)
        }
    }
}

@Composable
private fun ComplexPlaneBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ComplexPlane")
    val rot by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(12000, easing = LinearEasing)),
        label = "Rot"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.45f
        val r = size.width * 0.35f

        // Комплексная плоскость: Re & Im оси
        drawLine(
            color = Color(0xFFCE93D8).copy(alpha = 0.15f),
            start = Offset(cx - r - 20f, cy),
            end = Offset(cx + r + 20f, cy),
            strokeWidth = 1.5f
        )
        drawLine(
            color = Color(0xFFCE93D8).copy(alpha = 0.15f),
            start = Offset(cx, cy - r - 20f),
            end = Offset(cx, cy + r + 20f),
            strokeWidth = 1.5f
        )

        // Единичная окружность
        drawCircle(
            color = Color(0xFFAB47BC).copy(alpha = 0.18f),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )

        // Вращающийся вектор e^(i*phi)
        val rad = Math.toRadians(rot.toDouble())
        val vx = cx + r * cos(rad).toFloat()
        val vy = cy + r * sin(rad).toFloat()

        drawLine(
            brush = Brush.linearGradient(listOf(Color(0xFFFFD54F).copy(alpha = 0.5f), Color(0xFFAB47BC).copy(alpha = 0.8f))),
            start = Offset(cx, cy),
            end = Offset(vx, vy),
            strokeWidth = 2.5f
        )
        drawCircle(color = Color(0xFFFFD54F), radius = 4f, center = Offset(vx, vy))
    }
}

@Composable
private fun NeuralQuantumBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "QuantumNet")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "Qpulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val nodes = listOf(
            Offset(w * 0.25f, h * 0.3f),
            Offset(w * 0.75f, h * 0.3f),
            Offset(w * 0.5f, h * 0.5f),
            Offset(w * 0.3f, h * 0.7f),
            Offset(w * 0.7f, h * 0.7f)
        )

        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                drawLine(
                    color = Color(0xFF00E676).copy(alpha = 0.12f),
                    start = nodes[i],
                    end = nodes[j],
                    strokeWidth = 1.5f
                )
            }
        }

        nodes.forEachIndexed { idx, pt ->
            val scale = (sin((pulse + idx * 0.2f) * Math.PI * 2) * 0.3 + 1.0).toFloat()
            drawCircle(
                color = Color(0xFF69F0AE).copy(alpha = 0.25f),
                radius = 8f * scale,
                center = pt
            )
            drawCircle(
                color = Color(0xFF00E676),
                radius = 3.5f,
                center = pt
            )
        }
    }
}

@Composable
private fun DopamineAuraBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "DopamineAura")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f

        for (i in 0..2) {
            val ringProgress = (pulse + i * 0.33f) % 1f
            val radius = 40f + ringProgress * (size.width * 0.55f)
            val alpha = (1f - ringProgress) * 0.22f

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFFD54F).copy(alpha = alpha), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )

            drawCircle(
                color = Color(0xFFFFB300).copy(alpha = alpha * 0.6f),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )
        }
    }
}

@Composable
private fun CelebrationParticlesBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "Celebration")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AnimTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        for (i in 0..18) {
            val progress = (animTime + i * 0.055f) % 1f
            val px = ((i * 73 + 20) % w.toInt()).toFloat()
            val py = h * (1f - progress)
            val alpha = sin(progress * Math.PI).toFloat() * 0.25f

            val color = when (i % 3) {
                0 -> Color(0xFFFFD54F).copy(alpha = alpha)
                1 -> Color(0xFFAB47BC).copy(alpha = alpha)
                else -> Color(0xFF69F0AE).copy(alpha = alpha)
            }

            val rad = 4f + (i % 3) * 2f
            drawCircle(
                color = color,
                radius = rad,
                center = Offset(px + sin(progress * 6f) * 15f, py)
            )
        }
    }
}

// ── Слайды истории ───────────────────────────────────────────────────────────

@Composable
private fun SlideSavedTime(report: WeeklyDopamineReport) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(Color(0xFF00E676).copy(alpha = 0.15f))
                .border(2.dp, Color(0xFF00E676).copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.HourglassBottom,
                contentDescription = null,
                tint = Color(0xFF69F0AE),
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(text = "Ты вернул себе", fontSize = 17.sp, color = Color.White.copy(alpha = 0.8f))

        Text(
            text = "${report.savedHours} часов",
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF69F0AE)
        )

        Text(text = "жизни от залипания в экраны", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))

        Spacer(modifier = Modifier.height(22.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF69F0AE).copy(alpha = 0.2f), RoundedCornerShape(18.dp))
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
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Это эквивалентно:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFD54F)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Прочтению 2–3 глубоких книг, 8 тренировкам или полноценному освоению нового навыка.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SlideIntelligence(report: WeeklyDopamineReport) {
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
                        Text(text = "Задачи дня", fontSize = 12.sp, color = Color(0xFFCE93D8))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "${report.tasksSolved} / 7", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
            text = "Твой мозг переключился с пассивного потребления контента на активное решение сложных задач.",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SlideFavoriteBlitz(report: WeeklyDopamineReport) {
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

        Text(text = "Любимая тема блица", fontSize = 16.sp, color = Color.White.copy(alpha = 0.75f))

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
private fun SlideAiAssistance(report: WeeklyDopamineReport) {
    val isPureMind = report.tasksSolvedWithoutHints >= report.tasksSolvedWithHints

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(if (isPureMind) Color(0xFF00E676).copy(alpha = 0.18f) else Color(0xFF00B0FF).copy(alpha = 0.18f))
                .border(2.dp, if (isPureMind) Color(0xFF69F0AE) else Color(0xFF40C4FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPureMind) Icons.Rounded.Psychology else Icons.Rounded.SmartToy,
                contentDescription = null,
                tint = if (isPureMind) Color(0xFF69F0AE) else Color(0xFF40C4FF),
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = if (isPureMind) "Чистый разум" else "Тандем с ИИ",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isPureMind) Color(0xFF69F0AE) else Color(0xFF40C4FF)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${report.tasksSolvedWithoutHints} задач решено без единой подсказки",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16232E)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isPureMind) Color(0xFF00E676).copy(alpha = 0.3f) else Color(0xFF00B0FF).copy(alpha = 0.3f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
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
private fun SlideDiscipline(report: WeeklyDopamineReport) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF9100).copy(alpha = 0.18f))
                .border(2.dp, Color(0xFFFFB300).copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = null,
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(text = "Дофаминовый рейтинг", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Ты продуктивнее ${report.percentile}% пользователей!",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFFD54F)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CoinIcon(size = 24)
                    Column {
                        Text(text = "Заработано монет", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        Text(text = "+${report.coinsEarned}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2E7D32).copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = "TOP ${(100 - report.percentile).coerceAtLeast(1)}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SlideShareCard(report: WeeklyDopamineReport, onShare: () -> Unit) {
    val colors = AppTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191226)),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    brush = colors.primaryBrush,
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DURITY WRAPPED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFD54F)
                    )
                    Text(
                        text = report.dateRange,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "+${report.savedHours} ч",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF69F0AE)
                )
                Text(
                    text = "сэкономленного времени жизни",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Задачи", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        Text(text = "${report.tasksSolved}/7", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Тема", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        Text(text = report.favoriteBlitzTopic, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Ранг", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        Text(text = "Топ ${100 - report.percentile}%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF69F0AE))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "«${report.aiVerdict}»",
                    fontSize = 11.5.sp,
                    color = Color(0xFFE1BEE7),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


