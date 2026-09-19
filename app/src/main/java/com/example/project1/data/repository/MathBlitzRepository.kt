package com.example.project1.data.repository

import com.example.project1.data.model.BlitzCategory
import com.example.project1.data.model.BlitzConfig
import com.example.project1.data.model.BlitzTask
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

object MathBlitzRepository {

    fun calculateRewardMultiplier(difficulty: Int, durationMinutes: Int, taskCount: Int): Double {
        val diffBonus = when (difficulty) {
            1 -> 0.20
            2 -> 0.40
            else -> 0.60
        }

        val timePerTaskSeconds = (durationMinutes * 60.0) / taskCount.toDouble()
        val timeBonus = when {
            timePerTaskSeconds <= 30.0 -> 0.40
            timePerTaskSeconds <= 45.0 -> 0.25
            timePerTaskSeconds <= 60.0 -> 0.15
            else -> 0.05
        }

        val totalMult = 1.0 + diffBonus + timeBonus
        // Максимум увеличение в 2 раза, чтобы не сломать экономику монет
        return minOf(totalMult, 2.0)
    }

    fun calculatePotentialWin(betCoins: Int, difficulty: Int, durationMinutes: Int, taskCount: Int): Int {
        val mult = calculateRewardMultiplier(difficulty, durationMinutes, taskCount)
        return (betCoins * mult).roundToInt()
    }

    private val OFFLINE_BLITZ_BANK = mapOf(
        BlitzCategory.MATRICES to listOf(
            BlitzTask("m1", "Матрицы", "\$\$\\det \\begin{pmatrix} 2 & 1 \\\\ 3 & 4 \\end{pmatrix}\$\$", "5", "Формула: a*d - b*c = 2*4 - 1*3"),
            BlitzTask("m2", "Матрицы", "\$\$\\det \\begin{pmatrix} 5 & 2 \\\\ 1 & 1 \\end{pmatrix}\$\$", "3", "Формула: 5*1 - 2*1"),
            BlitzTask("m3", "Матрицы", "\$\$\\det \\begin{pmatrix} 3 & -1 \\\\ 2 & 4 \\end{pmatrix}\$\$", "14", "Определитель 2x2: 3*4 - (-1)*2 = 12 + 2"),
            BlitzTask("m4", "Матрицы", "\$\$\\det \\begin{pmatrix} 4 & 0 \\\\ 7 & 3 \\end{pmatrix}\$\$", "12", "Произведение диагональных элементов: 4*3"),
            BlitzTask("m5", "Матрицы", "\$\$\\operatorname{tr} \\begin{pmatrix} 7 & 2 \\\\ 1 & 5 \\end{pmatrix}\$\$", "12", "След tr(A) — это сумма элементов главной диагонали: 7 + 5"),
            BlitzTask("m6", "Матрицы", "\$\$\\det \\begin{pmatrix} 1 & 0 & 0 \\\\ 2 & 3 & 0 \\\\ 4 & 5 & 2 \\end{pmatrix}\$\$", "6", "Для треугольной матрицы перемножь элементы главной диагонали: 1*3*2"),
            BlitzTask("m7", "Матрицы", "\$\$\\det \\begin{pmatrix} 2 & 3 \\\\ 4 & 6 \\end{pmatrix}\$\$", "0", "Строки пропорциональны, поэтому определитель равен 0"),
            BlitzTask("m8", "Матрицы", "\$\$\\begin{pmatrix} 1 & 2 \\\\ 3 & 4 \\end{pmatrix} + \\begin{pmatrix} 2 & 0 \\\\ 1 & 1 \\end{pmatrix}\$\$", "3 2; 4 5", "Сложи соответствующие элементы матриц: 1+2, 2+0; 3+1, 4+1")
        ),
        BlitzCategory.DERIVATIVES to listOf(
            BlitzTask("d1", "Производные", "\$\$f(x) = x^4 - 3x^2 + 5, \\quad f'(1)\$\$", "-2", "Производная: 4x^3 - 6x, подставь x = 1"),
            BlitzTask("d2", "Производные", "\$\$f(x) = \\sin(2x), \\quad f'(0)\$\$", "2", "Производная сложной функции: 2*cos(2x), подставь x = 0"),
            BlitzTask("d3", "Производные", "\$\$f(x) = e^{3x}, \\quad f'(0)\$\$", "3", "Производная: 3*e^(3x)"),
            BlitzTask("d4", "Производные", "\$\$f(x) = \\ln(x), \\quad f'(2)\$\$", "1/2", "Производная логарифма: 1/x"),
            BlitzTask("d5", "Производные", "\$\$f(x) = \\sqrt{x}, \\quad f'(4)\$\$", "1/4", "Формула: 1/(2*sqrt(x)), подставь x = 4"),
            BlitzTask("d6", "Производные", "\$\$f(x) = x \\ln(x), \\quad f'(1)\$\$", "1", "Производная произведения: ln(x) + 1")
        ),
        BlitzCategory.INTEGRALS to listOf(
            BlitzTask("i1", "Интегралы", "\$\$\\int_0^2 (3x^2) \\, dx\$\$", "8", "Первообразная x^3, подставь пределы: 2^3 - 0 = 8"),
            BlitzTask("i2", "Интегралы", "\$\$\\int_0^{\\pi} \\sin(x) \\, dx\$\$", "2", "Первообразная -cos(x): -cos(pi) - (-cos(0)) = 1 + 1 = 2"),
            BlitzTask("i3", "Интегралы", "\$\$\\int_0^1 e^x \\, dx\$\$", "e - 1", "Первообразная e^x: e^1 - e^0 = e - 1"),
            BlitzTask("i4", "Интегралы", "\$\$\\int_1^3 2x \\, dx\$\$", "8", "Первообразная x^2: 3^2 - 1^2 = 9 - 1 = 8"),
            BlitzTask("i5", "Интегралы", "\$\$\\int_0^1 \\frac{dx}{1 + x^2}\$\$", "pi/4", "Первообразная arctg(x): arctg(1) - arctg(0) = pi/4"),
            BlitzTask("i6", "Интегралы", "\$\$\\int_0^4 \\sqrt{x} \\, dx\$\$", "16/3", "Первообразная (2/3)*x^(3/2): (2/3)*8 = 16/3")
        ),
        BlitzCategory.LIMITS to listOf(
            BlitzTask("l1", "Пределы", "\$\$\\lim_{x \\to 0} \\frac{\\sin(3x)}{x}\$\$", "3", "Первый замечательный предел: sin(kx)/x -> k"),
            BlitzTask("l2", "Пределы", "\$\$\\lim_{x \\to \\infty} \\frac{4x^2 + 1}{2x^2 - 5}\$\$", "2", "Отношение коэффициентов при старших степенях: 4/2 = 2"),
            BlitzTask("l3", "Пределы", "\$\$\\lim_{x \\to 0} \\frac{e^{2x} - 1}{x}\$\$", "2", "Эквивалентность e^(kx) - 1 ~ kx при x -> 0"),
            BlitzTask("l4", "Пределы", "\$\$\\lim_{x \\to 2} \\frac{x^2 - 4}{x - 2}\$\$", "4", "Разложи разность квадратов: (x-2)(x+2)/(x-2) = x+2"),
            BlitzTask("l5", "Пределы", "\$\$\\lim_{x \\to 0} \\frac{1 - \\cos(x)}{x^2}\$\$", "1/2", "Эквивалентность 1 - cos(x) ~ x^2 / 2")
        ),
        BlitzCategory.COMPLEX to listOf(
            BlitzTask("c1", "ТФКП", "\$\$|3 + 4i|\$\$", "5", "Модуль: sqrt(3^2 + 4^2) = sqrt(25) = 5"),
            BlitzTask("c2", "ТФКП", "\$\$i^6\$\$", "-1", "Периодичность: i^4 = 1, i^6 = i^2 = -1"),
            BlitzTask("c3", "ТФКП", "\$\$(1 + i)(1 - i)\$\$", "2", "Разность квадратов: 1^2 - i^2 = 1 - (-1) = 2"),
            BlitzTask("c4", "ТФКП", "\$\$\\operatorname{Re}((2 + 3i)^2)\$\$", "-5", "(2+3i)^2 = 4 + 12i - 9 = -5 + 12i, действительная часть -5"),
            BlitzTask("c5", "ТФКП", "\$\$\\operatorname{Arg}(1 + i)\$\$", "pi/4", "Главное значение аргумента Arg(1+i) = pi/4"),
            BlitzTask("c6", "ТФКП", "\$\$\\frac{1}{i}\$\$", "-i", "Домножь на сопряженное: i / (i^2) = i / (-1) = -i"),
            BlitzTask("c7", "ТФКП", "\$\$i^3\$\$", "-i", "i^3 = i^2 * i = -1 * i = -i")
        ),
        BlitzCategory.TRIGONOMETRY to listOf(
            BlitzTask("t1", "Тригонометрия", "\$\$\\sin(\\pi/6) + \\cos(\\pi/3)\$\$", "1", "1/2 + 1/2 = 1"),
            BlitzTask("t2", "Тригонометрия", "\$\$\\tan(\\pi/4)\$\$", "1", "Тангенс 45 градусов равен 1"),
            BlitzTask("t3", "Тригонометрия", "\$\$\\cos^2(\\pi/8) + \\sin^2(\\pi/8)\$\$", "1", "Основное тригонометрическое тождество sin^2(x) + cos^2(x) = 1"),
            BlitzTask("t4", "Тригонометрия", "\$\$\\sin(2x) \\text{ при } \\sin(x)=\\frac{3}{5}, \\cos(x)=\\frac{4}{5}\$\$", "24/25", "Формула двойного угла: 2 * sin(x) * cos(x) = 2 * (3/5) * (4/5) = 24/25"),
            BlitzTask("t5", "Тригонометрия", "\$\$\\cos(2\\pi/3)\$\$", "-1/2", "Косинус 120 градусов равен -1/2")
        )
    )

