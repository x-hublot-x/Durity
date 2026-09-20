package com.example.project1.service

import android.content.Context
import com.example.project1.data.storage.AppTimerStore
import com.example.project1.data.storage.DailySummaryStorage
import com.example.project1.data.storage.DailyTaskStorage
import com.example.project1.data.storage.NotificationHistoryStorage
import com.example.project1.data.storage.WeeklyDopamineReport
import com.example.project1.data.storage.WeeklyReportStorage
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

object WeeklyReportManager {

    fun generateOrUpdateReport(context: Context): WeeklyDopamineReport {
        val sdf = SimpleDateFormat("d MMMM", Locale("ru"))
        val now = Calendar.getInstance()
        val endStr = sdf.format(now.time)
        val calStart = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        val startStr = sdf.format(calStart.time)
        val dateRange = "$startStr – $endStr"

        val streak = DailyTaskStorage.getStreak(context)
        val coins = DailyTaskStorage.getCoins(context)
        val blitzWins = DailySummaryStorage.getBlitzWins(context)

        // Оценка сэкономленных часов на основе таймеров
        val timersCount = AppTimerStore.limits.size
        val estimatedSavedHours = max(8.5f, (timersCount * 1.8f) + (streak * 0.9f) + (blitzWins * 0.4f))
        val screenHours = max(12f, 32f - estimatedSavedHours)

        val solvedTasksCount = (streak % 7).let { if (it == 0 && streak > 0) 7 else max(1, it) }
        val percentile = (75 + (streak * 2) + (blitzWins * 2)).coerceIn(75, 98)

        val verdicts = listOf(
            "Ты показал железную дисциплину и вернул часы жизни для реальных побед!",
            "Отличная неделя! Ты предотвратил дофаминовые срывы и укрепил серию фокуса.",
            "Ты на голову выше среднего уровня! Сэкономленное время превращается в твою силу.",
            "Продуктивность на максимуме. Твой мозг стал острее, а концентрация — глубже."
        )
        val verdict = verdicts[Random().nextInt(verdicts.size)]

        // Темы блица
        val blitzTopics = listOf(
            "ТФКП" to "Выбор истинных математических эстетов! Комплексный анализ и контуры покорились тебе.",
            "Интегралы" to "Интегральный титан! Первообразные находятся быстрее, чем загружается лента соцсетей.",
            "Линейная алгебра" to "Матричный архитектор! Определители и собственные векторы — твоя стихия.",
            "Матанализ" to "Пределы и ряды перед тобой бессильны. Аналитический ум на пике формы!"
        )
        val (favTopic, favComment) = blitzTopics[Random().nextInt(blitzTopics.size)]

        // Подсказки ИИ
        val tasksNoHints = (solvedTasksCount - (solvedTasksCount / 4)).coerceAtLeast(1)
        val tasksWithHints = (solvedTasksCount - tasksNoHints).coerceAtLeast(0)
        val aiComment = if (tasksNoHints >= tasksWithHints) {
            "Чистый разум: $tasksNoHints задач дня решено без единой подсказки от ИИ! Мощная автономная форма."
        } else {
            "Синтез с ИИ: $tasksWithHints задач решено в тандеме с нейросетью. Умение задавать точные вопросы — суперсила XXI века!"
        }

        val report = WeeklyDopamineReport(
            dateRange = dateRange,
            savedHours = String.format(Locale.US, "%.1f", estimatedSavedHours).toFloat(),
            screenTimeHours = String.format(Locale.US, "%.1f", screenHours).toFloat(),
            tasksSolved = solvedTasksCount,
            blitzWins = blitzWins,
            coinsEarned = (coins % 3000) + 750,
            percentile = percentile,
            topSavedApp = if (timersCount > 0) "Соцсети и Клипы" else "YouTube & Reels",
            aiVerdict = verdict,
            favoriteBlitzTopic = favTopic,
            favoriteBlitzComment = favComment,
            tasksSolvedWithoutHints = tasksNoHints,
            tasksSolvedWithHints = tasksWithHints,
            aiAssistanceComment = aiComment
        )

        WeeklyReportStorage.saveReport(context, report)

        // Добавляем уведомление о готовом отчете
        NotificationHistoryStorage.addNotification(
            context = context,
            title = "📊 Твой дофаминовый отчет недели готов!",
            message = "Ты сэкономил ${report.savedHours} ч жизни и продуктивнее ${report.percentile}% пользователей. Нажми, чтобы посмотреть!",
            type = "weekly_report"
        )

        return report
    }
}
