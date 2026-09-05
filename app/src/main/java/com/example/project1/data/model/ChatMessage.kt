package com.example.project1.data.model

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val imageUri: String? = null
)

data class ChatSession(
    val id: String,
    val title: String,
    val messages: List<ChatMessage>,
    val createdAt: Long = System.currentTimeMillis(),
    val systemContext: String = "", // Дополнительный системный контекст (напр. задача дня)
    val autoPrompt: String = ""    // Скрытый промт: AI отвечает сам, без пузырька пользователя
)
