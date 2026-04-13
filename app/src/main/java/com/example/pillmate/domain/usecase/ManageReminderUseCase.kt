package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.notification.MedicationNotificationManager
import java.util.Calendar

class ManageReminderUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val notificationManager: MedicationNotificationManager
) {
    suspend operator fun invoke(profileId: String, schedule: Schedule): Result<Unit> {
        val result = scheduleRepository.saveSchedule(profileId, schedule)
        
        if (result.isSuccess) {
            schedule.reminders.forEach { reminder ->
                val requestCode = (schedule.id + reminder.minutesBefore).hashCode()
                
                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                val targetTimeInMillis = try {
                    format.parse(schedule.startTime)?.time ?: 0L
                } catch (e: Exception) { 0L }

                val delaySeconds = if (targetTimeInMillis > 0L) {
                    val target = Calendar.getInstance().apply {
                         timeInMillis = targetTimeInMillis
                         add(Calendar.MINUTE, -reminder.minutesBefore)
                    }
                    if (target.timeInMillis < System.currentTimeMillis()) {
                        // For ISO dates, if we missed the time, we might skip scheduling or still try.
                        // Since it's a fixed date, adding 1 day automatically breaks the precise datetime, 
                        // but let's keep the logic.
                        target.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    ((target.timeInMillis - System.currentTimeMillis()) / 1000).toInt()
                } else {
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
                        ((target.timeInMillis - System.currentTimeMillis()) / 1000).toInt()
                    } else -1
                }
                
                if (delaySeconds >= 0) {
                    notificationManager.scheduleNotification(
                        medId = schedule.eventSnapshot.sourceId,
                        scheduleId = schedule.id,
                        medName = schedule.eventSnapshot.title,
                        dose = "${schedule.eventSnapshot.dose}",
                        delaySeconds = delaySeconds,
                        requestCode = requestCode,
                        reminderType = reminder.type.name
                    )
                }
            }
        }
        
        return result
    }
    
    fun cancelReminder(scheduleId: String, minutesBefore: Int) {
        notificationManager.cancelNotification((scheduleId + minutesBefore).hashCode())
    }
}
