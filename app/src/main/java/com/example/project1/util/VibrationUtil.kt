package com.example.project1.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

object VibrationUtil {
    @SuppressLint("MissingPermission")
    fun vibrateTick(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(12)
                    }
                }
            }
        } catch (_: Exception) { }
    }

    @SuppressLint("MissingPermission")
    fun vibrateClick(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(22)
                    }
                }
            }
        } catch (_: Exception) { }
    }

    @SuppressLint("MissingPermission")
    fun vibrateLongPress(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(45)
                    }
                }
            }
        } catch (_: Exception) { }
    }

    @SuppressLint("MissingPermission")
    fun vibrateHeavy(context: Context) {
        vibrateLongPress(context)
    }

    @SuppressLint("MissingPermission")
    fun vibrateSuccess(context: Context) {
        vibrateClick(context)
    }

    @SuppressLint("MissingPermission")
    fun vibrateError(context: Context) {
        vibrateHeavy(context)
    }
}

fun triggerTickVibration(context: Context) {
    VibrationUtil.vibrateTick(context)
}

fun triggerClickVibration(context: Context) {
    VibrationUtil.vibrateClick(context)
}

fun triggerLongPressVibration(context: Context) {
    VibrationUtil.vibrateLongPress(context)
}

fun Modifier.hapticClickable(
    context: Context,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = this.clickable(enabled = enabled) {
    VibrationUtil.vibrateTick(context)
    onClick()
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.hapticCombinedClickable(
    context: Context,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = this.combinedClickable(
    enabled = enabled,
    onLongClick = onLongClick?.let { action ->
        {
            VibrationUtil.vibrateLongPress(context)
            action()
        }
    },
    onClick = {
        VibrationUtil.vibrateTick(context)
        onClick()
    }
)
