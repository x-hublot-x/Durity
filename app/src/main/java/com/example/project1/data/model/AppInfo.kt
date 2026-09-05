package com.example.project1.data.model

import android.graphics.drawable.Drawable

const val UNLOCK_BONUS_MINUTES = 30

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val timeLimitMinutes: Int,
    val usedMinutesThisWeek: Int,
    val addedTimestamp: Long
)
