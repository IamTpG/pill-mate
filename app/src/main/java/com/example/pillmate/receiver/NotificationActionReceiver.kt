package com.example.pillmate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pillmate.data.remote.firebase.FirestoreLogRepository
import com.example.pillmate.data.remote.firebase.FirestoreMedicationRepository
import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.domain.usecase.LogTaskUseCase
import com.example.pillmate.notification.TaskNotificationManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotificationActionReceiver : BroadcastReceiver(), KoinComponent {

    private val useCase: LogTaskUseCase by inject()
    private val profileId: String by inject()
    private val db: FirebaseFirestore by inject()
    private val manageReminderUseCase: com.example.pillmate.domain.usecase.ManageReminderUseCase by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val sourceId = intent.getStringExtra("SOURCE_ID") ?: return
        val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: sourceId
        val taskTypeString = intent.getStringExtra("TASK_TYPE") ?: "OTHER"
        val taskType = try { TaskType.valueOf(taskTypeString) } catch (e: Exception) { TaskType.OTHER }
        
        val status = when (action) {
            "ACTION_COMPLETE" -> LogStatus.COMPLETED
            "ACTION_SKIP" -> LogStatus.SKIPPED
            "ACTION_SNOOZE" -> {
                val title = intent.getStringExtra("TITLE") ?: "Task"
                val details = intent.getStringExtra("DETAILS") ?: ""
                
                TaskNotificationManager(context).scheduleTaskNotification(
                    sourceId, scheduleId, title, details, 10, sourceId.hashCode(), taskTypeString
                )
                android.widget.Toast.makeText(context, "Snoozed for 10 seconds", android.widget.Toast.LENGTH_SHORT).show()
                TaskNotificationManager(context).dismissNotification()
                return
            }
            else -> return
        }

        CoroutineScope(Dispatchers.IO).launch {
            useCase.execute(
                profileId = profileId,
                sourceId = sourceId,
                scheduleId = scheduleId,
                taskType = taskType,
                status = status,
                scheduledTime = Date(),
                dose = 1.0f
            )
            // Dismiss notification after action
            TaskNotificationManager(context).dismissNotification()
            
            // Advance RRULE next occurrence
            try {
                val scheduleDoc = db.collection("profiles").document(profileId)
                    .collection("schedules").document(scheduleId).get().await()
                val rrule = scheduleDoc.getString("recurrenceRule")
                if (rrule != null && rrule.contains("FREQ=DAILY")) {
                    val scheduleObj = scheduleDoc.toObject(com.example.pillmate.domain.model.Schedule::class.java)?.copy(id = scheduleId)
                    if (scheduleObj != null) {
                        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                        val currentStart = format.parse(scheduleObj.startTime)
                        if (currentStart != null) {
                            val nextStart = Date(currentStart.time + 24 * 60 * 60 * 1000)
                            val nextSchedule = scheduleObj.copy(startTime = format.format(nextStart))
                            manageReminderUseCase(profileId, nextSchedule)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
