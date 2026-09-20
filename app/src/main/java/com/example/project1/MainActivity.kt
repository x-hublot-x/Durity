package com.example.project1

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.project1.data.model.AppInfo
import com.example.project1.data.model.AppTimerData
import com.example.project1.data.model.ChatMessage
import com.example.project1.data.model.ChatSession
import com.example.project1.data.model.UNLOCK_BONUS_MINUTES
import com.example.project1.data.storage.AiTestManager
import com.example.project1.data.storage.AppTimerStore
import com.example.project1.service.AppBlockAccessibilityService
import com.example.project1.data.storage.DailySummaryData
import com.example.project1.data.storage.DailySummaryStorage
import com.example.project1.ui.components.GlassBottomNavigationBar
import com.example.project1.ui.screens.chat.ChatScreen
import com.example.project1.ui.screens.daily.DailyTaskScreen
import com.example.project1.ui.screens.home.DailySummaryScreen
import com.example.project1.ui.screens.home.HomeScreen
import com.example.project1.ui.screens.personality.AiPersonalityTestDialog
import com.example.project1.ui.screens.settings.SettingsScreen
import com.example.project1.ui.screens.stats.StatsScreen
import com.example.project1.ui.screens.timers.AppSelectionDialog
import com.example.project1.ui.screens.timers.PermissionRequestScreen
import com.example.project1.ui.screens.timers.TimerConfigDialog
import com.example.project1.ui.screens.timers.TimersScreen
import com.example.project1.ui.theme.AppTheme
import com.example.project1.ui.theme.Project1Theme
import com.example.project1.ui.theme.ThemeManager
import com.example.project1.util.IconCache
import com.example.project1.util.VibrationUtil
import com.example.project1.util.getAppUsageMinutesThisWeek
import com.example.project1.util.reelsLogoDrawable
import com.example.project1.util.shortsLogoDrawable
import com.example.project1.util.twitchClipsLogoDrawable
import com.example.project1.util.vkClipsLogoDrawable
import com.example.project1.data.model.BlitzConfig
import com.example.project1.data.model.BlitzSessionState
import com.example.project1.data.repository.MathBlitzRepository
import com.example.project1.data.storage.DailyTaskStorage
import com.example.project1.data.storage.MathBlitzStorage
import com.example.project1.service.DailySummaryManager
import com.example.project1.service.MathBlitzNotificationManager
import com.example.project1.ui.screens.blitz.MathBlitzScreen
import com.example.project1.ui.screens.blitz.MathBlitzSetupScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_DAILY_SUMMARY = "EXTRA_OPEN_DAILY_SUMMARY"
        val openBlitzRequested = mutableStateOf(false)
        val openDailySummaryRequested = mutableStateOf(false)
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getBooleanExtra(MathBlitzNotificationManager.EXTRA_OPEN_BLITZ, false) == true) {
            openBlitzRequested.value = true
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_DAILY_SUMMARY, false) == true) {
            openDailySummaryRequested.value = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!MathBlitzNotificationManager.hasNotificationPermission(this)) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        AppTimerStore.init(this)
        AiTestManager.init(this)
        ThemeManager.init(this)
        com.example.project1.ui.wallpaper.WallpaperManager.init(this)
        com.example.project1.data.storage.BlockMediaManager.init(this)
        enableEdgeToEdge()

        setContent {
            Project1Theme {
                var hasUsagePermission by remember { mutableStateOf(hasUsageStatsPermission()) }
                var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(this@MainActivity)) }
                var hasAccessibilityPermission by remember {
                    mutableStateOf(isAccessibilityServiceEnabled(this@MainActivity, AppBlockAccessibilityService::class.java))
                }

                LaunchedEffect(Unit) {
                    checkAndRequestPermissions()
                }

                if (!hasUsagePermission || !hasOverlayPermission || !hasAccessibilityPermission) {
                    PermissionRequestScreen(
                        hasUsage = hasUsagePermission,
                        hasOverlay = hasOverlayPermission,
                        hasAccessibility = hasAccessibilityPermission,
                        onRequestUsage = {
                            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        },
                        onRequestOverlay = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:$packageName".toUri()
                            )
                            startActivity(intent)
                        },
                        onRequestAccessibility = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            startActivity(intent)
                        },
                        onRefresh = {
                            hasUsagePermission = hasUsageStatsPermission()
                            hasOverlayPermission = Settings.canDrawOverlays(this@MainActivity)
                            hasAccessibilityPermission = isAccessibilityServiceEnabled(this@MainActivity, AppBlockAccessibilityService::class.java)
                        }
                    )
                } else {
                    MainScreen()
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (!hasUsageStatsPermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            startActivity(intent)
        } else if (!isAccessibilityServiceEnabled(this, AppBlockAccessibilityService::class.java)) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val expectedComponentName = ComponentName(context, serviceClass)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null) {
                if (enabledComponent == expectedComponentName ||
                    enabledComponent.packageName == context.packageName) {
                    return true
                }
            }
        }
        return false
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.getBooleanExtra(MathBlitzNotificationManager.EXTRA_OPEN_BLITZ, false) == true) {
            openBlitzRequested.value = true
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_DAILY_SUMMARY, false) == true) {
            openDailySummaryRequested.value = true
        }
    }

    override fun onPause() {
        super.onPause()
        ThemeManager.applyPendingAppIcon(applicationContext)
        val session = MathBlitzStorage.getActiveSession(this)
        if (session != null && !session.isFinished && session.deadlineTime > System.currentTimeMillis()) {
            MathBlitzNotificationManager.showActiveBlitzNotification(this, session)
        }
    }

    override fun onStop() {
        super.onStop()
        ThemeManager.applyPendingAppIcon(applicationContext)
        val session = MathBlitzStorage.getActiveSession(this)
        if (session != null && !session.isFinished && session.deadlineTime > System.currentTimeMillis()) {
            MathBlitzNotificationManager.showActiveBlitzNotification(this, session)
        }
    }

    override fun onResume() {
        super.onResume()
        val session = MathBlitzStorage.getActiveSession(this)
        if (session != null && (session.isFinished || session.deadlineTime <= System.currentTimeMillis())) {
            MathBlitzNotificationManager.cancelActiveNotification(this)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val hazeState = remember { HazeState() }
    var showDailyTask by remember { mutableStateOf(false) }
    var showStatsDetails by remember { mutableStateOf(false) }

    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var selectedAppForTimer by remember { mutableStateOf<AppInfo?>(null) }
    var selectedAppForEdit by remember { mutableStateOf<AppInfo?>(null) }
    var showAppSelectionDialog by remember { mutableStateOf(false) }

    suspend fun refreshApps() = withContext(Dispatchers.Default) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        val currentPackage = context.packageName
        val virtualShortPackages = setOf(
            "com.google.android.youtube.shorts",
            "com.instagram.android.reels",
            "com.vkontakte.android.clips",
            "tv.twitch.android.app.clips"
        )

        fun createShortAppInfo(name: String, pkg: String, rawDrawable: android.graphics.drawable.Drawable): AppInfo {
            val timerData = AppTimerStore.limits[pkg]
            val limit = timerData?.limitMinutes ?: 0
            val addedTimestamp = timerData?.addedTimestamp ?: 0L
            val used = getAppUsageMinutesThisWeek(context, pkg, addedTimestamp)
            val isFrozen = limit > 0 && used >= limit
            val displayIcon = IconCache.getIcon(context, pkg, rawDrawable, isFrozen)
            return AppInfo(
                name = name,
                packageName = pkg,
                icon = displayIcon,
                timeLimitMinutes = limit,
                usedMinutesThisWeek = used,
                addedTimestamp = addedTimestamp
            )
        }

        val shortsAppInfo = createShortAppInfo("YouTube Shorts", "com.google.android.youtube.shorts", shortsLogoDrawable(context))
        val reelsAppInfo = createShortAppInfo("Instagram Reels", "com.instagram.android.reels", reelsLogoDrawable(context))
        val vkClipsAppInfo = createShortAppInfo("VK Клипы", "com.vkontakte.android.clips", vkClipsLogoDrawable(context))
        val twitchClipsAppInfo = createShortAppInfo("Twitch Клипы", "tv.twitch.android.app.clips", twitchClipsLogoDrawable(context))

        val regularApps = resolvedInfos
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val timerData = AppTimerStore.limits[pkg]
                val limit = timerData?.limitMinutes ?: 0
                val addedTimestamp = timerData?.addedTimestamp ?: 0L
                val used = getAppUsageMinutesThisWeek(context, pkg, addedTimestamp)
                val isFrozen = limit > 0 && used >= limit

                val rawIcon = resolveInfo.loadIcon(pm)
                val displayIcon = IconCache.getIcon(context, pkg, rawIcon, isFrozen)

                AppInfo(
                    name = resolveInfo.loadLabel(pm).toString(),
                    packageName = pkg,
                    icon = displayIcon,
                    timeLimitMinutes = limit,
                    usedMinutesThisWeek = used,
                    addedTimestamp = addedTimestamp
                )
            }
            .filter { it.packageName != currentPackage && !virtualShortPackages.contains(it.packageName) }
            .distinctBy { it.packageName }
            .sortedBy { it.name }

        // Точный порядок: Шортсы выше всех, рилсы ниже, вк ниже и потом твич
        val newList = listOf(shortsAppInfo, reelsAppInfo, vkClipsAppInfo, twitchClipsAppInfo) + regularApps

        withContext(Dispatchers.Main) {
            installedApps = newList
        }
    }


    LaunchedEffect(Unit) {
        while (true) {
            refreshApps()
            delay(3000)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch { refreshApps() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun triggerDeletion(app: AppInfo) {
        AppTimerStore.removeLimit(app.packageName)
        IconCache.invalidate(app.packageName)
        coroutineScope.launch { refreshApps() }
        Toast.makeText(context, "Таймер для ${app.name} удален!", Toast.LENGTH_SHORT).show()
    }

    fun triggerUnlock(app: AppInfo) {
        val current = AppTimerStore.limits[app.packageName]
        val addedTimestamp = current?.addedTimestamp ?: System.currentTimeMillis()
        val newLimit = (current?.limitMinutes ?: app.timeLimitMinutes) + UNLOCK_BONUS_MINUTES
        AppTimerStore.setLimit(
            app.packageName,
            AppTimerData(
                limitMinutes = newLimit,
                addedTimestamp = addedTimestamp
            )
        )
        // Снимаем флаг исчерпания — иначе сервис продолжит мгновенно блокировать
        AppTimerStore.clearExhausted(app.packageName)
        IconCache.invalidate(app.packageName)
        if (app.packageName == "com.google.android.youtube.shorts" ||
            app.packageName == "com.instagram.android.reels" ||
            app.packageName == "com.vkontakte.android.clips" ||
            app.packageName == "tv.twitch.android.app.clips") {
            AppBlockAccessibilityService.onUnlocked()
        }

        coroutineScope.launch { refreshApps() }
        Toast.makeText(
            context,
            "+$UNLOCK_BONUS_MINUTES минут для ${app.name}! Приложение разблокировано.",
            Toast.LENGTH_SHORT
        ).show()
    }

    var isChatOpen by remember { mutableStateOf(false) }
    var isBottomBarStyleOpen by remember { mutableStateOf(false) }
    var showAiTestDialogFromHome by remember { mutableStateOf(false) }
    var pendingChatSession by remember { mutableStateOf<ChatSession?>(null) }
    var showBlitzSetupScreen by remember { mutableStateOf(false) }
    var activeBlitzSession by remember { mutableStateOf<BlitzSessionState?>(null) }
    var showBlitzScreen by remember { mutableStateOf(false) }
    var showDailySummary by remember { mutableStateOf(false) }
    var isNotificationCenterOpen by remember { mutableStateOf(false) }
    var isMathReferenceOpen by remember { mutableStateOf(false) }
    var dailySummaryData by remember { mutableStateOf<DailySummaryData?>(DailySummaryStorage.getLatestSummary(context)) }

    val pageHistory = remember { mutableStateListOf(0) }

    LaunchedEffect(MainActivity.openBlitzRequested.value) {
        if (MainActivity.openBlitzRequested.value) {
            val currentSession = MathBlitzStorage.getActiveSession(context)
            if (currentSession != null && !currentSession.isFinished && currentSession.deadlineTime > System.currentTimeMillis()) {
                activeBlitzSession = currentSession
                showBlitzScreen = true
                MathBlitzNotificationManager.cancelActiveNotification(context)
            }
            MainActivity.openBlitzRequested.value = false
        }
    }

    LaunchedEffect(MainActivity.openDailySummaryRequested.value) {
        if (MainActivity.openDailySummaryRequested.value) {
            dailySummaryData = DailySummaryStorage.getLatestSummary(context)
            if (dailySummaryData != null) {
                showDailySummary = true
            }
            MainActivity.openDailySummaryRequested.value = false
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val page = pagerState.currentPage
        if (pageHistory.isEmpty() || pageHistory.last() != page) {
            pageHistory.add(page)
        }
    }

    BackHandler(enabled = !showBlitzScreen && !showBlitzSetupScreen && !showDailySummary && (showStatsDetails || (pagerState.currentPage != 0 && !isChatOpen && !showDailyTask && !isBottomBarStyleOpen))) {
        if (showStatsDetails) {
            showStatsDetails = false
            return@BackHandler
        }
        while (pageHistory.isNotEmpty() && pageHistory.last() == pagerState.currentPage) {
            pageHistory.removeAt(pageHistory.lastIndex)
        }
        val targetPage = pageHistory.removeLastOrNull() ?: 0
        coroutineScope.launch {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    com.example.project1.ui.wallpaper.AppBackgroundWallpaper {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .haze(hazeState),
                userScrollEnabled = !isChatOpen && !showDailyTask && !showStatsDetails && !isBottomBarStyleOpen && !showBlitzScreen && !showBlitzSetupScreen && !showDailySummary && !isNotificationCenterOpen && !isMathReferenceOpen
            ) { page ->
                when (page) {
                    0 -> HomeScreen(
                        onNavigateToDailyTask = { showDailyTask = true },
                        onShowTestDialog = { showAiTestDialogFromHome = true },
                        onNavigateToStats = { showStatsDetails = true },
                        onOpenBlitz = {
                            val currentSession = MathBlitzStorage.getActiveSession(context)
                            if (currentSession != null && !currentSession.isFinished && currentSession.deadlineTime > System.currentTimeMillis()) {
                                activeBlitzSession = currentSession
                                showBlitzScreen = true
                                MathBlitzNotificationManager.cancelActiveNotification(context)
                            } else {
                                showBlitzSetupScreen = true
                            }
                        },
                        onOpenDailySummary = {
                            coroutineScope.launch {
                                val s = DailySummaryManager.checkAndGenerateDailySummary(context, BuildConfig.GEMINI_API_KEY)
                                dailySummaryData = s ?: DailySummaryStorage.getLatestSummary(context)
                                showDailySummary = true
                            }
                        },
                        appsWithTimers = installedApps.filter { it.timeLimitMinutes > 0 },
                        onNavigateToChat = { title: String, firstMsg: String, taskLatex: String ->
                            val newSession = ChatSession(
                                id = System.currentTimeMillis().toString(),
                                title = title,
                                messages = listOf(
                                    ChatMessage(text = firstMsg, isFromUser = false)
                                ),
                                systemContext = taskLatex
                            )
                            pendingChatSession = newSession
                            coroutineScope.launch { pagerState.animateScrollToPage(3) }
                        },
                        onDiscussDailySummary = { summary ->
                            val title = "Итоги дня (${summary.dateKey})"
                            val firstMsg = """
ИТОГИ ДНЯ (${summary.dateKey}):
• Экранное время: ${summary.totalScreenMinutes} мин (скроллинг шортсов/клипов: ${summary.scrollingMinutes} мин)
• Задача дня: ${if (summary.dailyTaskSolved) "Решена" else "Пропущена"}
• Блиц-поединки: ${summary.blitzWins} побед, ${summary.blitzLosses} поражений
• Изменение репутации: ${if (summary.ratingDelta >= 0) "+${summary.ratingDelta}" else "${summary.ratingDelta}"}
${if (summary.coinsAwarded > 0) "• Бонусные монеты: +${summary.coinsAwarded}\n" else ""}
РЕКОМЕНДАЦИЯ НАСТАВНИКА:
${summary.aiRecommendation}

Я готов обсудить ваши результаты дня, помочь поставить цели на завтра или разобрать любые вопросы! Что бы вы хотели улучшить или уточнить?
                            """.trimIndent()

                            val newSession = ChatSession(
                                id = "summary_${System.currentTimeMillis()}",
                                title = title,
                                messages = listOf(
                                    ChatMessage(text = firstMsg, isFromUser = false)
                                )
                            )
                            pendingChatSession = newSession
                            coroutineScope.launch { pagerState.animateScrollToPage(3) }
                        },
                        hazeState = hazeState,
                        onNotificationCenterOpenChanged = { isNotificationCenterOpen = it },
                        onMathReferenceOpenChanged = { isMathReferenceOpen = it }
                    )
                    1 -> TimersScreen(
                        appsWithTimers = installedApps.filter { it.timeLimitMinutes > 0 },
                        installedApps = installedApps,
                        onAddTimerClick = { showAppSelectionDialog = true },
                        onEditTimer = { app -> selectedAppForEdit = app },
                        onDeleteTimer = { app -> triggerDeletion(app) },
                        onUnlockTimer = { app -> triggerUnlock(app) },
                        onAutoAddTimers = { presetMap ->
                            var addedCount = 0
                            val now = System.currentTimeMillis()

                            presetMap.forEach { (pkg, limitMinutes) ->
                                if (!AppTimerStore.limits.containsKey(pkg)) {
                                    AppTimerStore.setLimit(
                                        pkg,
                                        AppTimerData(
                                            limitMinutes = limitMinutes,
                                            addedTimestamp = now
                                        )
                                    )
                                    IconCache.invalidate(pkg)
                                    addedCount++
                                }
                            }

                            coroutineScope.launch { refreshApps() }

                            if (addedCount > 0) {
                                Toast.makeText(context, "Автоматически добавлено приложений: $addedCount", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Рекомендуемые приложения уже добавлены или не найдены", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    2 -> SettingsScreen(
                        onSubScreenOpenChanged = { isBottomBarStyleOpen = it }
                    )
                    3 -> ChatScreen(
                        onChatOpenChanged = { isChatOpen = it },
                        pendingSession = pendingChatSession,
                        onPendingSessionConsumed = { pendingChatSession = null }
                    )
                }
            }

            // DailyTaskScreen — поверх всего, плавно въезжает справа
            AnimatedVisibility(
                visible = showDailyTask,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                DailyTaskScreen(
                    onNavigateToChat = { title: String, firstMsg: String, taskLatex: String ->
                        // Если firstMsg — готовая подсказка (отображается как сообщение бота),
                        // иначе — скрытый промт для AI (autoPrompt, бот генерирует сам)
                        val newSession = if (firstMsg.startsWith("**Подсказка") || firstMsg.startsWith("💡")) {
                            ChatSession(
                                id = System.currentTimeMillis().toString(),
                                title = title,
                                messages = listOf(ChatMessage(text = firstMsg, isFromUser = false)),
                                systemContext = taskLatex
                            )
                        } else {
                            ChatSession(
                                id = System.currentTimeMillis().toString(),
                                title = title,
                                messages = emptyList(),
                                systemContext = taskLatex,
                                autoPrompt = firstMsg
                            )
                        }
                        pendingChatSession = newSession
                        showDailyTask = false
                        coroutineScope.launch { pagerState.animateScrollToPage(3) }
                    },
                    onBack = { showDailyTask = false }
                )
            }

            // StatsScreen — подробности статистики при клике с главной
            AnimatedVisibility(
                visible = showStatsDetails,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                StatsScreen(
                    appsWithTimers = installedApps.filter { it.timeLimitMinutes > 0 },
                    onBack = { showStatsDetails = false }
                )
            }

            // DailySummaryScreen — отдельная вкладка итогов дня
            AnimatedVisibility(
                visible = showDailySummary && dailySummaryData != null,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                dailySummaryData?.let { summary ->
                    DailySummaryScreen(
                        summary = summary,
                        onBack = { showDailySummary = false },
                        onDiscussSummary = { s ->
                            val title = "Итоги дня (${s.dateKey})"
                            val firstMsg = """
ИТОГИ ДНЯ (${s.dateKey}):
• Экранное время: ${s.totalScreenMinutes} мин (шортсы/клипы: ${s.scrollingMinutes} мин)
• Задача дня: ${if (s.dailyTaskSolved) "Решена" else "Пропущена"}
• Блицы: ${s.blitzWins} побед, ${s.blitzLosses} поражений
• Изменение репутации: ${if (s.ratingDelta >= 0) "+${s.ratingDelta}" else "${s.ratingDelta}"}
${if (s.coinsAwarded > 0) "• Бонусные монеты: +${s.coinsAwarded}\n" else ""}
СОВЕТ НАСТАВНИКА:
${s.aiRecommendation}

Я готов обсудить ваши результаты дня! Что бы вы хотели улучшить или уточнить?
                            """.trimIndent()

                            val newSession = ChatSession(
                                id = "summary_${System.currentTimeMillis()}",
                                title = title,
                                messages = listOf(
                                    ChatMessage(text = firstMsg, isFromUser = false)
                                )
                            )
                            pendingChatSession = newSession
                            showDailySummary = false
                            coroutineScope.launch { pagerState.animateScrollToPage(3) }
                        }
                    )
                }
            }

            // MathBlitzScreen — полноэкранный режим блица
            AnimatedVisibility(
                visible = showBlitzScreen && activeBlitzSession != null,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                activeBlitzSession?.let { currentSession ->
                    MathBlitzScreen(
                        initialSession = currentSession,
                        onClose = {
                            showBlitzScreen = false
                            val cur = MathBlitzStorage.getActiveSession(context)
                            if (cur != null && !cur.isFinished && cur.deadlineTime > System.currentTimeMillis()) {
                                MathBlitzNotificationManager.showActiveBlitzNotification(context, cur)
                            }
                        },
                        onFinish = {
                            showBlitzScreen = false
                            activeBlitzSession = null
                            MathBlitzNotificationManager.cancelActiveNotification(context)
                            MathBlitzNotificationManager.cancelTimeoutAlarm(context)
                        }
                    )
                }
            }

            // MathBlitzSetupScreen — полноценная страница настройки блица
            AnimatedVisibility(
                visible = showBlitzSetupScreen,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(250)),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(250))
            ) {
                MathBlitzSetupScreen(
                    userCoins = DailyTaskStorage.getCoins(context),
                    onBack = { showBlitzSetupScreen = false },
                    onStartBlitz = { config, tasks ->
                        coroutineScope.launch {
                            DailyTaskStorage.addCoins(context, -config.betCoins)
                            val now = System.currentTimeMillis()
                            val deadline = now + config.durationMinutes * 60 * 1000L
                            val potWin = MathBlitzRepository.calculatePotentialWin(
                                config.betCoins, config.difficulty, config.durationMinutes, config.taskCount
                            )
                            val newSession = BlitzSessionState(
                                id = "blitz_${now}",
                                config = config,
                                startTime = now,
                                deadlineTime = deadline,
                                potentialWinCoins = potWin,
                                tasks = tasks,
                                currentTaskIndex = 0,
                                isFinished = false,
                                isWon = false
                            )
                            MathBlitzStorage.saveActiveSession(context, newSession)
                            MathBlitzNotificationManager.scheduleTimeoutAlarm(context, deadline, config.betCoins)
                            activeBlitzSession = newSession
                            showBlitzSetupScreen = false
                            showBlitzScreen = true
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = !isChatOpen && !showDailyTask && !showStatsDetails && !isBottomBarStyleOpen && !showBlitzScreen && !showBlitzSetupScreen && !showDailySummary && !isNotificationCenterOpen && !isMathReferenceOpen,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(100)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(100)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                GlassBottomNavigationBar(
                    pagerState = pagerState,
                    hazeState = hazeState,
                    onTabSelected = { tabIndex ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(tabIndex)
                        }
                    }
                )
            }

            if (showAppSelectionDialog) {
                val availableApps = installedApps.filter { it.timeLimitMinutes == 0 }
                AppSelectionDialog(
                    apps = availableApps,
                    onDismiss = { showAppSelectionDialog = false },
                    onAppSelect = { app ->
                        showAppSelectionDialog = false
                        selectedAppForTimer = app
                    }
                )
            }

            if (showAiTestDialogFromHome) {
                val top5 = installedApps.sortedByDescending { it.usedMinutesThisWeek }.take(5)
                AiPersonalityTestDialog(
                    top5Apps = top5,
                    onDismiss = { showAiTestDialogFromHome = false },
                    onApplyRecommendations = { recs ->
                        var addedCount = 0
                        val now = System.currentTimeMillis()
                        recs.forEach { (pkg, limitMinutes) ->
                            if (!AppTimerStore.limits.containsKey(pkg)) {
                                AppTimerStore.setLimit(pkg, AppTimerData(limitMinutes = limitMinutes, addedTimestamp = now))
                                IconCache.invalidate(pkg)
                                addedCount++
                            }
                        }
                        coroutineScope.launch { refreshApps() }
                    },
                    onResetTest = {}
                )
            }

            selectedAppForTimer?.let { app ->
                TimerConfigDialog(
                    app = app,
                    maxMinutesAllowed = 24 * 60,
                    isInfiniteWheel = true,
                    onDismiss = { selectedAppForTimer = null },
                    onConfirm = { minutes ->
                        if (minutes > 0) {
                            AppTimerStore.setLimit(
                                app.packageName,
                                AppTimerData(
                                    limitMinutes = minutes,
                                    addedTimestamp = System.currentTimeMillis()
                                )
                            )
                        } else {
                            AppTimerStore.removeLimit(app.packageName)
                        }
                        IconCache.invalidate(app.packageName)
                        coroutineScope.launch { refreshApps() }
                        selectedAppForTimer = null
                    }
                )
            }

            selectedAppForEdit?.let { app ->
                TimerConfigDialog(
                    app = app,
                    maxMinutesAllowed = app.timeLimitMinutes,
                    isInfiniteWheel = false,
                    onDismiss = { selectedAppForEdit = null },
                    onConfirm = { newMinutes ->
                        if (newMinutes > 0) {
                            val existingTimestamp = AppTimerStore.limits[app.packageName]?.addedTimestamp ?: System.currentTimeMillis()
                            AppTimerStore.setLimit(
                                app.packageName,
                                AppTimerData(
                                    limitMinutes = newMinutes,
                                    addedTimestamp = existingTimestamp
                                )
                            )
                        }
                        IconCache.invalidate(app.packageName)
                        coroutineScope.launch { refreshApps() }
                        selectedAppForEdit = null
                    }
                )
            }
        }
    }
}
}