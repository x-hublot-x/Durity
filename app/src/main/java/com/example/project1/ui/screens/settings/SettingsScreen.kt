package com.example.project1.ui.screens.settings

import android.os.Build.VERSION.SDK_INT
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.project1.R
import com.example.project1.data.storage.BlockMediaManager
import com.example.project1.ui.theme.AccentTheme
import com.example.project1.ui.theme.AppTheme
import com.example.project1.ui.theme.BottomBarStyle
import com.example.project1.ui.theme.ThemeManager
import com.example.project1.ui.wallpaper.WallpaperCatalog
import com.example.project1.ui.wallpaper.WallpaperManager
import com.example.project1.ui.wallpaper.WallpaperType
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.VolumeUp

enum class SettingsSubScreen {
    MAIN,
    ACCENT_COLOR,
    BOTTOM_BAR,
    WALLPAPERS,
    BLOCK_GIF,
    BLOCK_SOUND
}

@Composable
fun SettingsScreen(
    onSubScreenOpenChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val currentAccent = ThemeManager.currentAccent
    val currentBarStyle = ThemeManager.currentBottomBarStyle
    val currentWallpaper = WallpaperManager.currentWallpaper
    val currentBlockGif = BlockMediaManager.currentGif
    val currentBlockSound = BlockMediaManager.currentSound
    val colors = AppTheme.colors

    var currentSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }

    BackHandler(enabled = currentSubScreen != SettingsSubScreen.MAIN) {
        currentSubScreen = SettingsSubScreen.MAIN
    }

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

    DisposableEffect(currentSubScreen) {
        val isOpen = currentSubScreen != SettingsSubScreen.MAIN
        onSubScreenOpenChanged(isOpen)
        onDispose {
            if (isOpen) {
                onSubScreenOpenChanged(false)
            }
        }
    }

    AnimatedContent(
        targetState = currentSubScreen,
        transitionSpec = {
            if (targetState != SettingsSubScreen.MAIN) {
                (slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(250)))
                    .togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 3 },
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(200))
                    )
            } else {
                (slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth / 3 },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(250)))
                    .togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(200))
                    )
            }
        },
        label = "SettingsSubScreenTransition"
    ) { subScreen ->
        when (subScreen) {
            SettingsSubScreen.ACCENT_COLOR -> {
                AccentColorScreen(
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN }
                )
            }
            SettingsSubScreen.BOTTOM_BAR -> {
                BottomBarStyleScreen(
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN }
                )
            }
            SettingsSubScreen.WALLPAPERS -> {
                WallpaperScreen(
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN }
                )
            }
            SettingsSubScreen.BLOCK_GIF -> {
                BlockGifScreen(
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN }
                )
            }
            SettingsSubScreen.BLOCK_SOUND -> {
                BlockSoundScreen(
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN }
                )
            }
            SettingsSubScreen.MAIN -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 120.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Настройки",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Text(
                        text = "Внешний вид и персонализация",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── 1. Раздел: Цвет интерфейса ────────────────────────────
                    Card(
                        onClick = { currentSubScreen = SettingsSubScreen.ACCENT_COLOR },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.92f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Превью иконки с цветом
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(currentAccent.primary, currentAccent.secondary)
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.mipmap.ic_launcher_adaptive_fore),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Цвет интерфейса",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currentAccent.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.primary
                                        )
                                        Text(
                                            text = " • 21 стиль и градиенты",
                                            fontSize = 13.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Открыть",
                                tint = colors.textSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(180f)
                                    .padding(start = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── 2. Раздел: Стиль нижней панели ─────────────────────────
                    Card(
                        onClick = { currentSubScreen = SettingsSubScreen.BOTTOM_BAR },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.92f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Превью текущего стиля
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.surfaceElevated)
                                ) {
                                    if (currentBarStyle.drawableRes != null) {
                                        Image(
                                            painter = painterResource(id = currentBarStyle.drawableRes),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.35f))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color(0xFF28344A), Color(0xFF141828))
                                                    )
                                                )
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .align(Alignment.Center)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.22f))
                                            .border(0.8.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Стиль нижней панели",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currentBarStyle.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.primary
                                        )
                                        Text(
                                            text = " • ${BottomBarStyle.entries.size} тем",
                                            fontSize = 13.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Открыть",
                                tint = colors.textSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(180f)
                                    .padding(start = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── 3. Раздел: Обои приложения ─────────────────────────────
                    Card(
                        onClick = { currentSubScreen = SettingsSubScreen.WALLPAPERS },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.92f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Превью текущих обоев
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.surfaceElevated)
                                ) {
                                    if (currentWallpaper.type != WallpaperType.DEFAULT && currentWallpaper.imageModel != null) {
                                        AsyncImage(
                                            model = currentWallpaper.imageModel,
                                            imageLoader = imageLoader,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.25f))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF14141E)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Wallpaper,
                                                contentDescription = null,
                                                tint = colors.textSecondary.copy(alpha = 0.6f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Обои приложения",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentWallpaper.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "16 стилей • 12 GIF • Галерея",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Открыть",
                                tint = colors.textSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(180f)
                                    .padding(start = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── 4. Раздел: Анимация блокировки (GIF) ─────────────────────
                    Card(
                        onClick = { currentSubScreen = SettingsSubScreen.BLOCK_GIF },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.92f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF14141E))
                                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = currentBlockGif.rawRes,
                                        imageLoader = imageLoader,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Анимация блокировки",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentBlockGif.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "7 GIF-мемов при попытке входа",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Открыть",
                                tint = colors.textSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(180f)
                                    .padding(start = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── 5. Раздел: Звук блокировки (Sound) ───────────────────────
                    Card(
                        onClick = { currentSubScreen = SettingsSubScreen.BLOCK_SOUND },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.92f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF283248), Color(0xFF1A1F2C))
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VolumeUp,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Звук блокировки",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentBlockSound.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "11 звуковых эффектов",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Открыть",
                                tint = colors.textSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(180f)
                                    .padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}