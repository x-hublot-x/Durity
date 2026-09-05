package com.example.project1.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class AppMonitoringService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    // Тестовый список заблокированных пакетов (Chrome и YouTube)
    private val blockedApps = listOf("com.android.chrome", "com.google.android.youtube")

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundServiceWithNotification()
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "monitoring_channel"
        val channelName = "Фоновый мониторинг"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Блокировщик активен")
            .setContentText("Идет отслеживание активного приложения...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (isActive) {
                val currentApp = getForegroundAppPackageName()
                Log.d("AppMonitoringService", "В фокусе: $currentApp")

                if (blockedApps.contains(currentApp)) {
                    showBlockOverlay(currentApp ?: "")
                } else {
                    hideBlockOverlay()
                }

                delay(1.seconds)
            }
        }
    }

    private fun showBlockOverlay(packageName: String) {
        if (overlayView != null) return // Уже показан

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        // Создаем UI экрана блокировки
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
        }

        val titleText = TextView(this).apply {
            text = "Время вышло!"
            setTextColor(Color.RED)
            textSize = 28f
            gravity = Gravity.CENTER
        }

        val subtitleText = TextView(this).apply {
            text = "Приложение $packageName заблокировано."
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 64)
        }

        val closeButton = Button(this).apply {
            text = "Закрыть (На главный экран)"
            setOnClickListener {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
            }
        }

        layout.addView(titleText)
        layout.addView(subtitleText)
        layout.addView(closeButton)

        overlayView = layout
        windowManager?.addView(overlayView, params)
    }

    private fun hideBlockOverlay() {
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
    }

    private fun getForegroundAppPackageName(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10_000

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var currentForegroundApp: String? = null

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == ACTIVITY_RESUMED) {
                currentForegroundApp = event.packageName
            }
        }

        return currentForegroundApp
    }

    override fun onDestroy() {
        super.onDestroy()
        hideBlockOverlay()
        serviceJob.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
