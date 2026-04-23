package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.notification.TaskNotificationManager
import java.util.Calendar

class ManageReminderUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val notificationManager: TaskNotificationManager
) {
    suspend operator fun invoke(profileId: String, schedule: Schedule): Result<Unit> {
        val result = scheduleRepository.saveSchedule(profileId, schedule)
        
        if (result.isSuccess) {
            schedule.doseTimes.forEach { doseTime ->
                schedule.reminders.forEach { reminder ->
                    val requestCode = (schedule.id + doseTime.time + reminder.minutesBefore).hashCode()
                    
                    val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                    val targetTimeInMillis = try {
                        format.parse(doseTime.time)?.time ?: 0L
                    } catch (e: Exception) { 0L }

                    val delaySeconds = if (targetTimeInMillis > 0L) {
                        val target = Calendar.getInstance().apply {
                             timeInMillis = targetTimeInMillis
                             add(Calendar.MINUTE, -reminder.minutesBefore)
                        }
                        if (target.timeInMillis < System.currentTimeMillis()) {
                            target.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        ((target.timeInMillis - System.currentTimeMillis()) / 1000).toInt()
                    } else {
                        val timeParts = doseTime.time.split(":")
                        if (timeParts.size >= 2) {
                            val target = Calendar.getInstance().apply {
                                val hrStr = timeParts[0]
                                val cleanMin = timeParts[1].filter { it.isDigit() }.toInt()
                                var hr = hrStr.filter { it.isDigit() }.toInt()
                                if (doseTime.time.contains("PM", ignoreCase = true) && hr < 12) hr += 12
                                if (doseTime.time.contains("AM", ignoreCase = true) && hr == 12) hr = 0
                                
                                set(Calendar.HOUR_OF_DAY, hr)
                                set(Calendar.MINUTE, cleanMin)
                                set(Calendar.SECOND, 0)
                                add(Calendar.MINUTE, -reminder.minutesBefore)
                            }
                            if (target.timeInMillis < System.currentTimeMillis()) {
                                target.add(Calendar.DAY_OF_YEAR, 1)
                            }
                            ((target.timeInMillis - System.currentTimeMillis()) / 1000).toInt()
                        } else -1
                    }
                    
                    if (delaySeconds >= 0) {
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
        
        return result
    }
    
    fun cancelReminder(scheduleId: String, minutesBefore: Int) {
        notificationManager.cancelNotification((scheduleId + minutesBefore).hashCode())
    }
}
