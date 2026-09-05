package com.example.project1.data.model

data class ChessPuzzle(
    val id: Int,
    val fen: String,
    val instruction: String,
    val correctMove: String
)

data class FunnyCaptcha(
    val title: String,
    val question: String,
    val items: List<Any>,
    val correctIndexes: Set<Int>
)

data class TrigTask(
    val id: Int,
    val targetAngleRad: Double,
    val targetLabel: String
)

data class TargetNumberItem(
    val value: Int
)

enum class GraphFunction(val displayName: String) {
    INTEGRAL_SINE("Интегральный синус Si(x)"),
    RIEMANN_ZETA("Дзета-функция ζ(s)"),
    GAUSSIAN("Гауссиана e^(−x²)"),
    ERF("Функция ошибок erf(x)"),
    GAMMA("Гамма-функция Γ(x)"),
    BETA("Бета-функция B(x, 2)")
}

data class GraphFunctionConfig(
    val type: GraphFunction,
    val xRange: ClosedFloatingPointRange<Float>,
    val yRange: ClosedFloatingPointRange<Float>
)
