package com.example.project1.data.storage

import android.content.Context
import com.example.project1.data.model.DailyIntegralTask
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone

object DailyTaskStorage {
    private const val PREFS = "daily_task_prefs"
    private const val KEY_LAST_TASK_DATE = "last_task_date_msk"
    private const val KEY_SOLVED_DATE = "solved_date_msk"
    private const val KEY_COINS = "user_coins"
    private const val KEY_STREAK = "user_streak"
    private const val KEY_PREVIOUS_TASKS = "previous_tasks_history"
    private const val KEY_CURRENT_TASK_JSON = "current_task_json"
    private const val KEY_SOLVED_DAYS_SET = "solved_days_set" // Set<String> дней "yyyyDDD"
    private const val KEY_FREEZE_COUNT = "user_freeze_count" // доступные заморозки
    private const val KEY_FROZEN_DAYS_SET = "frozen_days_set" // Set<String> замороженных дней
    const val FREEZE_PRICE = 79

    fun currentMskDay(): Long {
        val msk = TimeZone.getTimeZone("Europe/Moscow")
        val cal = Calendar.getInstance(msk).apply {
            if (get(Calendar.HOUR_OF_DAY) < 3) add(Calendar.DAY_OF_YEAR, -1)
        }
        return cal.get(Calendar.YEAR).toLong() * 1000 + cal.get(Calendar.DAY_OF_YEAR)
    }

    /** Конвертирует year*1000 + dayOfYear в абсолютный номер дня от epoch (безопасно через границы лет) */
    fun dayCodeToEpochDay(dayCode: Long): Long {
        val year = (dayCode / 1000).toInt()
        val dayOfYear = (dayCode % 1000).toInt()
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow")).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.DAY_OF_YEAR, dayOfYear)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        return cal.timeInMillis / (24 * 60 * 60 * 1000L)
    }

    /** Конвертирует абсолютный номер дня обратно в year*1000 + dayOfYear */
    fun epochDayToDayCode(epochDay: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow")).apply {
            timeInMillis = epochDay * (24 * 60 * 60 * 1000L) + 12 * 3600 * 1000L
        }
        return cal.get(Calendar.YEAR).toLong() * 1000 + cal.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Проверяет пропущенные дни и применяет заморозку, если задача не была решена вплоть до 3:00 МСК
     */
    fun checkAndApplyFreezes(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = currentMskDay()
        val todayEpoch = dayCodeToEpochDay(today)

        val solvedDays = getSolvedDays(context)
        val frozenDays = getFrozenDays(context).toMutableSet()
        var freezeCount = prefs.getInt(KEY_FREEZE_COUNT, 0)

        val allActiveEpochs = (solvedDays + frozenDays).map { dayCodeToEpochDay(it) }.sorted()
        if (allActiveEpochs.isEmpty()) return

        val maxActiveEpoch = allActiveEpochs.last()

        // Если последний активный день раньше вчерашнего дня — есть пропуски!
        if (maxActiveEpoch < todayEpoch - 1) {
            var missedEpoch = maxActiveEpoch + 1
            var changed = false

            while (missedEpoch < todayEpoch) {
                if (freezeCount > 0) {
                    freezeCount--
                    val frozenCode = epochDayToDayCode(missedEpoch)
                    frozenDays.add(frozenCode)
                    changed = true
                    missedEpoch++
                } else {
                    // Заморозок больше нет, цепочка прерывается
                    break
                }
            }

            if (changed) {
                prefs.edit()
                    .putInt(KEY_FREEZE_COUNT, freezeCount)
                    .putStringSet(KEY_FROZEN_DAYS_SET, frozenDays.map { it.toString() }.toSet())
                    .apply()
            }
        }
    }

    fun getStreak(context: Context): Int {
        checkAndApplyFreezes(context)
        val solvedDays = getSolvedDays(context)
        val frozenDays = getFrozenDays(context)
        val allActive = (solvedDays + frozenDays).map { dayCodeToEpochDay(it) }.toSet()

        if (allActive.isEmpty()) return 0

        val todayEpoch = dayCodeToEpochDay(currentMskDay())
        val yesterdayEpoch = todayEpoch - 1

        val startEpoch = when {
            allActive.contains(todayEpoch) -> todayEpoch
            allActive.contains(yesterdayEpoch) -> yesterdayEpoch
            else -> return 0
        }

        var count = 0
        var cur = startEpoch
        while (allActive.contains(cur)) {
            count++
            cur--
        }
        return count
    }

