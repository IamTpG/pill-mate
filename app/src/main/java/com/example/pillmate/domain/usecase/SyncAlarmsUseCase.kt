package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.notification.TaskNotificationManager
import java.util.Calendar

class SyncAlarmsUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val notificationManager: TaskNotificationManager,
    private val alarmTracker: com.example.pillmate.util.AlarmTracker
) {
    suspend operator fun invoke(profileId: String) {
        val result = scheduleRepository.getSchedules(profileId)
        val schedules = result.getOrNull() ?: return
        
        val previouslyScheduledIds = alarmTracker.getScheduledIds()
        val desiredScheduledIds = mutableSetOf<Int>()
        
        schedules.forEach { schedule ->
            schedule.reminders.forEach { reminder ->
                val requestCode = (schedule.id + reminder.minutesBefore).hashCode()
                desiredScheduledIds.add(requestCode)
                
                // Parse time and schedule
                val timeParts = schedule.startTime.split(":")
                if (timeParts.size == 2) {
                    val target = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                        set(Calendar.MINUTE, timeParts[1].toInt())
                        set(Calendar.SECOND, 0)
                        add(Calendar.MINUTE, -reminder.minutesBefore)
                    }
                    if (target.timeInMillis < System.currentTimeMillis()) {
                        target.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    val delaySeconds = ((target.timeInMillis - System.currentTimeMillis()) / 1000).toInt()
                    
                    notificationManager.scheduleTaskNotification(
                        sourceId = schedule.eventSnapshot.sourceId,
                        scheduleId = schedule.id,
                        title = schedule.eventSnapshot.title,
                        details = schedule.eventSnapshot.instructions ?: "",
                        delaySeconds = delaySeconds,
                        requestCode = requestCode,
                        profileId = profileId,
                        taskType = schedule.type.name,
                        reminderType = reminder.type.name,
                        rrule = schedule.recurrenceRule,
                        startTime = schedule.startTime
                    )
                }
            }
        }
        
        // ORPHAN CANCELLATION: IDs that are on the device but no longer in the DB
        val orphans = previouslyScheduledIds - desiredScheduledIds
        orphans.forEach { orphanId ->
            notificationManager.cancelNotification(orphanId)
        }
        
        // Update local state
        alarmTracker.updateScheduledIds(desiredScheduledIds)
    }
}
