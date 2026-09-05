package com.example.project1.data.repository

import com.example.project1.R
import com.example.project1.data.model.FunnyCaptcha

object FunnyCaptchaRepository {

    private val captchas = listOf(
        FunnyCaptcha(
            title = "Выберите квадраты, где есть",
            question = "ВЕЛОСИПЕДЫ",
            items = listOf(
                R.drawable.v1, // Картинка 0
//                "🚗",                              // Можно комбинировать с эмодзи
//                "🚲",
                R.drawable.v2,
                R.drawable.v3,
                R.drawable.v4,
                R.drawable.v5,
                R.drawable.v6,
                R.drawable.v7,
                R.drawable.v8,
                R.drawable.v9,
            ),
            correctIndexes = setOf(1, 2, 4, 5)
        ),
        FunnyCaptcha(
            title = "Выберите квадраты, где есть",
            question = "ЧАСТИ ТЕЛА",
            items = listOf(
                R.drawable.z1, // Картинка 0
                R.drawable.z2,
                R.drawable.z3,
                R.drawable.z4,
                R.drawable.z5,
                R.drawable.z6,
                R.drawable.z7,
                R.drawable.z8,
                R.drawable.z9,
            ),
            correctIndexes = setOf(0, 3, 4, 5)
        ),
        FunnyCaptcha(
            title = "Выберите квадраты, где есть",
            question = "ФИНСКИЕ СНАЙПЕРЫ",
            items = listOf(
                R.drawable.f1, // Картинка 0
                R.drawable.f2,
                R.drawable.f3,
                R.drawable.f4,
                R.drawable.f5,
                R.drawable.f6,
                R.drawable.f7,
                R.drawable.f8,
                R.drawable.f9,
            ),
            correctIndexes = setOf(0, 1, 4, 7, 8)
        ),
        FunnyCaptcha(
            title = "Выберите квадраты, где есть",
            question = "МОТОЦИКЛИСТЫ",
            items = listOf(
                R.drawable.a1, // Картинка 0
                R.drawable.a2,
                R.drawable.a3,
                R.drawable.a4,
                R.drawable.a5,
                R.drawable.a6,
                R.drawable.a7,
                R.drawable.a8,
                R.drawable.a9,
            ),
            correctIndexes = setOf(6, 7, 8)
        ),
        FunnyCaptcha(
            title = "Выберите квадраты, где есть",
            question = "БЕЛОЧКА",
            items = listOf(
                R.drawable.q1, // Картинка 0
                R.drawable.q2,
                R.drawable.q3,
                R.drawable.q4,
                R.drawable.q5,
                R.drawable.q6,
                R.drawable.q7,
                R.drawable.q8,
                R.drawable.q9,
            ),
            correctIndexes = setOf(2, 4, 5, 7, 8)
        ),
        FunnyCaptcha(
            title = "Выберите квадраты, где есть",
            question = "ПРЕДМЕТЫ РОСКОШИ",
            items = listOf(
                R.drawable.w1, // Картинка 0
                R.drawable.w2,
                R.drawable.w3,
                R.drawable.w4,
                R.drawable.w5,
                R.drawable.w6,
                R.drawable.w7,
                R.drawable.w8,
                R.drawable.w9,
            ),
            correctIndexes = setOf(5, 8)
        ),
        FunnyCaptcha(
            title = "Выберите квадраты, где есть",
            question = "ЛОГОТИП НЕМЕЦКИХ РЕБЯТ",
            items = listOf(
                R.drawable.e1, // Картинка 0
                R.drawable.e2,
                R.drawable.e3,
                R.drawable.e4,
                R.drawable.e5,
                R.drawable.e6,
                R.drawable.e7,
                R.drawable.e8,
                R.drawable.e9,
            ),
            correctIndexes = setOf(1, 2)
        ),
        FunnyCaptcha(
            title = "Выберите квадраты, где есть",
            question = "УЖИН СТУДЕНТА",
            items = listOf(
                R.drawable.s1, // Картинка 0
                R.drawable.s2,
                R.drawable.s3,
                R.drawable.s4,
                R.drawable.s5,
                R.drawable.s6,
                R.drawable.s7,
                R.drawable.s8,
                R.drawable.s9,
            ),
            correctIndexes = setOf(1, 2, 5, 8)
        )
    )

    fun getRandomCaptcha(): FunnyCaptcha {
        return captchas.random()
    }
}
