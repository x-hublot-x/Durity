package com.example.project1.ui.screens.captchas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.project1.data.repository.NumberSettingsManager
import com.example.project1.ui.theme.AppTheme
import kotlinx.coroutines.delay

@Composable
fun NumberGuessCaptchaDialog(
    onDismiss: () -> Unit,
    onSolved: () -> Unit
) {
    val targetItem by remember { mutableStateOf(NumberSettingsManager.getRandomTarget()) }
    var userInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isFailed by remember { mutableStateOf(false) }

    // Автоматическое закрытие диалога через 2 секунды после ошибочного ответа
    LaunchedEffect(isFailed) {
        if (isFailed) {
            delay(2000L)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = { if (!isFailed) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1F1F2C),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Угадай число",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Загадано число в диапазоне [0, 1]",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0xFF2A2A3C),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Подсказка: Целое число",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF80D8FF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = userInput,
                    enabled = !isFailed,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            userInput = input
                        }
                    },
                    label = { Text("Введите число", color = Color.White.copy(alpha = 0.5f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AppTheme.accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedContainerColor = Color(0xFF14141E),
                        unfocusedContainerColor = Color(0xFF14141E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF5252),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isFailed,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена", color = Color.White.copy(alpha = if (isFailed) 0.2f else 0.6f))
                    }

                    Button(
                        onClick = {
                            val enteredValue = userInput.toIntOrNull()

                            if (enteredValue == null) {
                                errorMessage = "Введите корректное целое число!"
                                return@Button
                            }

                            if (enteredValue == targetItem.value) {
                                onSolved()
                            } else {
                                errorMessage = "Неправильно, это было ${targetItem.value}"
                                isFailed = true
                            }
                        },
                        enabled = !isFailed,
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.accent),
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
