package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.notification.TaskNotificationManager
import com.example.pillmate.util.AlarmTracker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ManageReminderUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val notificationManager: TaskNotificationManager,
    private val alarmTracker: AlarmTracker
) {
    suspend operator fun invoke(profileId: String, schedule: Schedule): Result<Unit> {
        val result = scheduleRepository.saveSchedule(profileId, schedule)
        
        if (result.isSuccess) {
            schedule.reminders.forEach { reminder ->
                val requestCode = "${schedule.id}_${reminder.minutesBefore}_${reminder.type.name}".hashCode()
                
                // ROBUST TIME PARSING (Matches SyncAlarmsUseCase)
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                val displayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val fallbackFormat = SimpleDateFormat("H:m", java.util.Locale.getDefault())

                val parsedStart: java.util.Date? = try {
                    when {
                        schedule.startTime.contains("T") -> isoFormat.parse(schedule.startTime)
                        schedule.startTime.isNotBlank() -> {
                            try { displayFormat.parse(schedule.startTime) } 
                            catch (e: Exception) { fallbackFormat.parse(schedule.startTime) }
                        }
                        else -> null
                    }
                } catch (e: Exception) { null }

                if (parsedStart != null) {
                    val target = Calendar.getInstance().apply {
                        time = parsedStart
                        // If it was just HH:mm, the date part might be 1970. 
                        // We need to set it to today if so.
                        val now = Calendar.getInstance()
                        if (get(Calendar.YEAR) < 2000) {
                            set(Calendar.YEAR, now.get(Calendar.YEAR))
                            set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR))
                        }
                        add(Calendar.MINUTE, -reminder.minutesBefore)
                    }
                    
                    if (target.timeInMillis < System.currentTimeMillis()) {
                        target.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    val delaySeconds = ((target.timeInMillis - System.currentTimeMillis()) / 1000).toInt()
                    
                    if (delaySeconds >= 0) {
                        val scheduled = notificationManager.scheduleTaskNotification(
                            sourceId = schedule.eventSnapshot.sourceId,
                            scheduleId = schedule.id,
                            title = schedule.eventSnapshot.title,
                            details = schedule.eventSnapshot.instructions,
                            delaySeconds = delaySeconds,
                            requestCode = requestCode,
                            profileId = profileId,
                            taskType = schedule.type.name,
                            reminderType = reminder.type.name,
                            rrule = schedule.recurrenceRule,
                            startTime = schedule.startTime
                        )
                        
                        if (scheduled) {
                            alarmTracker.addId(requestCode)
                        }
                    }
                }
            }
        }
        
        return result
    }
    
    fun cancelReminder(scheduleId: String, minutesBefore: Int) {
        notificationManager.cancelNotification((scheduleId + minutesBefore).hashCode())
    }
}
