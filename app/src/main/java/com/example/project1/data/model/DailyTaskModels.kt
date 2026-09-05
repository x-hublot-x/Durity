package com.example.project1.data.model

data class DailyIntegralTask(
    val id: String,
    val type: String, // "Найти производную", "Найти интеграл", "Найти предел", "Решить ДУ"
    val latexStatement: String,
    val correctAnswer: String,
    val description: String
)

enum class CheckResult { CORRECT, CLOSE, WRONG }

data class CheckResponse(
    val result: CheckResult,
    val comment: String
)

sealed class TaskSegment {
    data class PlainText(val text: String) : TaskSegment()
    data class InlineMath(val latex: String) : TaskSegment()
    data class BlockMath(val latex: String) : TaskSegment()
}
