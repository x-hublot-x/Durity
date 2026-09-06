package com.example.project1.ui.screens.captchas

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.project1.data.model.GraphFunction
import com.example.project1.data.model.GraphFunctionConfig
import com.example.project1.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlin.math.*

// Si(x) = ∫₀ˣ sin(t)/t dt  (метод трапеций)
private fun integralSine(x: Float): Float {
    if (x == 0f) return 0f
    val steps = 600
    val dx = x / steps
    var sum = 0.0
    for (i in 1..steps) {
        val t = dx * i
        sum += (sin(t.toDouble()) / t) * dx
    }
    return sum.toFloat()
}

// ζ(s) = Σ 1/nˢ  (Дирихле, N=300)
private fun riemannZeta(s: Float): Float {
    if (s <= 1f) return 10f
    var sum = 0.0
    for (n in 1..300) {
        sum += 1.0 / n.toDouble().pow(s.toDouble())
    }
    return sum.toFloat()
}

// e^(−x²)
private fun gaussian(x: Float): Float = exp(-x.toDouble().pow(2.0)).toFloat()

// erf(x) = (2/√π) ∫₀ˣ e^(−t²) dt  (метод трапеций)
private fun erf(x: Float): Float {
    if (x == 0f) return 0f
    val steps = 400
    val dx = x / steps
    val factor = 2.0 / sqrt(PI)
    var sum = 0.0
    for (i in 1..steps) {
        val t = dx * i
        sum += exp(-t.toDouble().pow(2.0)) * dx
    }
    return (factor * sum).toFloat()
}

// Γ(x) через логарифм Ланчоша (g=7)
private val LANCZOS_COEF = doubleArrayOf(
    0.99999999999980993, 676.5203681218851, -1259.1392167224028,
    771.32342877765313, -176.61502916214059, 12.507343278686905,
    -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7
)

private fun gammaLn(x: Double): Double {
    var xx = x
    if (xx < 0.5) return ln(PI / (sin(PI * xx) * gammaLn(1.0 - xx).let { exp(it) }))
    xx -= 1.0
    var a = LANCZOS_COEF[0]
    val t = xx + 7.5
    for (i in 1..8) a += LANCZOS_COEF[i] / (xx + i)
    return 0.5 * ln(2 * PI) + (xx + 0.5) * ln(t) - t + ln(a)
}

private fun gammaFn(x: Float): Float {
    if (x <= 0f && x == x.toInt().toFloat()) return Float.MAX_VALUE
    return exp(gammaLn(x.toDouble())).toFloat()
}

// B(x, 2) = Γ(x)·Γ(2)/Γ(x+2) = Γ(x)/( x·(x+1) )
private fun betaFn(x: Float): Float {
    if (x <= 0f) return Float.MAX_VALUE
    val g = gammaFn(x)
    if (g == Float.MAX_VALUE) return Float.MAX_VALUE
    return g / (x * (x + 1f))
}

// ─── Генерация точек ─────────────────────────────────────────────────────────

private fun generateFunctionPoints(config: GraphFunctionConfig, count: Int = 150): List<Offset> {
    val points = mutableListOf<Offset>()
    for (i in 0..count) {
        val t = i.toFloat() / count
        val x = config.xRange.start + t * (config.xRange.endInclusive - config.xRange.start)
        val raw = when (config.type) {
            GraphFunction.INTEGRAL_SINE -> integralSine(x)
            GraphFunction.RIEMANN_ZETA  -> riemannZeta(x)
            GraphFunction.GAUSSIAN      -> gaussian(x)
            GraphFunction.ERF           -> erf(x)
            GraphFunction.GAMMA         -> gammaFn(x)
            GraphFunction.BETA          -> betaFn(x)
        }
        if (raw == Float.MAX_VALUE || raw.isNaN() || raw.isInfinite()) continue
        val clampedY = raw.coerceIn(config.yRange.start, config.yRange.endInclusive)
        points.add(Offset(x, clampedY))
    }
    return points
}

// ─── Нормализация координат ──────────────────────────────────────────────────

