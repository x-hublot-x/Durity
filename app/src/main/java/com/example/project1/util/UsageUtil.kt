package com.example.project1.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.project1.data.storage.AppTimerStore
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

/**
 * Вычисляет точное время активного использования приложения в миллисекундах
 * на основе последовательности событий UsageEvents за указанный интервал [startTime, endTime].
 */
fun getAppUsageMs(
    context: Context,
    packageName: String,
    startTime: Long,
    endTime: Long,
    isCurrentlyForeground: Boolean = false
): Long {
    if (startTime >= endTime) return 0L

    if (packageName == "com.google.android.youtube.shorts" ||
        packageName == "com.instagram.android.reels" ||
        packageName == "com.vkontakte.android.clips" ||
        packageName == "tv.twitch.android.app.clips"
    ) {
        val seconds = maxOf(
            AppBlockAccessibilityService.getShortVideoTimeSpent(packageName),
            AppTimerStore.getShortVideoSpentSeconds(packageName)
        )
        return seconds * 1000L
    }

    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0L
    val events = usm.queryEvents(startTime, endTime) ?: return 0L
    val event = UsageEvents.Event()

    var totalMs = 0L
    var targetResumeTime = -1L
    val maxContinuousSessionMs = 4 * 60 * 60 * 1000L // 4 часа максимум для одной непрерывной сессии

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val pkg = event.packageName ?: continue
        val eventType = event.eventType
        val ts = event.timeStamp

        if (ts < startTime) continue
        if (ts > endTime) break

        when (eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> {
                if (pkg == packageName) {
                    if (targetResumeTime > 0) {
                        val dur = ts - targetResumeTime
                        if (dur in 1..maxContinuousSessionMs) {
                            totalMs += dur
                        }
                    }
                    targetResumeTime = ts
                } else {
                    // Другое приложение перешло на передний план — закрываем сессию целевого
                    if (targetResumeTime > 0) {
                        val dur = ts - targetResumeTime
                        if (dur in 1..maxContinuousSessionMs) {
                            totalMs += dur
                        }
                        targetResumeTime = -1L
                    }
                }
            }

            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.ACTIVITY_STOPPED -> {
                if (pkg == packageName) {
                    if (targetResumeTime > 0) {
                        val dur = ts - targetResumeTime
                        if (dur in 1..maxContinuousSessionMs) {
                            totalMs += dur
                        }
                        targetResumeTime = -1L
                    }
                }
            }

            UsageEvents.Event.SCREEN_NON_INTERACTIVE,
            UsageEvents.Event.KEYGUARD_SHOWN -> {
                // Экран выключен или заблокирован — закрываем сессию
                if (targetResumeTime > 0) {
                    val dur = ts - targetResumeTime
                    if (dur in 1..maxContinuousSessionMs) {
                        totalMs += dur
                    }
                    targetResumeTime = -1L
                }
            }
        }
    }

    // Если приложение всё ещё активно к моменту endTime
    if (targetResumeTime > 0) {
        val ongoingDur = endTime - targetResumeTime
        if (ongoingDur in 1..maxContinuousSessionMs) {
            // Добавляем текущую сессию только если приложение реально на переднем плане или закрылось < 60 сек назад
            if (isCurrentlyForeground || ongoingDur <= 60_000L) {
                totalMs += ongoingDur
            }
        }
    }

    return maxOf(0L, totalMs)
}

fun getAppUsageMinutesThisWeek(context: Context, packageName: String, addedTimestamp: Long): Int {
    val now = System.currentTimeMillis()

    val mskZone = TimeZone.getTimeZone("GMT+3")
    val calendar = Calendar.getInstance(mskZone).apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfDay = calendar.timeInMillis

    val usedMs = getAppUsageMs(
        context = context,
        packageName = packageName,
        startTime = startOfDay,
        endTime = now,
        isCurrentlyForeground = false
    )

    return (usedMs / 60_000L).toInt()
}

/**
 * Возвращает реальное суммарное экранное время по всем несистемным приложениям за текущие сутки (с 00:00).
 */
fun getTotalDeviceScreenMinutesToday(context: Context): Int {
    val now = System.currentTimeMillis()
    val mskZone = TimeZone.getTimeZone("GMT+3")
    val calendar = Calendar.getInstance(mskZone).apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfDay = calendar.timeInMillis

    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0
    val events = usm.queryEvents(startOfDay, now) ?: return 0
    val event = UsageEvents.Event()

    var totalMs = 0L
    var lastResumeTime = -1L
    var currentPkg: String? = null

    val ignored = setOf(
        "com.android.systemui",
        "android",
        context.packageName
    )

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val pkg = event.packageName ?: continue
        if (pkg in ignored || pkg.contains("launcher", ignoreCase = true)) continue

        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> {
                if (lastResumeTime > 0 && currentPkg != null && event.timeStamp > lastResumeTime) {
                    val dur = event.timeStamp - lastResumeTime
                    if (dur in 1..14400000L) {
                        totalMs += dur
                    }
                }
                lastResumeTime = event.timeStamp
                currentPkg = pkg
            }
            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.ACTIVITY_STOPPED -> {
                if (lastResumeTime > 0 && currentPkg == pkg) {
                    val dur = event.timeStamp - lastResumeTime
                    if (dur in 1..14400000L) {
                        totalMs += dur
                    }
                    lastResumeTime = -1L
                    currentPkg = null
                }
            }
            UsageEvents.Event.SCREEN_NON_INTERACTIVE,
            UsageEvents.Event.KEYGUARD_SHOWN -> {
                if (lastResumeTime > 0 && currentPkg != null) {
                    val dur = event.timeStamp - lastResumeTime
                    if (dur in 1..14400000L) {
                        totalMs += dur
                    }
                    lastResumeTime = -1L
                    currentPkg = null
                }
            }
        }
    }

    if (lastResumeTime > 0 && currentPkg != null && now > lastResumeTime) {
        val dur = now - lastResumeTime
        if (dur in 1..14400000L) {
            totalMs += dur
        }
    }

    val minutes = (totalMs / 60000L).toInt()
    return maxOf(minutes, 0)
}
