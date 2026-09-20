package com.example.project1.service

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.project1.data.storage.DailyTaskStorage
import com.example.project1.data.storage.FocusStorage
import com.example.project1.data.storage.NotificationHistoryStorage
import com.example.project1.util.AmbientSoundGenerator
import com.example.project1.util.AmbientSoundType
import com.example.project1.util.VibrationUtil
import kotlinx.coroutines.*

object FocusSessionManager {
    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var appContext: Context? = null

    var isActive by mutableStateOf(false)
        private set
    var isPaused by mutableStateOf(false)
        private set
    var totalSeconds by mutableIntStateOf(25 * 60)
        private set
    var remainingSeconds by mutableIntStateOf(25 * 60)
        private set
    var selectedSounds by mutableStateOf<Set<AmbientSoundType>>(emptySet())
        private set
    var strictMode by mutableStateOf(false)
        private set

    fun startSession(context: Context, minutes: Int, sounds: Set<AmbientSoundType>, isStrict: Boolean = false) {
        val appCtx = context.applicationContext
        appContext = appCtx

        timerJob?.cancel()
        timerJob = null
        try {
            AmbientSoundGenerator.stopSound()
        } catch (_: Exception) {}

        val mins = if (minutes <= 0) 25 else minutes
        totalSeconds = mins * 60
        remainingSeconds = mins * 60
        val cleanSounds = sounds.filter { it != AmbientSoundType.NONE }.toSet()
        selectedSounds = cleanSounds
        FocusStorage.saveSounds(appCtx, cleanSounds)
        strictMode = isStrict
        isActive = true
        isPaused = false

        if (selectedSounds.isNotEmpty()) {
            try {
                AmbientSoundGenerator.setSounds(appCtx, selectedSounds)
            } catch (_: Exception) {}
        }

        try {
            VibrationUtil.vibrateTick(appCtx)
        } catch (_: Exception) {}

        FocusService.startService(appCtx)

        if (!scope.isActive) {
            scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        }

        timerJob = scope.launch {
            var counter = 0
            while (isActive && remainingSeconds > 0) {
                delay(1000)
                if (!isPaused) {
                    remainingSeconds--
                    counter++
                    // Обновляем текст уведомления и медиа-состояние каждые 5 сек
                    if (counter % 5 == 0 || remainingSeconds <= 10) {
                        appContext?.let { FocusService.updateNotification(it) }
                    }
                }
            }
            if (isActive && remainingSeconds <= 0) {
                onSessionFinished(appCtx)
            }
        }
    }

    fun pauseSession(context: Context? = null) {
        if (!isActive) return
        isPaused = true
        try {
            AmbientSoundGenerator.pauseAll()
        } catch (_: Exception) {}
        (context ?: appContext)?.let { FocusService.updateNotification(it) }
    }

    fun resumeSession(context: Context? = null) {
        if (!isActive) return
        isPaused = false
        try {
            AmbientSoundGenerator.resumeAll()
        } catch (_: Exception) {}
        (context ?: appContext)?.let { FocusService.updateNotification(it) }
    }

    fun toggleSound(context: Context, sound: AmbientSoundType) {
        if (sound == AmbientSoundType.NONE) {
            selectedSounds = emptySet()
            if (isActive && !isPaused) {
                try {
                    AmbientSoundGenerator.stopSound()
                } catch (_: Exception) {}
            }
            FocusStorage.saveSounds(context, emptySet())
            appContext?.let { FocusService.updateNotification(it) }
            return
        }

        val updated = if (selectedSounds.contains(sound)) {
            selectedSounds - sound
        } else {
            selectedSounds + sound
        }
        selectedSounds = updated
        FocusStorage.saveSounds(context, updated)
        if (isActive && !isPaused) {
            try {
                AmbientSoundGenerator.setSounds(context, updated)
            } catch (_: Exception) {}
        }
        appContext?.let { FocusService.updateNotification(it) }
    }

    fun stopSession(context: Context? = null, completed: Boolean = false) {
        timerJob?.cancel()
        timerJob = null
        isActive = false
        isPaused = false
        try {
            AmbientSoundGenerator.stopSound()
        } catch (_: Exception) {}
        val ctx = context ?: appContext
        ctx?.let { FocusService.stopService(it) }
    }

    private fun onSessionFinished(context: Context) {
        val earnedMinutes = totalSeconds / 60
        stopSession(context, completed = true)

        // Начисляем монеты за фокус
        if (earnedMinutes > 0) {
            try {
                DailyTaskStorage.addCoins(context, earnedMinutes)
                NotificationHistoryStorage.addNotification(
                    context,
                    title = "Сессия фокуса завершена! 🎯",
                    message = "Вы сохраняли концентрацию $earnedMinutes мин. Начислено +$earnedMinutes монет!",
                    type = "timer"
                )
                VibrationUtil.vibrateSuccess(context)
            } catch (_: Exception) {}
        }
    }
}
