package com.example.pillmate.service

import com.example.pillmate.domain.usecase.SyncAlarmsUseCase
import com.example.pillmate.notification.TaskNotificationManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class PillMateFcmService : FirebaseMessagingService() {

    private val syncAlarmsUseCase: SyncAlarmsUseCase by inject()
    private val profileId: String by inject()
    private val notificationManager: TaskNotificationManager by inject()

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val action = message.data["action"]
        val type = message.data["type"]
        val requestCode = message.data["requestCode"]?.toIntOrNull()

        // Handle both legacy "action" and new Cloud Function "type"
        when {
            type == "alarm_event" || type?.startsWith("SCHEDULE_") == true || action == "SYNC" -> {
                if (profileId.isNotBlank()) {
                    MainScope().launch {
                        try {
                            syncAlarmsUseCase(profileId)
                        } catch (e: Exception) {
                            android.util.Log.e("PillMateFcmService", "Remote sync failed", e)
                        }
                    }
                }
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