private fun Offset.toCanvas(
    config: GraphFunctionConfig,
    canvasSize: Size,
    padding: Float
): Offset {
    val w  = canvasSize.width  - 2 * padding
    val h  = canvasSize.height - 2 * padding
    val nx = (x - config.xRange.start) / (config.xRange.endInclusive - config.xRange.start)
    val ny = 1f - (y - config.yRange.start) / (config.yRange.endInclusive - config.yRange.start)
    return Offset(padding + nx * w, padding + ny * h)
}

private fun Offset.fromCanvas(
    config: GraphFunctionConfig,
    canvasSize: Size,
    padding: Float
): Offset {
    val w  = canvasSize.width  - 2 * padding
    val h  = canvasSize.height - 2 * padding
    val nx = (x - padding) / w
    val ny = 1f - (y - padding) / h
    return Offset(
        config.xRange.start + nx * (config.xRange.endInclusive - config.xRange.start),
        config.yRange.start + ny * (config.yRange.endInclusive - config.yRange.start)
    )
}

// ─── Оценка точности ────────────────────────────────────────────────────────

private fun evaluateTrace(
    userPoints: List<Offset>,
    refPoints:  List<Offset>,
    config:     GraphFunctionConfig,
    canvasSize: Size,
    padding:    Float
): Float {
    if (userPoints.size < 5) return Float.MAX_VALUE

    val userFn = userPoints.map { it.fromCanvas(config, canvasSize, padding) }
    val refFn  = refPoints                                  // уже в координатах функции

    val xSpan  = config.xRange.endInclusive - config.xRange.start
    val ySpan  = config.yRange.endInclusive - config.yRange.start
    val diag   = sqrt(xSpan.pow(2) + ySpan.pow(2))

    // 1. ref → user: среднее
    var sumRefToUser = 0f
    for (ref in refFn) {
        sumRefToUser += userFn.minOf { u ->
            sqrt((u.x - ref.x).pow(2) + (u.y - ref.y).pow(2))
        }
    }
    val avgRefToUser = (sumRefToUser / refFn.size) / diag

    // 2. user → ref: 90-й перцентиль (штраф за хвосты)
    val userToRefDists = userFn.map { u ->
        refFn.minOf { ref -> sqrt((u.x - ref.x).pow(2) + (u.y - ref.y).pow(2)) }
    }.sorted()
    val p90Index = (userToRefDists.size * 0.90).toInt().coerceIn(0, userToRefDists.size - 1)
    val p90UserToRef = userToRefDists[p90Index] / diag

    // 3. Покрытие: пользователь должен покрыть ≥70% диапазона X
    val userXMin = userFn.minOf { it.x }
    val userXMax = userFn.maxOf { it.x }
    val coverage = (userXMax - userXMin) / xSpan
    if (coverage < 0.70f) return Float.MAX_VALUE           // не провёл линию до конца

    // Итог: взвешенная сумма
    return avgRefToUser * 0.6f + p90UserToRef * 0.4f
}

// ─── Случайная конфигурация ──────────────────────────────────────────────────

private fun randomFunctionConfig(): GraphFunctionConfig {
    return when ((0..5).random()) {
        0 -> GraphFunctionConfig(
            type   = GraphFunction.INTEGRAL_SINE,
            xRange = (-5f * PI.toFloat())..(5f * PI.toFloat()),
            yRange = (-PI.toFloat() / 2)..(PI.toFloat() / 2)
        )
        1 -> GraphFunctionConfig(
            type   = GraphFunction.RIEMANN_ZETA,
            xRange = 1.2f..6f,
            yRange = 1f..4f
        )
        2 -> GraphFunctionConfig(
            type   = GraphFunction.GAUSSIAN,
            xRange = -3f..3f,
            yRange = 0f..1f
        )
        3 -> GraphFunctionConfig(
            type   = GraphFunction.ERF,
            xRange = -2.5f..2.5f,
            yRange = -1f..1f
        )
        4 -> GraphFunctionConfig(
            type   = GraphFunction.GAMMA,
            xRange = 0.5f..4.5f,
            yRange = 0f..6f
        )
        else -> GraphFunctionConfig(
            type   = GraphFunction.BETA,
            xRange = 0.5f..5f,
            yRange = 0f..2f
        )
    }
}

// ─── Оси координат ───────────────────────────────────────────────────────────

