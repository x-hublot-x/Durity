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
import android.view.accessibility.AccessibilityNodeInfo

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

    @Volatile private var isInsideReels = false
    @Volatile private var isExitingReels = false
    @Volatile private var isInstagramForeground = false

    @Volatile private var isInsideVkClips = false
    @Volatile private var isExitingVkClips = false
    @Volatile private var isVkForeground = false
    @Volatile private var currentVkPackage: String = VK_PACKAGE

    @Volatile private var isInsideTwitchClips = false
    @Volatile private var isExitingTwitchClips = false
    @Volatile private var isTwitchForeground = false

    @Volatile private var lastShortsCheckTime = 0L
    @Volatile private var lastReelsCheckTime = 0L
    @Volatile private var lastVkClipsCheckTime = 0L
    @Volatile private var lastTwitchClipsCheckTime = 0L

    private var windowManager: WindowManager? = null
    private var overlayView: android.view.View? = null
    private var lastOverlayTime = 0L

    companion object {
        private const val TAG = "BLOCK"
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        const val YOUTUBE_SHORTS_PACKAGE = "com.google.android.youtube.shorts"
        private const val SHORTS_VIEW_ID = "$YOUTUBE_PACKAGE:id/reel_player_page_container"

        const val INSTAGRAM_PACKAGE = "com.instagram.android"
        const val INSTAGRAM_REELS_PACKAGE = "com.instagram.android.reels"

        const val VK_PACKAGE = "com.vkontakte.android"
        const val VK_CLIPS_STANDALONE_PACKAGE = "com.vk.clips"
        const val VK_CLIENT_PACKAGE = "com.vk.vkclient"
        const val VK_CLIPS_PACKAGE = "com.vkontakte.android.clips"

        const val TWITCH_PACKAGE = "tv.twitch.android.app"
        const val TWITCH_CLIPS_PACKAGE = "tv.twitch.android.app.clips"


        private const val SHORTS_CHECK_INTERVAL_MS = 1000L
        private const val BLOCK_COOLDOWN_MS = 2000L
        private const val BLOCK_TICKER_INTERVAL_MS = 1000L

        private const val NOTIF_CHANNEL_ID = "timer_warnings"
        private const val NOTIF_CHANNEL_NAME = "Предупреждения таймера"
        private const val WARN_MINUTES_BEFORE = 10

        @Volatile var shortsTimeSpentSeconds = 0L
            private set
        @Volatile var reelsTimeSpentSeconds = 0L
            private set
        @Volatile var vkClipsTimeSpentSeconds = 0L
            private set
        @Volatile var twitchClipsTimeSpentSeconds = 0L
            private set

        fun onUnlocked() {
            // Время использования НЕ сбрасывается — при разблокировке увеличивается лимит (+30 минут)
        }

        fun getShortVideoTimeSpent(pkg: String): Long {
            return when (pkg) {
                YOUTUBE_SHORTS_PACKAGE -> shortsTimeSpentSeconds
                INSTAGRAM_REELS_PACKAGE -> reelsTimeSpentSeconds
                VK_CLIPS_PACKAGE -> vkClipsTimeSpentSeconds
                TWITCH_CLIPS_PACKAGE -> twitchClipsTimeSpentSeconds
                else -> 0L
            }
        }

        val effectiveSeconds: Long
            get() = shortsTimeSpentSeconds
    }

    private var currentTrackingDay: String = AppTimerStore.getTodayKey()

    private val shortVideosTicker = object : Runnable {
        override fun run() {
            val todayKey = AppTimerStore.getTodayKey()
            if (todayKey != currentTrackingDay) {
                currentTrackingDay = todayKey
                shortsTimeSpentSeconds = 0L
                reelsTimeSpentSeconds = 0L
                vkClipsTimeSpentSeconds = 0L
                twitchClipsTimeSpentSeconds = 0L
                AppTimerStore.saveShortVideoSpentSeconds(YOUTUBE_SHORTS_PACKAGE, 0L)
                AppTimerStore.saveShortVideoSpentSeconds(INSTAGRAM_REELS_PACKAGE, 0L)
                AppTimerStore.saveShortVideoSpentSeconds(VK_CLIPS_PACKAGE, 0L)
                AppTimerStore.saveShortVideoSpentSeconds(TWITCH_CLIPS_PACKAGE, 0L)
            }

            if (isYouTubeForeground && !isExitingShorts) {
                bgHandler.post { try { checkYouTubeShortsState() } catch (_: Exception) {} }
            }
            if (isInstagramForeground && !isExitingReels) {
                bgHandler.post { try { checkInstagramReelsState() } catch (_: Exception) {} }
            }
            if (isVkForeground && !isExitingVkClips) {
                bgHandler.post { try { checkVkClipsState() } catch (_: Exception) {} }
            }
            if (isTwitchForeground && !isExitingTwitchClips) {
                bgHandler.post { try { checkTwitchClipsState() } catch (_: Exception) {} }
            }

            checkAndTick(YOUTUBE_SHORTS_PACKAGE, YOUTUBE_PACKAGE, isInsideShorts, isExitingShorts) {
                shortsTimeSpentSeconds++
                shortsTimeSpentSeconds
            }
            checkAndTick(INSTAGRAM_REELS_PACKAGE, INSTAGRAM_PACKAGE, isInsideReels, isExitingReels) {
                reelsTimeSpentSeconds++
                reelsTimeSpentSeconds
            }
            checkAndTick(VK_CLIPS_PACKAGE, currentVkPackage, isInsideVkClips, isExitingVkClips) {
                vkClipsTimeSpentSeconds++
                Log.d(TAG, "VK Clips TICK: $vkClipsTimeSpentSeconds seconds (inside=$isInsideVkClips)")
                vkClipsTimeSpentSeconds
            }
            checkAndTick(TWITCH_CLIPS_PACKAGE, TWITCH_PACKAGE, isInsideTwitchClips, isExitingTwitchClips) {
                twitchClipsTimeSpentSeconds++
                twitchClipsTimeSpentSeconds
            }

            mainHandler.postDelayed(this, 1000)
        }
    }

    private inline fun checkAndTick(
        virtualPkg: String,
        parentPkg: String,
        isInside: Boolean,
        isExiting: Boolean,
        increment: () -> Long
    ) {
        if (isInside && !isExiting) {
            val spent = increment()
            AppTimerStore.saveShortVideoSpentSeconds(virtualPkg, spent)
            val timerData = AppTimerStore.limits[virtualPkg]
            if (timerData != null && timerData.limitMinutes > 0) {
                if (spent >= timerData.limitMinutes * 60L) {
                    AppTimerStore.markExhausted(virtualPkg)
                    exitShortVideoToMain(parentPkg, virtualPkg)
                }
            }
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
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }

        bgThread = HandlerThread("BlockChecker").also { it.start() }
        bgHandler = Handler(bgThread.looper)

        currentTrackingDay = AppTimerStore.getTodayKey()
        shortsTimeSpentSeconds = AppTimerStore.getShortVideoSpentSeconds(YOUTUBE_SHORTS_PACKAGE)
        reelsTimeSpentSeconds = AppTimerStore.getShortVideoSpentSeconds(INSTAGRAM_REELS_PACKAGE)
        vkClipsTimeSpentSeconds = AppTimerStore.getShortVideoSpentSeconds(VK_CLIPS_PACKAGE)
        twitchClipsTimeSpentSeconds = AppTimerStore.getShortVideoSpentSeconds(TWITCH_CLIPS_PACKAGE)

        mainHandler.post(shortVideosTicker)
        mainHandler.post(blockTicker)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED)) return

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
            saveActiveShortVideoSeconds()
            resetForegroundShortVideoFlags()
            return
        }

        // Приложение сменилось — фиксируем время начала сессии
        if (packageName != currentForegroundPackage) {
            saveActiveShortVideoSeconds()
            resetForegroundShortVideoFlags()
            currentForegroundPackage = packageName
            if (!sessionStartTimes.containsKey(packageName)) {
                sessionStartTimes[packageName] = now
            }
        }

        when (packageName) {
            YOUTUBE_PACKAGE -> {
                isYouTubeForeground = true
                if (now - lastShortsCheckTime >= SHORTS_CHECK_INTERVAL_MS) {
                    lastShortsCheckTime = now
                    scheduleShortsCheck()
                }
            }
            INSTAGRAM_PACKAGE -> {
                isInstagramForeground = true
                if (now - lastReelsCheckTime >= SHORTS_CHECK_INTERVAL_MS) {
                    lastReelsCheckTime = now
                    scheduleReelsCheck()
                }
            }
            VK_PACKAGE, VK_CLIPS_STANDALONE_PACKAGE, VK_CLIENT_PACKAGE -> {
                isVkForeground = true
                currentVkPackage = packageName
                if (packageName == VK_CLIPS_STANDALONE_PACKAGE) {
                    isInsideVkClips = true
                }
                if (now - lastVkClipsCheckTime >= SHORTS_CHECK_INTERVAL_MS) {
                    lastVkClipsCheckTime = now
                    scheduleVkClipsCheck()
                }
            }
            TWITCH_PACKAGE -> {
                isTwitchForeground = true
                if (now - lastTwitchClipsCheckTime >= SHORTS_CHECK_INTERVAL_MS) {
                    lastTwitchClipsCheckTime = now
                    scheduleTwitchClipsCheck()
                }
            }
            else -> {
                saveActiveShortVideoSeconds()
                resetForegroundShortVideoFlags()
            }
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

    private fun saveActiveShortVideoSeconds() {
        if (isInsideShorts) AppTimerStore.saveShortVideoSpentSeconds(YOUTUBE_SHORTS_PACKAGE, shortsTimeSpentSeconds)
        if (isInsideReels) AppTimerStore.saveShortVideoSpentSeconds(INSTAGRAM_REELS_PACKAGE, reelsTimeSpentSeconds)
        if (isInsideVkClips) AppTimerStore.saveShortVideoSpentSeconds(VK_CLIPS_PACKAGE, vkClipsTimeSpentSeconds)
        if (isInsideTwitchClips) AppTimerStore.saveShortVideoSpentSeconds(TWITCH_CLIPS_PACKAGE, twitchClipsTimeSpentSeconds)
    }

    private fun resetForegroundShortVideoFlags() {
        isYouTubeForeground = false
        isInsideShorts = false
        if (isExitingShorts) isExitingShorts = false

        isInstagramForeground = false
        isInsideReels = false
        if (isExitingReels) isExitingReels = false

        isVkForeground = false
        isInsideVkClips = false
        if (isExitingVkClips) isExitingVkClips = false

        isTwitchForeground = false
        isInsideTwitchClips = false
        if (isExitingTwitchClips) isExitingTwitchClips = false
    }

    private fun scheduleShortsCheck() {
        bgHandler.post { try { checkYouTubeShortsState() } catch (_: Exception) {} }
    }

    private fun scheduleReelsCheck() {
        bgHandler.post { try { checkInstagramReelsState() } catch (_: Exception) {} }
    }

    private fun scheduleVkClipsCheck() {
        bgHandler.post { try { checkVkClipsState() } catch (_: Exception) {} }
    }

    private fun scheduleTwitchClipsCheck() {
        bgHandler.post { try { checkTwitchClipsState() } catch (_: Exception) {} }
    }

    private inline fun scanNodes(
        root: AccessibilityNodeInfo,
        maxNodes: Int = 120,
        predicate: (node: AccessibilityNodeInfo) -> Boolean
    ): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var count = 0
        while (queue.isNotEmpty() && count < maxNodes) {
            val node = queue.removeFirst()
            count++
            if (predicate(node)) {
                return true
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    queue.add(child)
                }
            }
        }
        return false
    }

    private inline fun scanNodesForMarker(
        root: AccessibilityNodeInfo,
        maxNodes: Int = 100,
        predicate: (viewId: String?, className: CharSequence?, text: CharSequence?, contentDescription: CharSequence?) -> Boolean
    ): Boolean {
        return scanNodes(root, maxNodes) { node ->
            predicate(node.viewIdResourceName, node.className, node.text, node.contentDescription)
        }
    }

    private fun checkYouTubeShortsState() {
        if (!isYouTubeForeground) { isInsideShorts = false; return }
        val rootNode = rootInActiveWindow ?: return
        try {
            var inShortsPlayer = false
            var inShortsTab = false
            var inHomeTab = false

            // Ищем элементы, уникальные для плеера YouTube Shorts (reel_player_)
            val shortsViewIds = listOf(
                "com.google.android.youtube:id/reel_player_page_container",
                "com.google.android.youtube:id/reel_player_like_button",
                "com.google.android.youtube:id/reel_player_comment_button",
                "com.google.android.youtube:id/reel_player_share_button",
                "com.google.android.youtube:id/reel_player_remix_button",
                "com.google.android.youtube:id/reel_player_pivot_button",
                "com.google.android.youtube:id/reel_player_view",
                "com.google.android.youtube:id/reel_player_root"
            )
            for (id in shortsViewIds) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (!nodes.isNullOrEmpty()) {
                    inShortsPlayer = true
                    break
                }
            }

            scanNodes(rootNode, maxNodes = 120) { node ->
                val vId = node.viewIdResourceName?.lowercase() ?: ""
                val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                val text = node.text?.toString()?.lowercase() ?: ""
                val isSelected = node.isSelected || desc.contains("selected") || desc.contains("выбрано")

                // Главная страница YouTube
                if (vId.contains("pivot_home") || (isSelected && (desc == "главная" || text == "главная" || desc == "home" || text == "home"))) {
                    inHomeTab = true
                }

                // Выбранная вкладка Shorts в нижней панели (строго со статусом isSelected!)
                if (isSelected && (vId.contains("reel") || desc == "shorts" || text == "shorts")) {
                    inShortsTab = true
                }

                // Элементы управления Shorts плеера
                if (vId.contains("reel_player_") || vId.contains("reel_like") || vId.contains("reel_comment")) {
                    inShortsPlayer = true
                }

                false
            }

            // Если открыта Главная страница и плеер Shorts не открыт — это главная лента, а не Shorts!
            val nowInShorts = if (inHomeTab && !inShortsPlayer) {
                false
            } else {
                inShortsPlayer || inShortsTab
            }

            Log.d(TAG, "YouTube Shorts check: inShorts=$nowInShorts (player=$inShortsPlayer, tab=$inShortsTab, home=$inHomeTab), isExiting=$isExitingShorts")

            if (!nowInShorts && isExitingShorts) isExitingShorts = false
            isInsideShorts = nowInShorts
            if (nowInShorts && !isExitingShorts && AppTimerStore.isExhausted(YOUTUBE_SHORTS_PACKAGE)) {
                mainHandler.post { exitShortVideoToMain(YOUTUBE_PACKAGE, YOUTUBE_SHORTS_PACKAGE) }
            }
        } catch (_: Exception) {
        }
    }

    private fun checkInstagramReelsState() {
        if (!isInstagramForeground) { isInsideReels = false; return }
        val rootNode = rootInActiveWindow ?: return
        try {
            var isReelsTabSelected = false
            var isFeedTabSelected = false
            var isOtherTabSelected = false
            var hasReelsHeader = false
            var hasHomeFeedVisible = false
            var hasClipsPagerVisible = false

            scanNodes(rootNode, maxNodes = 140) { node ->
                val vId = node.viewIdResourceName?.lowercase() ?: ""
                val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                val text = node.text?.toString()?.lowercase() ?: ""
                val isSelected = node.isSelected || desc.contains("selected") || desc.contains("выбрано") || desc.contains("активн")
                val isVisible = node.isVisibleToUser

                // 1. Проверяем выбранную вкладку в нижней панели
                if (isSelected) {
                    if (vId.contains("feed_tab") || desc == "главная" || desc == "home" || text == "главная" || text == "home") {
                        isFeedTabSelected = true
                    }
                    if (vId.contains("search_tab") || desc == "поиск" || desc == "explore" || text == "поиск" || text == "explore") {
                        isOtherTabSelected = true
                    }
                    if (vId.contains("profile_tab") || desc == "профиль" || desc == "profile" || text == "профиль" || text == "profile") {
                        isOtherTabSelected = true
                    }
                    if (vId.contains("clips_tab") || vId.contains("reels_tab") || desc == "reels" || desc == "рилс" || text == "reels" || text == "рилс") {
                        isReelsTabSelected = true
                    }
                }

                // 2. Элементы, видимые пользователю ТОЛЬКО на главной ленте Instagram (Home feed):
                // Stories (reel_tray), посты ленты (row_feed, feed_recycler), Direct (action_bar_inbox), логотип Instagram
                if (isVisible && (vId.contains("row_feed") || vId.contains("feed_recycler") ||
                    vId.contains("action_bar_inbox") || vId.contains("reel_tray") ||
                    (vId.contains("action_bar_title") && (text == "instagram" || desc == "instagram")))) {
                    hasHomeFeedVisible = true
                }

                // 3. Заголовок "Reels" вверху экрана вкладки Reels:
                if (isVisible && (text == "reels" || text == "рилс" || text == "рилсы") &&
                    !vId.contains("tab") && !vId.contains("nav")) {
                    hasReelsHeader = true
                }

                // 4. Полноэкранный вертикальный пейджер Reels:
                if (isVisible && vId.contains("clips_viewer_view_pager")) {
                    hasClipsPagerVisible = true
                }

                false
            }

            // Блокируем ТОЛЬКО саму вкладку Reels.
            // Если видима главная лента новостей или выбрана любая другая вкладка — это норма, НЕ блокируем!
            val found = if (hasHomeFeedVisible || isFeedTabSelected || isOtherTabSelected) {
                false
            } else {
                isReelsTabSelected || hasReelsHeader || hasClipsPagerVisible
            }

            Log.d(TAG, "Instagram Reels check: found=$found (reelsTab=$isReelsTabSelected, header=$hasReelsHeader, pager=$hasClipsPagerVisible, homeFeed=$hasHomeFeedVisible, feedTab=$isFeedTabSelected, otherTab=$isOtherTabSelected), isExiting=$isExitingReels")

            if (!found && isExitingReels) isExitingReels = false
            isInsideReels = found
            if (found && !isExitingReels && AppTimerStore.isExhausted(INSTAGRAM_REELS_PACKAGE)) {
                mainHandler.post { exitShortVideoToMain(INSTAGRAM_PACKAGE, INSTAGRAM_REELS_PACKAGE) }
            }
        } catch (_: Exception) {
        }
    }


    private fun checkVkClipsState() {
        if (!isVkForeground) { isInsideVkClips = false; return }
        if (currentVkPackage == VK_CLIPS_STANDALONE_PACKAGE) {
            isInsideVkClips = true
            if (!isExitingVkClips && AppTimerStore.isExhausted(VK_CLIPS_PACKAGE)) {
                mainHandler.post { exitShortVideoToMain(currentVkPackage, VK_CLIPS_PACKAGE) }
            }
            return
        }

        val rootNode = rootInActiveWindow ?: return
        try {
            var inClipsViewer = false
            var inHomeTab = false

            scanNodes(rootNode, maxNodes = 140) { node ->
                val vId = node.viewIdResourceName?.lowercase() ?: ""
                val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                val text = node.text?.toString()?.lowercase() ?: ""
                val isSelected = node.isSelected || desc.contains("selected") || desc.contains("выбрано")

                if (isSelected && (vId.contains("tab_home") || vId.contains("tab_news") || desc == "главная" || desc == "новости" || text == "главная" || text == "новости")) {
                    inHomeTab = true
                }

                // Элементы, уникальные для плеера клипов ВКонтакте
                if (vId.contains("clips_like") || vId.contains("clips_comment") ||
                    vId.contains("clips_share") || vId.contains("clips_sound") ||
                    vId.contains("clips_music") || vId.contains("clips_author") ||
                    vId.contains("clips_video") || vId.contains("clips_player") ||
                    vId.contains("clips_viewer") || vId.contains("clips_view_pager") ||
                    vId.contains("clips_pager") || vId.contains("clips_item_container") ||
                    vId.contains("clips_wrapper") || vId.contains("clips_fragment")) {
                    inClipsViewer = true
                }

                false
            }

            val found = if (inHomeTab && !inClipsViewer) {
                false
            } else {
                inClipsViewer
            }

            Log.d(TAG, "VK Clips check: found=$found (viewer=$inClipsViewer, homeTab=$inHomeTab), isExiting=$isExitingVkClips, spent=$vkClipsTimeSpentSeconds s")

            if (!found && isExitingVkClips) isExitingVkClips = false
            isInsideVkClips = found
            if (found && !isExitingVkClips && AppTimerStore.isExhausted(VK_CLIPS_PACKAGE)) {
                mainHandler.post { exitShortVideoToMain(currentVkPackage, VK_CLIPS_PACKAGE) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkVkClipsState", e)
        }
    }

    private fun checkTwitchClipsState() {
        if (!isTwitchForeground) { isInsideTwitchClips = false; return }
        val rootNode = rootInActiveWindow ?: return
        try {
            val clipIds = listOf(
                "tv.twitch.android.app:id/clips_player_view",
                "tv.twitch.android.app:id/clips_card_container",
                "tv.twitch.android.app:id/clip_player",
                "tv.twitch.android.app:id/discovery_feed",
                "tv.twitch.android.app:id/feed_recycler_view",
                "tv.twitch.android.app:id/clips_feed"
            )
            var found = false
            for (id in clipIds) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (!nodes.isNullOrEmpty()) {
                    found = true
                    break
                }
            }
            if (!found) {
                found = scanNodesForMarker(rootNode, maxNodes = 100) { viewId, cls, _, _ ->
                    val lowerId = viewId?.lowercase() ?: ""
                    val lowerCls = cls?.toString()?.lowercase() ?: ""
                    (lowerId.contains("clip") && !lowerId.contains("tab")) || lowerCls.contains("clip")
                }
            }
            if (!found && isExitingTwitchClips) isExitingTwitchClips = false
            isInsideTwitchClips = found
            if (found && !isExitingTwitchClips && AppTimerStore.isExhausted(TWITCH_CLIPS_PACKAGE)) {
                mainHandler.post { exitShortVideoToMain(TWITCH_PACKAGE, TWITCH_CLIPS_PACKAGE) }
            }
        } catch (_: Exception) {
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

    private fun clickTabNode(
        root: AccessibilityNodeInfo,
        targetIds: List<String>,
        targetKeywords: List<String>
    ): Boolean {
        for (idPart in targetIds) {
            val list = root.findAccessibilityNodeInfosByViewId(idPart)
            if (!list.isNullOrEmpty()) {
                for (node in list) {
                    if (clickNodeOrParent(node)) return true
                }
            }
        }

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var count = 0
        while (queue.isNotEmpty() && count < 120) {
            val node = queue.removeFirst()
            count++
            val vId = node.viewIdResourceName?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val text = node.text?.toString()?.lowercase() ?: ""

            val matchId = targetIds.any { vId.endsWith(it) || vId.contains(it) }
            val matchDesc = targetKeywords.any { desc == it || text == it }

            if (matchId || matchDesc) {
                if (clickNodeOrParent(node)) return true
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) queue.add(child)
            }
        }
        return false
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var curr: AccessibilityNodeInfo? = node
        var depth = 0
        while (curr != null && depth < 3) {
            if (curr.isClickable) {
                return curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            curr = curr.parent
            depth++
        }
        return false
    }

    private fun exitShortVideoToMain(parentPackage: String, virtualPackage: String) {
        val fg = currentForegroundPackage
        val isTargetFg = fg == parentPackage ||
                (parentPackage == VK_PACKAGE && (fg == VK_CLIPS_STANDALONE_PACKAGE || fg == VK_CLIENT_PACKAGE))
        if (!isTargetFg) return

        val exitingFlag = when (virtualPackage) {
            YOUTUBE_SHORTS_PACKAGE -> isExitingShorts
            INSTAGRAM_REELS_PACKAGE -> isExitingReels
            VK_CLIPS_PACKAGE -> isExitingVkClips
            TWITCH_CLIPS_PACKAGE -> isExitingTwitchClips
            else -> false
        }
        if (exitingFlag) return

        when (virtualPackage) {
            YOUTUBE_SHORTS_PACKAGE -> { isInsideShorts = false; isExitingShorts = true }
            INSTAGRAM_REELS_PACKAGE -> { isInsideReels = false; isExitingReels = true }
            VK_CLIPS_PACKAGE -> { isInsideVkClips = false; isExitingVkClips = true }
            TWITCH_CLIPS_PACKAGE -> { isInsideTwitchClips = false; isExitingTwitchClips = true }
        }

        val rootNode = try { rootInActiveWindow } catch (_: Exception) { null }
        var clickedHomeTab = false

        if (rootNode != null) {
            clickedHomeTab = when (virtualPackage) {
                INSTAGRAM_REELS_PACKAGE -> clickTabNode(rootNode, listOf("feed_tab"), listOf("главная", "home"))
                VK_CLIPS_PACKAGE -> clickTabNode(rootNode, listOf("tab_home", "tab_news", "nav_feed"), listOf("главная", "новости"))
                TWITCH_CLIPS_PACKAGE -> clickTabNode(rootNode, listOf("tab_following", "tab_browse", "nav_following"), listOf("отслеживаемое", "главная", "following", "browse"))
                YOUTUBE_SHORTS_PACKAGE -> clickTabNode(rootNode, listOf("pivot_home", "home_tab"), listOf("главная", "home"))
                else -> false
            }
        }

        // Если не удалось переключиться на вкладку "Главная" (например, открыт полноэкранный плеер), нажимаем НАЗАД
        if (!clickedHomeTab) {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }

        // Запуск через стандартный Launcher Intent родительского приложения
        try {
            val intent: Intent? = if (parentPackage == YOUTUBE_PACKAGE) {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    setPackage(YOUTUBE_PACKAGE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            } else {
                packageManager.getLaunchIntentForPackage(parentPackage)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
            if (intent != null && !clickedHomeTab) {
                startActivity(intent)
            }
        } catch (_: Exception) {
            if (!clickedHomeTab) {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }

        val now = System.currentTimeMillis()
        if (now - lastOverlayTime > 3000) {
            lastOverlayTime = now
            bumpVolumeAndPlaySound()
            showBlockOverlay(virtualPackage)
        }

        mainHandler.postDelayed({
            when (virtualPackage) {
                YOUTUBE_SHORTS_PACKAGE -> isExitingShorts = false
                INSTAGRAM_REELS_PACKAGE -> isExitingReels = false
                VK_CLIPS_PACKAGE -> isExitingVkClips = false
                TWITCH_CLIPS_PACKAGE -> isExitingTwitchClips = false
            }
        }, 3000)
    }

    private fun exitShortsToMainYouTube() {
        exitShortVideoToMain(YOUTUBE_PACKAGE, YOUTUBE_SHORTS_PACKAGE)
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
            INSTAGRAM_REELS_PACKAGE -> "Instagram Reels"
            VK_CLIPS_PACKAGE -> "VK Клипы"
            TWITCH_CLIPS_PACKAGE -> "Twitch Клипы"
            YOUTUBE_PACKAGE -> "YouTube"
            INSTAGRAM_PACKAGE -> "Instagram"
            VK_PACKAGE -> "ВКонтакте"
            TWITCH_PACKAGE -> "Twitch"
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
                pkg == YOUTUBE_SHORTS_PACKAGE ||
                pkg == INSTAGRAM_REELS_PACKAGE ||
                pkg == VK_CLIPS_PACKAGE ||
                pkg == TWITCH_CLIPS_PACKAGE ||
                pkg == VK_CLIPS_STANDALONE_PACKAGE ||
                pkg == VK_CLIENT_PACKAGE


    override fun onInterrupt() {}

    override fun onDestroy() {
        mainHandler.removeCallbacks(shortVideosTicker)
        mainHandler.removeCallbacks(blockTicker)
        bgHandler.removeCallbacksAndMessages(null)
        bgThread.quitSafely()
        hideBlockOverlay()
        super.onDestroy()
    }
}