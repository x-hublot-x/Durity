package com.example.project1.util

import com.example.project1.data.model.TaskSegment

sealed class MessageSegment {
    data class PlainText(val text: String) : MessageSegment()
    data class InlineMath(val latex: String) : MessageSegment()
    data class BlockMath(val latex: String) : MessageSegment()
}

fun parseMessageSegments(text: String): List<MessageSegment> {
    val segments = mutableListOf<MessageSegment>()
    val sb = StringBuilder()
    var i = 0
    while (i < text.length) {
        if (text.startsWith("$$", i)) {
            val end = text.indexOf("$$", i + 2)
            if (end != -1) {
                if (sb.isNotEmpty()) { segments += MessageSegment.PlainText(sb.toString()); sb.clear() }
                val latex = text.substring(i + 2, end).trim()
                if (latex.isNotEmpty()) segments += MessageSegment.BlockMath(latex)
                i = end + 2
                continue
            }
        }
        if (text[i] == '$') {
            val end = text.indexOf('$', i + 1)
            if (end != -1 && end > i + 1) {
                if (sb.isNotEmpty()) { segments += MessageSegment.PlainText(sb.toString()); sb.clear() }
                val latex = text.substring(i + 1, end).trim()
                if (latex.isNotEmpty()) segments += MessageSegment.InlineMath(latex)
                i = end + 1
                continue
            }
        }
        sb.append(text[i])
        i++
    }
    if (sb.isNotEmpty()) segments += MessageSegment.PlainText(sb.toString())
    return segments
}

fun parseTaskLatex(text: String): List<TaskSegment> {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return emptyList()

    if (trimmed.startsWith("$$") && trimmed.endsWith("$$") && trimmed.length > 4) {
        val content = trimmed.substring(2, trimmed.length - 2).trim()
        return listOf(TaskSegment.BlockMath(content))
    }

    if (trimmed.startsWith("$") && trimmed.endsWith("$") && trimmed.length > 2) {
        val content = trimmed.substring(1, trimmed.length - 1).trim()
        return listOf(TaskSegment.BlockMath(content))
    }

    if (!trimmed.contains("$") && (trimmed.contains("\\") || trimmed.contains("'"))) {
        return listOf(TaskSegment.BlockMath(trimmed))
    }

    val segments = mutableListOf<TaskSegment>()
    val sb = StringBuilder()
    var i = 0
    while (i < trimmed.length) {
        if (trimmed.startsWith("$$", i)) {
            val end = trimmed.indexOf("$$", i + 2)
            if (end != -1) {
                if (sb.isNotEmpty()) { segments += TaskSegment.PlainText(sb.toString()); sb.clear() }
                val l = trimmed.substring(i + 2, end).trim()
                if (l.isNotEmpty()) segments += TaskSegment.BlockMath(l)
                i = end + 2; continue
            }
        }
        if (trimmed[i] == '$') {
            val end = trimmed.indexOf('$', i + 1)
            if (end != -1 && end > i + 1) {
                if (sb.isNotEmpty()) { segments += TaskSegment.PlainText(sb.toString()); sb.clear() }
                val l = trimmed.substring(i + 1, end).trim()
                if (l.isNotEmpty()) segments += TaskSegment.InlineMath(l)
                i = end + 1; continue
            }
        }
        sb.append(trimmed[i]); i++
    }
    if (sb.isNotEmpty()) segments += TaskSegment.PlainText(sb.toString())
    return segments
}

fun latexToUnicode(s: String): String {
    var result = s.trim()
    var safety = 0
    while (result.contains("\\frac") && safety++ < 10) {
        result = result.replace(Regex("\\\\frac\\{([^{}]*)\\}\\{([^{}]*)\\}")) { m ->
            val num = latexToUnicodeSimple(m.groupValues[1])
            val den = latexToUnicodeSimple(m.groupValues[2])
            "($num)/($den)"
        }
    }

    result = latexToUnicodeSimple(result)
    return result
}

