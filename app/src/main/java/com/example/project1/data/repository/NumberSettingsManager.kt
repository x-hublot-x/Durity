package com.example.project1.data.repository

import com.example.project1.data.model.TargetNumberItem

/**
 * Менеджер настроек чисел для капчи.
 * Диапазон чисел: [0, 10] (только целые).
 */
object NumberSettingsManager {

    // Список целых чисел в диапазоне [0, 10]
    val integerNumbers = (0..1).map { TargetNumberItem(it) }

    fun getRandomTarget(): TargetNumberItem {
        return integerNumbers.random()
    }
}
