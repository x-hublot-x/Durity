package com.example.project1.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project1.data.storage.GeminiApiKeyManager
import com.example.project1.ui.theme.AppTheme
import com.example.project1.util.VibrationUtil
import kotlinx.coroutines.launch

@Composable
fun GeminiApiKeyScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var savedCustomKey by remember { mutableStateOf(GeminiApiKeyManager.getCustomKey(context) ?: "") }
    var inputKey by remember { mutableStateOf(savedCustomKey) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTestSuccess by remember { mutableStateOf(false) }

    val isCustomActive = savedCustomKey.isNotBlank()

    // Маскированное значение активного ключа
    val activeMaskedKey = remember(savedCustomKey) {
        GeminiApiKeyManager.getMaskedKey(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 120.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── Верхний бар с кнопкой "Назад" ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    VibrationUtil.vibrateTick(context)
                    onBack()
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Ключ Gemini API",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Персональный доступ к нейросети",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Статус активного ключа ────────────────────────────────────────────
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.92f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.surfaceBorder, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (isCustomActive) Color(0xFF66BB6A) else colors.primary)
                        )
                        Text(
                            text = if (isCustomActive) "Личный ключ активен" else "Встроенный ключ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isCustomActive) Color(0xFF66BB6A).copy(alpha = 0.15f)
                                else colors.primary.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isCustomActive) "Свой ключ" else "По умолчанию",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isCustomActive) Color(0xFF81C784) else colors.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceElevated.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Активный: $activeMaskedKey",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                    if (isCustomActive) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Поле ввода ключа ──────────────────────────────────────────────────
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.92f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.surfaceBorder, RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Установить личный ключ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Введите ваш API ключ от Google Gemini. Он будет надежно сохранен локально на устройстве.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = {
                        inputKey = it
                        testResult = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "AIzaSy...",
                            color = colors.textSecondary.copy(alpha = 0.5f)
                        )
                    },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (inputKey.isNotEmpty()) {
                                IconButton(onClick = { inputKey = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Очистить", tint = colors.textSecondary)
                                }
                            }
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Показать/Скрыть",
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.surfaceBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.primary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Кнопки: Вставить и Проверить
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            VibrationUtil.vibrateTick(context)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = clipboard?.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotBlank()) {
                                    inputKey = text.trim()
                                    Toast.makeText(context, "Вставлено из буфера", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Вставить", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val keyToTest = inputKey.trim().ifEmpty { GeminiApiKeyManager.getApiKey(context) }
                            if (keyToTest.isEmpty()) {
                                Toast.makeText(context, "Ключ пуст", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isTesting = true
                            testResult = null
                            focusManager.clearFocus()
                            scope.launch {
                                val res = GeminiApiKeyManager.testApiKey(keyToTest)
                                isTesting = false
                                if (res.isSuccess) {
                                    isTestSuccess = true
                                    testResult = "Ключ успешно проверен! Ответ получен."
                                    VibrationUtil.vibrateSuccess(context)
                                } else {
                                    isTestSuccess = false
                                    testResult = "Ошибка: ${res.exceptionOrNull()?.localizedMessage}"
                                    VibrationUtil.vibrateError(context)
                                }
                            }
                        },
                        enabled = !isTesting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colors.primary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.CheckCircleOutline, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Проверить", fontSize = 13.sp, color = colors.textPrimary)
                        }
                    }
                }

                // Результат теста
                if (testResult != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTestSuccess) Color(0xFF66BB6A).copy(alpha = 0.15f) else Color(0xFFFF5252).copy(alpha = 0.15f))
                            .border(1.dp, if (isTestSuccess) Color(0xFF66BB6A).copy(alpha = 0.3f) else Color(0xFFFF5252).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = testResult ?: "",
                            fontSize = 12.sp,
                            color = if (isTestSuccess) Color(0xFF81C784) else Color(0xFFFF8A80),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопка сохранения и сброса
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isCustomActive) {
                        OutlinedButton(
                            onClick = {
                                VibrationUtil.vibrateTick(context)
                                GeminiApiKeyManager.resetToDefault(context)
                                savedCustomKey = ""
                                inputKey = ""
                                testResult = null
                                Toast.makeText(context, "Сброшено к ключу по умолчанию", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text("Сбросить", fontSize = 13.sp, color = Color(0xFFFF8A80))
                        }
                    }

                    Button(
                        onClick = {
                            val clean = inputKey.trim()
                            if (clean.isEmpty()) {
                                Toast.makeText(context, "Введите ключ перед сохранением", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            VibrationUtil.vibrateSuccess(context)
                            GeminiApiKeyManager.saveCustomKey(context, clean)
                            savedCustomKey = clean
                            focusManager.clearFocus()
                            Toast.makeText(context, "Ключ сохранен", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        modifier = Modifier.weight(if (isCustomActive) 1.2f else 1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            text = "Сохранить",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Пошаговая инструкция получения ключа ──────────────────────────────
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.92f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.surfaceBorder, RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF29B6F6).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null, tint = Color(0xFF29B6F6), modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "Как получить свой ключ бесплатно?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                InstructionStep(
                    number = "1",
                    title = "Откройте Google AI Studio",
                    description = "Перейдите на официальный сайт Google AI Studio (aistudio.google.com) и войдите в свой Google аккаунт."
                )

                InstructionStep(
                    number = "2",
                    title = "Нажмите «Get API key»",
                    description = "В левом меню или на главной странице найдите синюю кнопку «Get API key» или «Create API key»."
                )

                InstructionStep(
                    number = "3",
                    title = "Создайте ключ в проекте",
                    description = "Нажмите «Create API key in new project». Google бесплатно выделит персональный ключ с щедрыми лимитами."
                )

                InstructionStep(
                    number = "4",
                    title = "Скопируйте и вставьте сюда",
                    description = "Скопируйте полученную строку (начинается на AIzaSy...), вернитесь в приложение, нажмите «Вставить» и затем «Сохранить»."
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        VibrationUtil.vibrateTick(context)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Не удалось открыть браузер", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B3E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF29B6F6).copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = Color(0xFF29B6F6),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Открыть Google AI Studio",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF29B6F6)
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionStep(
    number: String,
    title: String,
    description: String
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = colors.textSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
