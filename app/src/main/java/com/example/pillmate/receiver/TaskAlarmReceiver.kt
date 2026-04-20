package com.example.pillmate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pillmate.notification.TaskNotificationManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.google.firebase.auth.FirebaseAuth

class TaskAlarmReceiver : BroadcastReceiver(), KoinComponent {
    private val auth: FirebaseAuth by inject()
    private val currentProfileId: String by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val intentProfileId = intent.getStringExtra("PROFILE_ID")
        val currentUser = auth.currentUser

        // Security check: Only show if user is logged in and belongs to this profile
        if (currentUser == null) {
            android.util.Log.d("TaskAlarmReceiver", "Alarm skipped: No user logged in")
            return
        }
        
        if (intentProfileId != null && intentProfileId != currentProfileId) {
            android.util.Log.d("TaskAlarmReceiver", "Alarm skipped: Profile mismatch (Target: $intentProfileId, Current: $currentProfileId)")
            return
        }

        android.util.Log.d("TaskAlarmReceiver", "Alarm received!")
        val sourceId = intent.getStringExtra("SOURCE_ID") ?: return
        val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: return
        val title = intent.getStringExtra("TITLE") ?: "Task"
        val details = intent.getStringExtra("DETAILS") ?: ""
        val taskType = intent.getStringExtra("TASK_TYPE") ?: "OTHER"
        val reminderType = intent.getStringExtra("REMINDER_TYPE") ?: "NOTIFICATION"
        val rrule = intent.getStringExtra("EXTRA_RRULE")
        val startTime = intent.getStringExtra("EXTRA_START_TIME")
        val instructions = intent.getStringExtra("EXTRA_INSTRUCTIONS")
        val notificationManager = TaskNotificationManager(context)

        notificationManager.showTaskNotification(
            sourceId = sourceId,
            scheduleId = scheduleId,
            title = title,
            details = details,
            taskType = taskType,
            reminderType = reminderType,
            rrule = rrule,
            startTime = startTime,
            instructions = instructions
        )
    }
}
