package com.example.project1.ui.screens.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.project1.BuildConfig
import com.example.project1.data.model.ChatMessage
import com.example.project1.data.model.ChatSession
import com.example.project1.data.storage.AiTestManager
import com.example.project1.data.storage.DailyTaskStorage
import com.example.project1.data.storage.MultiChatStorage
import com.example.project1.ui.components.ChatIcon
import com.example.project1.ui.components.MixedMathText
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─── Personality system prompt builder ───────────────────────────────────────

fun buildPersonalitySystemPrompt(): String {
    val personality = AiTestManager.savedPersonality.trim()
    val recs = AiTestManager.savedRecommendations
    if (personality.isEmpty()) return ""

    val recsText = if (recs.isNotEmpty()) {
        recs.entries.joinToString("\n") { (app, minutes) -> "  • $app — лимит $minutes мин/день" }
    } else ""

    return buildString {
        appendLine("Ты персональный ИИ-ассистент. Ниже — профиль пользователя, который прошёл тест цифрового благополучия. Используй эту информацию чтобы давать персонализированные, точные и чуткие ответы. Не пересказывай профиль пользователю — просто учитывай его в каждом ответе.")
        appendLine()
        appendLine("=== ПРОФИЛЬ ПОЛЬЗОВАТЕЛЯ ===")
        appendLine(personality)
        if (recsText.isNotEmpty()) {
            appendLine()
            appendLine("=== УСТАНОВЛЕННЫЕ ЛИМИТЫ ЭКРАННОГО ВРЕМЕНИ ===")
            appendLine(recsText)
        }
        appendLine()
        appendLine("Отвечай на том языке, на котором пишет пользователь. Будь лаконичен, полезен и учитывай контекст профиля там, где это уместно.")
    }
}

// ─── Chat list screen ─────────────────────────────────────────────────────────

@Composable
fun ChatScreen(
    onChatOpenChanged: (Boolean) -> Unit = {},
    pendingSession: ChatSession? = null,
    onPendingSessionConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val isTestPassed = AiTestManager.isTestCompleted

    var sessions by remember { mutableStateOf(MultiChatStorage.loadSessions(context)) }
    // id сессии которая сейчас открыта; null = список чатов
    var openSessionId by remember { mutableStateOf<String?>(null) }
    // id "черновика" — сессии которую показываем но не сохраняем пока нет сообщений
    var pendingSessionId by remember { mutableStateOf<String?>(null) }

    // Автоматически открываем сессию переданную снаружи (из HomeScreen / DailyTaskScreen)
    LaunchedEffect(pendingSession) {
        if (pendingSession != null) {
            if (sessions.none { it.id == pendingSession.id }) {
                sessions = sessions + pendingSession
            }
            openSessionId = pendingSession.id
            onPendingSessionConsumed()
        }
    }

    // Notify parent when chat open state changes
    LaunchedEffect(openSessionId) {
        onChatOpenChanged(openSessionId != null)
    }

    // Persist only non-empty sessions
    LaunchedEffect(sessions) {
        val toSave = sessions.filter { it.messages.isNotEmpty() }
        MultiChatStorage.saveSessions(context, toSave)
    }

    val openSession = sessions.firstOrNull { it.id == openSessionId }

    if (openSession != null) {
        ChatConversationScreen(
            session = openSession,
            isTestPassed = isTestPassed,
            onBack = {
                // Если уходим из пустого нового чата — удаляем его
                if (pendingSessionId == openSessionId &&
                    (openSession.messages.isEmpty() || openSession.messages.all { !it.isFromUser })
                ) {
                    sessions = sessions.filter { it.id != openSessionId }
                }
                pendingSessionId = null
                openSessionId = null
            },
            onSessionUpdated = { updated ->
                sessions = sessions.map { if (it.id == updated.id) updated else it }
                // Как только появилось первое сообщение — чат больше не черновик
                if (updated.messages.any { it.isFromUser }) pendingSessionId = null
            }
        )
    } else {
        ChatListScreen(
            sessions = sessions.filter { it.messages.isNotEmpty() },
            isTestPassed = isTestPassed,
            onOpenSession = { id -> openSessionId = id },
            onCreateSession = {
                val newSession = ChatSession(
                    id = System.currentTimeMillis().toString(),
                    title = "Новый чат",
                    messages = emptyList()
                )
                sessions = sessions + newSession
                pendingSessionId = newSession.id
                openSessionId = newSession.id
            },
            onRenameSession = { id, newTitle ->
                sessions = sessions.map { if (it.id == id) it.copy(title = newTitle) else it }
            },
            onDeleteSession = { id ->
                sessions = sessions.filter { it.id != id }
            }
        )
    }
}

