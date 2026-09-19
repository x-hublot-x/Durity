package com.example.project1.service

import android.content.Context
import com.example.project1.data.storage.AppTimerStore
import com.example.project1.data.storage.DailySummaryData
import com.example.project1.data.storage.DailySummaryStorage
import com.example.project1.data.storage.DailyTaskStorage
import com.example.project1.data.storage.UserRatingStorage
import com.example.project1.util.getAppUsageMinutesThisWeek
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

object DailySummaryManager {

    /** Проверяет, активно ли вечернее окно показа итогов дня (с 21:00 по Москве) */
    fun isSummaryWindowActive(): Boolean {
        val msk = TimeZone.getTimeZone("Europe/Moscow")
        val cal = Calendar.getInstance(msk)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour >= 21 || hour in 0..2
    }

    /** Ключ даты для подведения итогов дня (сегодняшний день с 21:00 или вчерашний ночью) */
    fun getSummaryDateKey(): String {
        val msk = TimeZone.getTimeZone("Europe/Moscow")
        val cal = Calendar.getInstance(msk)
        if (cal.get(Calendar.HOUR_OF_DAY) in 0..4) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    suspend fun checkAndGenerateDailySummary(context: Context, apiKey: String, forceRefresh: Boolean = false): DailySummaryData? = withContext(Dispatchers.IO) {
        val dateKey = getSummaryDateKey()

        // 1. Подсчет времени коротких видео
        val shortPackages = listOf(
            "com.google.android.youtube.shorts",
            "com.instagram.android.reels",
            "com.vkontakte.android.clips",
            "tv.twitch.android.app.clips"
        )
        var scrollingMinutes = 0
        for (pkg in shortPackages) {
            scrollingMinutes += getAppUsageMinutesThisWeek(context, pkg, 0)
        }

        // 2. Реальное суммарное экранное время по всей системе за сегодня
        val realScreenMinutes = com.example.project1.util.getTotalDeviceScreenMinutesToday(context)
        val totalScreenMinutes = maxOf(realScreenMinutes, scrollingMinutes)

        // 3. Актуальное состояние задачи дня и блицев
        val taskSolved = DailyTaskStorage.isSolvedToday(context)
        val blitzWins = DailySummaryStorage.getBlitzWins(context)
        val blitzLosses = DailySummaryStorage.getBlitzLosses(context)

        // 4. Расчет изменения рейтинга (репутации) и монет
        var ratingDelta = 0
        var coinsAwarded = 0

        // Штраф если скроллил больше часа
        if (scrollingMinutes > 60) {
            val hours = scrollingMinutes / 60
            ratingDelta -= 10 * hours
        } else {
            // Награда если скроллинг минимален
            ratingDelta += when {
                scrollingMinutes == 0 -> 20
                scrollingMinutes <= 15 -> 15
                scrollingMinutes <= 30 -> 10
                else -> 5
            }
        }

        // Бонус монет если скроллинг < 5 минут
        if (scrollingMinutes < 5) {
            coinsAwarded += 15
        }

        // Бонус репутации если установлены лимиты
        val hasLimits = AppTimerStore.limits.isNotEmpty()
        if (hasLimits) {
            ratingDelta += 5
        }

        // Применяем разово награду за день (не дублируем при повторном открытии)
        if (!DailySummaryStorage.isAwardApplied(context, dateKey)) {
            UserRatingStorage.addRating(context, ratingDelta)
            if (coinsAwarded > 0) {
                DailyTaskStorage.addCoins(context, coinsAwarded)
            }
            DailySummaryStorage.markAwardApplied(context, dateKey)
        }

        // 5. Рекомендация ИИ (используем ранее сгенерированную или создаем новую если данные изменились)
        val existing = DailySummaryStorage.getLatestSummary(context)
        val statsSignificantlyChanged = existing == null ||
                kotlin.math.abs(totalScreenMinutes - existing.totalScreenMinutes) >= 15 ||
                kotlin.math.abs(scrollingMinutes - existing.scrollingMinutes) >= 5 ||
                taskSolved != existing.dailyTaskSolved ||
                blitzWins != existing.blitzWins ||
                ratingDelta != existing.ratingDelta

        val aiRecommendation = if (existing != null && existing.dateKey == dateKey && existing.aiRecommendation.isNotBlank() && !forceRefresh && !statsSignificantlyChanged) {
            existing.aiRecommendation
        } else {
            generateAiRecommendation(
                apiKey = apiKey,
                totalMinutes = totalScreenMinutes,
                scrollingMinutes = scrollingMinutes,
                taskSolved = taskSolved,
                blitzWins = blitzWins,
                blitzLosses = blitzLosses,
                ratingDelta = ratingDelta,
                coinsAwarded = coinsAwarded
            )
        }

        val summary = DailySummaryData(
            dateKey = dateKey,
            totalScreenMinutes = totalScreenMinutes,
            scrollingMinutes = scrollingMinutes,
            ratingDelta = ratingDelta,
            coinsAwarded = coinsAwarded,
            dailyTaskSolved = taskSolved,
            blitzWins = blitzWins,
            blitzLosses = blitzLosses,
            aiRecommendation = aiRecommendation,
            timestamp = System.currentTimeMillis()
        )

        DailySummaryStorage.saveSummary(context, summary)

        return@withContext summary
    }

    private suspend fun generateAiRecommendation(
        apiKey: String,
        totalMinutes: Int,
        scrollingMinutes: Int,
        taskSolved: Boolean,
        blitzWins: Int,
        blitzLosses: Int,
        ratingDelta: Int,
        coinsAwarded: Int
    ): String {
        if (apiKey.isBlank()) {
            return buildFallbackRecommendation(scrollingMinutes, taskSolved, blitzWins, ratingDelta)
        }

        val h = totalMinutes / 60
        val m = totalMinutes % 60
        val totalTimeStr = if (h > 0) "${h}ч ${m}м" else "${m}м"

        return try {
            val model = GenerativeModel(modelName = "gemini-3.5-flash-lite", apiKey = apiKey)
            val prompt = """
Ты персональный наставник по цифровой осознанности и математике в приложении Durity.
Точные показатели пользователя за сегодня:
- Экранное время: $totalMinutes мин ($totalTimeStr).
- Короткие видео (шортсы/клипы): $scrollingMinutes мин.
- Задача дня: ${if (taskSolved) "Решена (+1 к серии)" else "Не решена"}.
- Блиц-поединки: $blitzWins побед, $blitzLosses поражений.
- Изменение репутации: ${if (ratingDelta >= 0) "+$ratingDelta" else "$ratingDelta"}.
- Начислено монет: +$coinsAwarded.

СТРОГИЕ ПРАВИЛА:
1. Напиши ровно 3 предложения на русском языке без списков, markdown-разметки условий и шаблонности.
2. Первое предложение: краткий вывод по общему экранному времени ($totalTimeStr) и шортсам ($scrollingMinutes мин).
3. Второе предложение: комментарий по задаче дня и блицам.
4. Третье предложение: вдохновляющий совет или ориентир на следующий день.
5. ВАЖНО: Если упоминаешь цифры (минуты, шортсы, репутацию), используй ТОЛЬКО точные числа из данных выше, ни в коем случае не придумывай другие значения.
""".trimIndent()

            val res = model.generateContent(prompt)
            res.text?.trim() ?: buildFallbackRecommendation(scrollingMinutes, taskSolved, blitzWins, ratingDelta)
        } catch (_: Exception) {
            buildFallbackRecommendation(scrollingMinutes, taskSolved, blitzWins, ratingDelta)
        }
    }

    private fun buildFallbackRecommendation(
        scrollingMinutes: Int,
        taskSolved: Boolean,
        blitzWins: Int,
        ratingDelta: Int
    ): String {
        val s1 = if (scrollingMinutes < 20) {
            "Сегодня вы отлично контролировали экранное время и потратили на шортсы всего $scrollingMinutes мин."
        } else {
            "Сегодня на короткие видео ушло $scrollingMinutes мин, что немного превысило оптимальный баланс."
        }
        val s2 = if (taskSolved) {
            "Решенная задача дня и $blitzWins побед в блицах укрепили вашу репутацию на ${if (ratingDelta >= 0) "+$ratingDelta" else "$ratingDelta"}."
        } else {
            "Пропущенная задача дня снизила рейтинг, но завтра будет отличный повод восстановить серию."
        }
        val s3 = "Завтра сделайте упор на утреннюю задачу дня и сохраняйте фокус на ключевых задачах."

        return "$s1 $s2 $s3"
    }
}