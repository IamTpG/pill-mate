package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.model.TaskLog
import com.example.pillmate.domain.repository.LogRepository
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.util.RecurrenceEvaluator
import kotlinx.coroutines.flow.firstOrNull
import java.util.Date

data class NextTaskInfo(
    val title: String,
    val time: Date,
    val details: String,
    val scheduleId: String
)

class GetNextTaskUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val logRepository: LogRepository
) {
    suspend fun execute(profileId: String): NextTaskInfo? {
        if (profileId.isBlank()) return null
        
        val schedules = scheduleRepository.getAllOnce(profileId).getOrNull() ?: return null
        val now = Date()
        
        // Fetch today's logs to see what's already done
        val logs = logRepository.getLogsForDayFlow(profileId, now).firstOrNull() ?: emptyList()
        val completedScheduleIds = logs.filter { it.status == LogStatus.COMPLETED || it.status == LogStatus.SKIPPED }
            .map { it.scheduleId }.toSet()

        val nextTasks = mutableListOf<NextTaskInfo>()

        schedules.forEach { schedule ->
            // Skip "As Needed" medications as they aren't scheduled
            if (schedule.frequency == "As Needed" || schedule.recurrenceRule == null) return@forEach
            if (completedScheduleIds.contains(schedule.id)) return@forEach

            schedule.doseTimes.forEach { doseTime ->
                val nextOccurrence = RecurrenceEvaluator.getNextOccurrence(
                    fromDate = now,
                    rrule = schedule.recurrenceRule,
                    startTimeIso = schedule.startTime,
                    endDate = schedule.endDate,
                    doseTime = doseTime.time
                )

                if (nextOccurrence != null) {
                    nextTasks.add(
                        NextTaskInfo(
                            title = schedule.eventSnapshot.title,
                            time = nextOccurrence,
                            details = doseTime.doseContext.ifBlank { schedule.eventSnapshot.instructions ?: "" },
                            scheduleId = schedule.id
                        )
                    )
                }
            }
        }

        return nextTasks.minByOrNull { it.time }
    }
}
