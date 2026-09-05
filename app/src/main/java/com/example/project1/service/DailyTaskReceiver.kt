package com.example.project1.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.project1.MainActivity
import com.example.project1.R

class DailyTaskReceiver : BroadcastReceiver() {
    @android.annotation.SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_task_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Задача дня",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Уведомления об обновлении задачи дня" }
            notificationManager.createNotificationChannel(channel)
        }

        val intentLaunch = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intentLaunch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_youtube_shorts)
            .setContentTitle("Задача дня обновлена! 🔥")
            .setContentText("Новая задача уже доступна. Зайди и поддержи свой streak!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1001, notification)
        }

        DailyTaskNotificationManager.scheduleDailyNotification(context)
    }
}