@Composable
fun ChatListScreen(
    sessions: List<ChatSession>,
    isTestPassed: Boolean,
    onOpenSession: (String) -> Unit,
    onCreateSession: () -> Unit,
    onRenameSession: (String, String) -> Unit,
    onDeleteSession: (String) -> Unit
) {
    var renameTargetId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var editMode by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf<String?>(null) }

    // Анимация дрожания
    val shakeAnim = rememberInfiniteTransition(label = "shake")
    val shakeAngle by shakeAnim.animateFloat(
        initialValue = -1.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shakeAngle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .pointerInput(editMode) {
                detectTapGestures { if (editMode) editMode = false }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Чат бот",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!isTestPassed) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Чат заблокирован",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Пройдите тест персонализации\nво вкладке «Активные таймеры»",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Нет чатов",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(sessions.sortedByDescending { it.createdAt }, key = { it.id }) { session ->
                        ChatSessionCard(
                            session = session,
                            editMode = editMode,
                            shakeAngle = shakeAngle,
                            onOpen = { if (editMode) editMode = false else onOpenSession(session.id) },
                            onLongPress = { editMode = true },
                            onRename = {
                                renameTargetId = session.id
                                renameText = session.title
                                editMode = false
                            },
                            onDelete = {
                                deleteTargetId = session.id
                                editMode = false
                            }
                        )
                    }
                }
            }
        }

        if (isTestPassed) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 100.dp, end = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = onCreateSession,
                    containerColor = Color(0xFFFF5252),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Новый чат",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    // Rename dialog
    if (renameTargetId != null) {
        Dialog(onDismissRequest = { renameTargetId = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1F1F2C),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Переименовать чат",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF5252),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedContainerColor = Color(0xFF252533),
                            unfocusedContainerColor = Color(0xFF252533)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(onClick = { renameTargetId = null }, modifier = Modifier.weight(1f)) {
                            Text("Отмена", color = Color.White.copy(alpha = 0.6f))
                        }
                        Button(
                            onClick = {
                                val id = renameTargetId
                                if (id != null && renameText.isNotBlank()) onRenameSession(id, renameText.trim())
                                renameTargetId = null
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

    // Dialog подтверждения удаления
    if (deleteTargetId != null) {
        Dialog(onDismissRequest = { deleteTargetId = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1F1F2C),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Удалить чат?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Вся история переписки будет удалена безвозвратно.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(onClick = { deleteTargetId = null }, modifier = Modifier.weight(1f)) {
                            Text("Отмена", color = Color.White.copy(alpha = 0.6f))
                        }
                        Button(
                            onClick = {
                                val id = deleteTargetId
                                if (id != null) onDeleteSession(id)
                                deleteTargetId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Удалить", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatSessionCard(
    session: ChatSession,
    editMode: Boolean,
    shakeAngle: Float,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val lastMessage = session.messages.lastOrNull { it.isFromUser || !it.isFromUser }
    val preview = lastMessage?.text?.take(60)?.let {
        if (lastMessage.text.length > 60) "$it…" else it
    } ?: "Пустой чат"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationZ = if (editMode) shakeAngle else 0f
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A24))
                .combinedClickable(
                    onClick = onOpen,
                    onLongClick = onLongPress
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF252533)),
                contentAlignment = Alignment.Center
            ) {
                ChatIcon(isSelected = false)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = preview,
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        if (editMode) {
            // Кнопка удаления — правый верхний угол
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5252))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            // Кнопка переименования — чуть левее
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-20).dp, y = (-6).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF252533))
                    .clickable { onRename() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Переименовать",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

// ─── Single conversation screen ───────────────────────────────────────────────

@Composable
fun ChatConversationScreen(
    session: ChatSession,
    isTestPassed: Boolean,
    onBack: () -> Unit,
    onSessionUpdated: (ChatSession) -> Unit
) {
    val context = LocalContext.current
    val applicationScope = remember {
        kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember(session.id) { mutableStateOf(session.messages) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember(session.id) { mutableStateOf(false) }
    var titleSet by remember(session.id) { mutableStateOf(session.title != "Новый чат") }
    var showQuickActions by remember { mutableStateOf(true) }

    var attachedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var attachedImageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            attachedImageUri = uri
            attachedImageBitmap = try {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    android.graphics.ImageDecoder.decodeBitmap(
                        android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                    ) { decoder, _, _ -> decoder.isMutableRequired = true }
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (_: Exception) { null }
        }
    }

    // Системный промт из результатов теста + контекст сессии (напр. задача дня)
    val systemPrompt = remember(session.id) {
        val base = buildPersonalitySystemPrompt()
        val ctx = session.systemContext.trim()
        when {
            base.isNotEmpty() && ctx.isNotEmpty() -> "$base\n\n$ctx"
            ctx.isNotEmpty() -> ctx
            else -> base
        }
    }

    val generativeModel = remember(session.id) {
        GenerativeModel(
            modelName = "gemini-3.5-flash-lite",
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = if (systemPrompt.isNotEmpty())
                com.google.ai.client.generativeai.type.content { text(systemPrompt) }
            else null
        )
    }

    val chat = remember(session.id) {
        val historyContents = mutableListOf<com.google.ai.client.generativeai.type.Content>()
        val pairs = messages.windowed(2, 1).filter { it[0].isFromUser && !it[1].isFromUser }
        pairs.forEach { (userMsg, modelMsg) ->
            historyContents.add(com.google.ai.client.generativeai.type.content(role = "user") { text(userMsg.text) })
            historyContents.add(com.google.ai.client.generativeai.type.content(role = "model") { text(modelMsg.text) })
        }
        generativeModel.startChat(history = historyContents)
    }

    LaunchedEffect(session.id) {
        if (!isTestPassed) return@LaunchedEffect

        val prompt = session.autoPrompt.trim()
        if (prompt.isNotEmpty()) {
            // Скрытый промт — отправляем в AI без пузырька пользователя
            messages = listOf(ChatMessage(text = "", isFromUser = false))
            isLoading = true
            applicationScope.launch {
                try {
                    val stream = withContext(Dispatchers.IO) { chat.sendMessageStream(prompt) }
                    val sb = StringBuilder()
                    stream.collect { chunk ->
                        sb.append(chunk.text ?: "")
                        messages = listOf(ChatMessage(text = sb.toString(), isFromUser = false))
                    }
                    if (sb.isEmpty()) {
                        messages = listOf(ChatMessage(
                            text = "К сожалению, не удалось получить ответ.",
                            isFromUser = false
                        ))
                    }
                } catch (e: Exception) {
                    messages = listOf(ChatMessage(
                        text = "Ошибка при запросе: ${e.localizedMessage ?: "Неизвестная ошибка"}",
                        isFromUser = false
                    ))
                } finally {
                    isLoading = false
                }
            }
        } else if (messages.isEmpty()) {
            // Обычный новый чат — показываем приветствие
            val greeting = "Привет! Я готов помочь. Чем могу быть полезен?"
            messages = listOf(ChatMessage(text = greeting, isFromUser = false))
            onSessionUpdated(session.copy(messages = messages))
        } else if (messages.size == 1 && messages[0].isFromUser) {
            // Сессия открылась с предзаполненным запросом (устаревший путь) —
            // автоматически отправляем его в AI и ждём ответ
            val textToSend = messages[0].text
            val botMsgIndex = 1
            messages = messages + ChatMessage(text = "", isFromUser = false)
            isLoading = true
            applicationScope.launch {
                try {
                    val stream = withContext(Dispatchers.IO) { chat.sendMessageStream(textToSend) }
                    val sb = StringBuilder()
                    stream.collect { chunk ->
                        sb.append(chunk.text ?: "")
                        messages = messages.toMutableList().also {
                            it[botMsgIndex] = ChatMessage(text = sb.toString(), isFromUser = false)
                        }
                    }
                    if (sb.isEmpty()) {
                        messages = messages.toMutableList().also {
                            it[botMsgIndex] = ChatMessage(
                                text = "К сожалению, не удалось получить ответ.",
                                isFromUser = false
                            )
                        }
                    }
                } catch (e: Exception) {
                    messages = messages + ChatMessage(
                        text = "Ошибка при запросе: ${e.localizedMessage ?: "Неизвестная ошибка"}",
                        isFromUser = false
                    )
                } finally {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(messages) {
        onSessionUpdated(session.copy(messages = messages))
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage() {
        if (!isTestPassed) return
        val textToSend = inputText.trim()
        if (textToSend.isEmpty() || isLoading) return

        val imgBitmap = attachedImageBitmap
        val imgUriStr = attachedImageUri?.toString()

        val userMsg = ChatMessage(text = textToSend, isFromUser = true, imageUri = imgUriStr)
        messages = messages + userMsg
        inputText = ""
        attachedImageUri = null
        attachedImageBitmap = null
        isLoading = true

        applicationScope.launch {
            if (!titleSet) {
                titleSet = true
                try {
                    val titleModel = GenerativeModel(
                        modelName = "gemini-3.5-flash-lite",
                        apiKey = BuildConfig.GEMINI_API_KEY
                    )
                    val prompt = "Придумай короткое название (3–5 слов) для чата, тема которого: \"$textToSend\". Только название, без кавычек и лишнего текста."
                    val titleResponse = withContext(Dispatchers.IO) { titleModel.generateContent(prompt) }
                    val newTitle = titleResponse.text?.trim()?.take(40) ?: textToSend.take(30)
                    onSessionUpdated(session.copy(title = newTitle, messages = messages))
                } catch (_: Exception) {
                    onSessionUpdated(session.copy(title = textToSend.take(30), messages = messages))
                }
            }

            try {
                val botMsgIndex = messages.size
                messages = messages + ChatMessage(text = "", isFromUser = false)

                val stream = withContext(Dispatchers.IO) {
                    if (imgBitmap != null) {
                        val userContent = com.google.ai.client.generativeai.type.content(role = "user") {
                            image(imgBitmap)
                            text(textToSend)
                        }
                        chat.sendMessageStream(userContent)
                    } else {
                        chat.sendMessageStream(textToSend)
                    }
                }

                val sb = StringBuilder()
                stream.collect { chunk ->
                    sb.append(chunk.text ?: "")
                    messages = messages.toMutableList().also {
                        it[botMsgIndex] = ChatMessage(text = sb.toString(), isFromUser = false)
                    }
                }

                if (sb.isEmpty()) {
                    messages = messages.toMutableList().also {
                        it[botMsgIndex] = ChatMessage(
                            text = "К сожалению, не удалось получить ответ.",
                            isFromUser = false
                        )
                    }
                }
            } catch (e: Exception) {
                messages = messages + ChatMessage(
                    text = "Ошибка при запросе: ${e.localizedMessage ?: "Неизвестная ошибка"}",
                    isFromUser = false
                )
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .imePadding()
    ) {
        // ── Top bar ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(w * 0.6f, h * 0.2f)
                        lineTo(w * 0.25f, h * 0.5f)
                        lineTo(w * 0.6f, h * 0.8f)
                    }
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            Text(
                text = session.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
        }

        // ── Messages ───────────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 12.dp, top = 4.dp)
        ) {
            if (!isTestPassed) {
                item {
                    ChatMessageBubble(
                        message = ChatMessage(
                            text = "Чат временно недоступен. Пожалуйста, пройдите тест персонализации во вкладке «Активные таймеры».",
                            isFromUser = false
                        )
                    )
                }
            } else {
                items(messages) { msg ->
                    ChatMessageBubble(message = msg)
                }
                if (isLoading) {
                    item {
                        Text(
                            text = "Gemini печатает...",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }
            }
        }

        if (attachedImageBitmap != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(56.dp)) {
                    Image(
                        bitmap = attachedImageBitmap!!.asImageBitmap(),
                        contentDescription = "Прикреплено",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5252))
                            .clickable {
                                attachedImageUri = null
                                attachedImageBitmap = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("×", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Изображение прикреплено",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }

        // ── Быстрые кнопки ────────────────────────────────────────────────────
        if (isTestPassed) {
            // Кнопка-тоггл видимости панели
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 0.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF252533))
                        .clickable { showQuickActions = !showQuickActions }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (showQuickActions) "▲ скрыть" else "▼ быстрые",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            if (showQuickActions) {
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    // ── Задача дня ─────────────────────────────────────────
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1E1E30))
                                .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .clickable(enabled = !isLoading) {
                                    val savedTask = DailyTaskStorage.getSavedTaskSync(context)
                                    inputText = if (savedTask != null) {
                                        "Вот моя задача дня по теме «${savedTask.type}»:\n${savedTask.latexStatement}\n\nПомоги разобраться с решением пошагово."
                                    } else {
                                        "Вышли мне пример задачи по высшей математике (тип: интеграл или производная), аналогичный задаче дня."
                                    }
                                    sendMessage()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("📚 Задача дня", fontSize = 13.sp, color = Color(0xFFFF8A80))
                        }
                    }

                    // ── Экранное время ──────────────────────────────────────
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1E1E30))
                                .border(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .clickable(enabled = !isLoading) {
                                    val recs = AiTestManager.savedRecommendations
                                    val recsText = if (recs.isNotEmpty()) {
                                        recs.entries.joinToString(", ") { (app, min) -> "$app — ${min} мин" }
                                    } else "лимиты не установлены"
                                    inputText = "Мои установленные лимиты экранного времени: $recsText. Оцени, оптимально ли это для продуктивности, и порекомендуй, что стоит посмотреть на YouTube сегодня исходя из моего профиля."
                                    sendMessage()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("📱 Экранное время", fontSize = 13.sp, color = Color(0xFFB39DDB))
                        }
                    }

                    // ── План на день ────────────────────────────────────────
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1E1E30))
                                .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .clickable(enabled = !isLoading) {
                                    val savedTask = DailyTaskStorage.getSavedTaskSync(context)
                                    val taskHint = if (savedTask != null) "решить задачу дня по теме «${savedTask.type}»" else "решить математическую задачу"
                                    inputText = "Составь мне краткий план на сегодня: $taskHint, что-то почитать или посмотреть полезного. Учти мой профиль и интересы."
                                    sendMessage()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("📅 План на день", fontSize = 13.sp, color = Color(0xFF81C784))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // ── Input bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isTestPassed) {
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (attachedImageBitmap != null) Color(0xFFFF5252) else Color(0xFF252533)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Прикрепить",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                enabled = isTestPassed,
                placeholder = {
                    Text(
                        text = if (isTestPassed) "Задайте вопрос Gemini..." else "Пройдите тест для доступа",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = Color.Gray,
                    focusedBorderColor = Color(0xFFFF5252),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    disabledBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = Color(0xFF1A1A24),
                    unfocusedContainerColor = Color(0xFF1A1A24),
                    disabledContainerColor = Color(0xFF14141A)
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            val canSend = isTestPassed && (inputText.isNotBlank() || attachedImageBitmap != null) && !isLoading
            IconButton(
                onClick = { sendMessage() },
                enabled = canSend,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (canSend) Color(0xFFFF5252) else Color(0xFF252533))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Отправить",
                    tint = if (canSend) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─── Message bubble ───────────────────────────────────────────────────────────

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val alignment = if (message.isFromUser) Alignment.End else Alignment.Start
    val bgColor = if (message.isFromUser) Color(0xFFFF5252) else Color(0xFF1A1A24)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (message.imageUri != null) {
            val context = LocalContext.current
            val bmp = remember(message.imageUri) {
                try {
                    val uri = android.net.Uri.parse(message.imageUri)
                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                        android.graphics.ImageDecoder.decodeBitmap(
                            android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                        ) { decoder, _, _ -> decoder.isMutableRequired = true }
                    } else {
                        @Suppress("DEPRECATION")
                        android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                } catch (_: Exception) { null }
            }
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Изображение",
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        if (message.text.isNotBlank()) {
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                            bottomEnd = if (message.isFromUser) 4.dp else 16.dp
                        )
                    )
                    .background(bgColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .width(IntrinsicSize.Min)
            ) {
                MixedMathText(
                    text = message.text,
                    textColor = Color.White,
                    textSizeSp = 14
                )
            }
        }
    }
}
