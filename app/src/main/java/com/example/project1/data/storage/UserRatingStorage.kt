package com.example.project1.data.storage

import android.content.Context
import androidx.compose.ui.graphics.Color

object UserRatingStorage {
    private const val PREFS = "user_reputation_prefs"
    private const val KEY_RATING = "user_rating"
    const val INITIAL_RATING = 100
    const val MIN_RATING = -100
    const val SCALE_MIN = -100
    const val SCALE_MAX = 200

    fun getRating(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_RATING, INITIAL_RATING)
    }

    fun setRating(context: Context, value: Int) {
        val clamped = maxOf(MIN_RATING, value)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_RATING, clamped).apply()
    }

    fun addRating(context: Context, delta: Int): Int {
        val current = getRating(context)
        val updated = maxOf(MIN_RATING, current + delta)
        setRating(context, updated)
        return updated
    }

    fun isBlitzBlocked(context: Context): Boolean {
        return getRating(context) <= MIN_RATING
    }

    /** Прогресс для шкалы от -100 (0.0) до 200 (1.0) */
    fun getRatingProgress(rating: Int): Float {
        val span = (SCALE_MAX - SCALE_MIN).toFloat()
        return ((rating - SCALE_MIN) / span).coerceIn(0f, 1f)
    }

    /** Цвет шкалы в зависимости от значения:
     * < 0: Красный (с градиентом к оранжевому около 0)
     * 0..99: Жёлтый (с градиентом к салатовому)
     * >= 100: Зелёный (изумрудный)
     */
    fun getRatingColor(rating: Int): Color {
        return when {
            rating < 0 -> {
                // Плавный переход от чистого красного (-100) к теплому оранжево-красному (0)
                val t = ((rating - (-100)) / 100f).coerceIn(0f, 1f)
                Color(
                    red = 1f,
                    green = 0.2f + 0.35f * t,
                    blue = 0.25f * (1f - t)
                )
            }
            rating < 100 -> {
                // Плавный переход от оранжево-жёлтого к золотисто-жёлтому
                val t = (rating / 100f).coerceIn(0f, 1f)
                Color(
                    red = 1f - 0.15f * t,
                    green = 0.75f + 0.15f * t,
                    blue = 0.1f
                )
            }
            else -> {
                // Зелёный (изумрудный)
                val t = minOf(1f, (rating - 100) / 100f)
                Color(
                    red = 0.2f * (1f - t),
                    green = 0.85f + 0.15f * t,
                    blue = 0.4f + 0.3f * t
                )
            }
        }
    }

    fun getRatingStatusTitle(rating: Int): String {
        return when {
            rating <= MIN_RATING -> "Блиц заблокирован"
            rating < 0 -> "Критическая репутация"
            rating < 100 -> "Удовлетворительно"
            rating < 150 -> "Хорошая репутация"
            else -> "Безупречная репутация"
        }
    }
}