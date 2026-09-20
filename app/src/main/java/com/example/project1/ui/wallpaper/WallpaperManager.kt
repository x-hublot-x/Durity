package com.example.project1.ui.wallpaper

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

import com.example.project1.data.storage.DailyTaskStorage

object WallpaperManager {
    private const val PREFS = "wallpaper_prefs"
    private const val KEY_WALLPAPER_ID = "current_wallpaper_id"
    private const val KEY_CUSTOM_PATH = "custom_wallpaper_path"
    private const val KEY_DIM_OPACITY = "wallpaper_dim_opacity"
    private const val KEY_UNLOCKED_WALLPAPERS = "unlocked_wallpapers"

    var currentWallpaper by mutableStateOf<WallpaperItem>(WallpaperCatalog.DEFAULT)
        private set

    var dimOpacity by mutableFloatStateOf(0.40f)
        private set

    var unlockedWallpapers by mutableStateOf<Set<String>>(emptySet())
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_WALLPAPER_ID, WallpaperCatalog.DEFAULT.id) ?: WallpaperCatalog.DEFAULT.id
        val customPath = prefs.getString(KEY_CUSTOM_PATH, null)
        dimOpacity = prefs.getFloat(KEY_DIM_OPACITY, 0.40f)
        unlockedWallpapers = prefs.getStringSet(KEY_UNLOCKED_WALLPAPERS, emptySet()) ?: emptySet()

        currentWallpaper = WallpaperCatalog.findById(id, customPath)
    }

    fun isWallpaperUnlocked(item: WallpaperItem): Boolean {
        if (item.type != WallpaperType.GIF_PRESET) return true
        return unlockedWallpapers.contains(item.id)
    }

    fun unlockWallpaper(context: Context, item: WallpaperItem): Boolean {
        if (isWallpaperUnlocked(item)) return true
        val price = item.price
        val currentCoins = DailyTaskStorage.getCoins(context)
        if (currentCoins < price) return false

        DailyTaskStorage.addCoins(context, -price)
        val newSet = unlockedWallpapers + item.id
        unlockedWallpapers = newSet
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_UNLOCKED_WALLPAPERS, newSet)
            .apply()
        return true
    }

    fun setWallpaper(context: Context, item: WallpaperItem) {
        if (!isWallpaperUnlocked(item)) return
        currentWallpaper = item
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WALLPAPER_ID, item.id)
            .apply()
    }

    fun setDimOpacity(context: Context, opacity: Float) {
        val clamped = opacity.coerceIn(0.10f, 0.85f)
        dimOpacity = clamped
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_DIM_OPACITY, clamped)
            .apply()
    }

    suspend fun setCustomWallpaperFromUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            val ext = if (mimeType.contains("gif", ignoreCase = true)) "gif" else "jpg"

            val dir = File(context.filesDir, "wallpapers")
            if (!dir.exists()) dir.mkdirs()

            // Delete existing custom wallpapers in dir
            dir.listFiles()?.forEach { it.delete() }

            val destFile = File(dir, "custom_wallpaper_${System.currentTimeMillis()}.$ext")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val newItem = WallpaperItem(
                id = "custom",
                title = "Моё фото",
                description = "Пользовательское изображение из галереи",
                type = WallpaperType.CUSTOM,
                customFilePath = destFile.absolutePath
            )

            withContext(Dispatchers.Main) {
                currentWallpaper = newItem
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_WALLPAPER_ID, "custom")
                    .putString(KEY_CUSTOM_PATH, destFile.absolutePath)
                    .apply()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun removeCustomWallpaper(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val path = prefs.getString(KEY_CUSTOM_PATH, null)
        if (path != null) {
            try { File(path).delete() } catch (_: Exception) {}
        }
        prefs.edit().remove(KEY_CUSTOM_PATH).apply()
        if (currentWallpaper.type == WallpaperType.CUSTOM) {
            setWallpaper(context, WallpaperCatalog.DEFAULT)
        }
    }
}