    /** Возвращает все дни (dayCode), составляющие текущую непрерывную активную стрик-сессию */
    fun getActiveStreakDays(context: Context): Set<Long> {
        checkAndApplyFreezes(context)
        val solvedDays = getSolvedDays(context)
        val frozenDays = getFrozenDays(context)
        val allActive = (solvedDays + frozenDays).map { dayCodeToEpochDay(it) }.toSet()

        if (allActive.isEmpty()) return emptySet()

        val todayEpoch = dayCodeToEpochDay(currentMskDay())
        val yesterdayEpoch = todayEpoch - 1

        val startEpoch = when {
            allActive.contains(todayEpoch) -> todayEpoch
            allActive.contains(yesterdayEpoch) -> yesterdayEpoch
            else -> return emptySet()
        }

        val result = mutableSetOf<Long>()
        var cur = startEpoch
        while (allActive.contains(cur)) {
            result.add(epochDayToDayCode(cur))
            cur--
        }
        return result
    }

    fun isSolvedToday(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_SOLVED_DATE, -1L) == currentMskDay()
    }

    fun markSolvedToday(context: Context) {
        checkAndApplyFreezes(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = currentMskDay()
        if (prefs.getLong(KEY_SOLVED_DATE, -1L) == today) return

        val existing = prefs.getStringSet(KEY_SOLVED_DAYS_SET, mutableSetOf()) ?: mutableSetOf()
        val updated = existing.toMutableSet().apply { add(today.toString()) }

        prefs.edit()
            .putLong(KEY_SOLVED_DATE, today)
            .putStringSet(KEY_SOLVED_DAYS_SET, updated)
            .apply()

        val newStreak = getStreak(context)
        prefs.edit().putInt(KEY_STREAK, newStreak).apply()
    }

    /** Возвращает набор «закодированных» дней (year*1000+dayOfYear) когда задача была решена. */
    fun getSolvedDays(context: Context): Set<Long> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return (prefs.getStringSet(KEY_SOLVED_DAYS_SET, emptySet()) ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .toSet()
    }

    /** Возвращает набор дней (year*1000+dayOfYear), когда сработала заморозка. */
    fun getFrozenDays(context: Context): Set<Long> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return (prefs.getStringSet(KEY_FROZEN_DAYS_SET, emptySet()) ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .toSet()
    }

    fun getFreezes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_FREEZE_COUNT, 0)
    }

    fun addFreezes(context: Context, amount: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_FREEZE_COUNT, 0)
        prefs.edit().putInt(KEY_FREEZE_COUNT, maxOf(0, current + amount)).apply()
    }

    @Volatile
    private var lastBuyTimestamp = 0L

    @Synchronized
    fun buyFreezes(context: Context, count: Int): Boolean {
        if (count <= 0) return false
        val now = System.currentTimeMillis()
        if (now - lastBuyTimestamp < 1000L) return false
        val totalCost = count * FREEZE_PRICE
        val currentCoins = getCoins(context)
        if (currentCoins >= totalCost) {
            lastBuyTimestamp = now
            addCoins(context, -totalCost)
            addFreezes(context, count)
            return true
        }
        return false
    }

    fun getCoins(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_COINS, 0)

    fun addCoins(context: Context, amount: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_COINS, 0)
        prefs.edit().putInt(KEY_COINS, current + amount).apply()
    }

    fun getPreviousTasks(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PREVIOUS_TASKS, "[]") ?: "[]"
        val list = mutableListOf<String>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    fun saveTaskHistory(context: Context, statement: String) {
        val history = getPreviousTasks(context).toMutableList()
        history.add(statement)
        if (history.size > 30) history.removeAt(0)

        val array = JSONArray(history)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PREVIOUS_TASKS, array.toString()).apply()
    }

    fun getSavedTask(context: Context): DailyIntegralTask? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = currentMskDay()
        val savedDay = prefs.getLong(KEY_LAST_TASK_DATE, -1L)

        if (savedDay == today) {
            val taskJson = prefs.getString(KEY_CURRENT_TASK_JSON, null) ?: return null
            return try {
                val obj = JSONObject(taskJson)
                DailyIntegralTask(
                    id = obj.getString("id"),
                    type = obj.getString("type"),
                    latexStatement = obj.getString("latexStatement"),
                    correctAnswer = obj.getString("correctAnswer"),
                    description = obj.getString("description")
                )
            } catch (e: Exception) { null }
        }
        return null
    }

    fun saveCurrentTask(context: Context, task: DailyIntegralTask) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val obj = JSONObject().apply {
            put("id", task.id)
            put("type", task.type)
            put("latexStatement", task.latexStatement)
            put("correctAnswer", task.correctAnswer)
            put("description", task.description)
        }

        prefs.edit()
            .putLong(KEY_LAST_TASK_DATE, currentMskDay())
            .putString(KEY_CURRENT_TASK_JSON, obj.toString())
            .apply()

        saveTaskHistory(context, task.latexStatement)
    }

    /** Синхронная версия getSavedTask — для вызова из composable/click-handler */
    fun getSavedTaskSync(context: Context): DailyIntegralTask? = getSavedTask(context)
}
