package com.example.todolist.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.todolist.ui.screens.MainActivity // Załóżmy, że MainActivity jest punktem wejścia
import com.example.todolist.R // Upewnij się, że masz zdefiniowane ikony

object NotificationHelper {
    private const val CHANNEL_ID = "todo_channel_id"
    private const val CHANNEL_NAME = "Todo Task Reminders"
    const val NOTIFICATION_ID_EXTRA = "notification_id_extra"
    const val TASK_ID_EXTRA = "task_id_extra"
    const val TASK_TITLE_EXTRA = "task_title_extra"


    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Channel for Todo task reminders"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendNotification(context: Context, taskId: Int, title: String, message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Możesz dodać extra, aby nawigować do konkretnego zadania po kliknięciu
            putExtra(TASK_ID_EXTRA, taskId)
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, taskId, intent, pendingIntentFlags)


        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon) // Zastąp własną ikoną
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(taskId, notification) // Użyj taskId jako unikalnego ID powiadomienia
    }
}