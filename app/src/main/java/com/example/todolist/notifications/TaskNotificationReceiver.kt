package com.example.todolist.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todolist.data.Task // Załóżmy, że masz model Task

class TaskNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(NotificationHelper.TASK_ID_EXTRA, -1)
        val taskTitle = intent.getStringExtra(NotificationHelper.TASK_TITLE_EXTRA) ?: "Zadanie"

        if (taskId != -1) {
            NotificationHelper.sendNotification(
                context,
                taskId,
                "Przypomnienie o zadaniu!",
                "Czas wykonać: $taskTitle"
            )
        }
    }
}