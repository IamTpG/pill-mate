package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.notification.TaskNotificationManager
import java.util.Calendar

class SyncAlarmsUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val notificationManager: TaskNotificationManager
) {
    suspend operator fun invoke(profileId: String) {
        val result = scheduleRepository.getSchedules(profileId)
        val schedules = result.getOrNull() ?: return
        
        schedules.forEach { schedule ->
            schedule.doseTimes.forEach { doseTime ->
                schedule.reminders.forEach { reminder ->
                    val requestCode = (schedule.id + doseTime.time + reminder.minutesBefore).hashCode()
                    
                    val timeParts = doseTime.time.split(":")
                    if (timeParts.size >= 2) {
                        val hrStr = timeParts[0]
                        val cleanMin = timeParts[1].filter { it.isDigit() }.toInt()
                        var hr = hrStr.filter { it.isDigit() }.toInt()
                        if (doseTime.time.contains("PM", ignoreCase = true) && hr < 12) hr += 12
                        if (doseTime.time.contains("AM", ignoreCase = true) && hr == 12) hr = 0
                        
                        val target = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, hr)
                            set(Calendar.MINUTE, cleanMin)
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
                            details = doseTime.doseContext.ifBlank { schedule.eventSnapshot.instructions ?: "" },
                            delaySeconds = delaySeconds,
                            requestCode = requestCode,
                            taskType = schedule.type.name,
                            reminderType = reminder.type.name
                        )
                    }
                }
            }
        }
    }
}
