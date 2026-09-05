package com.example.project1.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project1.ui.components.KatexViewLeft

// ── Модели ────────────────────────────────────────────────────────────────────

sealed class MathEntry {
    /** Просто пара формула → пояснение, обе в LaTeX */
    data class Row(val formula: String, val hint: String) : MathEntry()
    /** Только большой блок LaTeX (для матриц, рядов и т.д.) */
    data class Block(val latex: String) : MathEntry()
    /** Подзаголовок внутри секции */
    data class SubHeader(val text: String) : MathEntry()
}

data class MathSection(
    val title: String,
    val emoji: String,
    val accentColor: Color,
    val entries: List<MathEntry>
)

// ── Данные ────────────────────────────────────────────────────────────────────

val mathSections: List<MathSection> = listOf(

    MathSection(
        title = "Производные",
        emoji = "∂",
        accentColor = Color(0xFF7C4DFF),
        entries = listOf(
            MathEntry.Row("C' = 0",                             "константа"),
            MathEntry.Row("(x^n)' = n x^{n-1}",                "степень"),
            MathEntry.Row("(\\sqrt{x})' = \\frac{1}{2\\sqrt{x}}", "корень"),
            MathEntry.Row("(e^x)' = e^x",                       "экспонента e"),
            MathEntry.Row("(a^x)' = a^x \\ln a",               "экспонента a"),
            MathEntry.Row("(\\ln x)' = \\frac{1}{x}",          "натур. логарифм"),
            MathEntry.Row("(\\log_a x)' = \\frac{1}{x \\ln a}","логарифм"),
            MathEntry.Row("(\\sin x)' = \\cos x",              "синус"),
            MathEntry.Row("(\\cos x)' = -\\sin x",             "косинус"),
            MathEntry.Row("(\\tan x)' = \\frac{1}{\\cos^2 x}", "тангенс"),
            MathEntry.Row("(\\cot x)' = -\\frac{1}{\\sin^2 x}","котангенс"),
            MathEntry.Row("(\\arcsin x)' = \\frac{1}{\\sqrt{1-x^2}}", "арксинус"),
            MathEntry.Row("(\\arccos x)' = -\\frac{1}{\\sqrt{1-x^2}}","арккосинус"),
            MathEntry.Row("(\\arctan x)' = \\frac{1}{1+x^2}",  "арктангенс"),
            MathEntry.SubHeader("Правила"),
            MathEntry.Row("(uv)' = u'v + uv'",                 "произведение"),
            MathEntry.Row("\\left(\\frac{u}{v}\\right)' = \\frac{u'v - uv'}{v^2}", "частное"),
            MathEntry.Row("(f(g(x)))' = f'(g) \\cdot g'(x)",   "сложная функция")
        )
    ),

    MathSection(
        title = "Первообразные",
        emoji = "∫",
        accentColor = Color(0xFF00BCD4),
        entries = listOf(
            MathEntry.Row("\\int x^n\\,dx = \\frac{x^{n+1}}{n+1} + C",      "степень, n≠−1"),
            MathEntry.Row("\\int \\frac{1}{x}\\,dx = \\ln|x| + C",           "обратная"),
            MathEntry.Row("\\int e^x\\,dx = e^x + C",                         "экспонента e"),
            MathEntry.Row("\\int a^x\\,dx = \\frac{a^x}{\\ln a} + C",        "экспонента a"),
            MathEntry.Row("\\int \\sin x\\,dx = -\\cos x + C",               "синус"),
            MathEntry.Row("\\int \\cos x\\,dx = \\sin x + C",                "косинус"),
            MathEntry.Row("\\int \\frac{dx}{\\cos^2 x} = \\tan x + C",       "1/cos²"),
            MathEntry.Row("\\int \\frac{dx}{\\sin^2 x} = -\\cot x + C",      "1/sin²"),
            MathEntry.Row("\\int \\frac{dx}{\\sqrt{1-x^2}} = \\arcsin x + C","арксинус"),
            MathEntry.Row("\\int \\frac{dx}{1+x^2} = \\arctan x + C",        "арктангенс"),
            MathEntry.Row("\\int \\frac{dx}{\\sqrt{x^2 \\pm a^2}} = \\ln\\left|x+\\sqrt{x^2\\pm a^2}\\right| + C", "гиперб."),
            MathEntry.SubHeader("Методы интегрирования"),
            MathEntry.Row("\\int u\\,dv = uv - \\int v\\,du",                "по частям"),
            MathEntry.Row("\\int f(g(x))g'(x)\\,dx = F(g(x)) + C",          "подстановка")
        )
    ),

    MathSection(
        title = "Эквивалентности",
        emoji = "≈",
        accentColor = Color(0xFF4CAF50),
        entries = listOf(
            MathEntry.SubHeader("При x → 0"),
            MathEntry.Row("\\sin x \\sim x",                   "синус"),
            MathEntry.Row("\\tan x \\sim x",                   "тангенс"),
            MathEntry.Row("\\arcsin x \\sim x",                "арксинус"),
            MathEntry.Row("\\arctan x \\sim x",                "арктангенс"),
            MathEntry.Row("1 - \\cos x \\sim \\frac{x^2}{2}", "косинус"),
            MathEntry.Row("\\ln(1+x) \\sim x",                 "логарифм"),
            MathEntry.Row("e^x - 1 \\sim x",                   "экспонента"),
            MathEntry.Row("a^x - 1 \\sim x\\ln a",             "общая экспонента"),
            MathEntry.Row("(1+x)^n - 1 \\sim nx",              "степень"),
            MathEntry.Row("\\sqrt[n]{1+x} - 1 \\sim \\frac{x}{n}", "корень n-й степени"),
            MathEntry.Row("\\sinh x \\sim x",                  "гиперб. синус"),
            MathEntry.Row("\\log_a(1+x) \\sim \\frac{x}{\\ln a}", "логарифм основание a")
        )
    ),

    MathSection(
        title = "Разложения в ряды",
        emoji = "Σ",
        accentColor = Color(0xFFFF9800),
        entries = listOf(
            MathEntry.SubHeader("Ряд Тейлора (общая формула)"),
            MathEntry.Block(
                "f(x) = \\sum_{n=0}^{\\infty}\\frac{f^{(n)}(a)}{n!}(x-a)^n = f(a) + f'(a)(x-a) + \\frac{f''(a)}{2!}(x-a)^2 + \\cdots"
            ),
            MathEntry.Block(
                "R_n(x) = \\frac{f^{(n+1)}(\\xi)}{(n+1)!}(x-a)^{n+1}, \\quad \\xi \\in (a,\\, x) \\quad \\text{(остаток Лагранжа)}"
            ),
            MathEntry.SubHeader("Ряды Маклорена (a = 0)"),
            MathEntry.Block(
                "e^x = \\sum_{n=0}^{\\infty}\\frac{x^n}{n!} = 1 + x + \\frac{x^2}{2!} + \\frac{x^3}{3!} + \\cdots"
            ),
            MathEntry.Block(
                "\\sin x = \\sum_{n=0}^{\\infty}\\frac{(-1)^n x^{2n+1}}{(2n+1)!} = x - \\frac{x^3}{6} + \\frac{x^5}{120} - \\cdots"
            ),
            MathEntry.Block(
                "\\cos x = \\sum_{n=0}^{\\infty}\\frac{(-1)^n x^{2n}}{(2n)!} = 1 - \\frac{x^2}{2} + \\frac{x^4}{24} - \\cdots"
            ),
            MathEntry.Block(
                "\\ln(1+x) = \\sum_{n=1}^{\\infty}\\frac{(-1)^{n+1}x^n}{n} = x - \\frac{x^2}{2} + \\frac{x^3}{3} - \\cdots, \\quad |x| \\leq 1"
            ),
            MathEntry.Block(
                "\\frac{1}{1-x} = \\sum_{n=0}^{\\infty} x^n = 1 + x + x^2 + x^3 + \\cdots, \\quad |x| < 1"
            ),
            MathEntry.Block(
                "\\arctan x = x - \\frac{x^3}{3} + \\frac{x^5}{5} - \\cdots, \\quad |x| \\leq 1"
            ),
            MathEntry.Block(
                "\\arcsin x = x + \\frac{x^3}{6} + \\frac{3x^5}{40} + \\cdots, \\quad |x| < 1"
            ),
            MathEntry.Block(
                "\\sinh x = x + \\frac{x^3}{6} + \\frac{x^5}{120} + \\cdots"
            ),
            MathEntry.Block(
                "\\cosh x = 1 + \\frac{x^2}{2} + \\frac{x^4}{24} + \\cdots"
            ),
            MathEntry.SubHeader("Бином Ньютона"),
            MathEntry.Block(
                "(a+b)^n = \\sum_{k=0}^{n}\\binom{n}{k}a^{n-k}b^k, \\quad \\binom{n}{k} = \\frac{n!}{k!\\,(n-k)!}"
            ),
            MathEntry.Block(
                "(a+b)^n = a^n + na^{n-1}b + \\frac{n(n-1)}{2!}a^{n-2}b^2 + \\cdots + nab^{n-1} + b^n"
            ),
            MathEntry.Block(
                "(1+x)^n = \\sum_{k=0}^{n}\\binom{n}{k}x^k = 1 + nx + \\frac{n(n-1)}{2!}x^2 + \\cdots + x^n"
            ),
            MathEntry.SubHeader("Разложение степенных разностей"),
            MathEntry.Block(
                "a^n - b^n = (a - b)\\left(a^{n-1} + a^{n-2}b + a^{n-3}b^2 + \\cdots + ab^{n-2} + b^{n-1}\\right) = (a-b)\\sum_{k=0}^{n-1}a^{n-1-k}b^k"
            ),
            MathEntry.Block(
                "a^n + b^n = (a + b)\\left(a^{n-1} - a^{n-2}b + a^{n-3}b^2 - \\cdots - ab^{n-2} + b^{n-1}\\right), \\quad n \\text{ — нечётное}"
            ),
            MathEntry.Block(
                "a^{2n} - b^{2n} = (a-b)(a+b)\\left(a^{2n-2} + a^{2n-4}b^2 + \\cdots + b^{2n-2}\\right)"
            ),
            MathEntry.SubHeader("Ряд Фурье"),
            MathEntry.Block(
                "f(x) = \\frac{a_0}{2} + \\sum_{n=1}^{\\infty}\\left(a_n \\cos\\frac{\\pi n x}{L} + b_n \\sin\\frac{\\pi n x}{L}\\right), \\quad x \\in [-L,\\, L]"
            ),
            MathEntry.Block(
                "a_0 = \\frac{1}{L}\\int_{-L}^{L} f(x)\\,dx, \\quad a_n = \\frac{1}{L}\\int_{-L}^{L} f(x)\\cos\\frac{\\pi n x}{L}\\,dx, \\quad b_n = \\frac{1}{L}\\int_{-L}^{L} f(x)\\sin\\frac{\\pi n x}{L}\\,dx"
            ),
            MathEntry.Block(
                "\\text{Комплексная форма: } f(x) = \\sum_{n=-\\infty}^{+\\infty} c_n e^{i\\pi n x/L}, \\quad c_n = \\frac{1}{2L}\\int_{-L}^{L} f(x)e^{-i\\pi n x/L}\\,dx"
            ),
            MathEntry.Block(
                "\\text{Равенство Парсеваля: } \\frac{1}{L}\\int_{-L}^{L}|f(x)|^2\\,dx = \\frac{a_0^2}{2} + \\sum_{n=1}^{\\infty}(a_n^2 + b_n^2) = 2\\sum_{n=-\\infty}^{+\\infty}|c_n|^2"
            ),
            MathEntry.SubHeader("Гармонический ряд"),
            MathEntry.Block(
                "\\sum_{n=1}^{\\infty}\\frac{1}{n} = 1 + \\frac{1}{2} + \\frac{1}{3} + \\cdots = +\\infty \\quad \\text{(расходится)}"
            ),
            MathEntry.Block(
                "H_n = \\sum_{k=1}^{n}\\frac{1}{k} = \\ln n + \\gamma + O\\!\\left(\\frac{1}{n}\\right), \\quad \\gamma \\approx 0.5772 \\quad \\text{(Эйлер–Маскерони)}"
            ),
            MathEntry.Block(
                "\\sum_{n=1}^{\\infty}\\frac{1}{n^p} \\text{ сходится при } p > 1, \\text{ расходится при } p \\leq 1 \\quad \\text{(p-ряд)}"
            )
        )
    ),

    MathSection(
        title = "Спец. функции",
        emoji = "Γ",
        accentColor = Color(0xFFAB47BC),
        entries = listOf(
            MathEntry.SubHeader("Гамма-функция Γ(z)"),
            MathEntry.Block(
                "\\Gamma(z) = \\int_0^{\\infty} t^{z-1} e^{-t}\\,dt, \\quad \\operatorname{Re}(z) > 0"
            ),
            MathEntry.Block(
                "\\Gamma(z+1) = z\\,\\Gamma(z) \\quad \\text{(функциональное уравнение)}"
            ),
            MathEntry.Block(
                "\\Gamma(n+1) = n!, \\quad n \\in \\mathbb{N}_0"
            ),
            MathEntry.Block(
                "\\Gamma\\!\\left(\\tfrac{1}{2}\\right) = \\sqrt{\\pi}, \\quad \\Gamma\\!\\left(\\tfrac{3}{2}\\right) = \\tfrac{\\sqrt{\\pi}}{2}, \\quad \\Gamma(1) = 1"
            ),
            MathEntry.Block(
                "\\Gamma(z)\\,\\Gamma(1-z) = \\frac{\\pi}{\\sin(\\pi z)} \\quad \\text{(формула отражения Эйлера)}"
            ),
            MathEntry.Block(
                "\\Gamma(2z) = \\frac{2^{2z-1}}{\\sqrt{\\pi}}\\,\\Gamma(z)\\,\\Gamma\\!\\left(z+\\tfrac{1}{2}\\right) \\quad \\text{(формула дублирования Лежандра)}"
            ),
            MathEntry.Block(
                "\\Gamma(z) \\approx \\sqrt{\\frac{2\\pi}{z}}\\left(\\frac{z}{e}\\right)^z \\quad \\text{(формула Стирлинга)}"
            ),

            MathEntry.SubHeader("Дигамма-функция ψ(z)"),
            MathEntry.Block(
                "\\psi(z) = \\frac{d}{dz}\\ln\\Gamma(z) = \\frac{\\Gamma'(z)}{\\Gamma(z)}"
            ),
            MathEntry.Block(
                "\\psi(z+1) = \\psi(z) + \\frac{1}{z} \\quad \\text{(рекуррентное соотношение)}"
            ),
            MathEntry.Block(
                "\\psi(1) = -\\gamma \\approx -0.5772 \\quad \\text{(постоянная Эйлера–Маскерони)}"
            ),
            MathEntry.Block(
                "\\psi(n) = -\\gamma + \\sum_{k=1}^{n-1}\\frac{1}{k}, \\quad n \\in \\mathbb{N}"
            ),
            MathEntry.Block(
                "\\psi(z) = -\\gamma - \\frac{1}{z} + \\sum_{n=1}^{\\infty}\\left(\\frac{1}{n} - \\frac{1}{n+z}\\right)"
            ),
            MathEntry.Block(
                "\\psi\\!\\left(\\tfrac{1}{2}\\right) = -\\gamma - 2\\ln 2"
            ),

            MathEntry.SubHeader("Бета-функция B(x, y)"),
            MathEntry.Block(
                "B(x,y) = \\int_0^1 t^{x-1}(1-t)^{y-1}\\,dt, \\quad \\operatorname{Re}(x),\\operatorname{Re}(y) > 0"
            ),
            MathEntry.Block(
                "B(x,y) = \\frac{\\Gamma(x)\\,\\Gamma(y)}{\\Gamma(x+y)} \\quad \\text{(связь с Γ)}"
            ),
            MathEntry.Block(
                "B(x,y) = B(y,x) \\quad \\text{(симметрия)}"
            ),
            MathEntry.Block(
                "B(x,y) = 2\\int_0^{\\pi/2}\\sin^{2x-1}\\theta\\cos^{2y-1}\\theta\\,d\\theta"
            ),
            MathEntry.Block(
                "B(x,y+1) = \\frac{y}{x+y}\\,B(x,y) \\quad \\text{(рекуррентность)}"
            ),
            MathEntry.Block(
                "B\\!\\left(\\tfrac{1}{2},\\tfrac{1}{2}\\right) = \\pi, \\quad B(1,1) = 1, \\quad B(n,m) = \\frac{(n-1)!(m-1)!}{(n+m-1)!}"
            ),

            MathEntry.SubHeader("Дзета-функция Римана ζ(s)"),
            MathEntry.Block(
                "\\zeta(s) = \\sum_{n=1}^{\\infty}\\frac{1}{n^s} = \\prod_{p \\in \\mathbb{P}}\\frac{1}{1-p^{-s}}, \\quad \\operatorname{Re}(s) > 1"
            ),
            MathEntry.Block(
                "\\zeta(s) = 2^s\\,\\pi^{s-1}\\sin\\!\\left(\\frac{\\pi s}{2}\\right)\\Gamma(1-s)\\zeta(1-s) \\quad \\text{(анал. продолжение)}"
            ),
            MathEntry.Block(
                "\\zeta(2) = \\frac{\\pi^2}{6}, \\quad \\zeta(4) = \\frac{\\pi^4}{90}, \\quad \\zeta(6) = \\frac{\\pi^6}{945}"
            ),
            MathEntry.Block(
                "\\zeta(2n) = \\frac{(-1)^{n+1}(2\\pi)^{2n}B_{2n}}{2\\,(2n)!}, \\quad B_{2n} \\text{ — числа Бернулли}"
            ),
            MathEntry.Block(
                "\\zeta(0) = -\\tfrac{1}{2}, \\quad \\zeta(-1) = -\\tfrac{1}{12}, \\quad \\zeta(-2n) = 0 \\; (n \\geq 1)"
            ),
            MathEntry.Block(
                "\\zeta(s) = \\frac{1}{\\Gamma(s)}\\int_0^{\\infty}\\frac{t^{s-1}}{e^t - 1}\\,dt \\quad \\text{(интегральное представление)}"
            ),

            MathEntry.SubHeader("Связи между функциями"),
            MathEntry.Block(
                "\\int_0^{\\infty} x^{s-1}e^{-x}\\,dx = \\Gamma(s), \\quad \\int_0^1 x^{a-1}(1-x)^{b-1}dx = B(a,b)"
            ),
            MathEntry.Block(
                "\\psi(x) = -\\gamma + \\int_0^1 \\frac{1-t^{x-1}}{1-t}\\,dt"
            )
        )
    ),

    MathSection(
        title = "ТФКП",
        emoji = "ℂ",
        accentColor = Color(0xFF26C6DA),
        entries = listOf(
            MathEntry.SubHeader("Основные определения"),
            MathEntry.Block("z = x + iy, \\quad \\bar{z} = x - iy, \\quad |z| = \\sqrt{x^2 + y^2}"),
            MathEntry.Block("z = r e^{i\\varphi} = r(\\cos\\varphi + i\\sin\\varphi), \\quad r = |z|, \\quad \\varphi = \\arg z"),
            MathEntry.Block("e^{i\\varphi} = \\cos\\varphi + i\\sin\\varphi \\quad \\text{(формула Эйлера)}"),
            MathEntry.Block("e^{i\\pi} + 1 = 0 \\quad \\text{(тождество Эйлера)}"),

            MathEntry.SubHeader("Степени и корни"),
            MathEntry.Block("z^n = r^n e^{in\\varphi} = r^n(\\cos n\\varphi + i\\sin n\\varphi) \\quad \\text{(Муавр)}"),
            MathEntry.Block("\\sqrt[n]{z} = \\sqrt[n]{r}\\, e^{i(\\varphi + 2\\pi k)/n}, \\quad k = 0,1,\\ldots,n-1"),

            MathEntry.SubHeader("Условия Коши–Римана (аналитичность)"),
            MathEntry.Block("\\frac{\\partial u}{\\partial x} = \\frac{\\partial v}{\\partial y}, \\qquad \\frac{\\partial u}{\\partial y} = -\\frac{\\partial v}{\\partial x}"),
            MathEntry.Block("f'(z) = \\frac{\\partial u}{\\partial x} + i\\frac{\\partial v}{\\partial x} = \\frac{\\partial v}{\\partial y} - i\\frac{\\partial u}{\\partial y}"),

            MathEntry.SubHeader("Контурное интегрирование"),
            MathEntry.Block("\\int_\\gamma f(z)\\,dz = \\int_a^b f(z(t))\\,z'(t)\\,dt"),
            MathEntry.Block(
                "\\oint_\\gamma f(z)\\,dz = 0 \\quad \\text{если } f \\text{ аналитична внутри } \\gamma \\quad \\text{(теорема Коши)}"
            ),
            MathEntry.Block(
                "f(z_0) = \\frac{1}{2\\pi i}\\oint_\\gamma \\frac{f(z)}{z - z_0}\\,dz \\quad \\text{(интегральная формула Коши)}"
            ),
            MathEntry.Block(
                "\\frac{f^{(n)}(z_0)}{n!} = \\frac{1}{2\\pi i}\\oint_\\gamma \\frac{f(z)}{(z-z_0)^{n+1}}\\,dz"
            ),

            MathEntry.SubHeader("Вычеты"),
            MathEntry.Block(
                "\\oint_\\gamma f(z)\\,dz = 2\\pi i \\sum_k \\operatorname{Res}_{z=z_k} f(z)"
            ),
            MathEntry.Block(
                "\\operatorname{Res}_{z=z_0} f(z) = \\lim_{z \\to z_0}(z - z_0)f(z) \\quad \\text{(простой полюс)}"
            ),
            MathEntry.Block(
                "\\operatorname{Res}_{z=z_0} f(z) = \\frac{1}{(m-1)!}\\lim_{z\\to z_0}\\frac{d^{m-1}}{dz^{m-1}}\\left[(z-z_0)^m f(z)\\right] \\quad \\text{(полюс порядка } m\\text{)}"
            ),

            MathEntry.SubHeader("Ряд Лорана"),
            MathEntry.Block(
                "f(z) = \\sum_{n=-\\infty}^{+\\infty} c_n (z-z_0)^n, \\quad c_n = \\frac{1}{2\\pi i}\\oint_\\gamma \\frac{f(z)}{(z-z_0)^{n+1}}\\,dz"
            ),
            MathEntry.Block(
                "\\operatorname{Res}_{z=z_0} f(z) = c_{-1}"
            ),

            MathEntry.SubHeader("Основные элементарные функции"),
            MathEntry.Block("\\sin z = \\frac{e^{iz} - e^{-iz}}{2i}, \\quad \\cos z = \\frac{e^{iz} + e^{-iz}}{2}"),
            MathEntry.Block("\\ln z = \\ln|z| + i\\arg z + 2\\pi i k, \\quad k \\in \\mathbb{Z}")
        )
    ),

    MathSection(
        title = "Матрицы",
        emoji = "M",
        accentColor = Color(0xFFE91E63),
        entries = listOf(

            MathEntry.SubHeader("Определитель 2×2"),
            MathEntry.Block(
                "\\det\\begin{pmatrix}a & b \\\\ c & d\\end{pmatrix} = ad - bc"
            ),

            MathEntry.SubHeader("Определитель 3×3 (разложение по первой строке)"),
            MathEntry.Block(
                "\\det A = a_{11}\\cdot M_{11} - a_{12}\\cdot M_{12} + a_{13}\\cdot M_{13}"
            ),
            MathEntry.Block(
                "\\det\\begin{pmatrix}a&b&c\\\\d&e&f\\\\g&h&i\\end{pmatrix} = a\\det\\begin{pmatrix}e&f\\\\h&i\\end{pmatrix} - b\\det\\begin{pmatrix}d&f\\\\g&i\\end{pmatrix} + c\\det\\begin{pmatrix}d&e\\\\g&h\\end{pmatrix}"
            ),

            MathEntry.SubHeader("Минор и алгебраическое дополнение"),
            MathEntry.Block(
                "M_{ij} = \\det A_{ij}, \\quad A_{ij} = (-1)^{i+j} M_{ij}"
            ),
            MathEntry.Block(
                "\\text{Минор } M_{12} \\text{ матрицы } 3\\times3: \\quad M_{12} = \\det\\begin{pmatrix}d&f\\\\g&i\\end{pmatrix}"
            ),

            MathEntry.SubHeader("Метод Крамера (система Ax = b)"),
            MathEntry.Block(
                "x_i = \\frac{\\det A_i}{\\det A}, \\quad \\det A \\neq 0"
            ),
            MathEntry.Block(
                "A_i \\text{ — матрица } A \\text{ с } i\\text{-м столбцом, заменённым на } b"
            ),
            MathEntry.SubHeader("Пример системы 2×2"),
            MathEntry.Block(
                "\\begin{cases} a_1 x + b_1 y = c_1 \\\\ a_2 x + b_2 y = c_2 \\end{cases} \\Rightarrow x = \\frac{\\begin{vmatrix}c_1 & b_1 \\\\ c_2 & b_2\\end{vmatrix}}{\\begin{vmatrix}a_1 & b_1 \\\\ a_2 & b_2\\end{vmatrix}},\\; y = \\frac{\\begin{vmatrix}a_1 & c_1 \\\\ a_2 & c_2\\end{vmatrix}}{\\begin{vmatrix}a_1 & b_1 \\\\ a_2 & b_2\\end{vmatrix}}"
            ),

            MathEntry.SubHeader("Обратная матрица"),
            MathEntry.Block(
                "A^{-1} = \\frac{1}{\\det A}\\cdot\\tilde{A}, \\quad \\tilde{A}_{ij} = A_{ji}"
            ),
            MathEntry.Block(
                "\\begin{pmatrix}a&b\\\\c&d\\end{pmatrix}^{-1} = \\frac{1}{ad-bc}\\begin{pmatrix}d&-b\\\\-c&a\\end{pmatrix}"
            ),

            MathEntry.SubHeader("Гауссово исключение"),
            MathEntry.Block(
                "[A|b] \\xrightarrow{\\text{эл. преобр.}} [E|x] \\quad \\text{(расширенная матрица)}"
            )
        )
    ),

    // ── 8. Теория вероятностей и статистика ──────────────────────────────────

    MathSection(
        title = "ТВиС",
        emoji = "𝕡",
        accentColor = Color(0xFF66BB6A),
        entries = listOf(
            MathEntry.SubHeader("Аксиомы вероятности"),
            MathEntry.Block("0 \\leq P(A) \\leq 1, \\quad P(\\Omega) = 1, \\quad P(A \\cup B) = P(A) + P(B) \\text{ если } A \\cap B = \\varnothing"),
            MathEntry.Block("P(\\bar{A}) = 1 - P(A), \\quad P(A \\cup B) = P(A) + P(B) - P(A \\cap B)"),
            MathEntry.SubHeader("Условная вероятность и независимость"),
            MathEntry.Block("P(A|B) = \\frac{P(A \\cap B)}{P(B)}, \\quad P(B) > 0"),
            MathEntry.Block("\\text{Независимость: } P(A \\cap B) = P(A)\\cdot P(B)"),
            MathEntry.Block("\\text{Формула Байеса: } P(A_i|B) = \\frac{P(B|A_i)P(A_i)}{\\sum_j P(B|A_j)P(A_j)}"),
            MathEntry.SubHeader("Числовые характеристики"),
            MathEntry.Block("\\mathbb{E}[X] = \\sum_i x_i p_i \\text{ (дискр.)}, \\quad \\mathbb{E}[X] = \\int_{-\\infty}^{+\\infty} x\\,f(x)\\,dx \\text{ (непр.)}"),
            MathEntry.Block("\\mathrm{Var}(X) = \\mathbb{E}[(X - \\mathbb{E}X)^2] = \\mathbb{E}[X^2] - (\\mathbb{E}X)^2"),
            MathEntry.Block("\\sigma = \\sqrt{\\mathrm{Var}(X)}, \\quad \\mathrm{Cov}(X,Y) = \\mathbb{E}[(X-\\mathbb{E}X)(Y-\\mathbb{E}Y)]"),
            MathEntry.Block("\\rho_{XY} = \\frac{\\mathrm{Cov}(X,Y)}{\\sigma_X \\sigma_Y}, \\quad -1 \\leq \\rho \\leq 1"),
            MathEntry.SubHeader("Основные распределения"),
            MathEntry.Row("X \\sim B(n,p)", "биномиальное"),
            MathEntry.Block("P(X=k) = \\binom{n}{k}p^k(1-p)^{n-k}, \\quad \\mathbb{E}X = np, \\quad \\mathrm{Var}X = np(1-p)"),
            MathEntry.Row("X \\sim \\text{Pois}(\\lambda)", "Пуассон"),
            MathEntry.Block("P(X=k) = \\frac{\\lambda^k e^{-\\lambda}}{k!}, \\quad \\mathbb{E}X = \\mathrm{Var}X = \\lambda"),
            MathEntry.Row("X \\sim \\mathcal{N}(\\mu,\\sigma^2)", "нормальное"),
            MathEntry.Block("f(x) = \\frac{1}{\\sigma\\sqrt{2\\pi}}\\exp\\!\\left(-\\frac{(x-\\mu)^2}{2\\sigma^2}\\right), \\quad \\mathbb{E}X = \\mu, \\quad \\mathrm{Var}X = \\sigma^2"),
            MathEntry.SubHeader("Предельные теоремы"),
            MathEntry.Block("\\text{ЗБЧ (Хинчин): } \\bar{X}_n = \\frac{1}{n}\\sum_{i=1}^n X_i \\xrightarrow{P} \\mathbb{E}X"),
            MathEntry.Block("\\text{ЦПТ: } \\frac{\\bar{X}_n - \\mu}{\\sigma/\\sqrt{n}} \\xrightarrow{d} \\mathcal{N}(0,1)"),
            MathEntry.SubHeader("Оценки и доверительные интервалы"),
            MathEntry.Block("\\bar{x} = \\frac{1}{n}\\sum_{i=1}^n x_i, \\quad s^2 = \\frac{1}{n-1}\\sum_{i=1}^n(x_i - \\bar{x})^2"),
            MathEntry.Block("\\text{ДИ для } \\mu: \\quad \\bar{x} \\pm t_{\\alpha/2,\\,n-1}\\cdot\\frac{s}{\\sqrt{n}}")
        )
    ),

    // ── 9. Тригонометрия ─────────────────────────────────────────────────────

    MathSection(
        title = "Тригонометрия",
        emoji = "⌒",
        accentColor = Color(0xFFFFA726),
        entries = listOf(
            MathEntry.SubHeader("Основные тождества"),
            MathEntry.Block("\\sin^2 x + \\cos^2 x = 1, \\quad 1 + \\tan^2 x = \\frac{1}{\\cos^2 x}, \\quad 1 + \\cot^2 x = \\frac{1}{\\sin^2 x}"),
            MathEntry.SubHeader("Формулы сложения"),
            MathEntry.Block("\\sin(a \\pm b) = \\sin a\\cos b \\pm \\cos a\\sin b"),
            MathEntry.Block("\\cos(a \\pm b) = \\cos a\\cos b \\mp \\sin a\\sin b"),
            MathEntry.Block("\\tan(a \\pm b) = \\frac{\\tan a \\pm \\tan b}{1 \\mp \\tan a\\tan b}"),
            MathEntry.SubHeader("Двойной и половинный угол"),
            MathEntry.Block("\\sin 2x = 2\\sin x\\cos x, \\quad \\cos 2x = \\cos^2 x - \\sin^2 x = 2\\cos^2 x - 1 = 1 - 2\\sin^2 x"),
            MathEntry.Block("\\tan 2x = \\frac{2\\tan x}{1 - \\tan^2 x}"),
            MathEntry.Block("\\sin\\frac{x}{2} = \\pm\\sqrt{\\frac{1-\\cos x}{2}}, \\quad \\cos\\frac{x}{2} = \\pm\\sqrt{\\frac{1+\\cos x}{2}},\\quad \\tan\\frac{x}{2} = \\frac{1-\\cos x}{\\sin x} = \\frac{\\sin x}{1+\\cos x}"),
            MathEntry.SubHeader("Формулы суммы и произведения"),
            MathEntry.Block("\\sin a + \\sin b = 2\\sin\\frac{a+b}{2}\\cos\\frac{a-b}{2}"),
            MathEntry.Block("\\sin a - \\sin b = 2\\cos\\frac{a+b}{2}\\sin\\frac{a-b}{2}"),
            MathEntry.Block("\\cos a + \\cos b = 2\\cos\\frac{a+b}{2}\\cos\\frac{a-b}{2}"),
            MathEntry.Block("\\cos a - \\cos b = -2\\sin\\frac{a+b}{2}\\sin\\frac{a-b}{2}"),
            MathEntry.Block("\\sin a\\sin b = \\tfrac{1}{2}[\\cos(a-b) - \\cos(a+b)]"),
            MathEntry.Block("\\cos a\\cos b = \\tfrac{1}{2}[\\cos(a-b) + \\cos(a+b)]"),
            MathEntry.Block("\\sin a\\cos b = \\tfrac{1}{2}[\\sin(a+b) + \\sin(a-b)]"),
            MathEntry.SubHeader("Универсальная подстановка"),
            MathEntry.Block("t = \\tan\\frac{x}{2}: \\quad \\sin x = \\frac{2t}{1+t^2},\\quad \\cos x = \\frac{1-t^2}{1+t^2},\\quad dx = \\frac{2\\,dt}{1+t^2}"),
            MathEntry.SubHeader("Обратные функции"),
            MathEntry.Block("\\arcsin(\\sin x) = x \\;(x\\in[-\\tfrac{\\pi}{2},\\tfrac{\\pi}{2}]),\\quad \\arccos(\\cos x) = x\\;(x\\in[0,\\pi])"),
            MathEntry.Block("\\arcsin x + \\arccos x = \\frac{\\pi}{2}, \\quad \\arctan x + \\text{arccot}\\,x = \\frac{\\pi}{2}"),
            MathEntry.SubHeader("Теорема синусов и косинусов"),
            MathEntry.Block("\\frac{a}{\\sin A} = \\frac{b}{\\sin B} = \\frac{c}{\\sin C} = 2R"),
            MathEntry.Block("c^2 = a^2 + b^2 - 2ab\\cos C")
        )
    ),

    // ── 10. Дифференциальные уравнения ────────────────────────────────────────

    MathSection(
        title = "Дифф. уравнения",
        emoji = "ℒ",
        accentColor = Color(0xFFEF5350),
        entries = listOf(
            MathEntry.SubHeader("ОДУ 1-го порядка — разделение переменных"),
            MathEntry.Block("\\frac{dy}{dx} = f(x)g(y) \\;\\Rightarrow\\; \\int\\frac{dy}{g(y)} = \\int f(x)\\,dx + C"),
            MathEntry.SubHeader("Линейное ОДУ 1-го порядка"),
            MathEntry.Block("y' + p(x)y = q(x)"),
            MathEntry.Block("\\text{Метод вариации постоянной: } y = u(x)\\cdot e^{-\\int p\\,dx}, \\text{ где } u' = q(x)e^{\\int p\\,dx}"),
            MathEntry.Block("y = e^{-\\int p\\,dx}\\!\\left(\\int q(x)e^{\\int p\\,dx}\\,dx + C\\right)"),
            MathEntry.SubHeader("ОДУ с однородной правой частью"),
            MathEntry.Block("y' = f\\!\\left(\\frac{y}{x}\\right) \\;\\xrightarrow{v=y/x}\\; xv' + v = f(v) \\quad \\Rightarrow \\text{разделение переменных}"),
            MathEntry.SubHeader("Уравнение Бернулли"),
            MathEntry.Block("y' + p(x)y = q(x)y^n \\;\\xrightarrow{z=y^{1-n}}\\; z' + (1-n)p(x)z = (1-n)q(x)"),
            MathEntry.SubHeader("Линейное ОДУ 2-го порядка с постоянными коэффициентами"),
            MathEntry.Block("y'' + py' + qy = f(x)"),
            MathEntry.Block("\\text{Характеристическое уравнение: } k^2 + pk + q = 0"),
            MathEntry.Block("\\text{Два вещественных корня } k_1 \\neq k_2:\\; y_0 = C_1 e^{k_1 x} + C_2 e^{k_2 x}"),
            MathEntry.Block("\\text{Один корень } k_1 = k_2:\\; y_0 = (C_1 + C_2 x)e^{k_1 x}"),
            MathEntry.Block("\\text{Комплексные } k_{1,2} = \\alpha \\pm i\\beta:\\; y_0 = e^{\\alpha x}(C_1\\cos\\beta x + C_2\\sin\\beta x)"),
            MathEntry.SubHeader("Метод вариации постоянных (2-й порядок)"),
            MathEntry.Block("y^* = C_1(x)y_1 + C_2(x)y_2, \\quad \\begin{cases}C_1'y_1 + C_2'y_2 = 0\\\\ C_1'y_1' + C_2'y_2' = f(x)\\end{cases}"),
            MathEntry.SubHeader("Система ОДУ — преобразование Лапласа"),
            MathEntry.Block("\\mathcal{L}\\{f'(t)\\} = sF(s) - f(0), \\quad \\mathcal{L}\\{f''\\} = s^2F - sf(0) - f'(0)"),
            MathEntry.Block("\\mathcal{L}\\{e^{at}\\} = \\frac{1}{s-a}, \\quad \\mathcal{L}\\{\\sin(\\omega t)\\} = \\frac{\\omega}{s^2+\\omega^2}, \\quad \\mathcal{L}\\{\\cos(\\omega t)\\} = \\frac{s}{s^2+\\omega^2}")
        )
    ),

    // ── 11. Методы интегрирования, дифференцирования и вычисления пределов ───

    MathSection(
        title = "Методы ∫ / ∂ / lim",
        emoji = "≡",
        accentColor = Color(0xFF42A5F5),
        entries = listOf(
            MathEntry.SubHeader("Интегрирование — основные методы"),
            MathEntry.Block("\\textbf{По частям:} \\quad \\int u\\,dv = uv - \\int v\\,du"),
            MathEntry.Block("\\textbf{Подстановка:} \\quad \\int f(g(x))g'(x)\\,dx \\;\\xrightarrow{t=g(x)}\\; \\int f(t)\\,dt"),
            MathEntry.Block("\\textbf{Тригонометрические подстановки:}"),
            MathEntry.Block("\\sqrt{a^2 - x^2}: \\; x = a\\sin\\theta \\quad \\sqrt{a^2 + x^2}: \\; x = a\\tan\\theta \\quad \\sqrt{x^2 - a^2}: \\; x = a/\\cos\\theta"),
            MathEntry.Block("\\textbf{Разложение на простые дроби:} \\quad \\frac{P(x)}{Q(x)} = \\sum_i \\frac{A_i}{(x-r_i)^{k_i}} + \\sum_j \\frac{B_j x + C_j}{(x^2+p_j x+q_j)^{m_j}}"),
            MathEntry.Block("\\textbf{Формула Ньютона-Лейбница:} \\quad \\int_a^b f(x)\\,dx = F(b) - F(a)"),
            MathEntry.Block("\\textbf{Несобственные интегралы:} \\quad \\int_a^{+\\infty}f(x)\\,dx = \\lim_{b\\to+\\infty}\\int_a^b f(x)\\,dx"),
            MathEntry.SubHeader("Дифференцирование — специальные приёмы"),
            MathEntry.Block("\\textbf{Логарифмическое:} \\quad y = f(x)^{g(x)} \\Rightarrow \\ln y = g(x)\\ln f(x) \\Rightarrow y' = y\\cdot(g\\ln f)'"),
            MathEntry.Block("\\textbf{Неявная функция:} \\quad F(x,y) = 0 \\Rightarrow y' = -\\frac{F_x'}{F_y'}"),
            MathEntry.Block("\\textbf{Параметрические уравнения:} \\quad y'_x = \\frac{y'_t}{x'_t}, \\quad y''_{xx} = \\frac{(y'_x)'_t}{x'_t}"),
            MathEntry.Block("\\textbf{Формула Лейбница:} \\quad (uv)^{(n)} = \\sum_{k=0}^{n}\\binom{n}{k}u^{(k)}v^{(n-k)}"),
            MathEntry.SubHeader("Вычисление пределов"),
            MathEntry.Block("\\textbf{Правило Лопиталя:} \\quad \\lim\\frac{f}{g} = \\lim\\frac{f'}{g'} \\text{ при } \\frac{0}{0} \\text{ или } \\frac{\\infty}{\\infty}"),
            MathEntry.Block("\\textbf{Замечательные пределы:} \\quad \\lim_{x\\to 0}\\frac{\\sin x}{x} = 1, \\quad \\lim_{x\\to\\infty}\\left(1+\\frac{1}{x}\\right)^x = e"),
            MathEntry.Block("\\textbf{Эквивалентности при } x\\to 0:\\; \\sin x \\sim x,\\; \\ln(1+x)\\sim x,\\; e^x-1\\sim x,\\; 1-\\cos x\\sim\\frac{x^2}{2}"),
            MathEntry.Block("\\textbf{Сжатие:} \\quad g(x) \\leq f(x) \\leq h(x), \\; \\lim g = \\lim h = L \\Rightarrow \\lim f = L \\quad \\text{(теорема о сжатой переменной)}"),
            MathEntry.Block("\\textbf{Разложение в ряд:} \\quad \\lim_{x\\to 0}\\frac{f(x)}{x^n} \\;\\Rightarrow\\; \\text{разложи числитель и знаменатель до степени } n")
        )
    )
)

