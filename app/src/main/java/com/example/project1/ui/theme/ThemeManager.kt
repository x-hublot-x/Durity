package com.example.project1.ui.theme

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.project1.data.storage.DailyTaskStorage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val title: String) {
    LIGHT("Светлая"),
    DARK("Темная"),
    SYSTEM("Системная")
}

enum class AccentTheme(
    val title: String,
    val primary: Color,
    val secondary: Color,
    val subtle: Color,
    val isGradient: Boolean = false
) {
    // ── Линия 1: Классические оттенки ──
    RED(
        title = "Красная",
        primary = Color(0xFFFF5252),
        secondary = Color(0xFFE53935),
        subtle = Color(0x26FF5252),
        isGradient = false
    ),
    ORANGE(
        title = "Оранжевая",
        primary = Color(0xFFFF7043),
        secondary = Color(0xFFF4511E),
        subtle = Color(0x26FF7043),
        isGradient = false
    ),
    YELLOW(
        title = "Желтая",
        primary = Color(0xFFFFB300),
        secondary = Color(0xFFFFA000),
        subtle = Color(0x26FFB300),
        isGradient = false
    ),
    GREEN(
        title = "Зеленая",
        primary = Color(0xFF43A047),
        secondary = Color(0xFF2E7D32),
        subtle = Color(0x2643A047),
        isGradient = false
    ),
    BLUE(
        title = "Синяя",
        primary = Color(0xFF3B82F6),
        secondary = Color(0xFF1D4ED8),
        subtle = Color(0x263B82F6),
        isGradient = false
    ),
    PURPLE(
        title = "Фиолетовая",
        primary = Color(0xFF8B5CF6),
        secondary = Color(0xFF6D28D9),
        subtle = Color(0x268B5CF6),
        isGradient = false
    ),
    PINK(
        title = "Розовая",
        primary = Color(0xFFEC4899),
        secondary = Color(0xFFDB2777),
        subtle = Color(0x26EC4899),
        isGradient = false
    ),

    // ── Линия 2: Альтернативные оттенки ──
    CRIMSON(
        title = "Рубиновая",
        primary = Color(0xFFF43F5E),
        secondary = Color(0xFFBE123C),
        subtle = Color(0x26F43F5E),
        isGradient = false
    ),
    CORAL(
        title = "Коралловая",
        primary = Color(0xFFFB923C),
        secondary = Color(0xFFEA580C),
        subtle = Color(0x26FB923C),
        isGradient = false
    ),
    LIME(
        title = "Лаймовая",
        primary = Color(0xFFA3E635),
        secondary = Color(0xFF65A30D),
        subtle = Color(0x26A3E635),
        isGradient = false
    ),
    MINT(
        title = "Мятная",
        primary = Color(0xFF10B981),
        secondary = Color(0xFF047857),
        subtle = Color(0x2610B981),
        isGradient = false
    ),
    CYAN(
        title = "Лазурная",
        primary = Color(0xFF06B6D4),
        secondary = Color(0xFF0284C7),
        subtle = Color(0x2606B6D4),
        isGradient = false
    ),
    INDIGO(
        title = "Индиго",
        primary = Color(0xFF6366F1),
        secondary = Color(0xFF4338CA),
        subtle = Color(0x266366F1),
        isGradient = false
    ),
    MAGENTA(
        title = "Маджента",
        primary = Color(0xFFD946EF),
        secondary = Color(0xFFA21CAF),
        subtle = Color(0x26D946EF),
        isGradient = false
    ),

    // ── Линия 3: Градиентные дуэты ──
    GRAD_SUNSET(
        title = "Закат",
        primary = Color(0xFFFF3366),
        secondary = Color(0xFFFF9933),
        subtle = Color(0x26FF3366),
        isGradient = true
    ),
    GRAD_CYBERPUNK(
        title = "Киберпанк",
        primary = Color(0xFF00F0FF),
        secondary = Color(0xFFE024C3),
        subtle = Color(0x2600F0FF),
        isGradient = true
    ),
    GRAD_AURORA(
        title = "Сияние",
        primary = Color(0xFF00F5A0),
        secondary = Color(0xFF00D9F5),
        subtle = Color(0x2600F5A0),
        isGradient = true
    ),
    GRAD_LAVA(
        title = "Магма",
        primary = Color(0xFFFF7A00),
        secondary = Color(0xFFFF0055),
        subtle = Color(0x26FF7A00),
        isGradient = true
    ),
    GRAD_COSMIC(
        title = "Космос",
        primary = Color(0xFFA855F7),
        secondary = Color(0xFF3B82F6),
        subtle = Color(0x26A855F7),
        isGradient = true
    ),
    GRAD_OCEAN(
        title = "Океан",
        primary = Color(0xFF2563EB),
        secondary = Color(0xFF06B6D4),
        subtle = Color(0x262563EB),
        isGradient = true
    ),
    GRAD_BERRY(
        title = "Ягода",
        primary = Color(0xFFF72585),
        secondary = Color(0xFF7209B7),
        subtle = Color(0x26F72585),
        isGradient = true
    );

    companion object {
        val row1 = listOf(RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE, PINK)
        val row2 = listOf(CRIMSON, CORAL, LIME, MINT, CYAN, INDIGO, MAGENTA)
        val row3 = listOf(GRAD_SUNSET, GRAD_CYBERPUNK, GRAD_AURORA, GRAD_LAVA, GRAD_COSMIC, GRAD_OCEAN, GRAD_BERRY)
    }
}

