package com.example.project1.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.project1.service.AppBlockAccessibilityService
import java.util.Calendar
import java.util.TimeZone

fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "$h ч $m мин"
        h > 0 -> "$h ч"
        else -> "$m мин"
    }
}

fun getAppUsageMinutesThisWeek(context: Context, packageName: String, addedTimestamp: Long): Int {
    val now = System.currentTimeMillis()

    val mskZone = TimeZone.getTimeZone("GMT+3")
    val calendar = Calendar.getInstance(mskZone).apply {
        timeInMillis = now
        if (get(Calendar.HOUR_OF_DAY) < 3) {
            add(Calendar.DAY_OF_MONTH, -1)
        }
        set(Calendar.HOUR_OF_DAY, 3)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val lastResetTimestamp = calendar.timeInMillis

    if (packageName == "com.google.android.youtube.shorts") {
        val seconds = maxOf(
            AppBlockAccessibilityService.shortsTimeSpentSeconds,
            com.example.project1.data.storage.AppTimerStore.getShortsSpentSeconds()
        )
        return (seconds / 60).toInt()
    }

    // queryEvents точнее чем queryUsageStats(INTERVAL_DAILY):
    // INTERVAL_DAILY игнорирует startTime и отдаёт данные за весь суточный интервал Android.
    // queryEvents корректно фильтрует по lastResetTimestamp (3:00 МСК).
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val events = usm.queryEvents(lastResetTimestamp, now)
    val event = UsageEvents.Event()

    var totalMs = 0L
    var lastResumeTime = -1L

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        if (event.packageName != packageName) continue

        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> {
                lastResumeTime = event.timeStamp
            }
            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.ACTIVITY_STOPPED -> {
                if (lastResumeTime >= 0) {
                    totalMs += event.timeStamp - lastResumeTime
                    lastResumeTime = -1L
                }
            }
        }
    }

    // Не считаем текущую незакрытую сессию самого приложения
    if (lastResumeTime >= 0 && packageName != context.packageName) {
        totalMs += now - lastResumeTime
    }

    return (totalMs / 60_000L).toInt()
}
