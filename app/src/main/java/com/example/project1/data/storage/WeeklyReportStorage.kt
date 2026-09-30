package com.example.project1.data.storage

import android.content.Context
import org.json.JSONObject
import java.util.*

data class WeeklyDopamineReport(
    val id: String = UUID.randomUUID().toString(),
    val weekKey: String = "",
    val dateRange: String,
    val savedHours: Float,
    val screenTimeHours: Float,
    val tasksSolved: Int,
    val blitzWins: Int,
    val coinsEarned: Int,
    val percentile: Int,
    val topSavedApp: String,
    val aiVerdict: String,
    val favoriteBlitzTopic: String = "ТФКП",
    val favoriteBlitzComment: String = "Выбор истинных математических эстетов! Комплексный анализ и контуры покорились тебе.",
    val tasksSolvedWithoutHints: Int = 4,
    val tasksSolvedWithHints: Int = 1,
    val aiAssistanceComment: String = "Чистый разум: 4 из 5 задач решены без единой подсказки от ИИ. Твой мозг работает на максимум!",
    val timestamp: Long = System.currentTimeMillis()
)

object WeeklyReportStorage {
    private const val PREFS = "weekly_report_prefs"
    private const val KEY_LATEST_REPORT = "latest_weekly_report"
    private const val KEY_LAST_WEEK_KEY = "last_weekly_report_key"

    fun getLatestReport(context: Context): WeeklyDopamineReport? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LATEST_REPORT, null) ?: return null
        return try {
            val obj = JSONObject(json)
            WeeklyDopamineReport(
                id = obj.optString("id", UUID.randomUUID().toString()),
                weekKey = obj.optString("weekKey", ""),
                dateRange = obj.optString("dateRange", "За последние 7 дней"),
                savedHours = obj.optDouble("savedHours", 14.2).toFloat(),
                screenTimeHours = obj.optDouble("screenTimeHours", 18.5).toFloat(),
                tasksSolved = obj.optInt("tasksSolved", 5),
                blitzWins = obj.optInt("blitzWins", 7),
                coinsEarned = obj.optInt("coinsEarned", 2150),
                percentile = obj.optInt("percentile", 89),
                topSavedApp = obj.optString("topSavedApp", "YouTube Shorts & Reels"),
                aiVerdict = obj.optString("aiVerdict", "Отличная неделя! Ты показал железную дисциплину и сохранил часы для важных побед."),
                favoriteBlitzTopic = obj.optString("favoriteBlitzTopic", "ТФКП"),
                favoriteBlitzComment = obj.optString("favoriteBlitzComment", "Выбор истинных математических эстетов! Комплексные числа трепещут перед твоим взором."),
                tasksSolvedWithoutHints = obj.optInt("tasksSolvedWithoutHints", 4),
                tasksSolvedWithHints = obj.optInt("tasksSolvedWithHints", 1),
                aiAssistanceComment = obj.optString("aiAssistanceComment", "Чистый разум: большинство задач решены без подсказок! Твой мозг работает автономно и мощно."),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveReport(context: Context, report: WeeklyDopamineReport) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val obj = JSONObject().apply {
            put("id", report.id)
            put("weekKey", report.weekKey)
            put("dateRange", report.dateRange)
            put("savedHours", report.savedHours.toDouble())
            put("screenTimeHours", report.screenTimeHours.toDouble())
            put("tasksSolved", report.tasksSolved)
            put("blitzWins", report.blitzWins)
            put("coinsEarned", report.coinsEarned)
            put("percentile", report.percentile)
            put("topSavedApp", report.topSavedApp)
            put("aiVerdict", report.aiVerdict)
            put("favoriteBlitzTopic", report.favoriteBlitzTopic)
            put("favoriteBlitzComment", report.favoriteBlitzComment)
            put("tasksSolvedWithoutHints", report.tasksSolvedWithoutHints)
            put("tasksSolvedWithHints", report.tasksSolvedWithHints)
            put("aiAssistanceComment", report.aiAssistanceComment)
            put("timestamp", report.timestamp)
        }
        prefs.edit()
            .putString(KEY_LATEST_REPORT, obj.toString())
            .putString(KEY_LAST_WEEK_KEY, report.weekKey)
            .apply()
    }

    fun getLastReportWeekKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_WEEK_KEY, "") ?: ""
    }
}

