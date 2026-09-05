package com.example.project1.data.repository

import com.example.project1.data.model.TrigTask

object TrigTaskRepository {
    private val tasks = listOf(
        TrigTask(1, Math.PI / 6.0, "\\frac{\\pi}{6}"),
        TrigTask(2, Math.PI / 4.0, "\\frac{\\pi}{4}"),
        TrigTask(3, Math.PI / 3.0, "\\frac{\\pi}{3}"),
        TrigTask(4, Math.PI / 2.0, "\\frac{\\pi}{2}"),
        TrigTask(5, 2.0 * Math.PI / 3.0, "\\frac{2\\pi}{3}"),
        TrigTask(6, 3.0 * Math.PI / 4.0, "\\frac{3\\pi}{4}"),
        TrigTask(7, 5.0 * Math.PI / 6.0, "\\frac{5\\pi}{6}"),
        TrigTask(8, Math.PI, "\\pi"),
        TrigTask(9, 7.0 * Math.PI / 6.0, "\\frac{7\\pi}{6}"),
        TrigTask(10, 5.0 * Math.PI / 4.0, "\\frac{5\\pi}{4}"),
        TrigTask(11, 4.0 * Math.PI / 3.0, "\\frac{4\\pi}{3}"),
        TrigTask(12, 3.0 * Math.PI / 2.0, "\\frac{3\\pi}{2}"),
        TrigTask(13, 5.0 * Math.PI / 3.0, "\\frac{5\\pi}{3}"),
        TrigTask(14, 7.0 * Math.PI / 4.0, "\\frac{7\\pi}{4}"),
        TrigTask(15, 11.0 * Math.PI / 6.0, "\\frac{11\\pi}{6}")
    )

    fun getRandomTask(): TrigTask = tasks.random()
}
