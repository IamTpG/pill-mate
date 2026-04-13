package com.example.pillmate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pillmate.notification.TaskNotificationManager

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("TaskAlarmReceiver", "Alarm received!")
        val sourceId = intent.getStringExtra("SOURCE_ID") ?: return
        val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: return
        val title = intent.getStringExtra("TITLE") ?: "Task"
        val details = intent.getStringExtra("DETAILS") ?: ""
        val taskType = intent.getStringExtra("TASK_TYPE") ?: "OTHER"
        val reminderType = intent.getStringExtra("REMINDER_TYPE") ?: "NOTIFICATION"

        val notificationManager = TaskNotificationManager(context)
        notificationManager.showTaskNotification(sourceId, scheduleId, title, details, taskType, reminderType)
    }
}
