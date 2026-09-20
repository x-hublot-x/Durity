package com.example.project1.service

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.project1.data.storage.DailyTaskStorage
import com.example.project1.data.storage.NotificationHistoryStorage
import com.example.project1.util.AmbientSoundGenerator
import com.example.project1.util.AmbientSoundType
import com.example.project1.util.VibrationUtil
import kotlinx.coroutines.*

object FocusSessionManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
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
        appContext = context.applicationContext
        stopSession(context, completed = false)

        totalSeconds = minutes * 60
        remainingSeconds = minutes * 60
        selectedSounds = sounds.filter { it != AmbientSoundType.NONE }.toSet()
        strictMode = isStrict
        isActive = true
        isPaused = false

        if (selectedSounds.isNotEmpty()) {
            AmbientSoundGenerator.setSounds(context, selectedSounds)
        }

        VibrationUtil.vibrateTick(context)
        FocusService.startService(context)

        timerJob = scope.launch {
            while (isActive && remainingSeconds > 0) {
                delay(1000)
                if (!isPaused) {
                    remainingSeconds--
                    appContext?.let { FocusService.updateNotification(it) }
                }
            }
            if (isActive && remainingSeconds <= 0) {
                onSessionFinished(context)
            }
        }
    }

    fun pauseSession(context: Context? = null) {
        if (!isActive) return
        isPaused = true
        AmbientSoundGenerator.pauseAll()
        (context ?: appContext)?.let { FocusService.updateNotification(it) }
    }

    fun resumeSession(context: Context? = null) {
        if (!isActive) return
        isPaused = false
        AmbientSoundGenerator.resumeAll()
        (context ?: appContext)?.let { FocusService.updateNotification(it) }
    }

    fun toggleSound(context: Context, sound: AmbientSoundType) {
        if (sound == AmbientSoundType.NONE) {
            selectedSounds = emptySet()
            if (isActive && !isPaused) {
                AmbientSoundGenerator.stopSound()
            }
            return
        }

        val updated = if (selectedSounds.contains(sound)) {
            selectedSounds - sound
        } else {
            selectedSounds + sound
        }
        selectedSounds = updated
        if (isActive && !isPaused) {
            AmbientSoundGenerator.setSounds(context, updated)
        }
    }

    fun stopSession(context: Context? = null, completed: Boolean = false) {
        timerJob?.cancel()
        timerJob = null
        isActive = false
        isPaused = false
        AmbientSoundGenerator.stopSound()
        (context ?: appContext)?.let { FocusService.stopService(it) }
    }

    private fun onSessionFinished(context: Context) {
        val earnedMinutes = totalSeconds / 60
        stopSession(context, completed = true)

        // Начисляем монеты за фокус
        if (earnedMinutes > 0) {
            DailyTaskStorage.addCoins(context, earnedMinutes)
            NotificationHistoryStorage.addNotification(
                context,
                title = "Сессия фокуса завершена! 🎯",
                message = "Вы сохраняли концентрацию $earnedMinutes мин. Начислено +$earnedMinutes монет!",
                type = "timer"
            )
            VibrationUtil.vibrateSuccess(context)
        }
    }
}

