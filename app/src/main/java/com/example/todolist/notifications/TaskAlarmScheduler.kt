package com.example.todolist.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.todolist.data.Task


class TaskAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNotification(task: Task, notificationOffsetMinutes: Int) {
        if (task.executionTime == null || !task.notificationEnabled) return

        val triggerTime = task.executionTime!! - (notificationOffsetMinutes * 60 * 1000L)

        if (triggerTime <= System.currentTimeMillis()) {
            // ODRAZU NOTIF - TODO
            return
        }

        val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
            putExtra(NotificationHelper.TASK_ID_EXTRA, task.id)
            putExtra(NotificationHelper.TASK_TITLE_EXTRA, task.title)
        }
        val pendingIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            pendingIntentFlags
        )

        try {
            if (true && !alarmManager.canScheduleExactAlarms()) {
            }
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (se: SecurityException) {
            se.printStackTrace()
        }
    }
    fun cancelNotification(task: Task) {
        val intent = Intent(context, TaskNotificationReceiver::class.java)
        val pendingIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            pendingIntentFlags
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}