package com.example.pillmate.service

import android.util.Log
import androidx.work.*
import com.example.pillmate.domain.usecase.SyncAlarmsUseCase
import com.example.pillmate.notification.TaskNotificationManager
import com.example.pillmate.workers.AlarmSyncWorker
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class PillMateFcmService : FirebaseMessagingService() {

    private val notificationManager: TaskNotificationManager by inject()

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        android.util.Log.d("PillMateFcmService", ">>> FCM RECEIVED! data=${message.data}")

        val action = message.data["action"]
        val type = message.data["type"]
        val requestCode = message.data["requestCode"]?.toIntOrNull()

        val profileIdFromFcm = message.data["profileId"]

        // Handle both legacy "action" and new Cloud Function "type"
        when {
            type == "alarm_event" || type?.startsWith("schedule_") == true || action == "SYNC" -> {
                val inputData = workDataOf("profileId" to profileIdFromFcm)
                val workRequest = OneTimeWorkRequestBuilder<AlarmSyncWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(inputData)
                    .build()
                WorkManager.getInstance(applicationContext).enqueue(workRequest)
                Log.d("PillMateFcmService", "Sync work enqueued for profile: $profileIdFromFcm")
            }
            action == "SILENCE" -> {
                requestCode?.let {
                    notificationManager.cancelNotification(it)
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token management handled by FcmTokenManager in common cases,
        // but could trigger a sync here if needed.
    }
}
