package com.example.project1.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.project1.ui.theme.BottomBarStyle
import kotlin.math.*
import kotlin.random.Random

@Composable
fun AnimatedBottomBarEffect(
    style: BottomBarStyle,
    modifier: Modifier = Modifier
) {
    when (style) {
        BottomBarStyle.RIVER -> PureFlowingWaterEffect(modifier)
        BottomBarStyle.PLANTS -> LushFoliageBushEffect(modifier)
        BottomBarStyle.METEORS -> RealisticMeteorsEffect(modifier)
        BottomBarStyle.SATURN -> RealisticSaturnEffect(modifier)
        BottomBarStyle.FIRE -> SoftRealisticFireEffect(modifier)
        BottomBarStyle.ANTS -> BigAntArmyEffect(modifier)
        else -> {}
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 1. МОРСКАЯ / РЕЧНАЯ ВОДА: Шелковистые волны, чистые каустики, БЕЗ БЕЛЫХ КРУГОВ
// Оптимизировано: 0 аллокаций Path и списков цветов в цикле отрисовки
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun PureFlowingWaterEffect(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "WaterAnim")
    val flowTime by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4600, easing = LinearEasing)),
        label = "waterFlow"
    )
    val swellTime by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "waterSwell"
    )

    // Переиспользуемые Path и неизменяемые списки цветов (0 аллокаций на кадр)
    val deepPath = remember { Path() }
    val midPath = remember { Path() }
    val causticPath1 = remember { Path() }
    val surfPath = remember { Path() }
    val causticPath2 = remember { Path() }

    val deepColors = remember { listOf(Color(0x35005F73), Color(0x550A335C), Color(0x75041B33)) }
    val midColors = remember { listOf(Color(0x450A9396), Color(0x35005F73), Color(0x60002E4A)) }
    val causticColors1 = remember { listOf(Color.Transparent, Color(0x4094D2BD), Color(0x60E9D8A6), Color(0x4094D2BD), Color.Transparent) }
    val surfColors = remember { listOf(Color(0x5094D2BD), Color(0x350A9396), Color(0x55005F73)) }
    val causticColors2 = remember { listOf(Color.Transparent, Color(0x50E0FBFC), Color(0x7098F5E1), Color(0x50E0FBFC), Color.Transparent) }
    val moonColors = remember { listOf(Color.Transparent, Color(0x2080DEEA), Color.Transparent) }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // ── Слой 1: Глубокая темно-синяя толща морской воды ──
        deepPath.reset()
        deepPath.moveTo(0f, h * 0.40f)
        var x = 0f
        while (x <= w) {
            val wave1 = sin((x * 0.009f + flowTime * 0.7f).toDouble()).toFloat() * 5.5f
            val wave2 = cos((x * 0.020f - flowTime * 0.4f).toDouble()).toFloat() * 3.2f
            deepPath.lineTo(x, h * 0.40f + wave1 + wave2)
            x += 10f
        }
        deepPath.lineTo(w, h)
        deepPath.lineTo(0f, h)
        deepPath.close()
        drawPath(
            deepPath,
            brush = Brush.verticalGradient(deepColors, startY = h * 0.35f, endY = h)
        )

        // ── Слой 2: Средние катящиеся бирюзовые волны ──
        midPath.reset()
        midPath.moveTo(0f, h * 0.54f)
        x = 0f
        while (x <= w) {
            val wave1 = sin((x * 0.015f - flowTime * 1.1f).toDouble()).toFloat() * 5.0f
            val wave2 = sin((x * 0.032f + flowTime * 0.8f).toDouble()).toFloat() * 2.5f
            midPath.lineTo(x, h * 0.54f + wave1 + wave2)
            x += 8f
        }
        midPath.lineTo(w, h)
        midPath.lineTo(0f, h)
        midPath.close()
        drawPath(
            midPath,
            brush = Brush.verticalGradient(midColors, startY = h * 0.50f, endY = h)
        )

        // ── Слой 3: Плавные текучие световые каустики (ленты света вдоль изгиба волны, БЕЗ КРУГОВ) ──
        causticPath1.reset()
        x = 0f
        while (x <= w) {
            val cy = h * 0.52f +
                    sin((x * 0.016f - flowTime * 1.1f).toDouble()).toFloat() * 4.8f +
                    cos((x * 0.035f + swellTime).toDouble()).toFloat() * 2.0f
            if (x == 0f) causticPath1.moveTo(x, cy) else causticPath1.lineTo(x, cy)
            x += 8f
        }
        drawPath(
            causticPath1,
            brush = Brush.horizontalGradient(causticColors1),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        // ── Слой 4: Поверхностная волна ──
        surfPath.reset()
        surfPath.moveTo(0f, h * 0.68f)
        x = 0f
        while (x <= w) {
            val wave1 = sin((x * 0.020f - flowTime * 1.4f).toDouble()).toFloat() * 3.8f
            val wave2 = cos((x * 0.045f + flowTime * 0.9f).toDouble()).toFloat() * 2.0f
            surfPath.lineTo(x, h * 0.68f + wave1 + wave2)
            x += 8f
        }
        surfPath.lineTo(w, h)
        surfPath.lineTo(0f, h)
        surfPath.close()
        drawPath(
            surfPath,
            brush = Brush.verticalGradient(surfColors, startY = h * 0.65f, endY = h)
        )

        // Вторая тонкая полупрозрачная лента светового переката на гребне
        causticPath2.reset()
        x = 0f
        while (x <= w) {
            val cy = h * 0.67f +
                    sin((x * 0.020f - flowTime * 1.4f).toDouble()).toFloat() * 3.8f +
                    cos((x * 0.045f + flowTime * 0.9f).toDouble()).toFloat() * 2.0f
            if (x == 0f) causticPath2.moveTo(x, cy) else causticPath2.lineTo(x, cy)
            x += 8f
        }
        drawPath(
            causticPath2,
            brush = Brush.horizontalGradient(causticColors2),
            style = Stroke(width = 1.8f, cap = StrokeCap.Round)
        )

        // Мягкое общее рассеянное лунное сияние над поверхностью воды
        drawRect(
            brush = Brush.verticalGradient(moonColors, startY = h * 0.40f, endY = h * 0.75f)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 2. ПЫШНЫЕ КУСТАРНИКИ, ЛИСТЬЯ И ЦВЕТОЧКИ ПО ВСЕЙ ГРАНИ
// Оптимизировано: Reusable Path, zero-allocation render loop (0 GC pauses)
// ══════════════════════════════════════════════════════════════════════════════
private data class ShrubBranch(
    val xFrac: Float,
    val heightPx: Float,
    val leafCount: Int,
    val branchAngle: Float,
    val leafSize: Float,
    val type: Int, // 0: broad bush leaf, 1: serrated/fern, 2: round clover, 3: spear leaf
    val layer: Int // 0: back, 1: mid, 2: fore
)

private data class FlowerData(
    val xFrac: Float,
    val heightPx: Float,
    val petalCount: Int,
    val petalColor: Color,
    val coreColor: Color,
    val radius: Float
)

@Composable
fun LushFoliageBushEffect(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "FoliageAnim")
    val windWave by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3800, easing = LinearEasing)),
        label = "windWave"
    )
    val gustSway by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gustSway"
    )

    // 28 густых ветвей кустарников, покрывающих 100% нижней границы
    val branches = remember {
        val list = mutableListOf<ShrubBranch>()
        val rnd = Random(777)
        val branchCount = 28
        for (i in 0 until branchCount) {
            val x = (i + rnd.nextFloat() * 0.5f) / branchCount.toFloat()
            val layer = when {
                i % 3 == 0 -> 0 // дальний
                i % 3 == 1 -> 1 // средний
                else -> 2       // передний
            }
            val type = i % 4
            val h = when (layer) {
                0 -> 40f + rnd.nextFloat() * 20f
                1 -> 32f + rnd.nextFloat() * 16f
                else -> 24f + rnd.nextFloat() * 14f
            }
            val leafSize = when (type) {
                1 -> 9f + rnd.nextFloat() * 4f // папоротник
                else -> 12f + rnd.nextFloat() * 5f
            }
            val angle = (rnd.nextFloat() - 0.5f) * 28f
            list.add(ShrubBranch(x, h, 5 + (i % 4), angle, leafSize, type, layer))
        }
        list
    }

    // Предварительное разделение по слоям (0 аллокаций при отрисовке)
    val layer0Branches = remember { branches.filter { it.layer == 0 } }
    val layer1Branches = remember { branches.filter { it.layer == 1 } }
    val layer2Branches = remember { branches.filter { it.layer == 2 } }

    // 4 нежных лесных цветочка в кустарнике
    val flowers = remember {
        listOf(
            FlowerData(0.18f, 38f, 5, Color(0xFFF48FB1), Color(0xFFFFEE58), 6.5f), // Розовый шиповник
            FlowerData(0.42f, 32f, 5, Color(0xFFFFD54F), Color(0xFFE65100), 6.0f), // Золотистый лютик
            FlowerData(0.68f, 36f, 5, Color(0xFFB39DDB), Color(0xFFFFF59D), 6.5f), // Нежная лаванда
            FlowerData(0.86f, 30f, 5, Color(0xFFFFAB91), Color(0xFFFFEB3B), 5.8f)  // Коралловый первоцвет
        )
    }

    // Переиспользуемые объекты Path для нулевой нагрузки на GC
    val bushBgPath = remember { Path() }
    val stemPath = remember { Path() }
    val leafPath = remember { Path() }
    val tipLeafPath = remember { Path() }
    val flowerStemPath = remember { Path() }
    val bushBgColors = remember { listOf(Color(0xFF1B4323), Color(0xFF0C2411)) }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // ── 1. Сплошная плотная подложка кустарника по всей нижней грани ──
        bushBgPath.reset()
        bushBgPath.moveTo(0f, h)
        var x = 0f
        while (x <= w) {
            val by = h - 26f - sin((x * 0.025f + windWave).toDouble()).toFloat() * 6f -
                     cos((x * 0.06f).toDouble()).toFloat() * 7f
            bushBgPath.lineTo(x, by)
            x += 12f
        }
        bushBgPath.lineTo(w, h)
        bushBgPath.close()
        drawPath(
            bushBgPath,
            brush = Brush.verticalGradient(bushBgColors, startY = h - 42f, endY = h)
        )

        // ── 2. Прорисовка ветвей кустарников по слоям ──
        for (layerIdx in 0..2) {
            val layerBranches = when (layerIdx) {
                0 -> layer0Branches
                1 -> layer1Branches
                else -> layer2Branches
            }

            val stemColor = when (layerIdx) {
                0 -> Color(0xFF143A1D)
                1 -> Color(0xFF225B2C)
                else -> Color(0xFF2E7D32)
            }

            for (branch in layerBranches) {
                val baseX = branch.xFrac * w
                val windOffset = sin((baseX * 0.008f - windWave * 1.5f).toDouble()).toFloat() * 8.0f +
                                 gustSway * 3.5f
                val branchAngle = branch.branchAngle + windOffset * 0.65f

                rotate(branchAngle, pivot = Offset(baseX, h)) {
                    val tipY = h - branch.heightPx

                    // Стебель ветви
                    stemPath.reset()
                    stemPath.moveTo(baseX, h)
                    stemPath.quadraticBezierTo(baseX + windOffset * 0.3f, h - branch.heightPx * 0.5f, baseX, tipY)
                    drawPath(stemPath, color = stemColor, style = Stroke(width = 2.2f, cap = StrokeCap.Round))

                    // Листья вдоль ветви
                    for (li in 1..branch.leafCount) {
                        val leafFrac = li / branch.leafCount.toFloat()
                        val leafY = h - branch.heightPx * leafFrac
                        val leafX = baseX

                        val isLeft = (li % 2 == 0)
                        val sideSign = if (isLeft) -1f else 1f
                        val leafLength = branch.leafSize * (0.85f + (1f - leafFrac) * 0.4f)
                        val leafWidth = leafLength * 0.65f

                        val leafAngle = sideSign * (32f + li * 4f) + windOffset * 0.4f
                        rotate(leafAngle, pivot = Offset(leafX, leafY)) {
                            leafPath.reset()
                            when (branch.type) {
                                1 -> {
                                    // ПАПОРОТНИК (Узкие частые резные листочки)
                                    leafPath.moveTo(leafX, leafY)
                                    leafPath.quadraticBezierTo(leafX + sideSign * 4f, leafY - leafLength * 0.5f, leafX, leafY - leafLength)
                                    leafPath.quadraticBezierTo(leafX - sideSign * 4f, leafY - leafLength * 0.5f, leafX, leafY)
                                    leafPath.close()
                                    val fCol = if (layerIdx == 2) Color(0xFF4CAF50) else Color(0xFF388E3C)
                                    drawPath(leafPath, color = fCol)
                                }
                                2 -> {
                                    // ОКРУГЛЫЕ ПЫШНЫЕ ЛИСТЬЯ КУСТА
                                    leafPath.moveTo(leafX, leafY)
                                    leafPath.cubicTo(
                                        leafX + sideSign * leafWidth, leafY - leafLength * 0.3f,
                                        leafX + sideSign * leafWidth * 0.8f, leafY - leafLength,
                                        leafX, leafY - leafLength
                                    )
                                    leafPath.cubicTo(
                                        leafX - sideSign * leafWidth * 0.8f, leafY - leafLength,
                                        leafX - sideSign * leafWidth, leafY - leafLength * 0.3f,
                                        leafX, leafY
                                    )
                                    leafPath.close()
                                    val fCol = if (layerIdx == 2) Color(0xFF66BB6A) else Color(0xFF2E7D32)
                                    drawPath(leafPath, color = fCol)
                                }
                                else -> {
                                    // КЛАССИЧЕСКИЕ КРУПНЫЕ ЛИСТЬЯ КУСТАРНИКА С ПРОЖИЛКАМИ
                                    leafPath.moveTo(leafX, leafY)
                                    leafPath.quadraticBezierTo(
                                        leafX + sideSign * leafWidth * 0.65f, leafY - leafLength * 0.5f,
                                        leafX, leafY - leafLength
                                    )
                                    leafPath.quadraticBezierTo(
                                        leafX - sideSign * leafWidth * 0.65f, leafY - leafLength * 0.5f,
                                        leafX, leafY
                                    )
                                    leafPath.close()
                                    val fCol = if (layerIdx == 2) Color(0xFF81C784) else Color(0xFF43A047)
                                    drawPath(leafPath, color = fCol)
                                    // Центральная прожилка
                                    drawLine(
                                        color = Color(0x70A5D6A7),
                                        start = Offset(leafX, leafY),
                                        end = Offset(leafX, leafY - leafLength * 0.85f),
                                        strokeWidth = 0.9f
                                    )
                                }
                            }
                        }
                    }

                    // Сочный верхушечный лист
                    tipLeafPath.reset()
                    tipLeafPath.moveTo(baseX, tipY)
                    tipLeafPath.quadraticBezierTo(baseX + 4.5f, tipY - 10f, baseX, tipY - 14f)
                    tipLeafPath.quadraticBezierTo(baseX - 4.5f, tipY - 10f, baseX, tipY)
                    tipLeafPath.close()
                    drawPath(tipLeafPath, color = if (layerIdx == 2) Color(0xFF81C784) else Color(0xFF4CAF50))
                }
            }
        }

        // ── 3. ЦВЕТЫ В КУСТАРНИКЕ (4 цветущих бутона) ──
        for (flower in flowers) {
            val fx = flower.xFrac * w
            val windOffset = sin((fx * 0.008f - windWave * 1.5f).toDouble()).toFloat() * 6.5f
            val flowerX = fx + windOffset
            val flowerY = h - flower.heightPx

            // Стебель цветка
            flowerStemPath.reset()
            flowerStemPath.moveTo(fx, h)
            flowerStemPath.quadraticBezierTo(fx + windOffset * 0.4f, h - flower.heightPx * 0.5f, flowerX, flowerY)
            drawPath(flowerStemPath, color = Color(0xFF2E7D32), style = Stroke(width = 1.8f, cap = StrokeCap.Round))

            // 5 лепестков вокруг центра
            val pRadius = flower.radius
            for (p in 0 until flower.petalCount) {
                val pAngle = (p / flower.petalCount.toFloat()) * 2 * PI.toFloat()
                val px = flowerX + cos(pAngle) * pRadius * 0.75f
                val py = flowerY + sin(pAngle) * pRadius * 0.75f
                drawCircle(
                    color = flower.petalColor,
                    radius = pRadius * 0.58f,
                    center = Offset(px, py)
                )
            }
            // Яркая сердцевина цветка (тычинки)
            drawCircle(
                color = flower.coreColor,
                radius = pRadius * 0.42f,
                center = Offset(flowerX, flowerY)
            )
            // Блик в центре
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = pRadius * 0.18f,
                center = Offset(flowerX - 0.8f, flowerY - 0.8f)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 3. ЗВЕЗДОПАД (Понравившийся стиль): Пологий угол, яркое ядро и шлейф
// ══════════════════════════════════════════════════════════════════════════════
private data class StarData(val xFrac: Float, val yFrac: Float, val radius: Float, val speed: Float, val phase: Float)

@Composable
fun RealisticMeteorsEffect(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "MeteorsAnim")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "meteorTime"
    )

    val backgroundStars = remember {
        val rnd = Random(404)
        List(32) {
            StarData(
                xFrac = rnd.nextFloat(),
                yFrac = rnd.nextFloat() * 0.85f,
                radius = if (rnd.nextFloat() > 0.8f) 1.8f else 1.1f,
                speed = 1.2f + rnd.nextFloat() * 2.5f,
                phase = rnd.nextFloat() * 2 * PI.toFloat()
            )
        }
    }

    val meteorConfigs = remember {
        listOf(
            Triple(0.85f, 0.05f, 0.0f),
            Triple(0.55f, 0.12f, 0.35f),
            Triple(0.98f, 0.22f, 0.65f),
            Triple(0.35f, 0.08f, 0.82f)
        )
    }

    val angleRad = (24.0 * PI / 180.0)
    val cosA = cos(angleRad).toFloat()
    val sinA = sin(angleRad).toFloat()

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        for (star in backgroundStars) {
            val starAlpha = 0.20f + 0.65f * (0.5f + 0.5f * sin((time * 2 * PI * star.speed + star.phase).toDouble()).toFloat())
            drawCircle(
                color = Color(0xFFE2E8F0).copy(alpha = starAlpha),
                radius = star.radius,
                center = Offset(star.xFrac * w, star.yFrac * h)
            )
        }

        for ((startXFrac, startYFrac, timeOffset) in meteorConfigs) {
            val localT = (time + timeOffset) % 1f
            if (localT < 0.32f) {
                val progress = localT / 0.32f
                val distance = progress * (w * 0.55f)
                val headX = startXFrac * w - distance * cosA
                val headY = startYFrac * h + distance * sinA

                val tailLen = 110f + sin(progress * PI.toFloat()) * 40f
                val tailEndX = headX + tailLen * cosA
                val tailEndY = headY - tailLen * sinA

                val alpha = (sin(progress * PI.toFloat()) * 1.1f).coerceIn(0f, 1f)

                drawLine(
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = alpha),
                            Color(0xFF80D8FF).copy(alpha = 0.8f * alpha),
                            Color(0xFF7C4DFF).copy(alpha = 0.35f * alpha),
                            Color.Transparent
                        ),
                        start = Offset(headX, headY),
                        end = Offset(tailEndX, tailEndY)
                    ),
                    start = Offset(headX, headY),
                    end = Offset(tailEndX, tailEndY),
                    strokeWidth = 2.0f,
                    cap = StrokeCap.Round
                )

                drawCircle(color = Color(0x6080D8FF).copy(alpha = alpha), radius = 4.5f, center = Offset(headX, headY))
                drawCircle(color = Color.White.copy(alpha = alpha), radius = 2.2f, center = Offset(headX, headY))

                for (s in 1..3) {
                    val sparkDist = tailLen * (s * 0.25f)
                    val spX = headX + sparkDist * cosA + (s * 3.5f - 5f)
                    val spY = headY - sparkDist * sinA + (s * 2f - 3f)
                    drawCircle(color = Color(0xFFB388FF).copy(alpha = alpha * 0.6f), radius = 1.2f, center = Offset(spX, spY))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 4. САТУРН (Понравившийся стиль): Тёмный кинематографичный Сатурн и звёзды
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun RealisticSaturnEffect(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "SaturnAnim")
    val starTwinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "twinkle"
    )
    val ringSpin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "ringSpin"
    )

    val cosmosStars = remember {
        val rnd = Random(505)
        List(28) {
            StarData(
                xFrac = rnd.nextFloat(),
                yFrac = rnd.nextFloat(),
                radius = if (rnd.nextFloat() > 0.75f) 1.9f else 1.1f,
                speed = 0.8f + rnd.nextFloat() * 1.6f,
                phase = rnd.nextFloat() * 2 * PI.toFloat()
            )
        }
    }

    val ringColorsBack = remember {
        listOf(
            Color.Transparent,
            Color(0x759E8055),
            Color(0xC0C4A470),
            Color(0x20000000),
            Color(0xA5A88B5A),
            Color.Transparent
        )
    }
    val planetColors = remember {
        listOf(
            Color(0xFFD4B278),
            Color(0xFFA6854C),
            Color(0xFF6D522B),
            Color(0xFF2A1C0E),
            Color(0xFF0C0703)
        )
    }
    val shadowColors = remember {
        listOf(Color.Transparent, Color(0x9505050A), Color.Transparent)
    }
    val ringColorsFront = remember {
        listOf(
            Color.Transparent,
            Color(0x859E8055),
            Color(0xD0C4A470),
            Color(0x30000000),
            Color(0xB5A88B5A),
            Color.Transparent
        )
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        for (star in cosmosStars) {
            val alpha = 0.15f + 0.70f * (0.5f + 0.5f * sin((starTwinkle * star.speed + star.phase).toDouble()).toFloat())
            drawCircle(
                color = Color(0xFFEDE7F6).copy(alpha = alpha),
                radius = star.radius,
                center = Offset(star.xFrac * w, star.yFrac * h)
            )
        }

        val cx = w * 0.5f
        val cy = h * 0.52f
        val planetR = h * 0.27f

        rotate(-22f, pivot = Offset(cx, cy)) {
            val ringRadiusX = planetR * 2.6f
            val ringRadiusY = planetR * 0.68f

            drawArc(
                brush = Brush.radialGradient(
                    colors = ringColorsBack,
                    center = Offset(cx, cy),
                    radius = ringRadiusX
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - ringRadiusX, cy - ringRadiusY),
                size = Size(ringRadiusX * 2, ringRadiusY * 2),
                style = Stroke(width = 8.5f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = planetColors,
                    center = Offset(cx + planetR * 0.35f, cy - planetR * 0.3f),
                    radius = planetR * 1.5f
                ),
                radius = planetR,
                center = Offset(cx, cy)
            )

            drawArc(
                color = Color(0x353E2723),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - planetR, cy - planetR * 0.25f),
                size = Size(planetR * 2, planetR * 0.5f),
                style = Stroke(width = 2.5f)
            )

            drawRect(
                brush = Brush.horizontalGradient(
                    shadowColors,
                    startX = cx - planetR * 0.9f,
                    endX = cx - planetR * 0.1f
                ),
                topLeft = Offset(cx - planetR * 0.9f, cy - ringRadiusY - 4f),
                size = Size(planetR * 0.8f, 10f)
            )

            drawArc(
                brush = Brush.radialGradient(
                    colors = ringColorsFront,
                    center = Offset(cx, cy),
                    radius = ringRadiusX
                ),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - ringRadiusX, cy - ringRadiusY),
                size = Size(ringRadiusX * 2, ringRadiusY * 2),
                style = Stroke(width = 8.5f)
            )

            val rad = ringSpin * PI.toFloat() / 180f
            val partX = cx + cos(rad) * ringRadiusX * 0.88f
            val partY = cy + sin(rad) * ringRadiusY * 0.88f
            drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 1.6f, center = Offset(partX, partY))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 5. ОГОНЬ: Медленный, мягкий, полупрозрачный, невысокий + легкий дым