private fun latexToUnicodeSimple(s: String): String = s
    // Раскрываем \operatorname{word} и \text{word} → word ДО всего остального,
    // чтобы nested {} не ломали регексы ниже
    .replace(Regex("""\\operatorname\{([^}]*)\}""")) { m -> m.groupValues[1] }
    .replace(Regex("""\\text\{([^}]*)\}""")) { m -> m.groupValues[1] }
    .replace(Regex("""\\mathrm\{([^}]*)\}""")) { m -> m.groupValues[1] }
    .replace(Regex("""\\mathbf\{([^}]*)\}""")) { m -> m.groupValues[1] }
    .replace(Regex("""\^\{([^}]*)\}""")) { m -> toSuperscriptStr(m.groupValues[1]) }
    .replace(Regex("""\^(\S)""")) { m -> toSuperscriptStr(m.groupValues[1]) }
    .replace(Regex("""_\{([^}]*)\}""")) { m -> toSubscriptStr(m.groupValues[1]) }
    .replace(Regex("""_(\S)""")) { m -> toSubscriptStr(m.groupValues[1]) }
    .replace(Regex("""\\sqrt\{([^}]*)\}""")) { m -> "√(${m.groupValues[1]})" }
    .replace("\\sqrt", "√")
    .replace("\\int", "∫")
    // Гиперболические — ПЕРЕД обычными тригонометрическими (иначе \sinh частично съедается \sin)
    .replace("\\sinh", "sinh")
    .replace("\\cosh", "cosh")
    .replace("\\tanh", "tanh")
    .replace("\\coth", "coth")
    .replace("\\sh", "sh")
    .replace("\\ch", "ch")
    .replace("\\th", "th")
    // Обычные тригонометрические
    .replace("\\sin", "sin")
    .replace("\\cos", "cos")
    .replace("\\tan", "tan")
    .replace("\\cot", "cot")
    .replace("\\arcsin", "arcsin")
    .replace("\\arccos", "arccos")
    .replace("\\arctan", "arctan")
    .replace("\\ln", "ln")
    .replace("\\log", "log")
    .replace("\\exp", "exp")
    .replace("\\pi", "π")
    .replace("\\alpha", "α")
    .replace("\\beta", "β")
    .replace("\\gamma", "γ")
    .replace("\\delta", "δ")
    .replace("\\epsilon", "ε")
    .replace("\\theta", "θ")
    .replace("\\lambda", "λ")
    .replace("\\mu", "μ")
    .replace("\\sigma", "σ")
    .replace("\\omega", "ω")
    .replace("\\Omega", "Ω")
    .replace("\\phi", "φ")
    .replace("\\psi", "ψ")
    .replace("\\cdot", "·")
    .replace("\\times", "×")
    .replace("\\div", "÷")
    .replace("\\pm", "±")
    .replace("\\infty", "∞")
    .replace("\\leq", "≤")
    .replace("\\geq", "≥")
    .replace("\\neq", "≠")
    .replace("\\approx", "≈")
    .replace("\\sum", "Σ")
    .replace("\\prod", "Π")
    .replace("\\to", "→")
    .replace("\\rightarrow", "→")
    .replace("\\,", " ")
    .replace("\\;", " ")
    .replace("\\!", "")
    .replace("\\quad", "  ")
    .replace("\\left(", "(")
    .replace("\\right)", ")")
    .replace("\\left[", "[")
    .replace("\\right]", "]")
    .replace("\\left\\{", "{")
    .replace("\\right\\}", "}")
    .replace("\\left|", "|")
    .replace("\\right|", "|")
    .replace("{", "").replace("}", "")
    .replace(Regex("""\\[a-zA-Z]+"""), "")
    .replace("\\", "")
    .replace(Regex(" {2,}"), " ")
    .trim()

private fun toSuperscriptStr(s: String): String {
    val map = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³',
        '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷',
        '8' to '⁸', '9' to '⁹', '+' to '⁺', '-' to '⁻',
        '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'x' to 'ˣ', 'a' to 'ᵃ', 'b' to 'ᵇ',
        'i' to 'ⁱ', 'T' to 'ᵀ'
    )
    return s.map { map[it] ?: it }.joinToString("")
}

private fun toSubscriptStr(s: String): String {
    val map = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃',
        '4' to '₄', '5' to '₅', '6' to '₆', '7' to '₇',
        '8' to '₈', '9' to '₉', '+' to '₊', '-' to '₋',
        '=' to '₌', '(' to '₍', ')' to '₎',
        'n' to 'ₙ', 'i' to 'ᵢ', 'e' to 'ₑ', 'a' to 'ₐ',
        'x' to 'ₓ', 'k' to 'ₖ'
    )
    return s.map { map[it] ?: it }.joinToString("")
}

