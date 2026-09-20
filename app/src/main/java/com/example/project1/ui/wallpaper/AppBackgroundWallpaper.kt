package com.example.project1.ui.wallpaper

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.project1.ui.theme.AppTheme

@Composable
fun AppBackgroundWallpaper(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val currentWp = WallpaperManager.currentWallpaper
    val dimOpacity = WallpaperManager.dimOpacity

    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        if (currentWp.type != WallpaperType.DEFAULT && currentWp.imageModel != null) {
            AsyncImage(
                model = currentWp.imageModel,
                imageLoader = imageLoader,
                contentDescription = "Фоновые обои",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Полупрозрачный слой затемнения для идеальной читаемости
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimOpacity))
            )
        }

        content()
    }
}