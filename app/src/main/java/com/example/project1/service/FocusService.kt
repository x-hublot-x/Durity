package com.example.project1.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.project1.MainActivity

class FocusService : Service() {

    companion object {
        const val CHANNEL_ID = "focus_timer_channel"
        const val NOTIFICATION_ID = 2026

        const val ACTION_START = "com.example.project1.action.FOCUS_START"
        const val ACTION_PAUSE = "com.example.project1.action.FOCUS_PAUSE"
        const val ACTION_RESUME = "com.example.project1.action.FOCUS_RESUME"
        const val ACTION_STOP = "com.example.project1.action.FOCUS_STOP"
        const val ACTION_UPDATE = "com.example.project1.action.FOCUS_UPDATE"

        @Volatile
        var isServiceRunning = false
            private set

        fun startService(context: Context) {
            try {
                val intent = Intent(context, FocusService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun updateNotification(context: Context) {
            if (!isServiceRunning || !FocusSessionManager.isActive) return
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notification = buildNotification(context)
                nm.notify(NOTIFICATION_ID, notification)
            } catch (_: Exception) {}
        }

        fun stopService(context: Context) {
            if (!isServiceRunning) return
            try {
                val intent = Intent(context, FocusService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun buildNotification(context: Context): Notification {
            val isActive = FocusSessionManager.isActive
            val isPaused = FocusSessionManager.isPaused
            val remainingSec = FocusSessionManager.remainingSeconds
            val strictMode = FocusSessionManager.strictMode

            val minutes = remainingSec / 60
            val seconds = remainingSec % 60
            val timeFormatted = String.format("%02d:%02d", minutes, seconds)

            // Клик по уведомлению: переход в MainActivity -> вкладка Таймеры -> раздел Фокус
            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_FOCUS, true)
            }
            val pendingContentIntent = PendingIntent.getActivity(
                context,
                100,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (isPaused) "⏸ Фокус на паузе" else "🧘 Фокус: сессия активна"
            val text = if (isPaused) "Пауза ($timeFormatted)" else "Осталось: $timeFormatted"
            val subText = if (strictMode) "Строгий режим" else "Фокусировка"

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setSubText(subText)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingContentIntent)

            if (!isPaused && isActive) {
                // Нативный хронометр обратного отсчета для статус-бара, Always On Display и Samsung Now Bar
                builder.setUsesChronometer(true)
                builder.setChronometerCountDown(true)
                builder.setWhen(System.currentTimeMillis() + remainingSec * 1000L)
                builder.setShowWhen(true)

                // Кнопка Пауза
                val pauseIntent = Intent(context, FocusService::class.java).apply { action = ACTION_PAUSE }
                val pausePendingIntent = PendingIntent.getService(
                    context,
                    101,
                    pauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    android.R.drawable.ic_media_pause,
                    "Пауза",
                    pausePendingIntent
                )
            } else if (isPaused) {
                builder.setUsesChronometer(false)
                builder.setShowWhen(false)

                // Кнопка Возобновить
                val resumeIntent = Intent(context, FocusService::class.java).apply { action = ACTION_RESUME }
                val resumePendingIntent = PendingIntent.getService(
                    context,
                    102,
                    resumeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    android.R.drawable.ic_media_play,
                    "Продолжить",
                    resumePendingIntent
                )
            }

            // Кнопка Завершить
            val stopIntent = Intent(context, FocusService::class.java).apply { action = ACTION_STOP }
            val stopPendingIntent = PendingIntent.getService(
                context,
                103,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Завершить",
                stopPendingIntent
            )

            return builder.build()
        }
    }

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val notification = buildNotification(this)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                        } catch (_: Exception) {
                            try {
                                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                            } catch (_: Exception) {
                                startForeground(NOTIFICATION_ID, notification)
                            }
                        }
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (_: Exception) {}
            }
            ACTION_PAUSE -> {
                FocusSessionManager.pauseSession(this)
                updateNotificationDirectly()
            }
            ACTION_RESUME -> {
                FocusSessionManager.resumeSession(this)
                updateNotificationDirectly()
            }
            ACTION_STOP -> {
                FocusSessionManager.stopSession(this, completed = false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                if (FocusSessionManager.isActive) {
                    updateNotificationDirectly()
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }
        return START_STICKY
    }

    private fun updateNotificationDirectly() {
        try {
            val notification = buildNotification(this)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Таймер фокуса",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Отображение активного таймера сессии фокуса на экране блокировки и в строке состояния"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        try {
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }
}
