package com.example.pillmate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pillmate.notification.MedicationNotificationManager

class MedicationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("MedicationAlarmReceiver", "Alarm received!")
        val medId = intent.getStringExtra("MED_ID") ?: return
        val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: return
        val medName = intent.getStringExtra("MED_NAME") ?: "Medication"
        val dose = intent.getStringExtra("DOSE") ?: "1.0"

        val reminderType = intent.getStringExtra("REMINDER_TYPE") ?: "NOTIFICATION"

        // Background activity starts are blocked on Android 10+, must use FullScreenIntent via NotificationManager
        val notificationManager = MedicationNotificationManager(context)
        notificationManager.showReminderNotification(medId, scheduleId, medName, dose, reminderType)
    }
}