private fun DrawScope.drawAxes(
    config:    GraphFunctionConfig,
    padding:   Float,
    gridColor: Color,
    axisColor: Color
) {
    val w = size.width  - 2 * padding
    val h = size.height - 2 * padding

    // Сетка 5×5
    for (i in 0..5) {
        val x = padding + i * w / 5
        drawLine(gridColor, Offset(x, padding), Offset(x, padding + h), strokeWidth = 0.8f)
    }
    for (j in 0..5) {
        val y = padding + j * h / 5
        drawLine(gridColor, Offset(padding, y), Offset(padding + w, y), strokeWidth = 0.8f)
    }

    // Ось X
    val zeroY = Offset(0f, 0f).toCanvas(config, size, padding).y
        .coerceIn(padding, padding + h)
    drawLine(axisColor, Offset(padding, zeroY), Offset(padding + w, zeroY), strokeWidth = 1.8f)

    // Ось Y
    val zeroX = Offset(0f, 0f).toCanvas(config, size, padding).x
        .coerceIn(padding, padding + w)
    drawLine(axisColor, Offset(zeroX, padding), Offset(zeroX, padding + h), strokeWidth = 1.8f)

    // Стрелки
    val arr = 8f
    drawLine(axisColor, Offset(padding + w, zeroY), Offset(padding + w - arr, zeroY - arr / 2), strokeWidth = 1.8f)
    drawLine(axisColor, Offset(padding + w, zeroY), Offset(padding + w - arr, zeroY + arr / 2), strokeWidth = 1.8f)
    drawLine(axisColor, Offset(zeroX, padding),     Offset(zeroX - arr / 2, padding + arr),     strokeWidth = 1.8f)
    drawLine(axisColor, Offset(zeroX, padding),     Offset(zeroX + arr / 2, padding + arr),     strokeWidth = 1.8f)
}

// ─── Диалог ──────────────────────────────────────────────────────────────────

