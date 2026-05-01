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
        val result = scheduleRepository.add(profileId, schedule)
        
        if (result.isSuccess) {
            schedule.doseTimes.forEach { doseTime ->
                schedule.reminders.forEach { reminder ->
                    val requestCode = "${schedule.id}_${doseTime.time}_${reminder.minutesBefore}_${reminder.type.name}".hashCode()
                    
                    // ROBUST TIME PARSING (Matches SyncAlarmsUseCase)
                    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                    val displayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val fallbackFormat = SimpleDateFormat("H:m", java.util.Locale.getDefault())

                    val parsedStart: java.util.Date? = try {
                        when {
                            doseTime.time.contains("T") -> isoFormat.parse(doseTime.time)
                            doseTime.time.isNotBlank() -> {
                                try { displayFormat.parse(doseTime.time) } 
                                catch (e: Exception) { fallbackFormat.parse(doseTime.time) }
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
                        
                        // Buffer of 2 seconds to account for parsing precision loss
                        while (target.timeInMillis < System.currentTimeMillis() - 2000) {
                            target.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        val delaySeconds = ((target.timeInMillis - System.currentTimeMillis()) / 1000).coerceAtLeast(0L).toInt()
                        
                        if (delaySeconds >= 0) {
                            val scheduled = notificationManager.scheduleTaskNotification(
                                sourceId = schedule.eventSnapshot.sourceId,
                                scheduleId = schedule.id,
                                title = schedule.eventSnapshot.title,
                                details = doseTime.doseContext.ifBlank { schedule.eventSnapshot.instructions ?: "" },
                                delaySeconds = delaySeconds,
                                requestCode = requestCode,
                                profileId = profileId,
                                taskType = schedule.type.name,
                                reminderType = reminder.type.name,
                                rrule = schedule.recurrenceRule,
                                startTime = doseTime.time,
                                instructions = schedule.eventSnapshot.instructions
                            )
                            
                            if (scheduled) {
                                alarmTracker.addId(requestCode)
                            }
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
