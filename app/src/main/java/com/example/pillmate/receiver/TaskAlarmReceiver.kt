package com.example.pillmate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.pillmate.notification.TaskNotificationManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TaskAlarmReceiver : BroadcastReceiver(), KoinComponent {
    private val auth: FirebaseAuth by inject()
    private val currentProfileId: String by inject()
    private val db: com.google.firebase.firestore.FirebaseFirestore by inject()

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(
            "TaskAlarmReceiver",
            "onReceive triggered. scheduleId=${intent.getStringExtra("SCHEDULE_ID")} profileId=${intent.getStringExtra("PROFILE_ID")}"
        )

        val intentProfileId = intent.getStringExtra("PROFILE_ID")
        val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: return
        val currentUser = auth.currentUser

        // Security check: Only show if user is logged in and belongs to this profile
        if (currentUser == null) {
            Log.d("TaskAlarmReceiver", "Alarm skipped: No user logged in")
            return
        }
        
        if (intentProfileId != null && intentProfileId != currentProfileId) {
            Log.d("TaskAlarmReceiver", "Alarm skipped: Profile mismatch")
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
        val dose = intent.getFloatExtra("EXTRA_DOSE", 1.0f)

        val pendingResult = goAsync()

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // Parse dose time for comparison
                val timeParts = (startTime ?: "08:00").split(":")
                val targetHour = if (startTime?.contains("PM", ignoreCase = true) == true) {
                    (timeParts[0].trim().toIntOrNull() ?: 0).let { if (it < 12) it + 12 else it }
                } else if (startTime?.contains("AM", ignoreCase = true) == true) {
                    (timeParts[0].trim().toIntOrNull() ?: 0).let { if (it == 12) 0 else it }
                } else {
                    timeParts[0].trim().toIntOrNull() ?: 0
                }
                val targetMinute = timeParts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0

                // Check for today's logs for this schedule
                val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
                val logs = db.collection("profiles").document(currentProfileId)
                    .collection("logs")
                    .whereEqualTo("scheduleId", scheduleId)
                    .get()
                    .await()

                val alreadyHandled = logs.documents.any { doc ->
                    val scheduledTime = doc.getTimestamp("scheduledTime")?.toDate()
                    val status = doc.getString("status")
                    if (scheduledTime != null) {
                        val cal = java.util.Calendar.getInstance().apply { time = scheduledTime }
                        val logDate = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(scheduledTime)
                        logDate == today && (status == "COMPLETED" || status == "SKIPPED") &&
                        cal.get(java.util.Calendar.HOUR_OF_DAY) == targetHour &&
                        cal.get(java.util.Calendar.MINUTE) == targetMinute
                    } else false
                }

                if (alreadyHandled) {
                    Log.d("TaskAlarmReceiver", "Alarm skipped: Already handled for today at $startTime")
                    return@launch
                }

                // Compute exact scheduled time millis for the notification
                val scheduledTimeMillis = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, targetHour)
                    set(java.util.Calendar.MINUTE, targetMinute)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis

                notificationManager.showTaskNotification(
                    sourceId = sourceId,
                    scheduleId = scheduleId,
                    title = title,
                    details = details,
                    taskType = taskType,
                    reminderType = reminderType,
                    rrule = rrule,
                    startTime = startTime,
                    instructions = instructions,
                    scheduledTimeMillis = scheduledTimeMillis,
                    dose = dose
                )
            } catch (e: Exception) {
                Log.e("TaskAlarmReceiver", "Failed to verify log completion, falling back to show notification: ${e.message}", e)
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
            } finally {
                pendingResult.finish()
            }
        }
    }
}
