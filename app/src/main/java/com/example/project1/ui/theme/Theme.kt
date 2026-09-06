package com.example.project1.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun Project1Theme(
    content: @Composable () -> Unit
) {
    val accent = ThemeManager.currentAccent

    val appColors = AppColors(
        isDark = true,
        primary = accent.primary,
        secondary = accent.secondary,
        primarySubtle = accent.subtle,
        background = Color(0xFF0F0F14),
        surface = Color(0xFF1A1A24),
        surfaceElevated = Color(0xFF252533),
        surfaceBorder = Color.White.copy(alpha = 0.12f),
        textPrimary = Color.White,
        textSecondary = Color.White.copy(alpha = 0.65f),
        textTertiary = Color.White.copy(alpha = 0.4f),
        bottomBarBackground = Color(0x35121626),
        bottomBarBorder = Color.White.copy(alpha = 0.18f),
        gradientBrush = Brush.horizontalGradient(listOf(accent.primary, accent.secondary))
    )

    val colorScheme = darkColorScheme(
        primary = accent.primary,
        secondary = accent.secondary,
        background = appColors.background,
        surface = appColors.surface,
        onPrimary = Color.White,
        onBackground = appColors.textPrimary,
        onSurface = appColors.textPrimary
    )

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}