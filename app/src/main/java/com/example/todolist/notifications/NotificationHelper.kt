package com.example.todolist.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.example.todolist.R

object NotificationHelper {
    private const val CHANNEL_ID = "todo_channel_id"
    private const val CHANNEL_NAME = "Todo Task Reminders"

    const val TASK_ID_EXTRA = "task_id_extra"
    const val TASK_TITLE_EXTRA = "task_title_extra"


    fun createNotificationChannel(context: Context) {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = "Channel for Todo task reminders"
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun sendNotification(context: Context, taskId: Int, title: String, message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        val deepLinkUri = Uri.parse("app://com.example.todolist/task/$taskId")
        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            `package` = context.packageName
        }

        val pendingIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(context, taskId, intent, pendingIntentFlags)


        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(taskId, notification)
    }
}
