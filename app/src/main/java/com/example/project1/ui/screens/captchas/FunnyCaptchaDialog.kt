package com.example.project1.ui.screens.captchas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.project1.data.repository.FunnyCaptchaRepository
import com.example.project1.ui.components.PrimaryGradientButton

@Composable
fun FunnyCaptchaDialog(
    onDismiss: () -> Unit,
    onSolved: () -> Unit
) {
    val captcha = remember { FunnyCaptchaRepository.getRandomCaptcha() }

    var selected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var error by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .background(Color(0xFF1A1A24), RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4285F4),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    "Докажите, что вы не робот",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "reCAPTCHA, но для лентяев",
                color = Color.White.copy(alpha = .5f),
                fontSize = 11.sp
            )

            Spacer(Modifier.height(18.dp))

            Text(
                captcha.title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(5.dp))

            Text(
                captcha.question,
                color = Color.White.copy(alpha = .7f),
                fontSize = 13.sp
            )

            Spacer(Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(285.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                itemsIndexed(captcha.items) { index, item ->

                    val isSelected = index in selected

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(
                                if (isSelected)
                                    Color(0x334285F4)
                                else
                                    Color(0xFF252533),
                                RoundedCornerShape(5.dp)
                            )
                            .border(
                                if (isSelected) 3.dp else 1.dp,
                                if (isSelected)
                                    Color(0xFF4285F4)
                                else
                                    Color.White.copy(alpha = .08f),
                                RoundedCornerShape(5.dp)
                            )
                            .clickable {
                                selected =
                                    if (isSelected)
                                        selected - index
                                    else
                                        selected + index

                                error = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Кастомизация отображения: Картинка vs Текст
                        when (item) {
                            is Int -> {
                                Image(
                                    painter = painterResource(id = item),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                            else -> {
                                Text(item.toString(), fontSize = 34.sp)
                            }
                        }

                        if (isSelected) {
                            // Затемнение поверх выбранной картинки для стиля Google reCAPTCHA
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x444285F4))
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .background(
                                        Color(0xFF4285F4),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (error) {
                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "CAPTCHA решила, что вы робот.",
                        color = Color(0xFFFF5252),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = Color.White.copy(alpha = .6f))
                }

                PrimaryGradientButton(
                    onClick = {
                        if (selected == captcha.correctIndexes) {
                            onSolved()
                        } else {
                            error = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("Я не робот", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
