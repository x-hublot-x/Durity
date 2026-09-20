package com.example.project1.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.example.project1.MainActivity
import com.example.project1.util.AmbientSoundType

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

        private var cachedArtwork: Bitmap? = null

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
                val intent = Intent(context, FocusService::class.java).apply {
                    action = ACTION_UPDATE
                }
                context.startService(intent)
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

        private fun getArtworkBitmap(): Bitmap {
            cachedArtwork?.let { return it }
            val size = 256
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, size.toFloat(), size.toFloat(),
                    intArrayOf(0xFFFF6600.toInt(), 0xFFFF8000.toInt(), 0xFFFF4500.toInt()),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), 48f, 48f, paint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 110f
                textAlign = Paint.Align.CENTER
            }
            val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText("🧘", size / 2f, yPos, textPaint)

            cachedArtwork = bitmap
            return bitmap
        }
    }

    private lateinit var notificationManager: NotificationManager
    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        initMediaSession()
    }

    private fun initMediaSession() {
        try {
            mediaSession = MediaSessionCompat(this, "DurityFocusMedia").apply {
                setFlags(
                    MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                )
                setCallback(object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        FocusSessionManager.resumeSession(this@FocusService)
                        updateNotificationDirectly()
                    }

                    override fun onPause() {
                        FocusSessionManager.pauseSession(this@FocusService)
                        updateNotificationDirectly()
                    }

                    override fun onStop() {
                        FocusSessionManager.stopSession(this@FocusService, completed = false)
                    }

                    override fun onCustomAction(action: String?, extras: Bundle?) {
                        if (action == ACTION_STOP) {
                            FocusSessionManager.stopSession(this@FocusService, completed = false)
                        }
                    }
                })
                isActive = true
            }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val notification = buildMediaNotification()
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
            val notification = buildMediaNotification()
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    private fun buildMediaNotification(): Notification {
        val isActive = FocusSessionManager.isActive
        val isPaused = FocusSessionManager.isPaused
        val remainingSec = FocusSessionManager.remainingSeconds
        val totalSec = FocusSessionManager.totalSeconds
        val strictMode = FocusSessionManager.strictMode
        val selectedSounds = FocusSessionManager.selectedSounds

        val elapsedSec = (totalSec - remainingSec).coerceAtLeast(0)
        val positionMs = elapsedSec * 1000L
        val durationMs = (totalSec * 1000L).coerceAtLeast(1000L)

        val minutes = remainingSec / 60
        val seconds = remainingSec % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)

        val soundTitle = if (selectedSounds.isEmpty() || selectedSounds.all { it == AmbientSoundType.NONE }) {
            "Глубокая концентрация 🧘"
        } else {
            selectedSounds.filter { it != AmbientSoundType.NONE }.joinToString(" + ") { it.title }
        }

        val title = soundTitle
        val artist = if (isPaused) "Пауза • Осталось $timeFormatted" else "Сессия фокуса • Осталось $timeFormatted"
        val album = if (strictMode) "Строгий режим" else "Durity Focus"

        val artwork = getArtworkBitmap()

        // 1. Обновляем PlaybackState медиа-сессии (для прогресс-бара и медиа-плеера в шторке/AOD/Now Bar)
        try {
            val state = if (isPaused || !isActive) PlaybackStateCompat.STATE_PAUSED else PlaybackStateCompat.STATE_PLAYING
            val playbackSpeed = if (isPaused || !isActive) 0f else 1f
            val playbackState = PlaybackStateCompat.Builder()
                .setState(state, positionMs, playbackSpeed)
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_STOP
                )
                .build()
            mediaSession?.setPlaybackState(playbackState)

            // 2. Обновляем метаданные медиа-сессии
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artwork)
                .build()
            mediaSession?.setMetadata(metadata)
        } catch (_: Exception) {}

        // Клик по уведомлению: переход в MainActivity -> вкладка Таймеры -> раздел Фокус
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_FOCUS, true)
        }
        val pendingContentIntent = PendingIntent.getActivity(
            this,
            100,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, FocusService::class.java).apply { action = ACTION_PAUSE }
        val pausePendingIntent = PendingIntent.getService(
            this,
            101,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = Intent(this, FocusService::class.java).apply { action = ACTION_RESUME }
        val resumePendingIntent = PendingIntent.getService(
            this,
            102,
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FocusService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this,
            103,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // MediaStyle стиль как у плеера Telegram / Spotify
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1)
            .setShowCancelButton(true)
            .setCancelButtonIntent(stopPendingIntent)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setLargeIcon(artwork)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(album)
            .setStyle(mediaStyle)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(!isPaused && isActive)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingContentIntent)

        if (!isPaused && isActive) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Пауза",
                pausePendingIntent
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Продолжить",
                resumePendingIntent
            )
        }

        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Завершить",
            stopPendingIntent
        )

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Таймер фокуса",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Медиа-плеер и статус сессии фокуса"
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
            mediaSession?.apply {
                isActive = false
                release()
            }
            mediaSession = null
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }
}
