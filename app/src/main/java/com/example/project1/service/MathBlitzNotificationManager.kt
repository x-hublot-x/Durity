package com.example.project1.service

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.project1.MainActivity
import com.example.project1.R
import com.example.project1.data.model.BlitzSessionState

object MathBlitzNotificationManager {
    const val CHANNEL_ID = "math_blitz_channel"
    const val CHANNEL_NAME = "Блиц по математике"
    const val NOTIF_ID_ACTIVE = 2001
    const val NOTIF_ID_TIMEOUT = 2002
    const val EXTRA_OPEN_BLITZ = "open_blitz"
    const val ACTION_BLITZ_TIMEOUT = "com.example.project1.BLITZ_TIMEOUT"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления об активном блице и результатах"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    2001
                )
            }
        }
    }

    /**
     * Показывает всплывающее пуш-уведомление при сворачивании / выходе из активного блица
     */
    fun showActiveBlitzNotification(context: Context, session: BlitzSessionState) {
        createNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val remainingMs = maxOf(0L, session.deadlineTime - System.currentTimeMillis())
        if (remainingMs <= 0 || session.isFinished) {
            cancelActiveNotification(context)
            return
        }

        val minutes = remainingMs / 1000 / 60
        val seconds = (remainingMs / 1000) % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_BLITZ, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIF_ID_ACTIVE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.fire1)
        } catch (_: Exception) {
            null
        }

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Активен блиц-бой!")
            .setContentText("Осталось $timeFormatted • Ставка: ${session.config.betCoins} монет!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Осталось времени: $timeFormatted.\nЗадача ${session.currentTaskIndex + 1} из ${session.tasks.size}.\nНажмите, чтобы вернуться и спасти ставку в ${session.config.betCoins} монет!")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (largeIcon != null) {
            notificationBuilder.setLargeIcon(largeIcon)
        }

        notificationManager.notify(NOTIF_ID_ACTIVE, notificationBuilder.build())
    }

    fun cancelActiveNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIF_ID_ACTIVE)
    }

    /**
     * Планирует будильник на момент истечения дедлайна блица
     */
    fun scheduleTimeoutAlarm(context: Context, deadlineEpochMs: Long, betCoins: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MathBlitzTimeoutReceiver::class.java).apply {
            action = ACTION_BLITZ_TIMEOUT
            putExtra("bet_coins", betCoins)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIF_ID_TIMEOUT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        deadlineEpochMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        deadlineEpochMs,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    deadlineEpochMs,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, deadlineEpochMs, pendingIntent)
        }
    }

    fun cancelTimeoutAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MathBlitzTimeoutReceiver::class.java).apply {
            action = ACTION_BLITZ_TIMEOUT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIF_ID_TIMEOUT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Показывает уведомление о проигрыше, если время вышло и игрок не вернулся
     */
    fun showTimeoutLossNotification(context: Context, betCoins: Int) {
        createNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Убираем уведомление об активном бое
        notificationManager.cancel(NOTIF_ID_ACTIVE)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIF_ID_TIMEOUT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.fire1)
        } catch (_: Exception) {
            null
        }

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Время вышло! Блиц проигран")
            .setContentText("Вы не успели завершить блиц вовремя. Сгорело $betCoins монет.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Время раунда истекло, пока вас не было в игре. Ставка в $betCoins монет сгорела. Попробуйте ещё раз!")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (largeIcon != null) {
            notificationBuilder.setLargeIcon(largeIcon)
        }

        notificationManager.notify(NOTIF_ID_TIMEOUT, notificationBuilder.build())

        com.example.project1.data.storage.NotificationHistoryStorage.addNotification(
            context = context,
            title = "Блиц проигран по таймауту",
            message = "Время раунда истекло. Сгорело $betCoins монет.",
            type = "blitz"
        )
    }
}