@Composable
fun GraphTraceCaptchaDialog(
    onDismiss: () -> Unit,
    onSolved:  () -> Unit
) {
    val config    = remember { randomFunctionConfig() }
    val refPoints = remember { generateFunctionPoints(config, 150) }

    var phase         by remember { mutableStateOf(0) }
    var attemptsLeft  by remember { mutableStateOf(3) }
    var resultMessage by remember { mutableStateOf("") }
    var isSuccess     by remember { mutableStateOf(false) }

    val dotProgress = remember { Animatable(0f) }
    val userPoints  = remember { mutableStateListOf<Offset>() }

    var canvasSize    by remember { mutableStateOf(Size(1f, 1f)) }
    val canvasPadding = 32f

    val tailLength = 20
    val dotTrail   = remember { mutableStateListOf<Offset>() }

    // 1 сек паузы → 2 сек анимации → фаза рисования
    LaunchedEffect(Unit) {
        delay(1000L)
        phase = 1
        dotProgress.animateTo(
            1f,
            animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
        )
        phase = 2
        dotTrail.clear()
    }

    // Хвост точки
    LaunchedEffect(dotProgress.value) {
        if (phase == 1 && canvasSize.width > 1f) {
            val idx = (dotProgress.value * (refPoints.size - 1)).toInt()
                .coerceIn(0, refPoints.size - 1)
            val pos = refPoints[idx].toCanvas(config, canvasSize, canvasPadding)
            dotTrail.add(pos)
            if (dotTrail.size > tailLength) dotTrail.removeAt(0)
        }
    }

    // Автозакрытие при провале
    LaunchedEffect(attemptsLeft) {
        if (attemptsLeft <= 0) {
            delay(2000L)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = { if (phase != 1 && attemptsLeft > 0) onDismiss() }) {
        Surface(
            shape    = RoundedCornerShape(24.dp),
            color    = Color(0xFF1F1F2C),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier            = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Повтори график", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text       = config.type.displayName,
                    fontSize   = 12.sp,
                    color      = Color(0xFF80D8FF),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                val hint = when {
                    attemptsLeft <= 0 -> "Попытки исчерпаны. Закрытие..."
                    isSuccess         -> "✓ Верно!"
                    phase == 0        -> "Смотри внимательно..."
                    phase == 1        -> "Запоминай траекторию"
                    phase == 2        -> "Нарисуй линию пальцем • Осталось: $attemptsLeft"
                    else              -> resultMessage
                }
                Text(
                    text      = hint,
                    fontSize  = 12.sp,
                    color     = when {
                        isSuccess         -> Color(0xFF69F0AE)
                        attemptsLeft <= 0 -> Color(0xFFFF5252)
                        phase == 2        -> Color.White.copy(alpha = 0.7f)
                        else              -> Color(0xFF80D8FF)
                    },
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Холст ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    val accentColor = AppTheme.accent
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { intSize ->
                                canvasSize = Size(intSize.width.toFloat(), intSize.height.toFloat())
                            }
                            .pointerInput(phase) {
                                if (phase == 2) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            userPoints.clear()
                                            userPoints.add(offset)
                                        },
                                        onDrag = { change, _ ->
                                            userPoints.add(change.position)
                                        },
                                        onDragEnd = {
                                            val error = evaluateTrace(
                                                userPoints.toList(),
                                                refPoints,
                                                config,
                                                canvasSize,
                                                canvasPadding
                                            )
                                            // Порог 7% — строгая проверка
                                            if (error < 0.07f) {
                                                isSuccess = true
                                                phase = 3
                                                onSolved()
                                            } else {
                                                attemptsLeft--
                                                resultMessage = if (attemptsLeft > 0)
                                                    "Не точно, попробуй ещё • Осталось: $attemptsLeft"
                                                else
                                                    "Попытки исчерпаны!"
                                                phase = 3
                                                if (attemptsLeft > 0) {
                                                    userPoints.clear()
                                                    phase = 2
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                    ) {
                        // Фон
                        drawRect(Color(0xFF12121C))

                        // Сетка и оси
                        drawAxes(
                            config    = config,
                            padding   = canvasPadding,
                            gridColor = Color(0xFF2A2A40),
                            axisColor = Color(0xFF4A4A6A)
                        )

                        // Движущаяся точка с хвостом — только во время анимации (фаза 0–1)
                        if (phase <= 1 && dotProgress.value > 0f && canvasSize.width > 1f) {
                            // Хвост
                            dotTrail.forEachIndexed { i, trailPt ->
                                val alpha  = (i.toFloat() / dotTrail.size) * 0.50f
                                val radius = 3f + (i.toFloat() / dotTrail.size) * 5f
                                drawCircle(
                                    color  = accentColor.copy(alpha = alpha),
                                    radius = radius,
                                    center = trailPt
                                )
                            }
                            // Сама точка
                            val idx    = (dotProgress.value * (refPoints.size - 1))
                                .toInt().coerceIn(0, refPoints.size - 1)
                            val dotPos = refPoints[idx].toCanvas(config, size, canvasPadding)
                            drawCircle(color = accentColor.copy(alpha = 0.25f), radius = 18f, center = dotPos)
                            drawCircle(color = accentColor,                     radius = 7f,  center = dotPos)
                            drawCircle(color = Color.White,                            radius = 3f,  center = dotPos)
                        }

                        // Линия пользователя (фаза 2–3)
                        if (phase >= 2 && userPoints.size >= 2) {
                            val userPath = Path()
                            userPoints.forEachIndexed { i, p ->
                                if (i == 0) userPath.moveTo(p.x, p.y) else userPath.lineTo(p.x, p.y)
                            }
                            val userColor = when {
                                isSuccess         -> Color(0xFF69F0AE)
                                attemptsLeft <= 0 -> Color(0xFFFF5252)
                                else              -> Color(0xFFFFD54F)
                            }
                            drawPath(
                                userPath,
                                color = userColor,
                                style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Кнопки ──
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick  = onDismiss,
                        enabled  = phase != 1 && attemptsLeft > 0 && !isSuccess,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Отмена",
                            color = Color.White.copy(
                                alpha = if (phase == 1 || attemptsLeft <= 0 || isSuccess) 0.2f else 0.6f
                            )
                        )
                    }

                    if (!isSuccess && attemptsLeft > 0 && phase >= 2) {
                        Button(
                            onClick  = { userPoints.clear() },
                            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3C)),
                            shape    = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Стереть", color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}
