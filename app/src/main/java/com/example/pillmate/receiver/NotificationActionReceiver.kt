package com.example.pillmate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pillmate.data.remote.firebase.FirestoreLogRepository
import com.example.pillmate.data.remote.firebase.FirestoreMedicationRepository
import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.usecase.LogMedicationUseCase
import com.example.pillmate.notification.MedicationNotificationManager
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

    private val useCase: LogMedicationUseCase by inject()
    private val profileId: String by inject()
    private val db: FirebaseFirestore by inject()
    private val manageReminderUseCase: com.example.pillmate.domain.usecase.ManageReminderUseCase by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val medId = intent.getStringExtra("MED_ID") ?: return
        val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: medId
        
        val status = when (action) {
            "ACTION_TAKE" -> LogStatus.COMPLETED
            "ACTION_SKIP" -> LogStatus.SKIPPED
            "ACTION_SNOOZE" -> {
                // For snooze, we just re-schedule the notification for later
                val medName = intent.getStringExtra("MED_NAME") ?: "Medication"
                val dose = intent.getStringExtra("DOSE") ?: "1.0"
                
                MedicationNotificationManager(context).scheduleNotification(medId, scheduleId, medName, dose, 10, medId.hashCode()) // Snooze for 10s for debug
                android.widget.Toast.makeText(context, "Snoozed for 10 seconds", android.widget.Toast.LENGTH_SHORT).show()
                MedicationNotificationManager(context).dismissNotification()
                return
            }
            else -> return
        }

        CoroutineScope(Dispatchers.IO).launch {
            useCase.execute(
                profileId = profileId,
                medId = medId,
                scheduleId = scheduleId,
                status = status,
                scheduledTime = Date(), // In a real app, pass the scheduled time
                dose = 1.0f
            )
            // Dismiss notification after action
            MedicationNotificationManager(context).dismissNotification()
            
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
