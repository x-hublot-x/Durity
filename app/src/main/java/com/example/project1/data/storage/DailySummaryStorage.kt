package com.example.project1.data.storage

import android.content.Context
import org.json.JSONObject

data class DailySummaryData(
    val dateKey: String,
    val totalScreenMinutes: Int,
    val scrollingMinutes: Int,
    val ratingDelta: Int,
    val coinsAwarded: Int,
    val dailyTaskSolved: Boolean,
    val blitzWins: Int,
    val blitzLosses: Int,
    val aiRecommendation: String,
    val timestamp: Long
)

object DailySummaryStorage {
    private const val PREFS = "daily_summary_prefs"
    private const val KEY_LATEST_SUMMARY = "latest_summary_json"
    private const val KEY_PROCESSED_DATE = "processed_summary_date"
    private const val KEY_AWARD_APPLIED_DATE = "award_applied_date"
    private const val KEY_BLITZ_WINS = "blitz_wins_today"
    private const val KEY_BLITZ_LOSSES = "blitz_losses_today"
    private const val KEY_BLITZ_DATE = "blitz_stats_date"
    private const val KEY_SUMMARY_NOTIF_SENT_DATE = "summary_notif_sent_date"
    private const val KEY_TASK_NOTIF_SENT_DATE = "task_notif_sent_date"

    fun isSummaryNotificationSent(context: Context, dateKey: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SUMMARY_NOTIF_SENT_DATE, null) == dateKey
    }

    fun markSummaryNotificationSent(context: Context, dateKey: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SUMMARY_NOTIF_SENT_DATE, dateKey).apply()
    }

    fun isTaskNotificationSent(context: Context, dateKey: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TASK_NOTIF_SENT_DATE, null) == dateKey
    }

    fun markTaskNotificationSent(context: Context, dateKey: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TASK_NOTIF_SENT_DATE, dateKey).apply()
    }

    fun isAwardApplied(context: Context, dateKey: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AWARD_APPLIED_DATE, null) == dateKey
    }

    fun markAwardApplied(context: Context, dateKey: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AWARD_APPLIED_DATE, dateKey).apply()
    }

    fun saveSummary(context: Context, data: DailySummaryData) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = JSONObject().apply {
            put("dateKey", data.dateKey)
            put("totalScreenMinutes", data.totalScreenMinutes)
            put("scrollingMinutes", data.scrollingMinutes)
            put("ratingDelta", data.ratingDelta)
            put("coinsAwarded", data.coinsAwarded)
            put("dailyTaskSolved", data.dailyTaskSolved)
            put("blitzWins", data.blitzWins)
            put("blitzLosses", data.blitzLosses)
            put("aiRecommendation", data.aiRecommendation)
            put("timestamp", data.timestamp)
        }
        prefs.edit()
            .putString(KEY_LATEST_SUMMARY, json.toString())
            .putString(KEY_PROCESSED_DATE, data.dateKey)
            .apply()
    }

    fun getLatestSummary(context: Context): DailySummaryData? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LATEST_SUMMARY, null) ?: return null
        return try {
            val json = JSONObject(raw)
            DailySummaryData(
                dateKey = json.getString("dateKey"),
                totalScreenMinutes = json.getInt("totalScreenMinutes"),
                scrollingMinutes = json.getInt("scrollingMinutes"),
                ratingDelta = json.getInt("ratingDelta"),
                coinsAwarded = json.getInt("coinsAwarded"),
                dailyTaskSolved = json.getBoolean("dailyTaskSolved"),
                blitzWins = json.getInt("blitzWins"),
                blitzLosses = json.getInt("blitzLosses"),
                aiRecommendation = json.getString("aiRecommendation"),
                timestamp = json.getLong("timestamp")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun hasProcessedDate(context: Context, dateKey: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PROCESSED_DATE, null) == dateKey
    }

    // ── Учет статистики блицев за день ────────────────────────────────────────

    private fun checkBlitzDate(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val todayKey = AppTimerStore.getTodayKey()
        val saved = prefs.getString(KEY_BLITZ_DATE, null)
        if (saved != todayKey) {
            prefs.edit()
                .putString(KEY_BLITZ_DATE, todayKey)
                .putInt(KEY_BLITZ_WINS, 0)
                .putInt(KEY_BLITZ_LOSSES, 0)
                .apply()
        }
    }

    fun recordBlitzWin(context: Context) {
        checkBlitzDate(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wins = prefs.getInt(KEY_BLITZ_WINS, 0)
        prefs.edit().putInt(KEY_BLITZ_WINS, wins + 1).apply()
    }

    fun recordBlitzLoss(context: Context) {
        checkBlitzDate(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val losses = prefs.getInt(KEY_BLITZ_LOSSES, 0)
        prefs.edit().putInt(KEY_BLITZ_LOSSES, losses + 1).apply()
    }

    fun getBlitzWins(context: Context): Int {
        checkBlitzDate(context)
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_BLITZ_WINS, 0)
    }

    fun getBlitzLosses(context: Context): Int {
        checkBlitzDate(context)
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_BLITZ_LOSSES, 0)
    }
}