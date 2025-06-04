package com.example.todolist.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.todolist.data.Task
import java.util.*

class TaskAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Dodaj notificationOffsetMinutes jako parametr
    fun scheduleNotification(task: Task, notificationOffsetMinutes: Int) {
        if (task.executionTime == null || !task.notificationEnabled) return

        val triggerTime = task.executionTime!! - (notificationOffsetMinutes * 60 * 1000L) // Odejmij offset

        // Sprawdź, czy czas wyzwolenia jest w przyszłości
        if (triggerTime <= System.currentTimeMillis()) {
            // Czas już minął lub jest teraz, nie planuj lub obsłuż inaczej
            // Można by np. wysłać powiadomienie natychmiast, jeśli executionTime jest bardzo blisko
            return
        }

        val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
            putExtra(NotificationHelper.TASK_ID_EXTRA, task.id)
            putExtra(NotificationHelper.TASK_TITLE_EXTRA, task.title)
        }
        // ... (reszta logiki PendingIntent bez zmian) ...
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            pendingIntentFlags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // Obsługa braku uprawnień
            }
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime, // Użyj zmodyfikowanego czasu wyzwolenia
                pendingIntent
            )
        } catch (se: SecurityException) {
            se.printStackTrace()
        }
    }
    fun cancelNotification(task: Task) {
        val intent = Intent(context, TaskNotificationReceiver::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
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