// ── Карточка на главном экране ────────────────────────────────────────────────

@Composable
fun MathReferenceCard() {
    var showSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E))))
            .border(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .clickable { showSheet = true }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF7C4DFF).copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) { Text("📐", fontSize = 24.sp) }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text("Справочник", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(3.dp))
                Text(
                    "Производные, интегралы, ряды, матрицы…",
                    fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f)
                )
            }

            Text("›", fontSize = 26.sp, color = Color.White.copy(alpha = 0.35f))
        }
    }

    if (showSheet) {
        MathReferenceFullScreen(onDismiss = { showSheet = false })
    }
}

// ── Полноэкранный справочник ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathReferenceFullScreen(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0F0F1A),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(Modifier.fillMaxSize()) {
            // Заголовок
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📐", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Математический справочник",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onDismiss() }
                        .background(Color.White.copy(alpha = 0.07f))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text("✕", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))

            // Контент
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                mathSections.forEach { section ->
                    MathSectionCard(section)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Сворачиваемая секция ──────────────────────────────────────────────────────

@Composable
fun MathSectionCard(section: MathSection) {
    var expanded by remember { mutableStateOf(false) }
    val arrowAngle by animateFloatAsState(if (expanded) 90f else 0f, label = "arrow")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF16162A))
            .border(1.dp, section.accentColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
    ) {
        // Шапка секции
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(section.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(section.emoji, fontSize = 19.sp, color = section.accentColor, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(13.dp))
            Text(
                section.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Text(
                "›",
                fontSize = 22.sp,
                color = section.accentColor.copy(alpha = 0.8f),
                modifier = Modifier.rotate(arrowAngle)
            )
        }

        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Шапка таблицы — только если есть Row-записи
                val hasRows = section.entries.any { it is MathEntry.Row }
                if (hasRows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(section.accentColor.copy(alpha = 0.10f))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "Формула", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = section.accentColor, modifier = Modifier.weight(1f)
                        )
                        Text(
                            "Пояснение", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = section.accentColor.copy(alpha = 0.65f)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                section.entries.forEachIndexed { idx, entry ->
                    when (entry) {

                        is MathEntry.SubHeader -> {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                entry.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = section.accentColor.copy(alpha = 0.85f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        is MathEntry.Row -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (idx % 2 == 0) Color.White.copy(alpha = 0.04f)
                                        else Color.Transparent
                                    )
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Формула — занимает оставшееся место, WebView скроллится сам
                                KatexViewLeft(
                                    latex = entry.formula,
                                    textSizeSp = 14,
                                    isBlock = false,
                                    textColor = Color.White,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                )
                                // Пояснение справа — фиксированная ширина
                                Text(
                                    entry.hint,
                                    fontSize = 11.sp,
                                    color = section.accentColor.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .width(88.dp)
                                        .padding(start = 6.dp)
                                )
                            }
                        }

                        is MathEntry.Block -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0A0A18))
                                    .border(
                                        0.5.dp,
                                        section.accentColor.copy(alpha = 0.25f),
                                        RoundedCornerShape(10.dp)
                                    )
                            ) {
                                // WebView сам скроллится горизонтально
                                KatexViewLeft(
                                    latex = entry.latex,
                                    textSizeSp = 15,
                                    isBlock = true,
                                    textColor = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 55.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}