data class AppColors(
    val isDark: Boolean,
    val primary: Color,
    val secondary: Color,
    val primarySubtle: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val bottomBarBackground: Color,
    val bottomBarBorder: Color,
    val isGradient: Boolean = false,
    val primaryBrush: Brush = if (isGradient) Brush.horizontalGradient(listOf(primary, secondary)) else Brush.linearGradient(listOf(primary, primary)),
    val gradientBrush: Brush = Brush.horizontalGradient(listOf(primary, secondary))
)

val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        isDark = true,
        primary = AccentTheme.RED.primary,
        secondary = AccentTheme.RED.secondary,
        primarySubtle = AccentTheme.RED.subtle,
        background = Color(0xFF0F0F14),
        surface = Color(0xFF1A1A24),
        surfaceElevated = Color(0xFF252533),
        surfaceBorder = Color.White.copy(alpha = 0.12f),
        textPrimary = Color.White,
        textSecondary = Color.White.copy(alpha = 0.65f),
        textTertiary = Color.White.copy(alpha = 0.4f),
        bottomBarBackground = Color(0xCC1A1A24),
        bottomBarBorder = Color.White.copy(alpha = 0.15f)
    )
}

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val accent: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.primary
}

enum class BottomBarStyle(
    val id: String,
    val title: String,
    val description: String,
    val drawableRes: Int? = null,
    val isAnimated: Boolean = false
) {
    DEFAULT("default", "Стандартный", "Матовое стекло с эффектом воды", null),
    NIGHT("night", "Ночь", "Светлячки и ночная трава", com.example.project1.R.drawable.bg_bottom_bar_night),
    TWILIGHT("twilight", "Сумерки", "Фиолетовая космическая дымка", com.example.project1.R.drawable.bg_bottom_bar_twilight),
    AURORA("aurora", "Сияние", "Бирюзовые звёзды и сияние", com.example.project1.R.drawable.bg_bottom_bar_aurora),
    SAKURA("sakura", "Сакура", "Парящие лепестки в ночи", com.example.project1.R.drawable.bg_bottom_bar_sakura),
    SUNSET("sunset", "Закат", "Теплый сумеречный горизонт", com.example.project1.R.drawable.bg_bottom_bar_sunset),
    NEON("neon", "Неон", "Киберпанк и неоновые огни", com.example.project1.R.drawable.bg_bottom_bar_neon),
    OCEAN("ocean", "Океан", "Биолюминесценция глубин", com.example.project1.R.drawable.bg_bottom_bar_ocean),
    FOREST("forest", "Изумруд", "Туманный хвойный лес", com.example.project1.R.drawable.bg_bottom_bar_forest),
    GALAXY("galaxy", "Галактика", "Спиральные рукава космоса", com.example.project1.R.drawable.bg_bottom_bar_galaxy),
    LAVA("lava", "Магма", "Огненные разломы базальта", com.example.project1.R.drawable.bg_bottom_bar_lava),
    RETROWAVE("retrowave", "Ретровейв", "Синтвейв закат 80-х", com.example.project1.R.drawable.bg_bottom_bar_retrowave),
    DESERT("desert", "Оазис", "Ночные песчаные дюны", com.example.project1.R.drawable.bg_bottom_bar_desert),
    RAIN("rain", "Дождь", "Огни ночного города", com.example.project1.R.drawable.bg_bottom_bar_rain),
    CRYSTAL("crystal", "Кристалл", "Ледяные грани аметиста", com.example.project1.R.drawable.bg_bottom_bar_crystal),
    GOLD("gold", "Золотой час", "Темный мрамор и золото", com.example.project1.R.drawable.bg_bottom_bar_gold),
    RIVER("river", "Речная вода", "Анимация: текущая вода и волны", com.example.project1.R.drawable.bg_bottom_bar_river, isAnimated = true),
    PLANTS("plants", "Кустарники и листва", "Анимация: пышные листья и ветви на ветру", com.example.project1.R.drawable.bg_bottom_bar_plants, isAnimated = true),
    METEORS("meteors", "Звездопад", "Анимация: падающие метеоры в ночи", com.example.project1.R.drawable.bg_bottom_bar_meteors, isAnimated = true),
    SATURN("saturn", "Сатурн", "Анимация: вращение колец планеты", com.example.project1.R.drawable.bg_bottom_bar_saturn, isAnimated = true),
    FIRE("fire", "Огонёк", "Анимация: пляшущее пламя и искры", com.example.project1.R.drawable.bg_bottom_bar_fire, isAnimated = true),
    ANTS("ants", "Муравьи", "Анимация: марш муравьев по кромке", com.example.project1.R.drawable.bg_bottom_bar_ants, isAnimated = true);

    val price: Int
        get() = when {
            this == DEFAULT -> 0
            isAnimated -> 1990
            else -> 679
        }
}

