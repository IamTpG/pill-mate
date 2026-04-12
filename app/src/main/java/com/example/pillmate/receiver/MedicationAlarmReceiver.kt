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

        if (reminderType == "ALARM") {
            val alarmIntent = Intent(context, com.example.pillmate.presentation.ui.TaskAlarmActivity::class.java).apply {
                putExtra("MED_ID", medId)
                putExtra("SCHEDULE_ID", scheduleId)
                putExtra("MED_NAME", medName)
                putExtra("DOSE_TEXT", dose)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(alarmIntent)
        } else {
            val notificationManager = MedicationNotificationManager(context)
            notificationManager.showReminderNotification(medId, scheduleId, medName, dose)
        }
    }
}
