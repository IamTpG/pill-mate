package com.example.pillmate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.pillmate.data.remote.firebase.FirestoreLogRepository
import com.example.pillmate.data.remote.firebase.FirestoreMedicationRepository
import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.domain.usecase.LogTaskUseCase
import com.example.pillmate.domain.usecase.ManageReminderUseCase
import com.example.pillmate.notification.TaskNotificationManager
import com.example.pillmate.util.AlarmTracker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import java.util.Locale.getDefault

class NotificationActionReceiver : BroadcastReceiver(), KoinComponent {

    private val useCase: LogTaskUseCase by inject()
    private val profileId: String by inject()
    private val db: FirebaseFirestore by inject()
    private val manageReminderUseCase: ManageReminderUseCase by inject()

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
                
                val snoozeRequestCode = ("SNOOZE_" + scheduleId).hashCode()
                TaskNotificationManager(context).scheduleTaskNotification(
                    sourceId, scheduleId, title, details, 10, snoozeRequestCode, taskTypeString
                )
                // Register snooze in tracker so sync doesn't kill it
                GlobalContext.get().get<AlarmTracker>().addId(snoozeRequestCode)
                Toast.makeText(context, "Snoozed for 10 seconds", android.widget.Toast.LENGTH_SHORT).show()
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
            // Advance RRULE next occurrence
            try {
                val scheduleDoc = db.collection("profiles").document(profileId)
                    .collection("schedules").document(scheduleId).get().await()
                
                val scheduleObj = scheduleDoc.toObject(Schedule::class.java)?.copy(id = scheduleId)
                if (scheduleObj != null) {
                    // CANCELLATION: Cancel other pending reminders AND snoozes
                    TaskNotificationManager(context).cancelAllReminders(scheduleId, scheduleObj.reminders, sourceId)

                    val rrule = scheduleDoc.getString("recurrenceRule")
                    if (rrule != null && rrule.contains("FREQ=DAILY")) {
                        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", getDefault())
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
