package com.example.project1.data.storage

import android.content.Context
import org.json.JSONObject
import java.util.*

data class MonthlyDopamineReport(
    val id: String = UUID.randomUUID().toString(),
    val monthKey: String = "",
    val monthTitle: String = "Сентябрь 2026",
    val dateRange: String = "1 сен – 30 сен",
    val savedHours: Float = 56.5f,
    val screenTimeHours: Float = 72.0f,
    val tasksSolved: Int = 22,
    val blitzWins: Int = 26,
    val coinsEarned: Int = 8400,
    val percentile: Int = 94,
    val topSavedApp: String = "Соцсети и Видео",
    val monthlyTrophy: String = "Магистр цифровой трезвости",
    val aiVerdict: String = "Невероятный месяц! Ты выиграл десятки часов у алгоритмов и прокачал свой фокус до гроссмейстерского уровня.",
    val favoriteBlitzTopic: String = "ТФКП",
    val favoriteBlitzComment: String = "Король комплексных плоскостей! Ты прошел весь месяц без единого математического сбоя.",
    val tasksSolvedWithoutHints: Int = 18,
    val tasksSolvedWithHints: Int = 4,
    val aiAssistanceComment: String = "Абсолютная автономия: более 80% задач месяца решены собственным разумом!",
    val timestamp: Long = System.currentTimeMillis()
)

object MonthlyReportStorage {
    private const val PREFS = "monthly_report_prefs"
    private const val KEY_LATEST_REPORT = "latest_monthly_report"
    private const val KEY_LAST_MONTH_KEY = "last_monthly_report_key"

    fun getLatestReport(context: Context): MonthlyDopamineReport? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LATEST_REPORT, null) ?: return null
        return try {
            val obj = JSONObject(json)
            MonthlyDopamineReport(
                id = obj.optString("id", UUID.randomUUID().toString()),
                monthKey = obj.optString("monthKey", ""),
                monthTitle = obj.optString("monthTitle", "За прошедший месяц"),
                dateRange = obj.optString("dateRange", "1 сен – 30 сен"),
                savedHours = obj.optDouble("savedHours", 56.5).toFloat(),
                screenTimeHours = obj.optDouble("screenTimeHours", 72.0).toFloat(),
                tasksSolved = obj.optInt("tasksSolved", 22),
                blitzWins = obj.optInt("blitzWins", 26),
                coinsEarned = obj.optInt("coinsEarned", 8400),
                percentile = obj.optInt("percentile", 94),
                topSavedApp = obj.optString("topSavedApp", "Соцсети и Видео"),
                monthlyTrophy = obj.optString("monthlyTrophy", "Магистр цифровой трезвости"),
                aiVerdict = obj.optString("aiVerdict", "Невероятный месяц! Ты выиграл десятки часов у алгоритмов и прокачал свой фокус до гроссмейстерского уровня."),
                favoriteBlitzTopic = obj.optString("favoriteBlitzTopic", "ТФКП"),
                favoriteBlitzComment = obj.optString("favoriteBlitzComment", "Король комплексных плоскостей! Ты прошел весь месяц без единого математического сбоя."),
                tasksSolvedWithoutHints = obj.optInt("tasksSolvedWithoutHints", 18),
                tasksSolvedWithHints = obj.optInt("tasksSolvedWithHints", 4),
                aiAssistanceComment = obj.optString("aiAssistanceComment", "Абсолютная автономия: более 80% задач месяца решены собственным разумом!"),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveReport(context: Context, report: MonthlyDopamineReport) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val obj = JSONObject().apply {
            put("id", report.id)
            put("monthKey", report.monthKey)
            put("monthTitle", report.monthTitle)
            put("dateRange", report.dateRange)
            put("savedHours", report.savedHours.toDouble())
            put("screenTimeHours", report.screenTimeHours.toDouble())
            put("tasksSolved", report.tasksSolved)
            put("blitzWins", report.blitzWins)
            put("coinsEarned", report.coinsEarned)
            put("percentile", report.percentile)
            put("topSavedApp", report.topSavedApp)
            put("monthlyTrophy", report.monthlyTrophy)
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
            .putString(KEY_LAST_MONTH_KEY, report.monthKey)
            .apply()
    }

    fun getLastReportMonthKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_MONTH_KEY, "") ?: ""
    }
}
