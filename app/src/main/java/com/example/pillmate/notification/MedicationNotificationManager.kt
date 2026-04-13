package com.example.pillmate.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.pillmate.R
import com.example.pillmate.presentation.ui.TaskAlarmActivity
import com.example.pillmate.receiver.MedicationAlarmReceiver
import com.example.pillmate.receiver.NotificationActionReceiver

class MedicationNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "medication_reminders"
        const val CHANNEL_NAME = "Medication Reminders"
        const val NOTIFICATION_ID = 1001
        const val ALARM_CHANNEL_ID = "medication_alarms"
        const val ALARM_CHANNEL_NAME = "Critical Medication Alarms"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Urgent reminders to take your medication"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            // Dedicated channel for Alarms
            val alarmChannel = NotificationChannel(ALARM_CHANNEL_ID, ALARM_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Critical alarms that bypass DND"
                enableLights(true)
                enableVibration(true)
                setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI, android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    fun showReminderNotification(medId: String, scheduleId: String, medName: String, dose: String, reminderType: String = "NOTIFICATION") {
        createNotificationChannel() // Ensure channel exists
        val fullScreenIntent = Intent(context, TaskAlarmActivity::class.java).apply {
            putExtra("MED_ID", medId)
            putExtra("SCHEDULE_ID", scheduleId)
            putExtra("MED_NAME", medName)
            putExtra("DOSE_TEXT", dose)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action Intents
        val takeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_TAKE"
            putExtra("MED_ID", medId)
            putExtra("SCHEDULE_ID", scheduleId)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context, 1, takeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_SKIP"
            putExtra("MED_ID", medId)
            putExtra("SCHEDULE_ID", scheduleId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context, 2, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("MED_ID", medId)
            putExtra("SCHEDULE_ID", scheduleId)
            putExtra("MED_NAME", medName)
            putExtra("DOSE", dose)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, 3, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (reminderType == "ALARM") ALARM_CHANNEL_ID else CHANNEL_ID
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle("Time for $medName")
            .setContentText("Check your dosage details")
            .setSubText(dose)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, "TAKE NOW", takePendingIntent)
            .addAction(0, "SNOOZE", snoozePendingIntent)
            .addAction(0, "SKIP", skipPendingIntent)
            .setOngoing(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun scheduleNotification(
        medId: String, 
        scheduleId: String, 
        medName: String, 
        dose: String, 
        delaySeconds: Int, 
        requestCode: Int,
        reminderType: String = "NOTIFICATION"
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra("MED_ID", medId)
            putExtra("SCHEDULE_ID", scheduleId)
            putExtra("MED_NAME", medName)
            putExtra("DOSE", dose)
            putExtra("REMINDER_TYPE", reminderType)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            requestCode, // Unique ID per reminder
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (delaySeconds * 1000)

        return try {
            if (reminderType == "ALARM") {
                val alarmClockInfo = android.app.AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    true
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    false // Scheduled but not exact
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                true
            } else {
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun dismissNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun cancelNotification(requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, MedicationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
