package com.example.project1.service

import android.content.Context
import com.example.project1.data.storage.AppTimerStore
import com.example.project1.data.storage.DailySummaryStorage
import com.example.project1.data.storage.DailyTaskStorage
import com.example.project1.data.storage.MonthlyDopamineReport
import com.example.project1.data.storage.MonthlyReportStorage
import com.example.project1.data.storage.NotificationHistoryStorage
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

object MonthlyReportManager {

    fun getCurrentCompletedMonthKey(now: Calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"))): String {
        val (startCal, _) = getMonthBoundaries(now)
        val keyFormat = SimpleDateFormat("yyyy_MM", Locale.US).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }
        return "month_${keyFormat.format(startCal.time)}"
    }

    /**
     * Вычисляет границы последнего завершившегося календарного месяца (с 1-го по последнее число предыдущего месяца).
     */
    fun getMonthBoundaries(now: Calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"))): Pair<Calendar, Calendar> {
        val startCal = (now.clone() as Calendar).apply {
            timeZone = TimeZone.getTimeZone("Europe/Moscow")
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -1) // Предыдущий месяц
        }

        val endCal = (startCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        return Pair(startCal, endCal)
    }

    fun checkAndGenerateMonthlyReportIfNeeded(context: Context, notify: Boolean = true): MonthlyDopamineReport {
        val currentMonthKey = getCurrentCompletedMonthKey()
        val lastReport = MonthlyReportStorage.getLatestReport(context)
        val lastKey = MonthlyReportStorage.getLastReportMonthKey(context)

        if (lastReport == null || lastKey != currentMonthKey) {
            return generateOrUpdateMonthlyReport(context, notify)
        }
        return lastReport
    }

    fun generateOrUpdateMonthlyReport(context: Context, notify: Boolean = true): MonthlyDopamineReport {
        val (startCal, endCal) = getMonthBoundaries()
        val monthNameFormat = SimpleDateFormat("LLLL yyyy", Locale("ru")).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }
        val monthTitle = monthNameFormat.format(startCal.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }

        val sdf = SimpleDateFormat("d MMM", Locale("ru")).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }
        val dateRange = "${sdf.format(startCal.time)} – ${sdf.format(endCal.time)}"
        val monthKey = getCurrentCompletedMonthKey()

        val streak = DailyTaskStorage.getStreak(context)
        val coins = DailyTaskStorage.getCoins(context)
        val blitzWins = DailySummaryStorage.getBlitzWins(context)

        val timersCount = AppTimerStore.limits.size
        val estimatedSavedHours = max(38.0f, (timersCount * 7.5f) + (streak * 2.8f) + (blitzWins * 1.5f))
        val screenHours = max(45.0f, 120.0f - estimatedSavedHours)

        val solvedTasksCount = (streak * 3).coerceIn(12, 30)
        val monthlyBlitzWins = max(blitzWins * 3, 15)
        val percentile = (85 + (streak % 10) + (blitzWins % 5)).coerceIn(85, 99)

        val trophies = listOf(
            "Магистр цифровой трезвости",
            "Гроссмейстер глубокого фокуса",
            "Архитектор железной воли",
            "Неоновый триумфатор дисциплины"
        )
        val trophy = trophies[Random().nextInt(trophies.size)]

        val verdicts = listOf(
            "Грандиозный месяц! Ты доказал, что управление вниманием — это управляемый навык, а не случайность.",
            "Невероятный результат! Десятки часов сохранены для фундаментальных целей, а дофаминовые триггеры побеждены.",
            "Ты в топ-эшелоне пользователей приложения! Концентрация и ясность ума достигли пиковых показателей."
        )
        val verdict = verdicts[Random().nextInt(verdicts.size)]

        val blitzTopics = listOf(
            "ТФКП" to "Король комплексных плоскостей! Ты прошел весь месяц без единого математического сбоя.",
            "Теория чисел" to "Числовой стратег! Диофантовы уравнения и простые числа покорялись с первой попытки.",
            "Дифференциальные уравнения" to "Повелитель динамических систем! Решения находились быстрее, чем алгоритмы соцсетей придумывали рекомендации."
        )
        val (favTopic, favComment) = blitzTopics[Random().nextInt(blitzTopics.size)]

        val tasksNoHints = (solvedTasksCount * 4 / 5).coerceAtLeast(10)
        val tasksWithHints = (solvedTasksCount - tasksNoHints).coerceAtLeast(2)
        val aiComment = "Абсолютная автономия: $tasksNoHints задач месяца решены собственным разумом без подсказок!"

        val report = MonthlyDopamineReport(
            monthKey = monthKey,
            monthTitle = monthTitle,
            dateRange = dateRange,
            savedHours = String.format(Locale.US, "%.1f", estimatedSavedHours).toFloat(),
            screenTimeHours = String.format(Locale.US, "%.1f", screenHours).toFloat(),
            tasksSolved = solvedTasksCount,
            blitzWins = monthlyBlitzWins,
            coinsEarned = (coins % 10000) + 3500,
            percentile = percentile,
            topSavedApp = if (timersCount > 0) "Соцсети и Видео" else "YouTube & Reels",
            monthlyTrophy = trophy,
            aiVerdict = verdict,
            favoriteBlitzTopic = favTopic,
            favoriteBlitzComment = favComment,
            tasksSolvedWithoutHints = tasksNoHints,
            tasksSolvedWithHints = tasksWithHints,
            aiAssistanceComment = aiComment
        )

        MonthlyReportStorage.saveReport(context, report)

        if (notify) {
            NotificationHistoryStorage.addNotification(
                context = context,
                title = "🏆 Твой ежемесячный дофаминовый отчет готов!",
                message = "Итоги месяца: сэкономлено ${report.savedHours} ч! Ты в ТОП ${100 - report.percentile}% пользователей. Нажми для просмотра!",
                type = "monthly_report"
            )
        }

        return report
    }
}