    fun sanitizeLatexStatement(raw: String): String {
        var s = raw.trim()

        // 1. Нормализуем популярные юникод-символы математики в стандартные команды LaTeX
        s = s.replace("·", "\\cdot ")
            .replace("±", "\\pm ")
            .replace("×", "\\times ")
            .replace("÷", "\\div ")
            .replace("π", "\\pi ")
            .replace("√", "\\sqrt ")
            .replace("≤", "\\le ")
            .replace("≥", "\\ge ")
            .replace("≠", "\\neq ")
            .replace("≈", "\\approx ")
            .replace("∞", "\\infty ")

        // 2. Удаляем обертки \text{...} и \mathrm{...} с естественным языком
        s = s.replace(Regex("""\\text\{[^}]*\}"""), "")
        s = s.replace(Regex("""\\mathrm\{[^}]*\}"""), "")

        // 3. Если нейросеть сгенерировала союз или естественные слова ("или", "or", "and", "вычислите", "найдите" и т.д.)
        val wordOrForeignPattern = Regex("""(?:\s*(?:или|or|and|и|при|если|если\s+дано|то|затем|также|вычислите|найдите|найти|решите|또는|혹은|이자|و|أو)\s+|[^\x00-\x7F]+)""", RegexOption.IGNORE_CASE)
        val parts = s.split(wordOrForeignPattern).map { it.trim() }.filter { it.length >= 2 }
        if (parts.isNotEmpty()) {
            s = parts.first()
        }

        // 4. Бескомпромиссно удаляем ЛЮБЫЕ оставшиеся символы за пределами стандартного печатного ASCII (0x20 - 0x7E).
        s = s.replace(Regex("""[^\x20-\x7E]"""), "")

        // 5. Удаляем знаки вопроса, "= ?", "=?" на конце
        s = s.replace(Regex("""=\s*\??\s*$"""), "")
        s = s.replace("?", "")

        // 6. Очищаем от внешних $$ или $
        var inner = s.trim().removePrefix("$$").removeSuffix("$$").removePrefix("$").removeSuffix("$").trim()

        // 7. Очистка случайных синтаксических артефактов в комплексных числах и модулях:
        //    Убираем внешние скобки вокруг модуля: "(|3-4i|)" -> "|3-4i|", "[|3-4i|]" -> "|3-4i|"
        inner = inner.replace(Regex("""^\s*[\(\[]\s*(\|[^\|]+\|)\s*[\)\]]\s*$"""), "$1")
        //    Убираем висящие скобки рядом с модулем: "|3+4i|)" -> "|3+4i|", "(|3+4i|" -> "|3+4i|"
        inner = inner.replace(Regex("""(\|[^\|]+\|)\s*[\)\]]"""), "$1")
        inner = inner.replace(Regex("""[\(\[]\s*(\|[^\|]+\|)"""), "$1")

        // 8. Исправляем непарные вертикальные черты модуля:
        val pipeCount = inner.count { it == '|' }
        if (pipeCount % 2 != 0) {
            if (inner.startsWith("|") && !inner.endsWith("|")) {
                inner = "$inner|"
            } else if (!inner.startsWith("|") && inner.endsWith("|")) {
                inner = "|$inner"
            } else {
                inner = "$inner|"
            }
        }

        // 9. Балансировка круглых скобок (убираем сиротские закрывающие скобки)
        val openParen = inner.count { it == '(' }
        val closeParen = inner.count { it == ')' }
        if (closeParen > openParen) {
            val sb = StringBuilder()
            var depth = 0
            for (ch in inner) {
                if (ch == '(') depth++
                if (ch == ')') {
                    if (depth > 0) {
                        depth--
                    } else {
                        continue // Пропускаем сиротскую ')'
                    }
                }
                sb.append(ch)
            }
            inner = sb.toString()
        } else if (openParen > closeParen) {
            val missing = openParen - closeParen
            inner = inner + ")".repeat(missing)
        }

        // 10. Для матриц: если дана голая матрица \begin{pmatrix}...\end{pmatrix} без оператора,
        //     преобразуем её в явное вычисление определителя \det
        if (inner.startsWith("\\begin{pmatrix}") && inner.endsWith("\\end{pmatrix}")) {
            val hasOperator = inner.contains("+") || inner.contains("-") || inner.contains("\\cdot") ||
                              inner.contains("\\times") || inner.contains("^T") || inner.contains("^{-1}") ||
                              inner.contains("\\det") || inner.contains("\\operatorname{tr}")
            if (!hasOperator) {
                inner = "\\det $inner"
            }
        } else if (inner.startsWith("\\begin{matrix}") && inner.endsWith("\\end{matrix}")) {
            inner = "\\det $inner"
        }

        // 11. Убираем случайные двойные пробелы
        inner = inner.replace(Regex("""\s+"""), " ").trim()

        return "$$$inner$$"
    }

