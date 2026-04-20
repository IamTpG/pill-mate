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
    private val db: com.google.firebase.firestore.FirebaseFirestore by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val intentProfileId = intent.getStringExtra("PROFILE_ID")
        val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: return
        val currentUser = auth.currentUser

        // Security check: Only show if user is logged in and belongs to this profile
        if (currentUser == null) {
            android.util.Log.d("TaskAlarmReceiver", "Alarm skipped: No user logged in")
            return
        }
        
        if (intentProfileId != null && intentProfileId != currentProfileId) {
            android.util.Log.d("TaskAlarmReceiver", "Alarm skipped: Profile mismatch")
            return
        }

        // Verification check: Don't show if already completed/skipped today
        val notificationManager = TaskNotificationManager(context)
        val sourceId = intent.getStringExtra("SOURCE_ID") ?: return
        val title = intent.getStringExtra("TITLE") ?: "Task"
        val details = intent.getStringExtra("DETAILS") ?: ""
        val taskType = intent.getStringExtra("TASK_TYPE") ?: "OTHER"
        val reminderType = intent.getStringExtra("REMINDER_TYPE") ?: "NOTIFICATION"
        val rrule = intent.getStringExtra("EXTRA_RRULE")
        val startTime = intent.getStringExtra("EXTRA_START_TIME")
        val instructions = intent.getStringExtra("EXTRA_INSTRUCTIONS")

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // Check for today's logs for this schedule
                val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
                val logs = db.collection("profiles").document(currentProfileId)
                    .collection("logs")
                    .whereEqualTo("scheduleId", scheduleId)
                    .get()
                    .await()

                val alreadyHandled = logs.documents.any { doc ->
                    val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: java.util.Date()
                    val logDate = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(timestamp)
                    val status = doc.getString("status")
                    logDate == today && (status == "COMPLETED" || status == "SKIPPED")
                }

                if (alreadyHandled) {
                    android.util.Log.d("TaskAlarmReceiver", "Alarm skipped: Already handled for today")
                    return@launch
                }

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
            } catch (e: Exception) {
                // If check fails, fallback to showing notification (better not to miss a med)
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
    }
}
