package com.example.project1.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.example.project1.data.storage.WeeklyDopamineReport
import com.example.project1.ui.theme.ThemeManager
import java.io.File
import java.io.FileOutputStream

object ReportImageExporter {

    fun shareReportCard(
        context: Context,
        report: WeeklyDopamineReport,
        primaryColorInt: Int = ThemeManager.currentAccent.primary.toArgb(),
        secondaryColorInt: Int = ThemeManager.currentAccent.secondary.toArgb()
    ) {
        try {
            // Формат 16:9 (Landscape) - 1920x1080
            val width = 1920
            val height = 1080
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val cardMargin = 36f
            val cardCornerRadius = 48f
            val cardRect = RectF(cardMargin, cardMargin, width - cardMargin, height - cardMargin)

            // Обрезаем всё по скругленной рамке карточки
            val clipPath = Path().apply {
                addRoundRect(cardRect, cardCornerRadius, cardCornerRadius, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)

            // 1. Космический глубокий градиент фона
            val bgPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(
                        android.graphics.Color.parseColor("#0C0816"),
                        android.graphics.Color.parseColor("#170F2B"),
                        android.graphics.Color.parseColor("#090611")
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(cardRect, bgPaint)

            // Декоративные ореолы свечения
            val glowPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
            glowPaint.shader = RadialGradient(
                width * 0.2f, height * 0.3f, 480f,
                intArrayOf(primaryColorInt and 0x00FFFFFF or 0x33000000, 0x00000000),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(width * 0.2f, height * 0.3f, 480f, glowPaint)

            glowPaint.shader = RadialGradient(
                width * 0.8f, height * 0.7f, 520f,
                intArrayOf(secondaryColorInt and 0x00FFFFFF or 0x2E000000, 0x00000000),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(width * 0.8f, height * 0.7f, 520f, glowPaint)

            // Утилита многострочного текста
            fun drawMultiline(
                text: String,
                x: Float,
                y: Float,
                boxWidth: Int,
                paint: TextPaint,
                align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
            ): Int {
                val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StaticLayout.Builder.obtain(text, 0, text.length, paint, boxWidth)
                        .setAlignment(align)
                        .setLineSpacing(4f, 1.15f)
                        .setIncludePad(false)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    StaticLayout(text, paint, boxWidth, align, 1.15f, 4f, false)
                }
                canvas.save()
                canvas.translate(x, y)
                layout.draw(canvas)
                canvas.restore()
                return layout.height
            }

            // ══════════════ ЛЕВАЯ КОЛОНКА (x: 80..910) ══════════════

            // Шапка
            val headerPaint = TextPaint().apply {
                isAntiAlias = true
                color = primaryColorInt
                textAlign = Paint.Align.LEFT
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 34f
                letterSpacing = 0.08f
            }
            canvas.drawText("DURITY WRAPPED", 80f, 100f, headerPaint)

            val subHeaderPaint = TextPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#9E9E9E")
                textAlign = Paint.Align.LEFT
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 22f
            }
            canvas.drawText("ИТОГИ НЕДЕЛИ • ${report.dateRange}", 80f, 138f, subHeaderPaint)

            // Главный блок: Сохранено времени (Hero)
            val heroBgPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#1B132C")
                style = Paint.Style.FILL
            }
            val heroBorderPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#3B2A59")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            val heroRect = RectF(80f, 175f, 910f, 385f)
            canvas.drawRoundRect(heroRect, 30f, 30f, heroBgPaint)
            canvas.drawRoundRect(heroRect, 30f, 30f, heroBorderPaint)

            val heroLabelPaint = TextPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#B388FF")
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 22f
                letterSpacing = 0.06f
            }
            canvas.drawText("СОХРАНЕНО ВРЕМЕНИ", heroRect.centerX(), 230f, heroLabelPaint)

            val heroNumPaint = TextPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#69F0AE")
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 80f
            }
            canvas.drawText("+${report.savedHours} ч", heroRect.centerX(), 335f, heroNumPaint)

            // 2x2 Сетка метрик (x: 80..910, y: 415..735)
            fun drawStatBadge(x: Float, y: Float, w: Float, h: Float, title: String, value: String, accentColor: Int) {
                val boxPaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.parseColor("#181226")
                    style = Paint.Style.FILL
                }
                val boxBorder = Paint().apply {
                    isAntiAlias = true
                    color = accentColor and 0x00FFFFFF or 0x4D000000
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                val r = RectF(x, y, x + w, y + h)
                canvas.drawRoundRect(r, 24f, 24f, boxPaint)
                canvas.drawRoundRect(r, 24f, 24f, boxBorder)

                val bTitlePaint = TextPaint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.parseColor("#90A4AE")
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textSize = 20f
                    letterSpacing = 0.04f
                }
                canvas.drawText(title, x + w / 2f, y + 46f, bTitlePaint)

                val bValPaint = TextPaint().apply {
                    isAntiAlias = true
                    color = accentColor
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 34f
                }
                canvas.drawText(value, x + w / 2f, y + 102f, bValPaint)
            }