    fun generateProceduralTasks(config: BlitzConfig): List<BlitzTask> {
        val random = java.util.Random()
        val list = mutableListOf<BlitzTask>()

        when (config.category) {
            BlitzCategory.MATRICES -> {
                for (i in 1..config.taskCount) {
                    val opType = (1..6).random()
                    when (opType) {
                        1 -> {
                            // Сложение матриц
                            val a1 = (-4..6).random(); val a2 = (-4..6).random()
                            val a3 = (-4..6).random(); val a4 = (-4..6).random()
                            val b1 = (-4..6).random(); val b2 = (-4..6).random()
                            val b3 = (-4..6).random(); val b4 = (-4..6).random()
                            val stmt = "$$\\begin{pmatrix} $a1 & $a2 \\\\ $a3 & $a4 \\end{pmatrix} + \\begin{pmatrix} $b1 & $b2 \\\\ $b3 & $b4 \\end{pmatrix}$$"
                            val ans = "${a1 + b1} ${a2 + b2}; ${a3 + b3} ${a4 + b4}"
                            list.add(BlitzTask("m_add_$i", "Матрицы", stmt, ans, "Сложи элементы на одинаковых позициях."))
                        }
                        2 -> {
                            // Вычитание матриц
                            val a1 = (-4..6).random(); val a2 = (-4..6).random()
                            val a3 = (-4..6).random(); val a4 = (-4..6).random()
                            val b1 = (-3..5).random(); val b2 = (-3..5).random()
                            val b3 = (-3..5).random(); val b4 = (-3..5).random()
                            val stmt = "$$\\begin{pmatrix} $a1 & $a2 \\\\ $a3 & $a4 \\end{pmatrix} - \\begin{pmatrix} $b1 & $b2 \\\\ $b3 & $b4 \\end{pmatrix}$$"
                            val ans = "${a1 - b1} ${a2 - b2}; ${a3 - b3} ${a4 - b4}"
                            list.add(BlitzTask("m_sub_$i", "Матрицы", stmt, ans, "Вычти соответствующие элементы матриц."))
                        }
                        3 -> {
                            // Умножение на число
                            val k = listOf(-2, 2, 3, -1, 4).random()
                            val a1 = (-4..5).random(); val a2 = (-4..5).random()
                            val a3 = (-4..5).random(); val a4 = (-4..5).random()
                            val stmt = "$$${k} \\cdot \\begin{pmatrix} $a1 & $a2 \\\\ $a3 & $a4 \\end{pmatrix}$$"
                            val ans = "${k * a1} ${k * a2}; ${k * a3} ${k * a4}"
                            list.add(BlitzTask("m_scal_$i", "Матрицы", stmt, ans, "Умножь каждый элемент матрицы на число $k."))
                        }
                        4 -> {
                            // След матрицы tr(A)
                            val a1 = (-6..9).random(); val a2 = (-5..8).random()
                            val a3 = (-5..8).random(); val a4 = (-6..9).random()
                            val stmt = "$$\\operatorname{tr} \\begin{pmatrix} $a1 & $a2 \\\\ $a3 & $a4 \\end{pmatrix}$$"
                            val ans = "${a1 + a4}"
                            list.add(BlitzTask("m_tr_$i", "Матрицы", stmt, ans, "След tr(A) — это сумма элементов главной диагонали: $a1 + ($a4)."))
                        }
                        5 -> {
                            // Транспонирование
                            val a1 = (-5..7).random(); val a2 = (-5..7).random()
                            val a3 = (-5..7).random(); val a4 = (-5..7).random()
                            val stmt = "$$\\begin{pmatrix} $a1 & $a2 \\\\ $a3 & $a4 \\end{pmatrix}^T$$"
                            val ans = "$a1 $a3; $a2 $a4"
                            list.add(BlitzTask("m_trans_$i", "Матрицы", stmt, ans, "При транспонировании строки становятся столбцами."))
                        }
                        else -> {
                            // Определитель 2х2
                            val a = (-5..6).random(); val b = (-4..5).random()
                            val c = (-4..5).random(); val d = (-5..6).random()
                            val stmt = "$$\\det \\begin{pmatrix} $a & $b \\\\ $c & $d \\end{pmatrix}$$"
                            val ans = "${a * d - b * c}"
                            list.add(BlitzTask("m_det_$i", "Матрицы", stmt, ans, "Определитель 2x2 равен a*d - b*c: ($a)*($d) - ($b)*($c)."))
                        }
                    }
                }
            }
            BlitzCategory.COMPLEX -> {
                for (i in 1..config.taskCount) {
                    val opType = (1..6).random()
                    when (opType) {
                        1 -> {
                            // Модуль (Пифагоровы тройки или симметричные)
                            val pairs = listOf(
                                Triple(3, 4, "5"), Triple(4, 3, "5"),
                                Triple(1, 1, "sqrt(2)"), Triple(5, 12, "13"),
                                Triple(6, 8, "10"), Triple(8, 6, "10"),
                                Triple(0, 5, "5"), Triple(7, 0, "7"),
                                Triple(-3, 4, "5"), Triple(3, -4, "5"),
                                Triple(-4, -3, "5"), Triple(8, -6, "10")
                            )
                            val p = pairs.random()
                            val sign = if (p.second >= 0) "+" else "-"
                            val absB = kotlin.math.abs(p.second)
                            val expr = if (p.first == 0) "${p.second}i" else if (p.second == 0) "${p.first}" else "${p.first} $sign ${absB}i"
                            list.add(BlitzTask("c_mod_$i", "ТФКП", "$$|$expr|$$", p.third, "Модуль |a+bi| = sqrt(a^2 + b^2)."))
                        }
                        2 -> {
                            // Степени i
                            val powers = listOf(3 to "-i", 4 to "1", 5 to "i", 6 to "-1", 7 to "-i", 8 to "1", 9 to "i", 14 to "-1", 19 to "-i", 23 to "-i", 100 to "1", 2026 to "-1")
                            val (n, ans) = powers.random()
                            list.add(BlitzTask("c_pow_$i", "ТФКП", "$$i^{$n}$$", ans, "i^4 = 1, поэтому раздели показатель степени на 4 с остатком."))
                        }
                        3 -> {
                            // Сопряженное число
                            val a = (-6..8).random()
                            val b = listOf(-5, -4, -3, -2, 2, 3, 4, 5, 6).random()
                            val sign = if (b >= 0) "+" else "-"
                            val oppSign = if (b >= 0) "-" else "+"
                            val absB = kotlin.math.abs(b)
                            val stmt = "$$\\overline{$a $sign ${absB}i}$$"
                            val ans = "$a $oppSign ${absB}i"
                            list.add(BlitzTask("c_conj_$i", "ТФКП", stmt, ans, "При комплексном сопряжении знак перед мнимой частью меняется на противоположный."))
                        }
                        4 -> {
                            // Произведение сопряженных (a+bi)(a-bi)
                            val a = (1..5).random()
                            val b = (1..5).random()
                            val stmt = "$$($a + ${b}i)($a - ${b}i)$$"
                            val ans = "${a * a + b * b}"
                            list.add(BlitzTask("c_prod_$i", "ТФКП", stmt, ans, "Формула: (a+bi)(a-bi) = a^2 + b^2 = ${a * a} + ${b * b}."))
                        }
                        5 -> {
                            // Дроби 1/i
                            val k = listOf(1, -1, 2, 3, 5).random()
                            val stmt = if (k == 1) "$$\\frac{1}{i}$$" else "$$\\frac{$k}{i}$$"
                            val ans = if (k == 1) "-i" else if (k == -1) "i" else "-${k}i"
                            list.add(BlitzTask("c_divi_$i", "ТФКП", stmt, ans, "Домножь числитель и знаменатель на i: 1/i = i/i^2 = -i."))
                        }
                        else -> {
                            // Re или Im
                            val a = (-5..6).random()
                            val b = (-5..6).random()
                            val isRe = random.nextBoolean()
                            val sign = if (b >= 0) "+" else "-"
                            val absB = kotlin.math.abs(b)
                            val stmt = if (isRe) "$$\\operatorname{Re}($a $sign ${absB}i)$$" else "$$\\operatorname{Im}($a $sign ${absB}i)$$"
                            val ans = if (isRe) "$a" else "$b"
                            val desc = if (isRe) "Re(z) — это действительная часть: $a." else "Im(z) — это коэффициент при i: $b."
                            list.add(BlitzTask("c_part_$i", "ТФКП", stmt, ans, desc))
                        }
                    }
                }
            }
            BlitzCategory.DERIVATIVES -> {
                for (i in 1..config.taskCount) {
                    val opType = (1..6).random()
                    when (opType) {
                        1 -> {
                            // f(x) = a*x^3 + b*x, f'(x0)
                            val a = (1..4).random(); val b = (-5..5).random(); val x0 = listOf(0, 1, -1, 2).random()
                            val sign = if (b >= 0) "+ $b" else "- ${kotlin.math.abs(b)}"
                            val stmt = "\$\$f(x) = ${a}x^3 $sign x, \\quad f'($x0)\$\$"
                            val ans = "${3 * a * x0 * x0 + b}"
                            list.add(BlitzTask("d_poly_$i", "Производные", stmt, ans, "Найди производную f'(x) = ${3*a}x^2 $sign и подставь x = $x0."))
                        }
                        2 -> {
                            // f(x) = sin(k x), f'(0)
                            val k = (2..6).random()
                            val stmt = "\$\$f(x) = \\sin(${k}x), \\quad f'(0)\$\$"
                            val ans = "$k"
                            list.add(BlitzTask("d_sin_$i", "Производные", stmt, ans, "Производная: $k \\cos(${k}x), при x=0 значение равно $k."))
                        }
                        3 -> {
                            // f(x) = e^{k x}, f'(0)
                            val k = (-4..5).filter { it != 0 }.random()
                            val stmt = "\$\$f(x) = e^{${k}x}, \\quad f'(0)\$\$"
                            val ans = "$k"
                            list.add(BlitzTask("d_exp_$i", "Производные", stmt, ans, "Производная экспоненты: $k e^{${k}x}, при x=0 равно $k."))
                        }
                        4 -> {
                            // f(x) = sqrt(x), f'(x0)
                            val x0 = listOf(1 to "1/2", 4 to "1/4", 9 to "1/6", 16 to "1/8").random()
                            val stmt = "\$\$f(x) = \\sqrt{x}, \\quad f'(${x0.first})\$\$"
                            list.add(BlitzTask("d_sqrt_$i", "Производные", stmt, x0.second, "Производная корня: 1 / (2\\sqrt{x})."))
                        }
                        5 -> {
                            // f(x) = ln(k x), f'(x0)
                            val x0 = (1..4).random()
                            val stmt = "\$\$f(x) = \\ln(x), \\quad f'($x0)\$\$"
                            val ans = if (x0 == 1) "1" else "1/$x0"
                            list.add(BlitzTask("d_ln_$i", "Производные", stmt, ans, "Производная логарифма ln(x) равна 1/x."))
                        }
                        else -> {
                            // f(x) = x * ln(x), f'(1) или f(x) = x * e^x, f'(0)
                            val choice = listOf(
                                Triple("f(x) = x \\ln(x), \\quad f'(1)", "1", "Производная произведения: ln(x) + 1, при x=1 ответ 1."),
                                Triple("f(x) = x e^x, \\quad f'(0)", "1", "Производная: e^x(x + 1), при x=0 ответ 1."),
                                Triple("f(x) = \\cos(2x), \\quad f'(0)", "0", "Производная: -2\\sin(2x), при x=0 ответ 0.")
                            ).random()
                            list.add(BlitzTask("d_prod_$i", "Производные", "\$\$${choice.first}\$\$", choice.second, choice.third))
                        }
                    }
                }
            }
            BlitzCategory.INTEGRALS -> {
                for (i in 1..config.taskCount) {
                    val opType = (1..5).random()
                    when (opType) {
                        1 -> {
                            // \int_0^b a*x^n dx
                            val b = (1..3).random()
                            val n = (1..2).random()
                            if (n == 1) {
                                val a = 2 * (1..4).random()
                                val ans = "${(a / 2) * b * b}"
                                val stmt = "\$\$\\int_0^{$b} ${a}x \\, dx\$\$"
                                list.add(BlitzTask("i_poly_$i", "Интегралы", stmt, ans, "Первообразная: ${a/2}x^2, подставь пределы."))
                            } else {
                                val a = 3 * (1..3).random()
                                val ans = "${(a / 3) * b * b * b}"
                                val stmt = "\$\$\\int_0^{$b} ${a}x^2 \\, dx\$\$"
                                list.add(BlitzTask("i_poly_$i", "Интегралы", stmt, ans, "Первообразная: ${a/3}x^3, подставь пределы."))
                            }
                        }
                        2 -> {
                            // \int_0^\pi sin(x) dx = 2, \int_0^{\pi/2} cos(x) dx = 1
                            val choice = listOf(
                                Triple("\\int_0^{\\pi} \\sin(x) \\, dx", "2", "-cos(pi) - (-cos(0)) = 1 + 1 = 2"),
                                Triple("\\int_0^{\\pi/2} \\cos(x) \\, dx", "1", "sin(pi/2) - sin(0) = 1"),
                                Triple("\\int_0^{\\pi} \\cos(x) \\, dx", "0", "sin(pi) - sin(0) = 0")
                            ).random()
                            list.add(BlitzTask("i_trig_$i", "Интегралы", "\$\$${choice.first}\$\$", choice.second, choice.third))
                        }
                        3 -> {
                            // \int_0^1 e^x dx = e - 1
                            val stmt = "\$\$\\int_0^1 e^x \\, dx\$\$"
                            list.add(BlitzTask("i_exp_$i", "Интегралы", stmt, "e - 1", "Первообразная e^x: e^1 - e^0 = e - 1."))
                        }
                        4 -> {
                            // \int_0^1 1/(1+x^2) dx = pi/4
                            val stmt = "\$\$\\int_0^1 \\frac{dx}{1 + x^2}\$\$"
                            list.add(BlitzTask("i_atan_$i", "Интегралы", stmt, "pi/4", "Первообразная arctg(x): arctg(1) - arctg(0) = pi/4."))
                        }
                        else -> {
                            // \int_1^e 1/x dx = 1
                            val stmt = "\$\$\\int_1^e \\frac{dx}{x}\$\$"
                            list.add(BlitzTask("i_ln_$i", "Интегралы", stmt, "1", "Первообразная ln(x): ln(e) - ln(1) = 1 - 0 = 1."))
                        }
                    }
                }
            }
            BlitzCategory.LIMITS -> {
                for (i in 1..config.taskCount) {
                    val opType = (1..5).random()
                    when (opType) {
                        1 -> {
                            // sin(kx)/x -> k
                            val k = (2..7).random()
                            val stmt = "\$\$\\lim_{x \\to 0} \\frac{\\sin(${k}x)}{x}\$\$"
                            list.add(BlitzTask("l_sin_$i", "Пределы", stmt, "$k", "Первый замечательный предел: sin(kx)/x -> $k."))
                        }
                        2 -> {
                            // (a*x^2 + b)/(c*x^2 + d) -> a/c
                            val a = listOf(2, 4, 6, 8).random()
                            val c = listOf(1, 2).random()
                            val stmt = "\$\$\\lim_{x \\to \\infty} \\frac{${a}x^2 + 1}{${c}x^2 - 3}\$\$"
                            val ans = if (a % c == 0) "${a / c}" else "$a/$c"
                            list.add(BlitzTask("l_inf_$i", "Пределы", stmt, ans, "Отношение коэффициентов при старших степенях: $a / $c."))
                        }
                        3 -> {
                            // (e^{kx} - 1)/x -> k
                            val k = (2..5).random()
                            val stmt = "\$\$\\lim_{x \\to 0} \\frac{e^{${k}x} - 1}{x}\$\$"
                            list.add(BlitzTask("l_exp_$i", "Пределы", stmt, "$k", "Эквивалентность: e^{kx} - 1 ~ kx при x -> 0."))
                        }
                        4 -> {
                            // (x^2 - a^2)/(x - a) -> 2a
                            val a = (2..5).random()
                            val stmt = "\$\$\\lim_{x \\to $a} \\frac{x^2 - ${a*a}}{x - $a}\$\$"
                            list.add(BlitzTask("l_poly_$i", "Пределы", stmt, "${2*a}", "Разложи разность квадратов: (x-$a)(x+$a)/(x-$a) = x+$a -> ${2*a}."))
                        }
                        else -> {
                            // (1 - cos(x))/x^2 -> 1/2
                            val stmt = "\$\$\\lim_{x \\to 0} \\frac{1 - \\cos(x)}{x^2}\$\$"
                            list.add(BlitzTask("l_cos_$i", "Пределы", stmt, "1/2", "Эквивалентность 1 - cos(x) ~ x^2 / 2."))
                        }
                    }
                }
            }
            BlitzCategory.TRIGONOMETRY -> {
                for (i in 1..config.taskCount) {
                    val opType = (1..5).random()
                    when (opType) {
                        1 -> {
                            val items = listOf(
                                Triple("\\sin(\\pi/6) + \\cos(\\pi/3)", "1", "1/2 + 1/2 = 1"),
                                Triple("\\tan(\\pi/4)", "1", "tg(45) = 1"),
                                Triple("\\cos(\\pi)", "-1", "cos(pi) = -1"),
                                Triple("\\sin(\\pi/2)", "1", "sin(pi/2) = 1"),
                                Triple("\\cos(2\\pi/3)", "-1/2", "cos(120) = -1/2"),
                                Triple("\\sin(5\\pi/6)", "1/2", "sin(150) = 1/2")
                            )
                            val item = items.random()
                            list.add(BlitzTask("t_val_$i", "Тригонометрия", "\$\$${item.first}\$\$", item.second, item.third))
                        }
                        2 -> {
                            val k = (2..9).random()
                            val stmt = "\$\$\\sin^2(\\pi/$k) + \\cos^2(\\pi/$k)\$\$"
                            list.add(BlitzTask("t_ident_$i", "Тригонометрия", stmt, "1", "Основное тригонометрическое тождество sin^2(x) + cos^2(x) = 1."))
                        }
                        3 -> {
                            val stmt = "\$\$\\cos^2(\\pi/4) - \\sin^2(\\pi/4)\$\$"
                            list.add(BlitzTask("t_dbl_$i", "Тригонометрия", stmt, "0", "Формула косинуса двойного угла: cos(2 * pi/4) = cos(pi/2) = 0."))
                        }
                        4 -> {
                            val stmt = "\$\$2 \\sin(\\pi/6) \\cos(\\pi/6)\$\$"
                            list.add(BlitzTask("t_sin2_$i", "Тригонометрия", stmt, "sqrt(3)/2", "Синус двойного угла: sin(2 * pi/6) = sin(pi/3) = sqrt(3)/2."))
                        }
                        else -> {
                            val stmt = "\$\$\\tan(\\pi/3)\$\$"
                            list.add(BlitzTask("t_tg_$i", "Тригонометрия", stmt, "sqrt(3)", "Тангенс 60 градусов равен sqrt(3)."))
                        }
                    }
                }
            }
        }
        return list
    }

