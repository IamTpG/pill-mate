package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.notification.MedicationNotificationManager
import java.util.Calendar

class SyncAlarmsUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val notificationManager: MedicationNotificationManager
) {
    suspend operator fun invoke(profileId: String) {
        val result = scheduleRepository.getSchedules(profileId)
        val schedules = result.getOrNull() ?: return
        
        schedules.forEach { schedule ->
            schedule.reminders.forEach { reminder ->
                val requestCode = (schedule.id + reminder.minutesBefore).hashCode()
                
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
                    
                    notificationManager.scheduleNotification(
                        medId = schedule.eventSnapshot.sourceId,
                        scheduleId = schedule.id,
                        medName = schedule.eventSnapshot.title,
                        dose = "${schedule.eventSnapshot.dose}",
                        delaySeconds = delaySeconds,
                        requestCode = requestCode
                    )
                }
            }
        }
    }
}
