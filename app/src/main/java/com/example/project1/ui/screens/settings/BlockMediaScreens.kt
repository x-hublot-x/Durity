package com.example.project1.ui.screens.settings

import androidx.activity.compose.BackHandler
import android.media.MediaPlayer
import android.os.Build.VERSION.SDK_INT
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.project1.data.storage.BlockGif
import com.example.project1.data.storage.BlockMediaManager
import com.example.project1.data.storage.BlockSound
import com.example.project1.ui.theme.AppTheme
import com.example.project1.util.VibrationUtil

@Composable
fun BlockGifScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val currentGif = BlockMediaManager.currentGif

    BackHandler {
        onBack()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── Top bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    VibrationUtil.vibrateTick(context)
                    onBack()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "Анимация блокировки",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Гифка при попытке зайти в заблокированное приложение",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp)
        ) {
            items(BlockGif.entries, key = { it.id }) { gif ->
                val isSelected = gif == currentGif
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) colors.primary else Color.White.copy(alpha = 0.08f),
                    animationSpec = tween(200),
                    label = "GifBorder"
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) colors.surfaceElevated else colors.surface.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            VibrationUtil.vibrateTick(context)
                            BlockMediaManager.setGif(context, gif)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // GIF Thumbnail
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF14141E))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = gif.rawRes,
                                imageLoader = imageLoader,
                                contentDescription = gif.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = gif.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = gif.description,
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Selection indicator
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isSelected) {
                                        if (colors.isGradient) Modifier.background(colors.primaryBrush)
                                        else Modifier.background(colors.primary)
                                    } else {
                                        Modifier
                                            .background(Color.Transparent)
                                            .border(1.5.dp, colors.textSecondary.copy(alpha = 0.4f), CircleShape)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlockSoundScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val currentSound = BlockMediaManager.currentSound
    var playingSoundId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    BackHandler {
        mediaPlayer?.release()
        mediaPlayer = null
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun playSound(rawRes: Int, soundId: String) {
        mediaPlayer?.release()
        try {
            val mp = MediaPlayer.create(context, rawRes)
            if (mp != null) {
                playingSoundId = soundId
                mediaPlayer = mp
                mp.setOnCompletionListener {
                    playingSoundId = null
                    it.release()
                    mediaPlayer = null
                }
                mp.start()
            }
        } catch (_: Exception) {
            playingSoundId = null
        }
    }

    fun stopSound() {
        mediaPlayer?.release()
        mediaPlayer = null
        playingSoundId = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── Top bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    stopSound()
                    VibrationUtil.vibrateTick(context)
                    onBack()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "Звук блокировки",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Звуковой эффект при попытке открыть приложение",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp)
        ) {
            items(BlockSound.entries, key = { it.id }) { sound ->
                val isSelected = sound == currentSound
                val isPlaying = playingSoundId == sound.id
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) colors.primary else Color.White.copy(alpha = 0.08f),
                    animationSpec = tween(200),
                    label = "SoundBorder"
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) colors.surfaceElevated else colors.surface.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            VibrationUtil.vibrateTick(context)
                            BlockMediaManager.setSound(context, sound)
                            playSound(sound.rawRes, sound.id)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play / Stop preview button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPlaying) {
                                        if (colors.isGradient) colors.primaryBrush else Brush.linearGradient(listOf(colors.primary, colors.primary))
                                    } else {
                                        Brush.linearGradient(listOf(colors.surface, colors.surface))
                                    }
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                .clickable {
                                    if (isPlaying) {
                                        stopSound()
                                    } else {
                                        playSound(sound.rawRes, sound.id)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = "Прослушать",
                                tint = if (isPlaying) Color.White else colors.textPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sound.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = sound.description,
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Selection indicator
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isSelected) {
                                        if (colors.isGradient) Modifier.background(colors.primaryBrush)
                                        else Modifier.background(colors.primary)
                                    } else {
                                        Modifier
                                            .background(Color.Transparent)
                                            .border(1.5.dp, colors.textSecondary.copy(alpha = 0.4f), CircleShape)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