fun cleanLatexInText(text: String): String {
    var r = text
    // Раскрываем \operatorname{word}, \text{word}, \mathrm{word} → word
    // ДО всего остального, чтобы nested {} не ломали другие регексы
    r = Regex("""\\operatorname\{([^}]*)\}""").replace(r) { it.groupValues[1] }
    r = Regex("""\\text\{([^}]*)\}""").replace(r) { it.groupValues[1] }
    r = Regex("""\\mathrm\{([^}]*)\}""").replace(r) { it.groupValues[1] }
    r = Regex("""\\mathbf\{([^}]*)\}""").replace(r) { it.groupValues[1] }
    r = applySymbols(r)
    // sqrt с содержимым → скобки
    r = Regex("""√\{([^}]*)\}""").replace(r) { m -> "√(${m.groupValues[1]})" }
    r = Regex("""\\?frac\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""")
        .replace(r) { m ->
            val num = cleanInner(m.groupValues[1])
            val den = cleanInner(m.groupValues[2])
            "($num/$den)"
        }
    r = Regex("""\\?int(?:_\{([^}]+)\}|_([^\s^]+))?(?:\^\{([^}]+)\}|\^([^\s_]+))?""")
        .replace(r) { m ->
            val sub = m.groupValues[1].ifEmpty { m.groupValues[2] }
            val sup = m.groupValues[3].ifEmpty { m.groupValues[4] }
            when {
                sub.isNotEmpty() && sup.isNotEmpty() -> "∫${toSuperscript(sup)}${toSubscript(sub)}"
                sub.isNotEmpty() -> "∫${toSubscript(sub)}"
                sup.isNotEmpty() -> "∫${toSuperscript(sup)}"
                else -> "∫"
            }
        }
    r = Regex("""\^\{([^}]*)\}""").replace(r) { m -> toSuperscript(m.groupValues[1]) }
    r = Regex("""\^([A-Za-z0-9\-∞])""").replace(r) { m -> toSuperscript(m.groupValues[1]) }
    r = Regex("""_\{([^}]*)\}""").replace(r) { m -> toSubscript(m.groupValues[1]) }
    r = Regex("""_([A-Za-z0-9\-∞])""").replace(r) { m -> toSubscript(m.groupValues[1]) }
    r = r.replace("\\,", "")
        .replace("\\!", "")
        .replace("\\", "")
        .replace("{", "")
        .replace("}", "")
    r = Regex(""" {2,}""").replace(r, " ")
    r = Regex("""\n{3,}""").replace(r, "\n\n")
    return r
}

private fun cleanInner(s: String): String {
    var r = s
    r = Regex("""_\{([^}]*)\}""").replace(r) { m -> toSubscript(m.groupValues[1]) }
    r = Regex("""_([A-Za-z0-9])""").replace(r) { m -> toSubscript(m.groupValues[1]) }
    r = Regex("""\^\{([^}]*)\}""").replace(r) { m -> toSuperscript(m.groupValues[1]) }
    return applySymbols(r)
}

private fun applySymbols(s: String): String = s
    .replace("\\pi",       "π")
    .replace("\\alpha",    "α")
    .replace("\\beta",     "β")
    .replace("\\gamma",    "γ")
    .replace("\\delta",    "δ")
    .replace("\\epsilon",  "ε")
    .replace("\\theta",    "θ")
    .replace("\\lambda",   "λ")
    .replace("\\mu",       "μ")
    .replace("\\sigma",    "σ")
    .replace("\\tau",      "τ")
    .replace("\\phi",      "φ")
    .replace("\\psi",      "ψ")
    .replace("\\omega",    "ω")
    .replace("\\Omega",    "Ω")
    .replace("\\infty",    "∞")
    .replace("\\cdot",     "·")
    .replace("cdot",       "·")
    .replace("\\times",    "×")
    .replace("\\div",      "÷")
    .replace("\\pm",       "±")
    .replace("\\leq",      "≤")
    .replace("\\geq",      "≥")
    .replace("\\neq",      "≠")
    .replace("\\approx",   "≈")
    .replace("\\sum",      "Σ")
    .replace("\\prod",     "Π")
    .replace("\\sqrt",     "√")
    .replace("\\partial",  "∂")
    .replace("\\nabla",    "∇")
    .replace("\\rightarrow", "→")
    .replace("\\to",         "→")
    .replace("\\leftarrow",  "←")
    .replace("\\Rightarrow", "⇒")
    // Гиперболические — перед обычными тригонометрическими
    .replace("\\sinh", "sinh")
    .replace("\\cosh", "cosh")
    .replace("\\tanh", "tanh")
    .replace("\\coth", "coth")
    .replace("\\operatorname{sh}", "sh")
    .replace("\\operatorname{ch}", "ch")
    .replace("\\sh",   "sh")
    .replace("\\ch",   "ch")
    .replace("\\th",   "th")
    // Обычные тригонометрические
    .replace("\\sin",  "sin")
    .replace("\\cos",  "cos")
    .replace("\\tan",  "tan")
    .replace("\\cot",  "cot")
    .replace("\\arcsin", "arcsin")
    .replace("\\arccos", "arccos")
    .replace("\\arctan", "arctan")
    .replace("\\ln",   "ln")
    .replace("\\log",  "log")
    .replace("\\exp",  "exp")