            val badgeW = 400f
            val badgeH = 145f
            val leftColX = 80f
            val rightColX = 510f

            drawStatBadge(leftColX, 415f, badgeW, badgeH, "ЗАДАЧИ ДНЯ", "${report.tasksSolved} / 7 решено", android.graphics.Color.parseColor("#CE93D8"))
            drawStatBadge(rightColX, 415f, badgeW, badgeH, "БЛИЦ-ДУЭЛИ", "${report.blitzWins} побед", android.graphics.Color.parseColor("#FFD54F"))
            drawStatBadge(leftColX, 580f, badgeW, badgeH, "ФОКУС-МОНЕТЫ", "+${report.coinsEarned}", android.graphics.Color.parseColor("#FFCA28"))
            drawStatBadge(rightColX, 580f, badgeW, badgeH, "ДОФАМИН-РАНГ", "ТОП ${100 - report.percentile}%", android.graphics.Color.parseColor("#69F0AE"))

            // Левый нижний брендинг
            val brandPaint = TextPaint().apply {
                isAntiAlias = true
                color = primaryColorInt
                textAlign = Paint.Align.LEFT
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 24f
                letterSpacing = 0.06f
            }
            canvas.drawText("DURITY • ТВОЙ ДОФАМИНОВЫЙ СТРАЖ", 80f, 960f, brandPaint)

            val subBrandPaint = TextPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#757575")
                textAlign = Paint.Align.LEFT
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 18f
            }
            canvas.drawText("Контролируй цифровые привычки и достигай целей", 80f, 995f, subBrandPaint)

            // ══════════════ ПРАВАЯ КОЛОНКА (x: 960..1840) ══════════════
            val insightBoxPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#150F22")
                style = Paint.Style.FILL
            }
            val insightBorderPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#2E1E4A")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }

            // Блок 1: Любимая тема блица
            val blitzBoxRect = RectF(960f, 80f, 1840f, 360f)
            canvas.drawRoundRect(blitzBoxRect, 28f, 28f, insightBoxPaint)
            canvas.drawRoundRect(blitzBoxRect, 28f, 28f, insightBorderPaint)

            val blitzTitlePaint = TextPaint().apply {
                isAntiAlias = true
                color = primaryColorInt
                textAlign = Paint.Align.LEFT
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 26f
            }
            canvas.drawText("Любимая тема блица: ${report.favoriteBlitzTopic}", 995f, 135f, blitzTitlePaint)

            val descPaint = TextPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#E0E0E0")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 22f
            }
            drawMultiline(
                report.favoriteBlitzComment,
                995f,
                165f,
                810,
                descPaint,
                Layout.Alignment.ALIGN_NORMAL
            )

            // Блок 2: Автономность мышления
            val mindBoxRect = RectF(960f, 390f, 1840f, 670f)
            canvas.drawRoundRect(mindBoxRect, 28f, 28f, insightBoxPaint)
            canvas.drawRoundRect(mindBoxRect, 28f, 28f, insightBorderPaint)

            val mindTitlePaint = TextPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#69F0AE")
                textAlign = Paint.Align.LEFT
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 26f
            }
            canvas.drawText("Чистый разум: ${report.tasksSolvedWithoutHints} задач без подсказок", 995f, 445f, mindTitlePaint)

            drawMultiline(
                report.aiAssistanceComment,
                995f,
                475f,
                810,
                descPaint,
                Layout.Alignment.ALIGN_NORMAL
            )

            // Блок 3: Вердикт недели
            val verdictBoxRect = RectF(960f, 700f, 1840f, 1000f)
            val verdictBoxPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#1B122C")
                style = Paint.Style.FILL
            }
            val verdictBorderPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#4A2D78")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRoundRect(verdictBoxRect, 28f, 28f, verdictBoxPaint)
            canvas.drawRoundRect(verdictBoxRect, 28f, 28f, verdictBorderPaint)

            val verdictLabelPaint = TextPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#E1BEE7")
                textAlign = Paint.Align.LEFT
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 23f
                letterSpacing = 0.05f
            }
            canvas.drawText("ВЕРДИКТ ПРОДУКТИВНОСТИ", 995f, 755f, verdictLabelPaint)

            val verdictQuotePaint = TextPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                textSize = 23f
            }
            val quoteText = "«${report.aiVerdict}»"
            drawMultiline(
                quoteText,
                995f,
                785f,
                810,
                verdictQuotePaint,
                Layout.Alignment.ALIGN_NORMAL
            )

            // ══════════════ ВНЕШНЯЯ ГРАДИЕНТНАЯ РАМКА ══════════════
            val borderPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 4f
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(
                        primaryColorInt,
                        secondaryColorInt,
                        android.graphics.Color.parseColor("#69F0AE")
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, borderPaint)

            // Сохраняем в кэш и отправляем как картинку (16:9)
            val cacheFile = File(context.cacheDir, "durity_wrapped_report.png")
            val fos = FileOutputStream(cacheFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Поделиться итогами Durity Wrapped")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