// Оптимизировано: Reusable Path, zero-allocation render loop
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun SoftRealisticFireEffect(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "SoftFireAnim")
    val flamePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3800, easing = LinearEasing)),
        label = "flamePhase"
    )
    val smokePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "smokePhase"
    )

    val outerFlame = remember { Path() }
    val midFlame = remember { Path() }
    val coreFlame = remember { Path() }

    val baseGlowColors = remember { listOf(Color.Transparent, Color(0x35FF3D00), Color(0x60BF360C)) }
    val outerFlameColors = remember { listOf(Color(0x35D84315), Color(0x55FF5722), Color(0x75E64A19)) }
    val midFlameColors = remember { listOf(Color(0x40FFA726), Color(0x65FF9800), Color(0x80F57C00)) }
    val coreFlameColors = remember { listOf(Color(0x55FFF9C4), Color(0x75FFE082)) }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val fireBaseY = h
        val maxFlameH = h * 0.22f

        for (i in 0..10) {
            val smokeLocal = (smokePhase + i * 0.09f) % 1f
            val smokeX = w * (0.05f + i * 0.09f) + sin((smokePhase * PI * 2 + i * 1.5f).toDouble()).toFloat() * 12f
            val smokeY = fireBaseY - maxFlameH - smokeLocal * (h * 0.45f)
            val smokeR = 8f + smokeLocal * 20f
            val smokeAlpha = (0.08f * (1f - smokeLocal)).coerceIn(0f, 0.10f)

            drawCircle(
                color = Color(0xFF90A4AE).copy(alpha = smokeAlpha),
                radius = smokeR,
                center = Offset(smokeX, smokeY)
            )
        }

        drawRect(
            brush = Brush.verticalGradient(
                baseGlowColors,
                startY = fireBaseY - maxFlameH * 1.4f,
                endY = fireBaseY
            ),
            topLeft = Offset(0f, fireBaseY - maxFlameH * 1.4f),
            size = Size(w, maxFlameH * 1.4f)
        )

        outerFlame.reset()
        outerFlame.moveTo(0f, fireBaseY)
        var x = 0f
        val step = 10f
        while (x <= w) {
            val f1 = sin((x * 0.025f + flamePhase * 1.2f).toDouble()).toFloat()
            val f2 = cos((x * 0.055f - flamePhase * 1.6f).toDouble()).toFloat()
            val f3 = sin((x * 0.095f + flamePhase * 2.0f).toDouble()).toFloat()
            val peak = ((f1 * 0.5f + f2 * 0.35f + f3 * 0.15f) + 1f) * 0.5f
            val flameY = fireBaseY - peak * maxFlameH
            outerFlame.lineTo(x, flameY)
            x += step
        }
        outerFlame.lineTo(w, fireBaseY)
        outerFlame.close()
        drawPath(
            outerFlame,
            brush = Brush.verticalGradient(outerFlameColors, startY = fireBaseY - maxFlameH, endY = fireBaseY)
        )

        midFlame.reset()
        midFlame.moveTo(0f, fireBaseY)
        x = 0f
        while (x <= w) {
            val f1 = sin((x * 0.035f - flamePhase * 1.4f).toDouble()).toFloat()
            val f2 = cos((x * 0.075f + flamePhase * 1.8f).toDouble()).toFloat()
            val peak = ((f1 * 0.6f + f2 * 0.4f) + 1f) * 0.5f
            val flameY = fireBaseY - peak * (maxFlameH * 0.68f)
            midFlame.lineTo(x, flameY)
            x += step
        }
        midFlame.lineTo(w, fireBaseY)
        midFlame.close()
        drawPath(
            midFlame,
            brush = Brush.verticalGradient(midFlameColors, startY = fireBaseY - maxFlameH * 0.68f, endY = fireBaseY)
        )

        coreFlame.reset()
        coreFlame.moveTo(0f, fireBaseY)
        x = 0f
        while (x <= w) {
            val f = (sin((x * 0.045f + flamePhase * 1.9f).toDouble()).toFloat() + 1f) * 0.5f
            val flameY = fireBaseY - f * (maxFlameH * 0.38f)
            coreFlame.lineTo(x, flameY)
            x += 12f
        }
        coreFlame.lineTo(w, fireBaseY)
        coreFlame.close()
        drawPath(
            coreFlame,
            brush = Brush.verticalGradient(coreFlameColors, startY = fireBaseY - maxFlameH * 0.38f, endY = fireBaseY)
        )

        for (i in 0..8) {
            val sparkProg = (smokePhase + i * 0.11f) % 1f
            val sparkX = w * ((i * 0.11f + sparkProg * 0.03f) % 1f) + sin((i * 2.5f + sparkProg * 4f).toDouble()).toFloat() * 8f
            val sparkY = fireBaseY - sparkProg * (maxFlameH * 1.6f)
            val sparkAlpha = (0.7f * (1f - sparkProg)).coerceIn(0f, 0.7f)

            drawCircle(
                color = Color(0xFFFFD54F).copy(alpha = sparkAlpha),
                radius = 1.1f * (1f - sparkProg * 0.3f),
                center = Offset(sparkX, sparkY)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 6. МУРАВЬИ: БОЛЬШОЙ РАЗМЕР (~44 px), МНОГО МУРАВЬЕВ (11 штук в колонне)
// Оптимизировано: Reusable Path для листьев, zero-allocation render loop
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun BigAntArmyEffect(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "AntArmyAnim")
    val walkProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(13000, easing = LinearEasing)),
        label = "armyWalk"
    )
    val legCycle by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(180, easing = LinearEasing), RepeatMode.Reverse),
        label = "legCycle"
    )

    val leafPath = remember { Path() }
    val legMounts = remember { floatArrayOf(-2.0f, 6.5f, 13.5f) }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val antGroundY = h - 14f
        val antCount = 11

        val chitinDark = Color(0xFF160E0A)
        val chitinRedBrown = Color(0xFF421C11)
        val legColor = Color(0xFF2E170C)

        for (i in 0 until antCount) {
            val antX = ((i / antCount.toFloat() + walkProgress) % 1f) * (w + 100f) - 50f

            // ── 1. ОЧЕНЬ КРУПНОЕ БРЮШКО (Gaster) — длина 23px, высота 15px ──
            val gasterX = antX - 18f
            val gasterY = antGroundY - 5.5f
            drawOval(
                color = chitinDark,
                topLeft = Offset(gasterX - 11.5f, gasterY - 7.5f),
                size = Size(23f, 15f)
            )
            // Реалистичный глянцевый блик света на панцире
            drawOval(
                color = Color(0x45FFFFFF),
                topLeft = Offset(gasterX - 7.5f, gasterY - 6.0f),
                size = Size(13f, 5.5f)
            )

            // ── 2. СТЕБЕЛЁК (Petiolus — талия из 2 крупных узелков) ──
            drawCircle(color = chitinRedBrown, radius = 3.4f, center = Offset(antX - 3.8f, antGroundY - 4.2f))
            drawCircle(color = chitinRedBrown, radius = 2.8f, center = Offset(antX - 0.4f, antGroundY - 4.2f))

            // ── 3. ГРУДЬ (Mesosoma) — продолговатая рельефная часть ──
            val thoraxX = antX + 7.5f
            val thoraxY = antGroundY - 4.4f
            drawOval(
                color = chitinRedBrown,
                topLeft = Offset(thoraxX - 8.5f, thoraxY - 5.8f),
                size = Size(17f, 11.6f)
            )
            drawOval(
                color = Color(0x30FFFFFF),
                topLeft = Offset(thoraxX - 5.5f, thoraxY - 4.8f),
                size = Size(9.5f, 4.0f)
            )

            // ── 4. ГОЛОВА (Caput) с мощными челюстями ──
            val headX = antX + 19.5f
            val headY = antGroundY - 4.6f
            drawOval(
                color = chitinDark,
                topLeft = Offset(headX - 6.5f, headY - 6.8f),
                size = Size(13.5f, 12.8f)
            )
            // Жвалы (челюсти спереди)
            drawLine(
                color = Color(0xFF5D2E1C),
                start = Offset(headX + 6.5f, headY - 1.2f),
                end = Offset(headX + 12.0f, headY + 2.2f),
                strokeWidth = 2.4f
            )

            // ── 5. ДЛИННЫЕ КОЛЕНЧАТЫЕ УСИКИ ──
            val antennaWiggle = sin((walkProgress * 22f + i * 2.5f).toDouble()).toFloat() * 2.8f
            val antKneeX = headX + 6.0f
            val antKneeY = headY - 6.5f
            val antTipX = headX + 16.0f
            val antTipY = headY - 15.0f + antennaWiggle
            // Первый усик
            drawLine(color = legColor, start = Offset(headX + 2f, headY - 4f), end = Offset(antKneeX, antKneeY), strokeWidth = 1.8f)
            drawLine(color = legColor, start = Offset(antKneeX, antKneeY), end = Offset(antTipX, antTipY), strokeWidth = 1.4f)
            // Второй усик (в перспективе)
            drawLine(color = legColor.copy(alpha = 0.8f), start = Offset(headX + 2f, headY - 2.5f), end = Offset(antKneeX - 1.5f, antKneeY - 2.5f), strokeWidth = 1.6f)
            drawLine(color = legColor.copy(alpha = 0.8f), start = Offset(antKneeX - 1.5f, antKneeY - 2.5f), end = Offset(antTipX - 3.0f, antTipY - 3.5f), strokeWidth = 1.2f)

            // ── 6. ВЫСОКИЕ СУСТАВЧАТЫЕ ЛАПКИ (Триподная походка) ──
            for (legIdx in legMounts.indices) {
                val mountX = legMounts[legIdx]
                val phaseSign = if (legIdx % 2 == 0) 1f else -1f
                val swing = legCycle * phaseSign * 6.8f

                // Верхняя лапка (дуга вверх над телом)
                val knee1X = antX + mountX + swing * 0.5f
                val knee1Y = antGroundY - 13.5f
                val foot1X = antX + mountX + swing - 4.5f
                val foot1Y = antGroundY - 7.5f
                drawLine(color = legColor, start = Offset(antX + mountX, thoraxY - 2f), end = Offset(knee1X, knee1Y), strokeWidth = 2.0f)
                drawLine(color = legColor, start = Offset(knee1X, knee1Y), end = Offset(foot1X, foot1Y), strokeWidth = 1.6f)

                // Нижняя лапка (опирается о землю)
                val knee2X = antX + mountX - swing * 0.4f
                val knee2Y = antGroundY + 2.5f
                val foot2X = antX + mountX - swing
                val foot2Y = antGroundY + 9.5f
                drawLine(color = legColor, start = Offset(antX + mountX, thoraxY + 2f), end = Offset(knee2X, knee2Y), strokeWidth = 2.0f)
                drawLine(color = legColor, start = Offset(knee2X, knee2Y), end = Offset(foot2X, foot2Y), strokeWidth = 1.6f)
            }

            // ── 7. Ноша муравья (Большой сочный лист над спиной у каждого 2-го муравья) ──
            if (i % 2 == 0) {
                leafPath.reset()
                leafPath.moveTo(headX + 6f, headY - 1f)
                leafPath.quadraticBezierTo(headX + 2f, headY - 25f, headX - 14f, headY - 22f)
                leafPath.quadraticBezierTo(headX - 2f, headY - 10f, headX + 6f, headY - 1f)
                leafPath.close()
                drawPath(leafPath, color = Color(0xEE388E3C))
                // Рельефная центральная прожилка листа
                drawLine(
                    color = Color(0x90C8E6C9),
                    start = Offset(headX + 6f, headY - 1f),
                    end = Offset(headX - 12f, headY - 22f),
                    strokeWidth = 1.5f
                )
            }
        }
    }
}
