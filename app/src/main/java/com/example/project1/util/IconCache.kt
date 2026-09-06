package com.example.project1.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.example.project1.R

// Кэш иконок с фиксированной загрузкой
object IconCache {
    private val rawIconCache = mutableMapOf<String, Bitmap>()
    private val frozenIconCache = mutableMapOf<String, Drawable>()
    private val normalIconCache = mutableMapOf<String, Drawable>()

    fun getIcon(context: Context, packageName: String, rawDrawable: Drawable, isFrozen: Boolean): Drawable {
        val cacheKey = packageName
        return if (isFrozen) {
            frozenIconCache.getOrPut(cacheKey) {
                val rawBitmap = rawIconCache.getOrPut(cacheKey) {
                    getHighResAppIcon(context, rawDrawable)
                }
                applyFrozenEffect(context, rawBitmap)
            }
        } else {
            normalIconCache.getOrPut(cacheKey) {
                val rawBitmap = rawIconCache.getOrPut(cacheKey) {
                    getHighResAppIcon(context, rawDrawable)
                }
                rawBitmap.toDrawable(context.resources)
            }
        }
    }

    fun invalidate(packageName: String) {
        frozenIconCache.remove(packageName)
        normalIconCache.remove(packageName)
    }

    fun clear() {
        rawIconCache.clear()
        frozenIconCache.clear()
        normalIconCache.clear()
    }
}

fun shortsLogoDrawable(context: Context): Drawable {
    return ContextCompat.getDrawable(context, R.drawable.ic_youtube_shorts)
        ?: ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)!!
}

fun reelsLogoDrawable(context: Context): Drawable {
    return ContextCompat.getDrawable(context, R.drawable.ic_instagram_reels)
        ?: ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)!!
}

fun vkClipsLogoDrawable(context: Context): Drawable {
    return ContextCompat.getDrawable(context, R.drawable.ic_vk_clips)
        ?: ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)!!
}

fun twitchClipsLogoDrawable(context: Context): Drawable {
    return ContextCompat.getDrawable(context, R.drawable.ic_twitch_clips)
        ?: ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)!!
}

fun getHighResAppIcon(context: Context, rawIcon: Drawable): Bitmap {
    val size = 128
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    rawIcon.setBounds(0, 0, canvas.width, canvas.height)
    rawIcon.draw(canvas)
    return bitmap
}

fun applyFrozenEffect(context: Context, originalBitmap: Bitmap): Drawable {
    val frozenBitmap = createBitmap(originalBitmap.width, originalBitmap.height)
    val canvas = Canvas(frozenBitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val colorMatrix = ColorMatrix().apply {
        setSaturation(0.1f)
    }
    val blueTintMatrix = ColorMatrix(
        floatArrayOf(
            0.6f, 0f, 0f, 0f, 0f,
            0f, 0.8f, 0f, 0f, 0f,
            0f, 0f, 1.3f, 0f, 50f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    colorMatrix.postConcat(blueTintMatrix)
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

    canvas.drawBitmap(originalBitmap, 0f, 0f, paint)

    val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#6680D8FF".toColorInt()
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }
    canvas.drawRect(0f, 0f, originalBitmap.width.toFloat(), originalBitmap.height.toFloat(), overlayPaint)

    return frozenBitmap.toDrawable(context.resources)
}