object ThemeManager {
    private const val PREFS = "theme_preferences"
    private const val KEY_MODE = "theme_mode"
    private const val KEY_ACCENT = "accent_theme"
    private const val KEY_BOTTOM_BAR_STYLE = "bottom_bar_style"
    private const val KEY_UNLOCKED_BAR_STYLES = "unlocked_bar_styles"

    var currentMode by mutableStateOf(ThemeMode.DARK)
        private set

    var currentAccent by mutableStateOf(AccentTheme.RED)
        private set

    var currentBottomBarStyle by mutableStateOf(BottomBarStyle.DEFAULT)
        private set

    var unlockedBarStyles by mutableStateOf<Set<String>>(setOf(BottomBarStyle.DEFAULT.id))
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val modeStr = prefs.getString(KEY_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val accentStr = prefs.getString(KEY_ACCENT, AccentTheme.RED.name) ?: AccentTheme.RED.name
        val barStyleStr = prefs.getString(KEY_BOTTOM_BAR_STYLE, BottomBarStyle.DEFAULT.name) ?: BottomBarStyle.DEFAULT.name
        val savedUnlocked = prefs.getStringSet(KEY_UNLOCKED_BAR_STYLES, null)
        unlockedBarStyles = if (savedUnlocked != null) {
            savedUnlocked + BottomBarStyle.DEFAULT.id
        } else {
            setOf(BottomBarStyle.DEFAULT.id)
        }

        currentMode = try {
            ThemeMode.valueOf(modeStr)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
        currentAccent = try {
            AccentTheme.valueOf(accentStr)
        } catch (e: Exception) {
            AccentTheme.RED
        }
        currentBottomBarStyle = try {
            val style = BottomBarStyle.valueOf(barStyleStr)
            if (isStyleUnlocked(style)) style else BottomBarStyle.DEFAULT
        } catch (e: Exception) {
            BottomBarStyle.DEFAULT
        }
    }

    private var pendingIconAccent: AccentTheme? = null

    fun isStyleUnlocked(style: BottomBarStyle): Boolean {
        if (style == BottomBarStyle.DEFAULT) return true
        return unlockedBarStyles.contains(style.id)
    }

    fun unlockStyle(context: Context, style: BottomBarStyle): Boolean {
        if (isStyleUnlocked(style)) return true
        val price = style.price
        val currentCoins = DailyTaskStorage.getCoins(context)
        if (currentCoins < price) return false

        DailyTaskStorage.addCoins(context, -price)
        val newSet = unlockedBarStyles + style.id
        unlockedBarStyles = newSet
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_UNLOCKED_BAR_STYLES, newSet)
            .apply()
        return true
    }