private fun toSuperscript(s: String): String {
    val map = mapOf(
        '0' to "⁰", '1' to "¹", '2' to "²", '3' to "³", '4' to "⁴",
        '5' to "⁵", '6' to "⁶", '7' to "⁷", '8' to "⁸", '9' to "⁹",
        '-' to "⁻", '+' to "⁺", '=' to "⁼",
        'a' to "ᵃ", 'b' to "ᵇ", 'c' to "ᶜ", 'd' to "ᵈ", 'e' to "ᵉ",
        'f' to "ᶠ", 'g' to "ᵍ", 'h' to "ʰ", 'i' to "ⁱ", 'j' to "ʲ",
        'k' to "ᵏ", 'l' to "ˡ", 'm' to "ᵐ", 'n' to "ⁿ", 'o' to "ᵒ",
        'p' to "ᵖ", 'r' to "ʳ", 's' to "ˢ", 't' to "ᵗ", 'u' to "ᵘ",
        'v' to "ᵛ", 'w' to "ʷ", 'x' to "ˣ", 'y' to "ʸ", 'z' to "ᶻ",
        'A' to "ᴬ", 'B' to "ᴮ", 'D' to "ᴰ", 'E' to "ᴱ", 'G' to "ᴳ",
        'H' to "ᴴ", 'I' to "ᴵ", 'J' to "ᴶ", 'K' to "ᴷ", 'L' to "ᴸ",
        'M' to "ᴹ", 'N' to "ᴺ", 'O' to "ᴼ", 'P' to "ᴾ", 'R' to "ᴿ",
        'T' to "ᵀ", 'U' to "ᵁ", 'W' to "ᵂ",
        'β' to "ᵝ", 'γ' to "ᵞ", 'δ' to "ᵟ", 'φ' to "ᵠ", 'π' to "ᵖ",
        ' ' to "",  '·' to "·", ',' to ","
    )
    return s.map { map[it] ?: it.toString() }.joinToString("")
}

private fun toSubscript(s: String): String {
    val map = mapOf(
        '0' to "₀", '1' to "₁", '2' to "₂", '3' to "₃", '4' to "₄",
        '5' to "₅", '6' to "₆", '7' to "₇", '8' to "₈", '9' to "₉",
        '+' to "₊", '-' to "₋", '=' to "₌",
        'a' to "ₐ", 'e' to "ₑ", 'i' to "ᵢ", 'j' to "ⱼ", 'o' to "ₒ",
        'u' to "ᵤ", 'v' to "ᵥ", 'x' to "ₓ", 'n' to "ₙ", 'k' to "ₖ",
        'p' to "ₚ", 's' to "ₛ", 't' to "ₜ", 'm' to "ₘ", 'r' to "ᵣ",
        'h' to "ₕ", 'l' to "ₗ", 'c' to "꜀", 'A' to "ₐ", 'P' to "ₚ"
    )
    return s.map { map[it] ?: it.toString() }.joinToString("")
}

fun renderIntegralLatex(latex: String): String {
    return latex
        .replace(Regex("\\\\int_\\{([^}]*)\\}\\^\\{([^}]*)\\}")) { m -> "∫${latexArgToUnicode(m.groupValues[2])}${latexArgToUnicode(m.groupValues[1])}" }
        .replace("\\int", "∫")
        .replace(Regex("\\\\frac\\{([^}]*)\\}\\{([^}]*)\\}")) { m -> "(${latexArgToUnicode(m.groupValues[1])})/(${latexArgToUnicode(m.groupValues[2])})" }
        .replace(Regex("\\^\\{([^}]*)\\}")) { m -> toSuperscript(latexArgToUnicode(m.groupValues[1])) }
        .replace(Regex("\\^(\\S)")) { m -> toSuperscript(m.groupValues[1]) }
        .replace(Regex("_\\{([^}]*)\\}")) { m -> toSubscript(latexArgToUnicode(m.groupValues[1])) }
        .replace("\\lim", "lim")
        .replace("\\to", "→")
        .replace("\\,", " ").replace("\\!", "")
        .replace("\\sin", "sin").replace("\\cos", "cos").replace("\\tan", "tan")
        .replace("\\ln", "ln").replace("\\log", "log").replace("\\exp", "exp")
        .replace("\\pi", "π").replace("\\cdot", "·").replace("\\infty", "∞")
        .replace("\\left(", "(").replace("\\right)", ")")
        .replace(Regex("\\\\[a-zA-Z]+"), "").replace("\\", "").trim()
}

private fun latexArgToUnicode(s: String): String = renderIntegralLatex(s)

fun containsLatex(text: String): Boolean =
    Regex("""\$\$?.+?\$\$?""", RegexOption.DOT_MATCHES_ALL).containsMatchIn(text)
