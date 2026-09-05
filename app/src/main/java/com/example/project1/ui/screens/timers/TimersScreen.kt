package com.example.project1.ui.screens.timers

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.example.project1.data.model.AppInfo
import com.example.project1.data.model.UNLOCK_BONUS_MINUTES
import com.example.project1.data.repository.ChessPuzzleRepository
import com.example.project1.data.repository.TrigTaskRepository
import com.example.project1.ui.components.AutoAddIcon
import com.example.project1.ui.components.SamsungAiStarsIcon
import com.example.project1.ui.components.WheelPicker
import com.example.project1.ui.screens.captchas.*
import com.example.project1.ui.screens.personality.AiPersonalityTestDialog
import com.example.project1.util.formatMinutes

@Composable
fun PermissionRequestScreen(
    hasUsage: Boolean,
    hasOverlay: Boolean,
    hasAccessibility: Boolean,
    onRequestUsage: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Требуются разрешения",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Для отслеживания времени и корректной работы блокировок необходимо предоставить разрешения:",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!hasUsage) {
            Button(
                onClick = onRequestUsage,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("1. Доступ к статистике использования")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (!hasOverlay) {
            Button(
                onClick = onRequestOverlay,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("2. Оверлей поверх окон")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (!hasAccessibility) {
            Button(
                onClick = onRequestAccessibility,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("3. Специальные возможности (для блокировки)")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Проверить снова", color = Color.White)
        }
    }
}

@Composable
fun AutoAddDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1F1F2C),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Автоматическая настройка",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Автоматическое добавление всех приложений с рекомендуемым временем, которые вероятно тратят ваше время.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Добавить автоматически",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Отмена", color = Color.White.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun TimersScreen(
    appsWithTimers: List<AppInfo>,
    installedApps: List<AppInfo>,
    onAddTimerClick: () -> Unit,
    onEditTimer: (AppInfo) -> Unit,
    onDeleteTimer: (AppInfo) -> Unit,
    onUnlockTimer: (AppInfo) -> Unit,
    onAutoAddTimers: (Map<String, Int>) -> Unit
) {
    var isEditMode by remember { mutableStateOf(false) }
    var showAutoAddDialog by remember { mutableStateOf(false) }
    var showAiTestDialog by remember { mutableStateOf(false) }

    LaunchedEffect(appsWithTimers.size) {
        if (appsWithTimers.isEmpty()) {
            isEditMode = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { isEditMode = false })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Активные таймеры",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            if (appsWithTimers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет установленных таймеров",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 180.dp)
                ) {
                    items(appsWithTimers, key = { it.packageName }) { app ->
                        ActiveTimerCard(
                            app = app,
                            isEditMode = isEditMode,
                            onLongPress = { isEditMode = !isEditMode },
                            onEdit = { onEditTimer(app) },
                            onDelete = { onDeleteTimer(app) },
                            onUnlock = { onUnlockTimer(app) }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = onAddTimerClick,
                containerColor = Color(0xFFFF5252),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить таймер")
            }

            FloatingActionButton(
                onClick = { showAutoAddDialog = true },
                containerColor = Color(0xFF252533),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                AutoAddIcon()
            }

            FloatingActionButton(
                onClick = { showAiTestDialog = true },
                containerColor = Color(0xFF252533),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                SamsungAiStarsIcon(tint = Color(0xFFFFFFFF))
            }
        }

        if (showAutoAddDialog) {
            AutoAddDialog(
                onDismiss = { showAutoAddDialog = false },
                onConfirm = {
                    showAutoAddDialog = false

                    val presetApps = mapOf(
                        "com.google.android.youtube.shorts" to 15,
                        "com.reddit.frontpage" to 15,
                        "com.instagram.android" to 15,
                        "com.vkontakte.android" to 120,
                        "com.zhiliaoapp.musically" to 15,
                        "com.pinterest" to 15
                    )

                    val installedPackageNames = installedApps.map { it.packageName }.toMutableSet()

                    if ("com.google.android.youtube" in installedPackageNames) {
                        installedPackageNames.add("com.google.android.youtube.shorts")
                    }

                    val filteredPreset = presetApps.filterKeys { it in installedPackageNames }

                    onAutoAddTimers(filteredPreset)
                }
            )
        }

        if (showAiTestDialog) {
            val top5 = installedApps.sortedByDescending { it.usedMinutesThisWeek }.take(5)
            AiPersonalityTestDialog(
                top5Apps = top5,
                onDismiss = { showAiTestDialog = false },
                onApplyRecommendations = { recs ->
                    onAutoAddTimers(recs)
                },
                onResetTest = {}
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActiveTimerCard(
    app: AppInfo,
    isEditMode: Boolean,
    onLongPress: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUnlock: () -> Unit
) {
    val isFrozen = app.usedMinutesThisWeek >= app.timeLimitMinutes

    var showChessCaptcha by remember { mutableStateOf(false) }
    var showFunnyCaptcha by remember { mutableStateOf(false) }
    var showTrigTask by remember { mutableStateOf(false) }
    var showNumberCaptcha by remember { mutableStateOf(false) }
    var showGraphCaptcha by remember { mutableStateOf(false) }

    var currentPuzzle by remember { mutableStateOf(ChessPuzzleRepository.getRandomPuzzle()) }
    var currentTrigTask by remember { mutableStateOf(TrigTaskRepository.getRandomTask()) }

    val infiniteTransition = rememberInfiniteTransition(label = "wiggleTransition")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 140,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotationAngle"
    )

    val cardScale by animateFloatAsState(
        targetValue = if (isEditMode) 0.98f else 1.0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, end = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    rotationZ = if (isEditMode) rotationAngle else 0f
                }
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A24))
                .border(
                    1.dp,
                    if (isFrozen) Color(0xFF80D8FF) else Color(0xFFFF5252).copy(alpha = 0.4f),
                    RoundedCornerShape(16.dp)
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val bitmap = remember(app.icon) {
                    app.icon.toBitmap(128, 128).asImageBitmap()
                }

                Image(
                    bitmap = bitmap,
                    contentDescription = app.name,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = app.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "Лимит на день: ${formatMinutes(app.timeLimitMinutes)}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )

                    if (isFrozen) {
                        Text(
                            text = "❄ Заморожено (${formatMinutes(app.usedMinutesThisWeek)} за день)",
                            color = Color(0xFF80D8FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "За день: ${formatMinutes(app.usedMinutesThisWeek)} / ${formatMinutes(app.timeLimitMinutes)}",
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isEditMode,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = (-10).dp),
            enter = fadeIn(animationSpec = tween(200)) +
                    scaleIn(
                        initialScale = 0.4f,
                        transformOrigin = TransformOrigin(0.8f, 0.8f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
            exit = fadeOut(animationSpec = tween(150)) +
                    scaleOut(
                        targetScale = 0.4f,
                        animationSpec = tween(150)
                    )
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF252533))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (isFrozen) {
                            showChessCaptcha = true
                        } else {
                            onDelete()
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isFrozen) Icons.Default.Lock else Icons.Default.Delete,
                        contentDescription = if (isFrozen) "Разблокировать (+$UNLOCK_BONUS_MINUTES минут)" else "Удалить",
                        tint = if (isFrozen) Color(0xFF80D8FF) else Color(0xFFFF5252),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showChessCaptcha) {
        ChessCaptchaDialog(
            puzzle = currentPuzzle,
            onDismiss = { showChessCaptcha = false },
            onSolved = {
                showChessCaptcha = false
                currentPuzzle = ChessPuzzleRepository.getRandomPuzzle()
                showFunnyCaptcha = true
            }
        )
    }

    if (showFunnyCaptcha) {
        FunnyCaptchaDialog(
            onDismiss = { showFunnyCaptcha = false },
            onSolved = {
                showFunnyCaptcha = false
                currentTrigTask = TrigTaskRepository.getRandomTask()
                showTrigTask = true
            }
        )
    }

    if (showTrigTask) {
        TrigTaskDialog(
            task = currentTrigTask,
            onDismiss = { showTrigTask = false },
            onSolved = {
                showTrigTask = false
                showNumberCaptcha = true
            }
        )
    }

    if (showNumberCaptcha) {
        NumberGuessCaptchaDialog(
            onDismiss = { showNumberCaptcha = false },
            onSolved = {
                showNumberCaptcha = false
                showGraphCaptcha = true
            }
        )
    }

    if (showGraphCaptcha) {
        GraphTraceCaptchaDialog(
            onDismiss = { showGraphCaptcha = false },
            onSolved = {
                showGraphCaptcha = false
                onUnlock()
            }
        )
    }
}

@Composable
fun AppItemCard(app: AppInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A24))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bitmap = remember(app.icon) { app.icon.toBitmap(128, 128).asImageBitmap() }
        Image(
            bitmap = bitmap,
            contentDescription = app.name,
            modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Использовано сегодня: ${formatMinutes(app.usedMinutesThisWeek)}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun AppSelectionDialog(
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onAppSelect: (AppInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(searchQuery, apps) {
        if (searchQuery.isBlank()) apps
        else apps.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1F1F2C),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Выберите приложение",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Поиск приложения...",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Text("🔍", fontSize = 14.sp)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("✕", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF5252),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color(0xFF161622),
                        unfocusedContainerColor = Color(0xFF161622)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                if (apps.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Все приложения уже добавлены",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 15.sp
                        )
                    }
                } else if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Приложения не найдены",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            AppItemCard(app = app, onClick = { onAppSelect(app) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Отмена", color = Color.White.copy(0.6f))
                }
            }
        }
    }
}

