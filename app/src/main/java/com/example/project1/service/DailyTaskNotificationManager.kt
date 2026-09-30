package com.example.project1.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar
import java.util.TimeZone

object DailyTaskNotificationManager {
    const val ACTION_DAILY_TASK = "com.example.project1.ACTION_DAILY_TASK"
    const val ACTION_DAILY_SUMMARY = "com.example.project1.ACTION_DAILY_SUMMARY"
    const val ACTION_WEEKLY_REPORT = "com.example.project1.ACTION_WEEKLY_REPORT"
    const val ACTION_MONTHLY_REPORT = "com.example.project1.ACTION_MONTHLY_REPORT"

    const val REQUEST_CODE_DAILY_TASK = 101
    const val REQUEST_CODE_DAILY_SUMMARY = 102
    const val REQUEST_CODE_WEEKLY_REPORT = 103
    const val REQUEST_CODE_MONTHLY_REPORT = 104

    fun scheduleDailyNotification(context: Context) {
        scheduleDailyTaskNotification(context)
        scheduleDailySummaryNotification(context)
        scheduleWeeklyReportNotification(context)
        scheduleMonthlyReportNotification(context)
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (_: Exception) {}
        }
    }

    fun scheduleDailyTaskNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyTaskReceiver::class.java).apply {
            action = ACTION_DAILY_TASK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_TASK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val msk = TimeZone.getTimeZone("Europe/Moscow")
        val calendar = Calendar.getInstance(msk).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        setAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    fun scheduleDailySummaryNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyTaskReceiver::class.java).apply {
            action = ACTION_DAILY_SUMMARY
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_SUMMARY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val msk = TimeZone.getTimeZone("Europe/Moscow")
        val calendar = Calendar.getInstance(msk).apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        setAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    fun scheduleWeeklyReportNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyTaskReceiver::class.java).apply {
            action = ACTION_WEEKLY_REPORT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WEEKLY_REPORT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val msk = TimeZone.getTimeZone("Europe/Moscow")
        val calendar = Calendar.getInstance(msk).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            while (get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY || timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        setAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    fun scheduleMonthlyReportNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyTaskReceiver::class.java).apply {
            action = ACTION_MONTHLY_REPORT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MONTHLY_REPORT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val msk = TimeZone.getTimeZone("Europe/Moscow")
        val calendar = Calendar.getInstance(msk).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.MONTH, 1)
            }
        }

        setAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
    }
}

