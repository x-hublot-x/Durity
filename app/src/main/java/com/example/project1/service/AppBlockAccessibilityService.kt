package com.example.project1.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import androidx.core.app.NotificationCompat
import com.example.project1.R
import com.example.project1.data.storage.AppTimerStore
import java.util.Calendar
import java.util.TimeZone

class AppBlockAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var bgThread: HandlerThread
    private lateinit var bgHandler: Handler

    private val blockCooldowns = mutableMapOf<String, Long>()

    // Время когда приложение стало foreground в текущей сессии (только в памяти)
    // packageName -> timestamp открытия
    private val sessionStartTimes = mutableMapOf<String, Long>()

    @Volatile private var currentForegroundPackage: String? = null
    @Volatile private var isRecentsOpen = false
    @Volatile private var isInsideShorts = false
    @Volatile private var isExitingShorts = false
    @Volatile private var isYouTubeForeground = false
    @Volatile private var lastShortsCheckTime = 0L

    private var windowManager: WindowManager? = null
    private var overlayView: android.view.View? = null
    private var lastOverlayTime = 0L

    companion object {
        private const val TAG = "BLOCK"
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        private const val YOUTUBE_SHORTS_PACKAGE = "com.google.android.youtube.shorts"
        private const val SHORTS_VIEW_ID = "$YOUTUBE_PACKAGE:id/reel_player_page_container"

        private const val SHORTS_CHECK_INTERVAL_MS = 1000L
        private const val BLOCK_COOLDOWN_MS = 2000L
        private const val BLOCK_TICKER_INTERVAL_MS = 1000L

        private const val NOTIF_CHANNEL_ID = "timer_warnings"
        private const val NOTIF_CHANNEL_NAME = "Предупреждения таймера"
        private const val WARN_MINUTES_BEFORE = 10

        @Volatile var shortsTimeSpentSeconds = 0L
            private set

        fun onUnlocked() {
            // Время использования НЕ сбрасывается — при разблокировке увеличивается лимит (+30 минут),
            // поэтому использованное время честно сохраняется (например, 5/35 минут вместо 0/35 минут).
        }

        val effectiveSeconds: Long
            get() = shortsTimeSpentSeconds
    }

    private var currentTrackingDay: String = AppTimerStore.getTodayKey()

    private val shortsTicker = object : Runnable {
        override fun run() {
            val todayKey = AppTimerStore.getTodayKey()
            if (todayKey != currentTrackingDay) {
                currentTrackingDay = todayKey
                shortsTimeSpentSeconds = 0L
                AppTimerStore.saveShortsSpentSeconds(0L)
            }

            // Периодически проверяем Shorts пока открыт YouTube, чтобы сразу подхватывать переход
            if (isYouTubeForeground && !isExitingShorts) {
                bgHandler.post {
                    try { checkYouTubeShortsState() } catch (_: Exception) {}
                }
            }

            if (isInsideShorts && !isExitingShorts) {
                shortsTimeSpentSeconds++
                AppTimerStore.saveShortsSpentSeconds(shortsTimeSpentSeconds)
                val timerData = AppTimerStore.limits[YOUTUBE_SHORTS_PACKAGE]
                if (timerData != null && timerData.limitMinutes > 0) {
                    if (shortsTimeSpentSeconds >= timerData.limitMinutes * 60L) {
                        AppTimerStore.markExhausted(YOUTUBE_SHORTS_PACKAGE)
                        exitShortsToMainYouTube()
                    }
                }
            }
            mainHandler.postDelayed(this, 1000)
        }
    }

    @Volatile private var recentsOpenedAt = 0L

    private val blockTicker = object : Runnable {
        override fun run() {
            bgHandler.post {
                try {
                    if (isRecentsOpen) {
                        if (System.currentTimeMillis() - recentsOpenedAt > 10_000L)
                            isRecentsOpen = false
                        else return@post
                    }

                    val pkg = currentForegroundPackage ?: return@post
                    if (pkg == applicationContext.packageName) return@post
                    if (isSystemPackage(pkg)) return@post

                    val now = System.currentTimeMillis()
                    if (now < (blockCooldowns[pkg] ?: 0L)) return@post

                    if (AppTimerStore.isExhausted(pkg)) {
                        mainHandler.post { doBlock(pkg) }
                        return@post
                    }

                    checkAndBlockOrWarnApp(pkg, now)
                } catch (_: Exception) {}
            }
            mainHandler.postDelayed(this, BLOCK_TICKER_INTERVAL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppTimerStore.init(applicationContext)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }

        bgThread = HandlerThread("BlockChecker").also { it.start() }
        bgHandler = Handler(bgThread.looper)

        currentTrackingDay = AppTimerStore.getTodayKey()
        shortsTimeSpentSeconds = AppTimerStore.getShortsSpentSeconds()

        mainHandler.post(shortsTicker)
        mainHandler.post(blockTicker)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        val now = System.currentTimeMillis()
        val className = event.className?.toString() ?: ""

        if (packageName == "com.android.systemui" || isRecentsPackage(packageName)) {
            if (isRecentsClassName(className) || isRecentsPackage(packageName)) {
                isRecentsOpen = true
                recentsOpenedAt = now
            }
            return
        }

        if (isRecentsOpen) isRecentsOpen = false
        if (isSystemPackage(packageName)) return
        if (packageName == applicationContext.packageName) {
            currentForegroundPackage = null
            if (isInsideShorts) {
                AppTimerStore.saveShortsSpentSeconds(shortsTimeSpentSeconds)
            }
            isYouTubeForeground = false
            isInsideShorts = false
            return
        }

        // Приложение сменилось — фиксируем время начала сессии
        if (packageName != currentForegroundPackage) {
            currentForegroundPackage = packageName
            // Запоминаем момент открытия если ещё не записан
            if (!sessionStartTimes.containsKey(packageName)) {
                sessionStartTimes[packageName] = now
            }
        }

        if (packageName == YOUTUBE_PACKAGE) {
            isYouTubeForeground = true
            if (now - lastShortsCheckTime >= SHORTS_CHECK_INTERVAL_MS) {
                lastShortsCheckTime = now
                scheduleShortsCheck()
            }
        } else {
            if (isInsideShorts) {
                AppTimerStore.saveShortsSpentSeconds(shortsTimeSpentSeconds)
            }
            isYouTubeForeground = false
            isInsideShorts = false
            if (isExitingShorts) isExitingShorts = false
        }

        if (now >= (blockCooldowns[packageName] ?: 0L)) {
            bgHandler.post {
                try {
                    if (AppTimerStore.isExhausted(packageName)) {
                        mainHandler.post { doBlock(packageName) }
                    } else {
                        checkAndBlockOrWarnApp(packageName, now)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun scheduleShortsCheck() {
        bgHandler.post { try { checkYouTubeShortsState() } catch (_: Exception) {} }
    }

    private fun checkYouTubeShortsState() {
        if (!isYouTubeForeground) { isInsideShorts = false; return }
        val rootNode = rootInActiveWindow ?: return
        try {
            val shortsNodes = rootNode.findAccessibilityNodeInfosByViewId(SHORTS_VIEW_ID)
            val nowInShorts = !shortsNodes.isNullOrEmpty()
            if (!nowInShorts && isExitingShorts) isExitingShorts = false
            isInsideShorts = nowInShorts
            shortsNodes?.forEach { it.recycle() }
        } catch (_: Exception) {
        } finally {
            try { rootNode.recycle() } catch (_: Exception) {}
        }
    }

    // ─── Подсчёт времени: UsageStats + текущая сессия из памяти ─────────────

    private fun getTotalUsedMs(packageName: String, startTime: Long, now: Long): Long {
        // 1. Историческое время из UsageStats (надёжно, но с задержкой ~30-60 сек)
        val historicalMs = getUsageViaEvents(packageName, startTime, now)

        // 2. Текущая сессия из памяти сервиса (мгновенно, без задержки API)
        val sessionStart = sessionStartTimes[packageName]
        val currentSessionMs = if (sessionStart != null && sessionStart >= startTime) {
            now - sessionStart
        } else 0L

        // Берём максимум: UsageStats иногда уже включает текущую сессию,
        // а иногда нет — берём большее значение чтобы не занижать
        return maxOf(historicalMs, currentSessionMs)
    }

    private fun checkAndBlockOrWarnApp(packageName: String, now: Long) {
        val timerData = AppTimerStore.limits[packageName] ?: run {
            Log.d(TAG, "$packageName — нет в limits")
            return
        }
        if (timerData.limitMinutes <= 0) {
            Log.d(TAG, "$packageName — лимит = 0")
            return
        }

        val todayStart = getTodayStart()
        val startTime = maxOf(todayStart, timerData.addedTimestamp)
        val usedMs = getTotalUsedMs(packageName, startTime, now)
        val usedMinutes = (usedMs / 60_000L).toInt()
        val remainingMinutes = timerData.limitMinutes - usedMinutes

        Log.d(TAG, "$packageName | лимит=${timerData.limitMinutes} | использовано=$usedMinutes | осталось=$remainingMinutes | exhausted=${AppTimerStore.isExhausted(packageName)}")

        when {
            remainingMinutes <= 0 -> {
                AppTimerStore.markExhausted(packageName)
                mainHandler.post { doBlock(packageName) }
            }
            remainingMinutes <= WARN_MINUTES_BEFORE && !AppTimerStore.isWarned(packageName) -> {
                AppTimerStore.markWarned(packageName)
                sendWarningNotification(packageName, remainingMinutes)
            }
        }
    }

    // ─── Блокировка ───────────────────────────────────────────────────────────

    private fun doBlock(packageName: String) {
        val now = System.currentTimeMillis()
        if (now < (blockCooldowns[packageName] ?: 0L)) return
        if (currentForegroundPackage != packageName) return

        blockCooldowns[packageName] = now + BLOCK_COOLDOWN_MS
        currentForegroundPackage = null
        // Сбрасываем sessionStart чтобы не накапливать время пока заблокировано
        sessionStartTimes.remove(packageName)

        performGlobalAction(GLOBAL_ACTION_HOME)

        if (now - lastOverlayTime > 3000) {
            lastOverlayTime = now
            bumpVolumeAndPlaySound()
            showBlockOverlay(packageName)
        }
    }

    private fun exitShortsToMainYouTube() {
        if (isExitingShorts) return
        isInsideShorts = false
        isExitingShorts = true

        performGlobalAction(GLOBAL_ACTION_BACK)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
            setPackage(YOUTUBE_PACKAGE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        try { startActivity(intent) } catch (_: Exception) { performGlobalAction(GLOBAL_ACTION_BACK) }

        val now = System.currentTimeMillis()
        if (now - lastOverlayTime > 3000) {
            lastOverlayTime = now
            bumpVolumeAndPlaySound()
            showBlockOverlay(YOUTUBE_SHORTS_PACKAGE)
        }

        mainHandler.postDelayed({ isExitingShorts = false }, 3000)
    }

    // ─── Звук + громкость ────────────────────────────────────────────────────

    private fun bumpVolumeAndPlaySound() {
        try {
            val audio = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val stream = AudioManager.STREAM_MUSIC
            val current = audio.getStreamVolume(stream)
            val max = audio.getStreamMaxVolume(stream)
            if (current < max) audio.setStreamVolume(stream, current + 1, 0)
        } catch (_: Exception) {}

        try {
            val mp: MediaPlayer? = MediaPlayer.create(applicationContext, R.raw.block_sound)
            if (mp != null) {
                mp.setOnCompletionListener { it.release() }
                mp.start()
            }
        } catch (_: Exception) {}
    }

    // ─── Уведомление ─────────────────────────────────────────────────────────

    private fun sendWarningNotification(packageName: String, remainingMinutes: Int) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val appName = try {
            applicationContext.packageManager.getApplicationLabel(
                applicationContext.packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (_: Exception) { packageName.substringAfterLast('.') }

        val notif: Notification = NotificationCompat.Builder(applicationContext, NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⏰ Осталось $remainingMinutes мин — $appName")
            .setContentText("Скоро доступ к $appName будет заблокирован.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify(packageName.hashCode(), notif)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID, NOTIF_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Предупреждения об истечении времени" }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    // ─── Оверлей ─────────────────────────────────────────────────────────────

    private fun showBlockOverlay(packageName: String) {
        if (overlayView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        val minWidthPx = (280 * resources.displayMetrics.density).toInt()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            minimumWidth = minWidthPx
            setPadding(64, 52, 64, 52)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EE1A1A2E"))
                cornerRadius = 40f
            }
        }

        val gifView = ImageView(this).apply {
            val sizePx = (120 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (12 * resources.displayMetrics.density).toInt()
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        container.addView(gifView)
        // Glide загружает GIF и запускает анимацию
        Glide.with(applicationContext)
            .asGif()
            .load(R.raw.block_anim)
            .into(gifView)

        TextView(this).apply {
            text = "ЗАСИДЕЛИСЬ ДА?"; setTextColor(Color.WHITE); textSize = 20f
            gravity = Gravity.CENTER; setTypeface(typeface, Typeface.BOLD)
            container.addView(this)
        }

        val label = when (packageName) {
            YOUTUBE_SHORTS_PACKAGE -> "YouTube Shorts"
            YOUTUBE_PACKAGE -> "YouTube"
            else -> try {
                applicationContext.packageManager.getApplicationLabel(
                    applicationContext.packageManager.getApplicationInfo(packageName, 0)
                ).toString()
            } catch (_: Exception) { packageName.substringAfterLast('.') }
        }

        TextView(this).apply {
            text = "$label на сегодня хватит"
            setTextColor(Color.parseColor("#BBBBBB")); textSize = 13f
            gravity = Gravity.CENTER
            textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 8, 0, 32)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            container.addView(this)
        }

        Button(this).apply {
            text = "Закрыть"; setTextColor(Color.WHITE); textSize = 14f
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC3A3A")); cornerRadius = 24f
            }
            setPadding(48, 20, 48, 20)
            setOnClickListener { hideBlockOverlay() }
            container.addView(this)
        }

        overlayView = container
        try {
            windowManager?.addView(overlayView, params)
            mainHandler.postDelayed({ hideBlockOverlay() }, 8000)
        } catch (_: Exception) { overlayView = null }
    }

    private fun hideBlockOverlay() {
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
            overlayView = null
        }
    }

    // ─── UsageStats (исторические данные) ────────────────────────────────────

    private fun getUsageViaEvents(packageName: String, startTime: Long, endTime: Long): Long {
        val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usm.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var totalMs = 0L
        var lastResumeTime = -1L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != packageName) continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (lastResumeTime >= 0) totalMs += event.timeStamp - lastResumeTime
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
        if (lastResumeTime >= 0) totalMs += endTime - lastResumeTime
        return totalMs
    }

    // ─── Утилиты ─────────────────────────────────────────────────────────────

    private fun getTodayStart(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+3"))
        cal.timeInMillis = System.currentTimeMillis()
        if (cal.get(Calendar.HOUR_OF_DAY) < 3) cal.add(Calendar.DAY_OF_MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 3); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun isRecentsPackage(pkg: String) =
        pkg == "com.miui.home" ||
                pkg == "com.samsung.android.app.cocktailbarservice" ||
                pkg == "com.huawei.android.launcher"

    private fun isRecentsClassName(className: String): Boolean {
        val lower = className.lowercase()
        return lower.contains("recents") || lower.contains("recenttasks") ||
                lower.contains("recentapps") || lower.contains("tasksthumbnail")
    }

    private fun isSystemPackage(pkg: String) =
        pkg == applicationContext.packageName ||
                pkg == "com.android.settings" ||
                pkg == "com.android.systemui" ||
                pkg.contains("launcher", ignoreCase = true) ||
                pkg == YOUTUBE_SHORTS_PACKAGE

    override fun onInterrupt() {}

    override fun onDestroy() {
        mainHandler.removeCallbacks(shortsTicker)
        mainHandler.removeCallbacks(blockTicker)
        bgHandler.removeCallbacksAndMessages(null)
        bgThread.quitSafely()
        hideBlockOverlay()
        super.onDestroy()
    }
}