    fun setMode(context: Context, mode: ThemeMode) {
        currentMode = mode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_MODE, mode.name).apply()
    }

    fun setAccent(context: Context, accent: AccentTheme) {
        currentAccent = accent
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ACCENT, accent.name).apply()
        pendingIconAccent = accent
    }

    fun applyPendingAppIcon(context: Context) {
        val accent = pendingIconAccent ?: return
        pendingIconAccent = null
        updateAppIcon(context.applicationContext, accent)
    }

    fun setBottomBarStyle(context: Context, style: BottomBarStyle) {
        if (!isStyleUnlocked(style)) return
        currentBottomBarStyle = style
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_BOTTOM_BAR_STYLE, style.name).apply()
    }

    private fun updateAppIcon(context: Context, accent: AccentTheme) {
        try {
            val pm = context.packageManager
            val packageName = context.packageName

            val aliasMap = mapOf(
                AccentTheme.RED to "$packageName.MainActivityAliasRed",
                AccentTheme.ORANGE to "$packageName.MainActivityAliasOrange",
                AccentTheme.YELLOW to "$packageName.MainActivityAliasYellow",
                AccentTheme.GREEN to "$packageName.MainActivityAliasGreen",
                AccentTheme.BLUE to "$packageName.MainActivityAliasBlue",
                AccentTheme.PURPLE to "$packageName.MainActivityAliasPurple",
                AccentTheme.PINK to "$packageName.MainActivityAliasPink",
                AccentTheme.CRIMSON to "$packageName.MainActivityAliasCrimson",
                AccentTheme.CORAL to "$packageName.MainActivityAliasCoral",
                AccentTheme.LIME to "$packageName.MainActivityAliasLime",
                AccentTheme.MINT to "$packageName.MainActivityAliasMint",
                AccentTheme.CYAN to "$packageName.MainActivityAliasCyan",
                AccentTheme.INDIGO to "$packageName.MainActivityAliasIndigo",
                AccentTheme.MAGENTA to "$packageName.MainActivityAliasMagenta",
                AccentTheme.GRAD_SUNSET to "$packageName.MainActivityAliasGradSunset",
                AccentTheme.GRAD_CYBERPUNK to "$packageName.MainActivityAliasGradCyberpunk",
                AccentTheme.GRAD_AURORA to "$packageName.MainActivityAliasGradAurora",
                AccentTheme.GRAD_LAVA to "$packageName.MainActivityAliasGradLava",
                AccentTheme.GRAD_COSMIC to "$packageName.MainActivityAliasGradCosmic",
                AccentTheme.GRAD_OCEAN to "$packageName.MainActivityAliasGradOcean",
                AccentTheme.GRAD_BERRY to "$packageName.MainActivityAliasGradBerry"
            )

            val targetAlias = aliasMap[accent] ?: return
            val targetComponent = ComponentName(context, targetAlias)

            // 1. Сначала включаем целевой компонент-алиас
            val currentSetting = pm.getComponentEnabledSetting(targetComponent)
            if (currentSetting != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                pm.setComponentEnabledSetting(
                    targetComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            // 2. Отключаем все остальные алиасы
            aliasMap.values.forEach { aliasName ->
                if (aliasName != targetAlias) {
                    val comp = ComponentName(context, aliasName)
                    if (pm.getComponentEnabledSetting(comp) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        pm.setComponentEnabledSetting(
                            comp,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
