package com.example.project1.ui.screens.captchas

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.project1.data.model.TrigTask
import com.example.project1.ui.components.MathView
import kotlin.math.*

@SuppressLint("UnrememberedMutableState")
@Composable
fun TrigTaskDialog(
    task: TrigTask,
    onDismiss: () -> Unit,
    onSolved: () -> Unit
) {
    var selectedAngle by remember { mutableStateOf<Double?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    // Погрешность попадания в радианах (около 12 градусов)
    val tolerance = Math.toRadians(12.0)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1F1F2C),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Тригонометрическая капча",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Отметьте точку на окружности:",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Рендеринг LaTeX-формулы угла
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Угол = ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    MathView(
                        latex = task.targetLabel,
                        textSizeSp = 22,
                        textColor = Color(0xFFFF5252),
                        height = 48.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .background(Color(0xFF14141E), shape = RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = (offset.x - center.x).toDouble()
                                    val dy = (center.y - offset.y).toDouble() // Инвертируем Y для стандартной СК

                                    var angle = atan2(dy, dx)
                                    if (angle < 0) {
                                        angle += 2.0 * Math.PI
                                    }
                                    selectedAngle = angle
                                    errorMessage = ""
                                }
                            }
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f - 32.dp.toPx()

                        // Оси X и Y
                        drawLine(
                            color = Color.White.copy(alpha = 0.2f),
                            start = Offset(16.dp.toPx(), center.y),
                            end = Offset(size.width - 16.dp.toPx(), center.y),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.2f),
                            start = Offset(center.x, 16.dp.toPx()),
                            end = Offset(center.x, size.height - 16.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx()
                        )

                        // Единичная окружность
                        drawCircle(
                            color = Color.White.copy(alpha = 0.6f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Рисуем точку выбора
                        selectedAngle?.let { angle ->
                            val ptX = center.x + (radius * cos(angle)).toFloat()
                            val ptY = center.y - (radius * sin(angle)).toFloat()

                            // Линия от центра к точке
                            drawLine(
                                color = Color(0xFFFF5252).copy(alpha = 0.5f),
                                start = center,
                                end = Offset(ptX, ptY),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )

                            // Точка выбора
                            drawCircle(
                                color = Color(0xFFFF5252),
                                radius = 7.dp.toPx(),
                                center = Offset(ptX, ptY)
                            )
                        }
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF5252),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена", color = Color.White.copy(alpha = 0.6f))
                    }

                    Button(
                        onClick = {
                            val sel = selectedAngle
                            if (sel == null) {
                                errorMessage = "Поставьте точку на окружности!"
                                return@Button
                            }

                            // Проверяем с учетом периодичности (близость к 0 / 2π)
                            val diff = abs(sel - task.targetAngleRad)
                            val normalizedDiff = min(diff, 2.0 * Math.PI - diff)

                            if (normalizedDiff <= tolerance) {
                                onSolved()
                            } else {
                                errorMessage = "Неверный угол! Попробуйте еще раз."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Проверить", color = Color.White)
                    }
                }
            }
        }
    }
}
