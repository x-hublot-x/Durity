package com.example.project1.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.project1.BuildConfig
import com.example.project1.MainActivity
import com.example.project1.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyTaskReceiver : BroadcastReceiver() {
    @android.annotation.SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val taskChannelId = "daily_task_channel"
        val summaryChannelId = "daily_summary_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val taskChannel = NotificationChannel(
                taskChannelId,
                "Задача дня",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Уведомления об обновлении задачи дня" }
            notificationManager.createNotificationChannel(taskChannel)

            val summaryChannel = NotificationChannel(
                summaryChannelId,
                "Итоги дня",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Уведомления с итогами прошедшего дня" }
            notificationManager.createNotificationChannel(summaryChannel)
        }

        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        val action = intent?.action
        val dateKey = DailySummaryManager.getSummaryDateKey()

        when (action) {
            DailyTaskNotificationManager.ACTION_DAILY_TASK -> {
                // 1. Уведомление об обновлении задачи дня (00:00 МСК) - строго 1 раз в сутки
                if (!com.example.project1.data.storage.DailySummaryStorage.isTaskNotificationSent(context, dateKey)) {
                    com.example.project1.data.storage.DailySummaryStorage.markTaskNotificationSent(context, dateKey)

                    val intentTask = Intent(context, MainActivity::class.java)
                    val pendingIntentTask = PendingIntent.getActivity(
                        context, 0, intentTask,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val taskNotification = NotificationCompat.Builder(context, taskChannelId)
                        .setSmallIcon(R.drawable.ic_youtube_shorts)
                        .setContentTitle("Задача дня обновлена!")
                        .setContentText("Новая задача уже доступна. Зайди и поддержи свой streak!")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntentTask)
                        .setAutoCancel(true)
                        .build()

                    if (canNotify) {
                        notificationManager.notify(1001, taskNotification)
                    }
                    com.example.project1.data.storage.NotificationHistoryStorage.addNotification(
                        context = context,
                        title = "Задача дня обновлена!",
                        message = "Новая задача уже доступна. Зайди и поддержи свой стрик!",
                        type = "task"
                    )
                }
                DailyTaskNotificationManager.scheduleDailyTaskNotification(context)
            }

            DailyTaskNotificationManager.ACTION_DAILY_SUMMARY -> {
                // 2. Уведомление об итогах дня (21:00 МСК) - строго 1 раз в сутки
                if (!com.example.project1.data.storage.DailySummaryStorage.isSummaryNotificationSent(context, dateKey)) {
                    com.example.project1.data.storage.DailySummaryStorage.markSummaryNotificationSent(context, dateKey)

                    val intentSummary = Intent(context, MainActivity::class.java).apply {
                        putExtra(MainActivity.EXTRA_OPEN_DAILY_SUMMARY, true)
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntentSummary = PendingIntent.getActivity(
                        context, 1002, intentSummary,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val summaryNotification = NotificationCompat.Builder(context, summaryChannelId)
                        .setSmallIcon(R.drawable.ic_youtube_shorts)
                        .setContentTitle("Итоги дня готовы!")
                        .setContentText("Узнай свои результаты за день, изменение рейтинга и совет ИИ.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntentSummary)
                        .setAutoCancel(true)
                        .build()

                    if (canNotify) {
                        notificationManager.notify(1002, summaryNotification)
                    }

                    com.example.project1.data.storage.NotificationHistoryStorage.addNotification(
                        context = context,
                        title = "Итоги дня готовы!",
                        message = "Узнай свои результаты за день, изменение рейтинга и совет ИИ.",
                        type = "summary"
                    )

                    // В фоне генерируем и сохраняем итоги дня с советом ИИ
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            DailySummaryManager.checkAndGenerateDailySummary(context.applicationContext, BuildConfig.GEMINI_API_KEY)
                        } catch (_: Exception) {}
                    }
                }
                DailyTaskNotificationManager.scheduleDailySummaryNotification(context)
            }

            else -> {
                // При перезагрузке устройства или непредвиденных вызовах без action — только планируем будильники
                DailyTaskNotificationManager.scheduleDailyNotification(context)
            }
        }
    }
}
