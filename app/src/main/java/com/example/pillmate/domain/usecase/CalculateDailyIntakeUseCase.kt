package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.model.TaskLog
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.domain.repository.LogRepository
import com.example.pillmate.util.RecurrenceEvaluator
import kotlinx.coroutines.flow.firstOrNull
import java.util.Date
import java.util.Calendar

class CalculateDailyIntakeUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val logRepository: LogRepository
) {
    /**
     * Executes the dynamic intake calculation for a profile on a given date.
     * @return Map of medId to total planned dose for that day.
     */
    suspend fun execute(profileId: String, date: Date): Map<String, Float> {
        val schedules = scheduleRepository.getAllOnce(profileId).getOrNull() ?: return emptyMap()
        val logs = logRepository.getLogsForDayFlow(profileId, date).firstOrNull() ?: emptyList()
        return computeIntake(schedules, date, logs)
    }

    /**
     * Pure logic to calculate intake from a list of schedules.
     */
    fun computeIntake(
        schedules: List<Schedule>,
        date: Date,
        logs: List<TaskLog> = emptyList()
    ): Map<String, Float> {
        val result = mutableMapOf<String, Float>()
        val now = Calendar.getInstance()
        val isToday = isSameDay(date, now.time)

        schedules.forEach { schedule ->
            if (schedule.type == TaskType.MEDICATION && schedule.enabled && schedule.deletedAt == null) {
                
                val isOccurring = RecurrenceEvaluator.isOccurringOn(
                    targetDate = date,
                    rrule = schedule.recurrenceRule,
                    startTimeIso = schedule.startTime,
                    endDate = schedule.endDate
                ) || (schedule.frequency == "As Needed" && schedule.recurrenceRule == null) 
                
                if (isOccurring) {
                    val medId = schedule.eventSnapshot.sourceId
                    if (medId.isNotBlank()) {
                        val dailyTotal = schedule.doseTimes.sumOf { doseTime ->
                            if (shouldCountDose(schedule.id, doseTime, date, now, isToday, logs)) {
                                doseTime.dose.toDouble()
                            } else {
                                0.0
                            }
                        }.toFloat()

                        if (dailyTotal > 0f) {
                            result[medId] = (result[medId] ?: 0f) + dailyTotal
                        }
                    }
                }
            }
        }
        
        return result
    }

    private fun shouldCountDose(
        scheduleId: String,
        doseTime: com.example.pillmate.domain.model.DoseTime,
        date: Date,
        now: Calendar,
        isToday: Boolean,
        logs: List<com.example.pillmate.domain.model.TaskLog>
    ): Boolean {
        if (date.before(now.time) && !isToday) return false
        
        val scheduledCal = Calendar.getInstance().apply {
            time = date
            val (hour, minute) = parseTime(doseTime.time)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (isToday && scheduledCal.before(now)) {
            return false
        }

        val slotLog = logs.find { log ->
            log.scheduleId == scheduleId && isSameTime(log.scheduledTime, scheduledCal.time)
        }

        if (slotLog != null) {
            if (slotLog.status == LogStatus.SKIPPED || slotLog.status == LogStatus.COMPLETED) {
                return false
            }
        }

        return true
    }

    private fun parseTime(timeStr: String): Pair<Int, Int> {
        return try {
            if (timeStr.contains("AM", ignoreCase = true) || timeStr.contains("PM", ignoreCase = true)) {
                val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
                val date = sdf.parse(timeStr)
                val cal = Calendar.getInstance().apply { time = date }
                cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
            } else {
                val parts = timeStr.split(":")
                val hour = parts[0].toInt()
                val minute = parts.getOrNull(1)?.takeWhile { it.isDigit() }?.toInt() ?: 0
                hour to minute
            }
        } catch (e: Exception) {
            android.util.Log.e("IntakeUC", "Error parsing time: $timeStr", e)
            0 to 0
        }
    }

    private fun isSameDay(d1: Date, d2: Date): Boolean {
        val c1 = Calendar.getInstance().apply { time = d1 }
        val c2 = Calendar.getInstance().apply { time = d2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
               c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameTime(d1: Date, d2: Date): Boolean {
        val c1 = Calendar.getInstance().apply { time = d1 }
        val c2 = Calendar.getInstance().apply { time = d2 }
        return c1.get(Calendar.HOUR_OF_DAY) == c2.get(Calendar.HOUR_OF_DAY) &&
               c1.get(Calendar.MINUTE) == c2.get(Calendar.MINUTE)
    }
}
