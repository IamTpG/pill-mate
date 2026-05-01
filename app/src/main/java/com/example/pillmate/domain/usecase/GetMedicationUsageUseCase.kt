package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.domain.repository.ScheduleRepository
import java.util.Calendar
import java.util.Date

class GetMedicationUsageUseCase(
    private val scheduleRepository: ScheduleRepository
) {
    /**
     * Calculates the predicted dose requirement for a medication on a specific date.
     */
    suspend fun execute(profileId: String, medicationId: String, date: Date): Float {
        val result = scheduleRepository.getSchedulesBySourceId(profileId, medicationId)
        val schedules = result.getOrNull() ?: return 0f

        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetDayStart = cal.time
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val targetDayEnd = cal.time

        return schedules
            .filter { it.enabled && it.type == TaskType.MEDICATION }
            .filter { schedule ->
                // Basic check: schedule must be active on this date
                val startsBeforeEnd = schedule.createdAt.before(targetDayEnd)
                val endsAfterStart = schedule.endDate == null || schedule.endDate.after(targetDayStart)
                
                // For now, simplicity: if it's daily, it applies. 
                // In a fuller app, we'd parse the RRULE here.
                val isDaily = schedule.recurrenceRule?.contains("FREQ=DAILY") ?: true
                
                startsBeforeEnd && endsAfterStart && isDaily
            }
            .sumOf { schedule ->
                schedule.doseTimes.sumOf { it.dose.toDouble() }
            }.toFloat()
    }
}
