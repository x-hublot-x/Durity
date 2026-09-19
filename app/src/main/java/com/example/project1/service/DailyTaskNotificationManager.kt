package com.example.project1.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
import java.util.TimeZone

object DailyTaskNotificationManager {
    const val ACTION_DAILY_TASK = "com.example.project1.ACTION_DAILY_TASK"
    const val ACTION_DAILY_SUMMARY = "com.example.project1.ACTION_DAILY_SUMMARY"

    const val REQUEST_CODE_DAILY_TASK = 101
    const val REQUEST_CODE_DAILY_SUMMARY = 102

    fun scheduleDailyNotification(context: Context) {
        scheduleDailyTaskNotification(context)
        scheduleDailySummaryNotification(context)
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

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {}
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

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {}
    }
}