    suspend fun generateBlitzTasks(config: BlitzConfig, apiKey: String): List<BlitzTask> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext generateProceduralTasks(config)
        }

        try {
            val model = GenerativeModel(modelName = "gemini-3.5-flash-lite", apiKey = apiKey)
            val diversityHint = when (config.category) {
                BlitzCategory.MATRICES -> "РАЗНООБРАЗЬ ОПЕРАЦИИ: используй не только определители, но и сложение матриц A+B, вычитание A-B, умножение на скаляр k*A, след tr(A), транспонирование A^T, определители 2x2. ВАЖНО: Если требуется найти определитель — ОБЯЗАТЕЛЬНО пиши \\det \\begin{pmatrix} ... \\end{pmatrix}, НИКОГДА не оставляй одинокую матрицу без оператора!"
                BlitzCategory.COMPLEX -> "Раздел: ТФКП и комплексные числа. РАЗНООБРАЗЬ ЗАДАЧИ: модуль |z|, степени мнимой единицы i^n, Re(z), Im(z), сопряженное число, произведение (a+bi)(a-bi). ВАЖНО: Для модуля пиши строго |a+bi|, НИ В КОЕМ СЛУЧАЕ НЕ ставь лишние скобки вроде (|a+bi|) или |a+bi|). Следи за строгим балансом скобок!"
                BlitzCategory.DERIVATIVES -> "Раздел: Производные. Формулируй: f(x) = ..., \\quad f'(x_0) или \\frac{d}{dx}(...)|_{x=x_0}."
                BlitzCategory.INTEGRALS -> "Раздел: Определенные интегралы \\int_a^b ... dx с простым числовым или константным ответом."
                BlitzCategory.LIMITS -> "Раздел: Пределы \\lim_{x \\to a} ... с числовым ответом."
                BlitzCategory.TRIGONOMETRY -> "Раздел: Тригонометрические значения и тождества."
            }

            val prompt = """
Сгенерируй ровно ${config.taskCount} быстрых математических задач для блица на время.
Раздел: ${config.category.title}
Сложность (1-легко, 2-средне, 3-сложно): ${config.difficulty}
Случайный сид: ${System.currentTimeMillis() % 100000}

КРИТИЧЕСКИЕ ТРЕБОВАНИЯ:
- $diversityHint
- Задачи должны решаться устно или за 30-45 секунд на черновике.
- latexStatement: СТРОГО ОДНА чистая математическая формула в двойных долларах $${'$'}...$${'$'}.
  КАТЕГОРИЧЕСКИ ЗАПРЕЩЕНО писать в latexStatement любые слова на ЛЮБОМ языке (русский, английский, корейский и др.)! НИКАКИХ "или", "or", "and", "вычислите", "найдите"!
  Запрещены любые не-математические символы, знаки вопроса, текст, альтернативы, лишние/непарные скобки.
  ТОЛЬКО латинские математические символы, цифры и стандартные команды LaTeX.
  НЕ повторяй одни и те же числа! Используй разные числа.
- Для матриц: если ответом является матрица 2х2, запиши её элементы через пробелы в строке и точку с запятой между строками: например "3 5; 1 2".
- correctAnswer: краткий эталон ответа (число, формула или матрица "a b; c d").
- hint: подсказка в 1 короткое предложение для ученика при ошибке на русском языке.

Верни СТРОГО валидный JSON массив без лишних пояснений:
[
  {
    "latexStatement": "$${'$'}...$${'$'}",
    "correctAnswer": "...",
    "hint": "..."
  }
]
            """.trimIndent()

            val response = model.generateContent(prompt)
            val raw = response.text?.trim() ?: ""
            val cleaned = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val safeJson = cleaned.replace(Regex("""(?<!\\)\\to(?![a-zA-Z])"""), "\\\\to")
            val arr = JSONArray(safeJson)

            val result = mutableListOf<BlitzTask>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val sanitized = sanitizeLatexStatement(obj.getString("latexStatement"))
                val inner = sanitized.removePrefix("$$").removeSuffix("$$").trim()
                if (inner.length < 2) continue

                result.add(
                    BlitzTask(
                        id = "ai_${System.currentTimeMillis()}_$i",
                        category = config.category.title,
                        latexStatement = sanitized,
                        correctAnswer = obj.getString("correctAnswer").trim(),
                        hint = obj.optString("hint", "Внимательно проверь вычисления.")
                    )
                )
            }
            if (result.size >= config.taskCount) result.take(config.taskCount) else generateProceduralTasks(config)
        } catch (_: Exception) {
            generateProceduralTasks(config)
        }
    }

    private fun generateFallback(config: BlitzConfig): List<BlitzTask> {
        return (1..config.taskCount).map { i ->
            BlitzTask(
                id = "gen_$i",
                category = config.category.title,
                latexStatement = "\$\$2 + $i \\cdot 3\$\$",
                correctAnswer = "${2 + i * 3}",
                hint = "Сначала умножение, затем сложение."
            )
        }
    }

    private fun normalizeMatrixAnswer(s: String): List<String>? {
        val trimmed = s.trim()
        if (!trimmed.contains(";") && !trimmed.contains("\n") && !trimmed.contains("],") && !trimmed.contains("\\\\")) {
            return null
        }
        val rows = trimmed.replace("[", "").replace("]", "").replace("pmatrix", "")
            .split(Regex("""(?:\s*;\s*|\s*\\\\\s*|\n+)"""))
            .filter { it.isNotBlank() }
        val tokens = rows.flatMap { row ->
            row.split(Regex("""[\s,&]+""")).filter { it.isNotBlank() }
        }
        return if (tokens.isNotEmpty()) tokens else null
    }

    private fun normalizeComplexOrMath(s: String): String {
        var str = s.trim().lowercase()
            .replace(" ", "")
            .replace("$$", "")
            .replace("$", "")
            .replace("\\frac{", "")
            .replace("}{", "/")
            .replace("}", "")
            .replace("\\cdot", "*")
            .replace("·", "*")
            .replace("\\pm", "±")
            .replace("\\sqrt{", "sqrt(")
            .replace("\\pi", "pi")

        // 1/i = -i
        if (str == "1/i" || str == "(1/i)" || str == "+1/i") return "-i"
        if (str == "-1/i" || str == "-(1/i)") return "i"
        if (str == "1/-i" || str == "1/(-i)") return "i"
        if (str == "-1/-i") return "-i"

        // -1i -> -i, +1i -> i, 1i -> i
        if (str == "-1i" || str == "-1*i" || str == "0-1i" || str == "0-i" || str == "-i+0") return "-i"
        if (str == "1i" || str == "+1i" || str == "+1*i" || str == "1*i" || str == "0+1i" || str == "0+i" || str == "i+0") return "i"

        // Степени i
        if (str == "i^1" || str == "i^{1}") return "i"
        if (str == "i^2" || str == "i^{2}") return "-1"
        if (str == "i^3" || str == "i^{3}") return "-i"
        if (str == "i^4" || str == "i^{4}" || str == "i^0") return "1"
        if (str == "i^5" || str == "i^{5}") return "i"
        if (str == "i^6" || str == "i^{6}") return "-1"
        if (str == "i^7" || str == "i^{7}") return "-i"
        if (str == "i^8" || str == "i^{8}") return "1"

        return str
    }

    suspend fun checkBlitzAnswer(
        task: BlitzTask,
        userAnswer: String,
        apiKey: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanUser = normalizeComplexOrMath(userAnswer)
        val cleanExpected = normalizeComplexOrMath(task.correctAnswer)

        // 1. Быстрая прямая и нормализованная проверка
        if (cleanUser == cleanExpected) {
            return@withContext Pair(true, "Верно!")
        }

        // Числовая нормализация (0.5 vs 1/2, 0.25 vs 1/4, 0.75 vs 3/4)
        if ((cleanUser == "1/2" && cleanExpected == "0.5") || (cleanUser == "0.5" && cleanExpected == "1/2")) return@withContext Pair(true, "Верно!")
        if ((cleanUser == "1/4" && cleanExpected == "0.25") || (cleanUser == "0.25" && cleanExpected == "1/4")) return@withContext Pair(true, "Верно!")
        if ((cleanUser == "3/4" && cleanExpected == "0.75") || (cleanUser == "0.75" && cleanExpected == "3/4")) return@withContext Pair(true, "Верно!")
        if ((cleanUser == "-1/2" && cleanExpected == "-0.5") || (cleanUser == "-0.5" && cleanExpected == "-1/2")) return@withContext Pair(true, "Верно!")

        // Прямая проверка условия задачи для популярных случаев ТФКП
        val stmt = normalizeComplexOrMath(task.latexStatement)
        if ((stmt.contains("1/i") || stmt.contains("\\frac{1}{i}")) && cleanUser == "-i") {
            return@withContext Pair(true, "Верно!")
        }
        if ((stmt.contains("i^3") || stmt.contains("i^{3}") || stmt.contains("i^7") || stmt.contains("i^{7}") || stmt.contains("i^{19}") || stmt.contains("i^{23}")) && cleanUser == "-i") {
            return@withContext Pair(true, "Верно!")
        }
        if ((stmt.contains("i^2") || stmt.contains("i^{2}") || stmt.contains("i^6") || stmt.contains("i^{6}") || stmt.contains("i^{14}") || stmt.contains("i^{2026}")) && cleanUser == "-1") {
            return@withContext Pair(true, "Верно!")
        }
        if ((stmt.contains("i^4") || stmt.contains("i^{4}") || stmt.contains("i^8") || stmt.contains("i^{8}") || stmt.contains("i^{100}")) && cleanUser == "1") {
            return@withContext Pair(true, "Верно!")
        }
        if ((stmt.contains("|3+4i|") || stmt.contains("|3-4i|") || stmt.contains("|-3+4i|") || stmt.contains("|-3-4i|") || stmt.contains("|4+3i|") || stmt.contains("|4-3i|")) && cleanUser == "5") {
            return@withContext Pair(true, "Верно!")
        }
        if ((stmt.contains("|5+12i|") || stmt.contains("|5-12i|") || stmt.contains("|12+5i|") || stmt.contains("|12-5i|")) && cleanUser == "13") {
            return@withContext Pair(true, "Верно!")
        }

        // Матричная нормализация
        val userMatrix = normalizeMatrixAnswer(userAnswer)
        val expectedMatrix = normalizeMatrixAnswer(task.correctAnswer)
        if (userMatrix != null && expectedMatrix != null && userMatrix == expectedMatrix) {
            return@withContext Pair(true, "Верно!")
        }

        // 2. Если не совпало и есть API-ключ — спрашиваем Gemini для независимого математического решения
        if (apiKey.isNotBlank()) {
            try {
                val model = GenerativeModel(modelName = "gemini-3.5-flash-lite", apiKey = apiKey)
                val prompt = """
Ты беспристрастный математический эксперт и проверяешь ответ ученика на короткую задачу блица.
Условие задачи: ${task.latexStatement}
Ориентировочный ответ в системе: ${task.correctAnswer}
Ответ ученика: $userAnswer

ИНСТРУКЦИЯ ПО ПРОВЕРКЕ:
1. Сначала самостоятельно реши задачу из Условия строго математически (Chain of Thought).
   (Например: для \frac{1}{i} верный ответ строго -i; для i^3 ответ -i; для i^6 ответ -1; для |3-4i| ответ 5; для \det [[a,b],[c,d]] ответ a*d-b*c).
2. Сравни ответ ученика с математически правильным результатом решения Условия. Не полагайся слепо на ориентировочный ответ, если в нем опечатка!
3. Формат записи может отличаться (например матрицы через точку с запятой / пробелы / скобки, знаки пробелов в комплексных числах a+bi, дроби 1/2 vs 0.5, корни sqrt(2) vs \sqrt{2}). Если ответ ученика равен правильному значению, считай его верным!
4. Если ответ ученика математически верен, верни isCorrect: true.
5. Если ответ не верен, верни isCorrect: false и краткую подсказку в 1 предложение на русском языке без прямого спойлера ответа.

Ответь СТРОГО в формате JSON:
{
  "thought": "Математическое решение задачи и сравнение",
  "isCorrect": true/false,
  "hint": "Краткая подсказка в 1 предложение на русском при ошибке"
}
                """.trimIndent()

                val response = model.generateContent(prompt)
                val raw = response.text?.trim() ?: ""
                val cleaned = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = JSONObject(cleaned)
                val isCorrect = json.optBoolean("isCorrect", false)
                val hint = json.optString("hint", task.hint.ifBlank { "Проверь вычисления и знак." })
                return@withContext Pair(isCorrect, if (isCorrect) "Верно!" else hint)
            } catch (_: Exception) {
            }
        }

        val fallbackHint = task.hint.ifBlank { "Неверный ответ. Попробуй еще раз!" }
        return@withContext Pair(false, fallbackHint)
    }
}
