package com.example.project1.data.model

enum class BlitzCategory(val title: String) {
    MATRICES("Матрицы и СЛАУ"),
    DERIVATIVES("Производные"),
    INTEGRALS("Интегралы"),
    LIMITS("Пределы"),
    COMPLEX("ТФКП"),
    TRIGONOMETRY("Тригонометрия")
}

data class BlitzTask(
    val id: String,
    val category: String,
    val latexStatement: String,
    val correctAnswer: String,
    val hint: String
)

data class BlitzConfig(
    val category: BlitzCategory,
    val difficulty: Int, // 1, 2, 3
    val durationMinutes: Int, // 1..5
    val taskCount: Int, // 2..5
    val betCoins: Int
)

data class BlitzSessionState(
    val id: String,
    val config: BlitzConfig,
    val startTime: Long,
    val deadlineTime: Long,
    val potentialWinCoins: Int,
    val tasks: List<BlitzTask>,
    val currentTaskIndex: Int,
    val isFinished: Boolean,
    val isWon: Boolean
)