@Composable
fun TimerConfigDialog(
    app: AppInfo,
    maxMinutesAllowed: Int,
    isInfiniteWheel: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val maxHours = maxMinutesAllowed / 60
    val maxMinsInMaxHour = maxMinutesAllowed % 60

    val hoursList = remember(maxMinutesAllowed) { (0..maxHours).map { it.toString().padStart(2, '0') } }

    var selectedHourIndex by remember { mutableIntStateOf(0) }
    var selectedMinuteIndex by remember { mutableIntStateOf(0) }

    val currentSelectedHour = hoursList.getOrNull(selectedHourIndex)?.toIntOrNull() ?: 0

    val minutesList = remember(currentSelectedHour, maxMinutesAllowed) {
        if (currentSelectedHour == maxHours) {
            (0..maxMinsInMaxHour).map { it.toString().padStart(2, '0') }
        } else {
            (0..59).map { it.toString().padStart(2, '0') }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1F1F2C),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Лимит времени (День)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = app.name,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Часы", fontSize = 12.sp, color = Color.White.copy(0.5f))
                        Spacer(modifier = Modifier.height(6.dp))
                        WheelPicker(
                            items = hoursList,
                            initialIndex = 0,
                            isInfinite = isInfiniteWheel,
                            onItemSelected = { selectedHourIndex = it }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(top = 18.dp)
                            .height(132.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ":",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Мин", fontSize = 12.sp, color = Color.White.copy(0.5f))
                        Spacer(modifier = Modifier.height(6.dp))
                        WheelPicker(
                            items = minutesList,
                            initialIndex = 0,
                            isInfinite = isInfiniteWheel,
                            onItemSelected = { selectedMinuteIndex = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена", color = Color.White.copy(0.6f))
                    }

                    Button(
                        onClick = {
                            val selectedHour = hoursList.getOrNull(selectedHourIndex)?.toIntOrNull() ?: 0
                            val selectedMinute = minutesList.getOrNull(selectedMinuteIndex)?.toIntOrNull() ?: 0
                            val totalMinutes = selectedHour * 60 + selectedMinute
                            onConfirm(totalMinutes)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сохранить", color = Color.White)
                    }
                }
            }
        }
    }
}
