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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val medId = intent.getStringExtra("MED_ID") ?: return
        val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: medId
        
        val auth = FirebaseAuth.getInstance()
        val profileId = auth.currentUser?.uid ?: "xY1SqtnTwiQqDkQDaZHGsZ6gHrh2" // Fallback for debug

        val db = FirebaseFirestore.getInstance()
        val medicationRepo = FirestoreMedicationRepository(db, profileId)
        val logRepo = FirestoreLogRepository(db)
        val useCase = LogMedicationUseCase(medicationRepo, logRepo)

        val status = when (action) {
            "ACTION_TAKE" -> LogStatus.COMPLETED
            "ACTION_SKIP" -> LogStatus.SKIPPED
            "ACTION_SNOOZE" -> {
                // For snooze, we just re-schedule the notification for later
                val medName = intent.getStringExtra("MED_NAME") ?: "Medication"
                val dose = intent.getStringExtra("DOSE") ?: "1.0"
                
                MedicationNotificationManager(context).scheduleNotification(medId, scheduleId, medName, dose, 10) // Snooze for 10s for debug
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
        }
    }
}
