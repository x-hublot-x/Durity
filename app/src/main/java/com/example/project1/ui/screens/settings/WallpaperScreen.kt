package com.example.project1.ui.screens.settings

import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.project1.data.storage.DailyTaskStorage
import com.example.project1.ui.components.CoinIcon
import com.example.project1.ui.theme.AppTheme
import com.example.project1.ui.wallpaper.WallpaperCatalog
import com.example.project1.ui.wallpaper.WallpaperItem
import com.example.project1.ui.wallpaper.WallpaperManager
import com.example.project1.ui.wallpaper.WallpaperType
import com.example.project1.util.VibrationUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun WallpaperScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = AppTheme.colors
    val currentWallpaper = WallpaperManager.currentWallpaper
    val dimOpacity = WallpaperManager.dimOpacity

    var userCoins by remember { mutableIntStateOf(DailyTaskStorage.getCoins(context)) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    var previewItem by remember { mutableStateOf<WallpaperItem?>(null) }

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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val success = WallpaperManager.setCustomWallpaperFromUri(context, uri)
                if (success) {
                    VibrationUtil.vibrateSuccess(context)
                    Toast.makeText(context, "Обои из галереи установлены!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Не удалось загрузить изображение", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    BackHandler {
        if (previewItem != null) {
            previewItem = null
        } else {
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Фиксированная верхняя часть: Заголовок и Табы ───────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Верхняя панель с кнопкой "Назад" и балансом монет
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Обои приложения",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Стили, GIF и фото из галереи",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    // Баланс монет с кастомной иконкой CoinIcon
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF221A0F))
                            .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CoinIcon(size = 16)
                            Text(
                                text = "$userCoins",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Переключатель разделов (Табы с поддержкой смахов)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .padding(4.dp)
                ) {
                    val isStaticSelected = pagerState.currentPage == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (isStaticSelected) colors.primary else Color.Transparent)
                            .clickable {
                                VibrationUtil.vibrateTick(context)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = if (isStaticSelected) Color.White else colors.textSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "Статичные (${WallpaperCatalog.staticPresets.size})",
                                fontSize = 13.sp,
                                fontWeight = if (isStaticSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isStaticSelected) Color.White else colors.textSecondary
                            )
                        }
                    }

                    val isAnimatedSelected = pagerState.currentPage == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (isAnimatedSelected) colors.primary else Color.Transparent)
                            .clickable {
                                VibrationUtil.vibrateTick(context)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isAnimatedSelected) Color.White else colors.textSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "GIF-обои (${WallpaperCatalog.gifPresets.size})",
                                fontSize = 13.sp,
                                fontWeight = if (isAnimatedSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAnimatedSelected) Color.White else colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Свайп-пейджер с разделами обоев ──────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                if (page == 0) {
                    // ── ВКЛАДКА 0: СТАТИЧНЫЕ ОБОИ И ГАЛЕРЕЯ ──────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 120.dp)
                    ) {
                        // Настройка затемнения фона (Dim Opacity)
                        if (currentWallpaper.type != WallpaperType.DEFAULT) {
                            DimOpacitySliderCard(
                                dimOpacity = dimOpacity,
                                onOpacityChange = { WallpaperManager.setDimOpacity(context, it) }
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Карточка загрузки из галереи
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.92f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(colors.primary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = null,
                                                tint = colors.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Своё фото из галереи",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                text = "Поддерживаются фото и арты",
                                                fontSize = 12.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }

                                    if (currentWallpaper.type == WallpaperType.CUSTOM) {
                                        IconButton(
                                            onClick = {
                                                WallpaperManager.removeCustomWallpaper(context)
                                                VibrationUtil.vibrateTick(context)
                                                Toast.makeText(context, "Пользовательские обои удалены", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color.Red.copy(alpha = 0.15f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Удалить",
                                                tint = Color(0xFFFF5252),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.primaryBrush)
                                        .clickable {
                                            VibrationUtil.vibrateTick(context)
                                            galleryLauncher.launch("image/*")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (currentWallpaper.type == WallpaperType.CUSTOM) "Выбрать другое фото" else "Загрузить из галереи",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Карточка "По умолчанию"
                        Card(
                            onClick = {
                                WallpaperManager.setWallpaper(context, WallpaperCatalog.DEFAULT)
                                VibrationUtil.vibrateSuccess(context)
                            },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.92f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (currentWallpaper.id == WallpaperCatalog.DEFAULT.id) {
                                        Modifier.border(2.dp, colors.primary, RoundedCornerShape(18.dp))
                                    } else {
                                        Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF0D0E12))
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    )
                                    Column {
                                        Text(
                                            text = "Стандартный тёмный фон",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "Классический чистый стиль интерфейса",
                                            fontSize = 12.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }

                                if (currentWallpaper.id == WallpaperCatalog.DEFAULT.id) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(colors.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Выбрано",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Сетка из 16 статичных обоев
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            maxItemsInEachRow = 2,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WallpaperCatalog.staticPresets.forEach { item ->
                                WallpaperCardItem(
                                    item = item,
                                    isSelected = currentWallpaper.id == item.id,
                                    isUnlocked = true,
                                    imageLoader = imageLoader,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        previewItem = item
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // ── ВКЛАДКА 1: АНИМИРОВАННЫЕ GIF ОБОИ (12 ШТ) ────────────
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 120.dp)
                    ) {
                        // Настройка затемнения фона (Dim Opacity)
                        if (currentWallpaper.type != WallpaperType.DEFAULT) {
                            DimOpacitySliderCard(
                                dimOpacity = dimOpacity,
                                onOpacityChange = { WallpaperManager.setDimOpacity(context, it) }
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Информационная плашка про GIF-обои
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1728)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFAB47BC).copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF9C27B0).copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFE1BEE7),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Плавные живые обои",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF3E5F5)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Замедленная slow-mo анимация",
                                        fontSize = 12.sp,
                                        color = Color(0xFFCE93D8)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Стоимость: 799",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFE1BEE7)
                                        )
                                        CoinIcon(size = 12)
                                        Text(
                                            text = "за стиль",
                                            fontSize = 12.sp,
                                            color = Color(0xFFCE93D8)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Сетка из 12 анимированных GIF обоев
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            maxItemsInEachRow = 2,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WallpaperCatalog.gifPresets.forEach { item ->
                                val isUnlocked = WallpaperManager.isWallpaperUnlocked(item)
                                WallpaperCardItem(
                                    item = item,
                                    isSelected = currentWallpaper.id == item.id,
                                    isUnlocked = isUnlocked,
                                    imageLoader = imageLoader,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        previewItem = item
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── МОДАЛЬНОЕ ОКНО ПРЕДПРОСМОТРА ОБОЕВ ─────────────────────────────
        previewItem?.let { item ->
            val isUnlocked = WallpaperManager.isWallpaperUnlocked(item)
            val isSelected = currentWallpaper.id == item.id

            WallpaperPreviewDialog(
                item = item,
                isUnlocked = isUnlocked,
                isSelected = isSelected,
                userCoins = userCoins,
                dimOpacity = dimOpacity,
                imageLoader = imageLoader,
                onDismiss = { previewItem = null },
                onApply = {
                    WallpaperManager.setWallpaper(context, item)
                    VibrationUtil.vibrateSuccess(context)
                    Toast.makeText(context, "Обои «${item.title}» применены!", Toast.LENGTH_SHORT).show()
                    previewItem = null
                },
                onUnlockAndApply = {
                    if (userCoins < item.price) {
                        VibrationUtil.vibrateError(context)
                        Toast.makeText(context, "Недостаточно монет! Требуется ${item.price}", Toast.LENGTH_SHORT).show()
                    } else {
                        val success = WallpaperManager.unlockWallpaper(context, item)
                        if (success) {
                            userCoins = DailyTaskStorage.getCoins(context)
                            WallpaperManager.setWallpaper(context, item)
                            VibrationUtil.vibrateSuccess(context)
                            Toast.makeText(context, "Обои «${item.title}» разблокированы!", Toast.LENGTH_SHORT).show()
                            previewItem = null
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun WallpaperCardItem(
    item: WallpaperItem,
    isSelected: Boolean,
    isUnlocked: Boolean,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.92f)),
        modifier = modifier
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, colors.primary, RoundedCornerShape(18.dp))
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                }
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(colors.surfaceElevated)
            ) {
                AsyncImage(
                    model = item.imageModel,
                    imageLoader = imageLoader,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Затемнение снизу превью для текста
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )

                // Бейдж типа (GIF)
                if (item.isAnimated) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "GIF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00D1FF)
                        )
                    }
                }

                // Правый верхний бейдж: Активно / Куплено / Заблокировано (CoinIcon + цена)
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(colors.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Активно",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    } else if (item.isAnimated && !isUnlocked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.82f))
                                .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CoinIcon(size = 12)
                                Text(
                                    text = "${item.price}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD54F)
                                )
                            }
                        }
                    } else if (item.isAnimated && isUnlocked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E7D32).copy(alpha = 0.85f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Куплено",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Кнопка-индикатор предпросмотра по центру снизу
                Box(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Предпросмотр",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WallpaperPreviewDialog(
    item: WallpaperItem,
    isUnlocked: Boolean,
    isSelected: Boolean,
    userCoins: Int,
    dimOpacity: Float,
    imageLoader: ImageLoader,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onUnlockAndApply: () -> Unit
) {
    val colors = AppTheme.colors

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Живые обои на весь экран
            AsyncImage(
                model = item.imageModel,
                imageLoader = imageLoader,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Слой затемнения
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimOpacity))
            )

            // Верхний заголовок и кнопка закрыть
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CoinIcon(size = 16)
                        Text(
                            text = "Баланс: $userCoins",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                    }
                }
            }

            // Нижняя панель действий
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14151B).copy(alpha = 0.94f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.description,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        if (item.isAnimated) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF9C27B0).copy(alpha = 0.35f))
                                    .border(1.dp, Color(0xFFCE93D8).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFE1BEE7),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Живые",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE1BEE7)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF2E7D32).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Эти обои уже установлены",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF81C784)
                                )
                            }
                        }
                    } else if (isUnlocked) {
                        Button(
                            onClick = onApply,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Применить обои",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        val canAfford = userCoins >= item.price
                        Button(
                            onClick = onUnlockAndApply,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canAfford) Color(0xFFFFB300) else Color(0xFF373737)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (canAfford) {
                                    CoinIcon(size = 18)
                                    Text(
                                        text = "Разблокировать за ${item.price}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Недостаточно монет (${item.price})",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    CoinIcon(size = 14)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DimOpacitySliderCard(
    dimOpacity: Float,
    onOpacityChange: (Float) -> Unit
) {
    val colors = AppTheme.colors
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.92f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Затемнение фона",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
                Text(
                    text = "${(dimOpacity * 100).toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = dimOpacity,
                onValueChange = onOpacityChange,
                valueRange = 0.10f..0.85f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.primary,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = colors.surfaceElevated
                )
            )
        }
    }
}