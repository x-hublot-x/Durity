package com.example.project1.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project1.util.triggerTickVibration

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    isInfinite: Boolean,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val itemHeight = 44.dp
    val visibleItemsCount = 3
    val count = items.size

    val safeInitialIndex = initialIndex.coerceIn(0, (count - 1).coerceAtLeast(0))

    val totalItems = if (isInfinite && count > 0) 10_000 * count else count
    val startIndex = if (isInfinite && count > 0) {
        (totalItems / 2) - ((totalItems / 2) % count) + safeInitialIndex
    } else {
        safeInitialIndex
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val currentIndex by remember {
        derivedStateOf {
            if (count > 0) {
                if (isInfinite) listState.firstVisibleItemIndex % count else listState.firstVisibleItemIndex.coerceIn(0, count - 1)
            } else 0
        }
    }

    var isInitialLoad by remember { mutableStateOf(true) }

    LaunchedEffect(currentIndex) {
        if (isInitialLoad) {
            isInitialLoad = false
        } else {
            triggerTickVibration(context)
        }
        onItemSelected(currentIndex)
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .width(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(com.example.project1.ui.theme.AppTheme.colors.primarySubtle)
                .border(1.dp, com.example.project1.ui.theme.AppTheme.accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            items(totalItems) { index ->
                val actualIndex = if (isInfinite && count > 0) index % count else index
                val isSelected = remember {
                    derivedStateOf { listState.firstVisibleItemIndex == index }
                }

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items.getOrElse(actualIndex) { "00" },
                        fontSize = if (isSelected.value) 22.sp else 16.sp,
                        fontWeight = if (isSelected.value) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected.value) com.example.project1.ui.theme.AppTheme.accent else Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
