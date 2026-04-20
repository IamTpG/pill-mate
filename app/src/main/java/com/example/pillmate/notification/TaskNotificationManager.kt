package com.example.pillmate.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.pillmate.receiver.TaskAlarmReceiver
import com.example.pillmate.receiver.NotificationActionReceiver

class TaskNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "task_reminders"
        const val CHANNEL_NAME = "Task Reminders"
        const val NOTIFICATION_ID = 1001
        const val ALARM_CHANNEL_ID = "task_alarms"
        const val ALARM_CHANNEL_NAME = "Critical Task Alarms"
        const val LOW_STOCK_CHANNEL_ID = "low_stock_alerts"
        const val LOW_STOCK_CHANNEL_NAME = "Low Stock Alerts"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Reminders for scheduled tasks"
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

            // Low Stock Channel
            val lowStockChannel = NotificationChannel(LOW_STOCK_CHANNEL_ID, LOW_STOCK_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Alerts when medication stock is low"
            }
            notificationManager.createNotificationChannel(lowStockChannel)
        }
    }

    fun showTaskNotification(
        sourceId: String,
        scheduleId: String,
        title: String,
        details: String,
        taskType: String = "OTHER",
        reminderType: String = "NOTIFICATION",
        rrule: String? = null,
        startTime: String? = null,
        instructions: String? = null
    ) {
        createNotificationChannel()
        val encTitle = android.net.Uri.encode(title.ifBlank { " " })
        val encDetails = android.net.Uri.encode(details.ifBlank { " " })
        val encType = android.net.Uri.encode(taskType.ifBlank { "OTHER" })
        val encInstr = android.net.Uri.encode((instructions ?: "").ifBlank { " " })
        val encTime = android.net.Uri.encode((startTime ?: "").ifBlank { " " })
        val encRrule = android.net.Uri.encode((rrule ?: "").ifBlank { " " })

        val deepLinkUri = android.net.Uri.parse("pillmate://alarm?sourceId=$sourceId&scheduleId=$scheduleId&title=$encTitle&details=$encDetails&type=$encType&instructions=$encInstr&time=$encTime&rrule=$encRrule")

        val fullScreenIntent = Intent(context, com.example.pillmate.MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = deepLinkUri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action Intents
        val actionText = when (taskType) {
            "MEDICATION" -> "TAKE NOW"
            "APPOINTMENT" -> "ATTEND"
            "EXERCISE" -> "START"
            else -> "COMPLETE"
        }

        val primaryIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_COMPLETE"
            putExtra("SOURCE_ID", sourceId)
            putExtra("SCHEDULE_ID", scheduleId)
            putExtra("TASK_TYPE", taskType)
        }
        val primaryPendingIntent = PendingIntent.getBroadcast(
            context, 1, primaryIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_SKIP"
            putExtra("SOURCE_ID", sourceId)
            putExtra("SCHEDULE_ID", scheduleId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context, 2, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("SOURCE_ID", sourceId)
            putExtra("SCHEDULE_ID", scheduleId)
            putExtra("TITLE", title)
            putExtra("DETAILS", details)
            putExtra("TASK_TYPE", taskType)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, 3, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = when (taskType) {
            "MEDICATION" -> android.R.drawable.ic_dialog_info
            "APPOINTMENT" -> android.R.drawable.ic_menu_myplaces
            "EXERCISE" -> android.R.drawable.ic_menu_directions
            else -> android.R.drawable.ic_menu_agenda
        }

        val channelId = if (reminderType == "ALARM") ALARM_CHANNEL_ID else CHANNEL_ID
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon) 
            .setContentTitle(title)
            .setContentText(details)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, actionText, primaryPendingIntent)
            .addAction(0, "SNOOZE", snoozePendingIntent)
            .addAction(0, "SKIP", skipPendingIntent)
            .setOngoing(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun scheduleTaskNotification(
        sourceId: String,
        scheduleId: String,
        title: String,
        details: String,
        delaySeconds: Int,
        requestCode: Int,
        profileId: String, // Added profileId
        taskType: String = "OTHER",
        reminderType: String = "NOTIFICATION",
        rrule: String? = null,
        startTime: String? = null,
        instructions: String? = null
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("SOURCE_ID", sourceId)
            putExtra("SCHEDULE_ID", scheduleId)
            putExtra("PROFILE_ID", profileId) // Added PROFILE_ID
            putExtra("TITLE", title)
            putExtra("DETAILS", details)
            putExtra("TASK_TYPE", taskType)
            putExtra("REMINDER_TYPE", reminderType)
            putExtra("EXTRA_RRULE", rrule)
            putExtra("EXTRA_START_TIME", startTime)
            putExtra("EXTRA_INSTRUCTIONS", instructions)
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
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelAllReminders(scheduleId: String, reminders: List<com.example.pillmate.domain.model.Reminder>, sourceId: String? = null) {
        reminders.forEach { reminder ->
            val requestCode = (scheduleId + reminder.minutesBefore).hashCode()
            cancelNotification(requestCode)
        }
        // Also cancel standard snooze if sourceId provided
        sourceId?.let {
            cancelNotification(it.hashCode())
        }
    }

    fun showLowStockNotification(medName: String, remaining: Float) {
        val builder = NotificationCompat.Builder(context, LOW_STOCK_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Low Stock Alert")
            .setContentText("You only have $remaining remaining of $medName.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(medName.hashCode(), builder.build())
    }
}
