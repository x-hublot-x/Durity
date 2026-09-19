package com.example.project1.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.project1.data.storage.MathBlitzStorage

class MathBlitzTimeoutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val session = MathBlitzStorage.getActiveSession(context)
        val betCoins = intent?.getIntExtra("bet_coins", session?.config?.betCoins ?: 0)
            ?: (session?.config?.betCoins ?: 0)

        if (session != null && !session.isWon) {
            val isAlreadyNotified = MathBlitzStorage.isTimeoutNotified(context, session.id)
            if (!isAlreadyNotified) {
                MathBlitzStorage.setTimeoutNotified(context, session.id)
                val expiredSession = session.copy(
                    isFinished = true,
                    isWon = false
                )
                MathBlitzStorage.saveActiveSession(context, expiredSession)
                com.example.project1.data.storage.DailySummaryStorage.recordBlitzLoss(context)
                com.example.project1.data.storage.UserRatingStorage.addRating(context, -10)
                MathBlitzNotificationManager.showTimeoutLossNotification(context, betCoins)
            }
        }
    }
}
