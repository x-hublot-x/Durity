package com.example.project1.ui.screens.captchas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.project1.api.AiEssayValidator
import com.example.project1.data.model.AppInfo
import com.example.project1.data.storage.AiTestManager
import com.example.project1.data.storage.UnblockEssayStorage
import com.example.project1.ui.components.GradientIcon
import com.example.project1.ui.components.GradientText
import com.example.project1.ui.components.PrimaryGradientButton
import com.example.project1.ui.theme.AppTheme
import com.example.project1.util.VibrationUtil
import kotlinx.coroutines.launch

@Composable
fun AiEssayCaptchaDialog(
    app: AppInfo,
    onDismiss: () -> Unit,
    onSolved: () -> Unit,
    onLockout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = AppTheme.colors

    var essayText by remember { mutableStateOf("") }
    var attemptsLeft by remember { mutableIntStateOf(5) }
    var isLoading by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isLockoutTriggered by remember { mutableStateOf(false) }

    val wordCount = remember(essayText) {
        AiEssayValidator.countWords(essayText)
    }

    val isWordCountMet = wordCount >= 100

    Dialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            color = colors.surface,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Шапка
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        GradientIcon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Проверка намерений (ИИ)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Разблокировка ${app.name}",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    // Бейдж оставшихся попыток
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (attemptsLeft > 2) colors.primary.copy(alpha = 0.15f)
                                else Color(0xFFFF5252).copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$attemptsLeft/5 поп.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (attemptsLeft > 2) colors.primary else Color(0xFFFF5252)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Карточка с темой сочинения
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.background,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Тема сочинения:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "«Что полезного я извлек из данного приложения и зачем мне дальше в нем сидеть?»",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Минимум 100 слов\n• ИИ проверяет искренность с учетом теста личности\n• Скопированный текст и шаблоны отклоняются",
                            fontSize = 11.sp,
                            color = colors.textTertiary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Поле ввода текста
                OutlinedTextField(
                    value = essayText,
                    onValueChange = {
                        if (!isLoading) {
                            essayText = it
                            feedbackMessage = null
                        }
                    },
                    enabled = !isLoading && !isLockoutTriggered,
                    placeholder = {
                        Text(
                            "Опишите ваши реальные мысли и пользу от использования ${app.name}...",
                            color = colors.textTertiary,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.surfaceBorder,
                        focusedContainerColor = colors.background,
                        unfocusedContainerColor = colors.background
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Счетчик слов
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isWordCountMet) "✓ Длина достаточна" else "Нужно ещё ${100 - wordCount} сл.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isWordCountMet) Color(0xFF00E676) else colors.textSecondary
                    )

                    Text(
                        text = "Слов: $wordCount / 100",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWordCountMet) Color(0xFF00E676) else colors.primary
                    )
                }

                // Сообщение с отказом или ошибкой от ИИ
                if (feedbackMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFF5252).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = feedbackMessage!!,
                                fontSize = 12.sp,
                                color = Color(0xFFFF8A80),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colors.primary
                        )
                        Text(
                            text = "ИИ анализирует ваше сочинение...",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f, fill = false))
                Spacer(modifier = Modifier.height(18.dp))

                // Кнопки действий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена", color = colors.textSecondary)
                    }

                    PrimaryGradientButton(
                        onClick = {
                            if (isLoading) return@PrimaryGradientButton

                            VibrationUtil.vibrateClick(context)
                            isLoading = true
                            feedbackMessage = null

                            coroutineScope.launch {
                                val result = AiEssayValidator.validateEssay(
                                    context = context,
                                    appName = app.name,
                                    essayText = essayText
                                )
                                isLoading = false

                                if (result.isValid) {
                                    VibrationUtil.vibrateSuccess(context)
                                    onSolved()
                                } else {
                                    VibrationUtil.vibrateError(context)
                                    attemptsLeft--
                                    feedbackMessage = result.feedback

                                    if (attemptsLeft <= 0) {
                                        isLockoutTriggered = true
                                        UnblockEssayStorage.setLockout(context, app.packageName, 3600_000L)
                                        onLockout()
                                    }
                                }
                            }
                        },
                        enabled = !isLoading && !isLockoutTriggered && isWordCountMet,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text(
                            text = "Проверить ИИ",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
