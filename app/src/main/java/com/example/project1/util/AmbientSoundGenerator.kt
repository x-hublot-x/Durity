package com.example.project1.util

import android.content.Context
import android.media.MediaPlayer
import com.example.project1.R

enum class AmbientSoundType(val title: String, val rawResIds: List<Int>) {
    NONE("Без звука", emptyList()),
    RAIN(
        "Дождь",
        listOf(R.raw.sound_rain_1, R.raw.sound_rain_2, R.raw.sound_rain_3)
    ),
    CAMPFIRE(
        "Костёр",
        listOf(R.raw.sound_campfire_1, R.raw.sound_campfire_2, R.raw.sound_campfire_3)
    ),
    CRICKETS(
        "Сверчки",
        listOf(R.raw.sound_crickets_1, R.raw.sound_crickets_2, R.raw.sound_crickets_3)
    ),
    BIRDS(
        "Птицы",
        listOf(R.raw.sound_birds_1, R.raw.sound_birds_2, R.raw.sound_birds_3)
    ),
    WATER_STREAM(
        "Ручей",
        listOf(R.raw.sound_water_1, R.raw.sound_water_2, R.raw.sound_water_3)
    ),
    WINTER_STORM(
        "Зимняя буря",
        listOf(R.raw.sound_winter_1, R.raw.sound_winter_2, R.raw.sound_winter_3)
    ),
    WIND(
        "Ветер",
        listOf(R.raw.sound_wind_1, R.raw.sound_wind_2, R.raw.sound_wind_3)
    ),
    THUNDERSTORM(
        "Гроза",
        listOf(R.raw.sound_thunder_1, R.raw.sound_thunder_2, R.raw.sound_thunder_3)
    ),
    COFFEE_SHOP(
        "Кофейня",
        listOf(R.raw.sound_cafe_1, R.raw.sound_cafe_2, R.raw.sound_cafe_3)
    );

    fun getRandomResId(): Int? = rawResIds.randomOrNull()
}

object AmbientSoundGenerator {
    private val activePlayers = mutableMapOf<AmbientSoundType, MediaPlayer>()
    private val lock = Any()

    var activeSounds: Set<AmbientSoundType> = emptySet()
        private set

    var volume: Float = 0.8f
        set(value) {
            field = value.coerceIn(0f, 1f)
            synchronized(lock) {
                activePlayers.values.forEach { player ->
                    try {
                        player.setVolume(field, field)
                    } catch (_: Exception) {}
                }
            }
        }

    fun setSounds(context: Context, sounds: Set<AmbientSoundType>) {
        synchronized(lock) {
            val validSounds = sounds.filter { it != AmbientSoundType.NONE && it.rawResIds.isNotEmpty() }.toSet()
            activeSounds = validSounds

            // Stop players for sounds that are no longer active
            val toRemove = activePlayers.keys.filter { !validSounds.contains(it) }
            for (type in toRemove) {
                activePlayers.remove(type)?.let { player ->
                    try {
                        if (player.isPlaying) player.stop()
                        player.release()
                    } catch (_: Exception) {}
                }
            }

            // Start players for newly activated sounds (randomly picking 1 of the 3 variations)
            for (type in validSounds) {
                if (!activePlayers.containsKey(type)) {
                    val resId = type.getRandomResId() ?: continue
                    try {
                        val player = MediaPlayer.create(context.applicationContext, resId)
                        if (player != null) {
                            player.isLooping = true
                            player.setVolume(volume, volume)
                            player.start()
                            activePlayers[type] = player
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun toggleSound(context: Context, type: AmbientSoundType) {
        if (type == AmbientSoundType.NONE) {
            stopSound()
            return
        }

        val updated = if (activeSounds.contains(type)) {
            activeSounds - type
        } else {
            activeSounds + type
        }
        setSounds(context, updated)
    }

    fun pauseAll() {
        synchronized(lock) {
            activePlayers.values.forEach { player ->
                try {
                    if (player.isPlaying) player.pause()
                } catch (_: Exception) {}
            }
        }
    }

    fun resumeAll() {
        synchronized(lock) {
            activePlayers.values.forEach { player ->
                try {
                    player.start()
                } catch (_: Exception) {}
            }
        }
    }

    fun stopSound() {
        synchronized(lock) {
            activeSounds = emptySet()
            for (player in activePlayers.values) {
                try {
                    if (player.isPlaying) player.stop()
                    player.release()
                } catch (_: Exception) {}
            }
            activePlayers.clear()
        }
    }
}
