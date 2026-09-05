package com.example.project1.data.storage

import android.content.Context
import com.example.project1.data.model.AppTimerData
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone

object AppTimerStore {
    private const val PREFS_NAME = "app_timer_limits"
    private const val KEY_LIMITS = "limits"
    private const val KEY_EXHAUSTED = "exhausted_today"
    private const val KEY_EXHAUSTED_DAY = "exhausted_day"
    // Пакеты которым уже отправили уведомление "осталось 10 минут" сегодня
    private const val KEY_WARNED = "warned_today"

    val limits = mutableMapOf<String, AppTimerData>()

    private val _exhaustedToday = mutableSetOf<String>()
    val exhaustedToday: Set<String> get() = _exhaustedToday

    private val _warnedToday = mutableSetOf<String>()
    val warnedToday: Set<String> get() = _warnedToday

    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        limits.clear()
        limits.putAll(load(appContext!!))
        loadDailyState(appContext!!)
    }

    fun setLimit(packageName: String, data: AppTimerData) {
        limits[packageName] = data
        appContext?.let { save(it, limits) }
    }

    fun removeLimit(packageName: String) {
        limits.remove(packageName)
        _exhaustedToday.remove(packageName)
        _warnedToday.remove(packageName)
        appContext?.let {
            save(it, limits)
            saveDailyState(it)
        }
    }

    fun getLimit(packageName: String): AppTimerData? = limits[packageName]
    fun hasLimit(packageName: String): Boolean = limits.containsKey(packageName)

    fun markExhausted(packageName: String) {
        _exhaustedToday.add(packageName)
        appContext?.let { saveDailyState(it) }
    }

    fun clearExhausted(packageName: String) {
        _exhaustedToday.remove(packageName)
        _warnedToday.remove(packageName)
        appContext?.let { saveDailyState(it) }
    }

    fun isExhausted(packageName: String): Boolean = _exhaustedToday.contains(packageName)

    fun markWarned(packageName: String) {
        _warnedToday.add(packageName)
        appContext?.let { saveDailyState(it) }
    }

    fun isWarned(packageName: String): Boolean = _warnedToday.contains(packageName)

    private const val KEY_SHORTS_TIME = "shorts_time_spent_seconds"
    private const val KEY_SHORTS_DAY = "shorts_day_key"

    fun getShortsSpentSeconds(): Long {
        val ctx = appContext ?: return 0L
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedDay = prefs.getString(KEY_SHORTS_DAY, null)
        if (savedDay != getTodayKey()) return 0L
        return prefs.getLong(KEY_SHORTS_TIME, 0L)
    }

    fun saveShortsSpentSeconds(seconds: Long) {
        val ctx = appContext ?: return
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(KEY_SHORTS_TIME, seconds)
            .putString(KEY_SHORTS_DAY, getTodayKey())
            .apply()
    }

    // ─── Приватные методы ─────────────────────────────────────────────────────

    fun getTodayKey(): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+3"))
        cal.timeInMillis = System.currentTimeMillis()
        if (cal.get(Calendar.HOUR_OF_DAY) < 3) cal.add(Calendar.DAY_OF_MONTH, -1)
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun loadDailyState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedDay = prefs.getString(KEY_EXHAUSTED_DAY, null)
        val todayKey = getTodayKey()

        _exhaustedToday.clear()
        _warnedToday.clear()

        if (savedDay == todayKey) {
            try {
                val exArr = org.json.JSONArray(prefs.getString(KEY_EXHAUSTED, "[]"))
                for (i in 0 until exArr.length()) _exhaustedToday.add(exArr.getString(i))
            } catch (_: Exception) {}
            try {
                val wArr = org.json.JSONArray(prefs.getString(KEY_WARNED, "[]"))
                for (i in 0 until wArr.length()) _warnedToday.add(wArr.getString(i))
            } catch (_: Exception) {}
        }
    }

    private fun saveDailyState(context: Context) {
        val exArr = org.json.JSONArray().also { arr -> _exhaustedToday.forEach { arr.put(it) } }
        val wArr = org.json.JSONArray().also { arr -> _warnedToday.forEach { arr.put(it) } }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_EXHAUSTED, exArr.toString())
            .putString(KEY_WARNED, wArr.toString())
            .putString(KEY_EXHAUSTED_DAY, getTodayKey())
            .apply()
    }

    private fun load(context: Context): MutableMap<String, AppTimerData> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LIMITS, null) ?: return mutableMapOf()
        return try {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { pkg ->
                    val entry = json.getJSONObject(pkg)
                    put(pkg, AppTimerData(
                        limitMinutes = entry.getInt("limitMinutes"),
                        addedTimestamp = entry.getLong("addedTimestamp")
                    ))
                }
            }.toMutableMap()
        } catch (_: Exception) { mutableMapOf() }
    }

    private fun save(context: Context, limits: Map<String, AppTimerData>) {
        val json = JSONObject()
        limits.forEach { (pkg, data) ->
            json.put(pkg, JSONObject().apply {
                put("limitMinutes", data.limitMinutes)
                put("addedTimestamp", data.addedTimestamp)
            })
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_LIMITS, json.toString())
            .apply()
    }
}