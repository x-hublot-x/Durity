package com.example.project1.data.storage

import android.content.Context
import com.example.project1.data.model.ChatMessage
import com.example.project1.data.model.ChatSession

object MultiChatStorage {
    private const val PREFS_NAME = "gemini_multi_chat_prefs"
    private const val KEY_SESSIONS = "chat_sessions"

    fun saveSessions(context: Context, sessions: List<ChatSession>) {
        val arr = org.json.JSONArray()
        sessions.forEach { session ->
            val sessionObj = org.json.JSONObject()
            sessionObj.put("id", session.id)
            sessionObj.put("title", session.title)
            sessionObj.put("createdAt", session.createdAt)
            val msgsArr = org.json.JSONArray()
            session.messages.forEach { msg ->
                val msgObj = org.json.JSONObject()
                msgObj.put("text", msg.text)
                msgObj.put("isFromUser", msg.isFromUser)
                if (msg.imageUri != null) msgObj.put("imageUri", msg.imageUri)
                msgsArr.put(msgObj)
            }
            sessionObj.put("messages", msgsArr)
            arr.put(sessionObj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SESSIONS, arr.toString()).apply()
    }

    fun loadSessions(context: Context): List<ChatSession> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SESSIONS, null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            (0 until arr.length()).map { i ->
                val sessionObj = arr.getJSONObject(i)
                val msgsArr = sessionObj.getJSONArray("messages")
                val messages = (0 until msgsArr.length()).map { j ->
                    val msgObj = msgsArr.getJSONObject(j)
                    ChatMessage(
                        text = msgObj.getString("text"),
                        isFromUser = msgObj.getBoolean("isFromUser"),
                        imageUri = if (msgObj.has("imageUri")) msgObj.getString("imageUri") else null
                    )
                }
                ChatSession(
                    id = sessionObj.getString("id"),
                    title = sessionObj.getString("title"),
                    messages = messages,
                    createdAt = sessionObj.optLong("createdAt", System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

// Legacy stub (kept so nothing else breaks)
object ChatStorage {
    fun save(context: Context, messages: List<ChatMessage>) {}
    fun load(context: Context): List<ChatMessage> = emptyList()
    fun clear(context: Context) {}
}
