package com.example.project1.data.storage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AppNotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: String, // "summary", "task", "blitz", "timer", "streak", "general"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

object NotificationHistoryStorage {
    private const val PREFS = "notification_history_prefs"
    private const val KEY_NOTIFICATIONS = "notifications_json_list"
    private const val MAX_NOTIFICATIONS = 50

    @Synchronized
    fun getNotifications(context: Context): List<AppNotificationItem> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_NOTIFICATIONS, null) ?: return initializeDefaultIfNeeded(context)
        return parseNotifications(json)
    }

    private fun parseNotifications(json: String): List<AppNotificationItem> {
        val list = mutableListOf<AppNotificationItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AppNotificationItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", "Уведомление"),
                        message = obj.optString("message", ""),
                        type = obj.optString("type", "general"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isRead = obj.optBoolean("isRead", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun addNotification(context: Context, title: String, message: String, type: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = getNotifications(context).toMutableList()

        // Avoid duplicate summary/task notifications on the same day, or exact same notifications in 10 minutes
        val now = System.currentTimeMillis()
        if (type == "summary" || type == "task") {
            val hasRecentSameType = existing.any { it.type == type && (now - it.timestamp < 12 * 3600 * 1000L) }
            if (hasRecentSameType) return
        } else {
            val duplicate = existing.firstOrNull { 
                it.title == title && it.message == message && (now - it.timestamp < 10 * 60 * 1000L) 
            }
            if (duplicate != null) return
        }

        val item = AppNotificationItem(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            type = type,
            timestamp = now,
            isRead = false
        )
        existing.add(0, item)
        if (existing.size > MAX_NOTIFICATIONS) {
            existing.removeAt(existing.lastIndex)
        }

        saveNotifications(context, existing)
    }

    @Synchronized
    fun markAllAsRead(context: Context) {
        val existing = getNotifications(context)
        if (existing.none { !it.isRead }) return
        val updated = existing.map { it.copy(isRead = true) }
        saveNotifications(context, updated)
    }

    @Synchronized
    fun hasUnread(context: Context): Boolean {
        return getNotifications(context).any { !it.isRead }
    }

    @Synchronized
    fun clearAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NOTIFICATIONS, "[]").apply()
    }

    private fun saveNotifications(context: Context, list: List<AppNotificationItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("message", item.message)
                put("type", item.type)
                put("timestamp", item.timestamp)
                put("isRead", item.isRead)
            }
            array.put(obj)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NOTIFICATIONS, array.toString())
            .apply()
    }

    private fun initializeDefaultIfNeeded(context: Context): List<AppNotificationItem> {
        val initial = mutableListOf<AppNotificationItem>()
        val now = System.currentTimeMillis()

        initial.add(
            AppNotificationItem(
                title = "Задача дня готова",
                message = "Решите сегодняшнюю математическую задачу, чтобы прокачать репутацию и сохранить стрик!",
                type = "task",
                timestamp = now - 2 * 3600 * 1000L,
                isRead = false
            )
        )
        initial.add(
            AppNotificationItem(
                title = "Математический Блиц",
                message = "Проверьте свои навыки счета и заработайте кристаллы и монеты в режиме Блица.",
                type = "blitz",
                timestamp = now - 6 * 3600 * 1000L,
                isRead = true
            )
        )

        saveNotifications(context, initial)
        return initial
    }
}
