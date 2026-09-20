package com.example.project1.data.storage

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.project1.R

enum class BlockGif(
    val id: String,
    val title: String,
    val description: String,
    val rawRes: Int
) {
    ROCK("default_rock", "Скала", "Классический взгляд Дуэйна Джонсона", R.raw.block_anim),
    CAT_MEME("gif_0", "Шрек", "Осуждающий взгляд", R.raw.block_gif_0),
    BRAWL_A4("brawl_a4", "Серега А4", "Кушает что-то сладкое", R.raw.block_gif_brawl_a4),
    DARK_MEME("dark_meme", "Эрик Кинг", "Он знает, но не может доказать", R.raw.block_gif_dark_meme),
    MEME_REACTION("meme_reaction", "Лыба + ЫЫЫ", "Шокированная реакция", R.raw.block_gif_meme_reaction),
    SPIDERMAN("spiderman", "Человек-Паук", "Задумчивый Человек-Паук", R.raw.block_gif_spiderman),
    PLANKTON("plankton", "Планктон", "Злобный смех Планктона", R.raw.block_gif_plankton)
}

enum class BlockSound(
    val id: String,
    val title: String,
    val description: String,
    val rawRes: Int
) {
    SIREN("default_siren", "Сирена", "Громкий сигнал тревоги", R.raw.block_sound),
    SKALA_BOOM("skala_boom", "Взгляд Скалы (Бум)", "Звуковой эффект драматического бума", R.raw.block_sound_skala_boom),
    AMONG_US("among_us", "Among Us", "Звук предателя / раскрытия роли", R.raw.block_sound_among_us),
    NESPRAVEDLIVOST("nespravedlivost", "Несправедливость", "Ааааааа несправедливость!", R.raw.block_sound_nespravedlivost),
    DISCIPLINE("discipline", "Discipline", "Звук дисциплины", R.raw.block_sound_discipline),
    DU_BIST("du_bist", "Du bist gut genug", "Мемный звук", R.raw.block_sound_du_bist),
    ERROR("error", "Ошибка Виндовс", "Классический звук системной ошибки", R.raw.block_sound_error),
    GTA_WASTED("gta_wasted", "GTA V Wasted", "Звук гибели из GTA 5", R.raw.block_sound_gta_wasted),
    MELLSTROY("mellstroy", "Меллстрой кричит", "Эмоциональный крик меллстроя", R.raw.block_sound_mellstroy),
    VYKLYUCHI("vyklyuchi", "Выключи его на###", "Мемный звук", R.raw.block_sound_vyklyuchi),
    LITVIN("litvin", "Звук Литвина", "Фирменный звук от Литвина", R.raw.block_sound_litvin)
}

object BlockMediaManager {
    private const val PREFS = "block_media_preferences"
    private const val KEY_GIF = "selected_block_gif"
    private const val KEY_SOUND = "selected_block_sound"

    var currentGif by mutableStateOf(BlockGif.ROCK)
        private set

    var currentSound by mutableStateOf(BlockSound.SIREN)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val gifId = prefs.getString(KEY_GIF, BlockGif.ROCK.id)
        val soundId = prefs.getString(KEY_SOUND, BlockSound.SIREN.id)

        currentGif = BlockGif.entries.find { it.id == gifId } ?: BlockGif.ROCK
        currentSound = BlockSound.entries.find { it.id == soundId } ?: BlockSound.SIREN
    }

    fun setGif(context: Context, gif: BlockGif) {
        currentGif = gif
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GIF, gif.id)
            .apply()
    }

    fun setSound(context: Context, sound: BlockSound) {
        currentSound = sound
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SOUND, sound.id)
            .apply()
    }

    fun getCurrentGifRes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val gifId = prefs.getString(KEY_GIF, BlockGif.ROCK.id)
        return (BlockGif.entries.find { it.id == gifId } ?: BlockGif.ROCK).rawRes
    }

    fun getCurrentSoundRes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val soundId = prefs.getString(KEY_SOUND, BlockSound.SIREN.id)
        return (BlockSound.entries.find { it.id == soundId } ?: BlockSound.SIREN).rawRes
    }
}
