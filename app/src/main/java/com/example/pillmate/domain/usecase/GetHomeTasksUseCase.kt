package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.domain.repository.LogRepository
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.presentation.model.HomeTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import java.util.Date

data class HomeData(
    val tasks: List<HomeTask>,
    val completedCount: Int,
    val totalCount: Int
)

class GetHomeTasksUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val logRepository: LogRepository
) {
    fun execute(profileId: String, date: Date): Flow<HomeData> {
        return scheduleRepository.getSchedulesFlow(profileId)
            .combine(logRepository.getLogsForDayFlow(profileId, date)) { schedules, logs ->
                val now = Date()
                val homeTasks = schedules.map { schedule ->
                    val matchingLog = logs.find { it.scheduleId == schedule.id }
                    
                    var status = matchingLog?.status
                    if (status == null) {
                        val timeParts = schedule.startTime.split(":")
                        if (timeParts.size == 2) {
                            val hour = timeParts[0].toInt()
                            val min = timeParts[1].toInt()
                            val scheduledCal = Calendar.getInstance()
                            scheduledCal.time = date
                            scheduledCal.set(Calendar.HOUR_OF_DAY, hour)
                            scheduledCal.set(Calendar.MINUTE, min)
                            scheduledCal.set(Calendar.SECOND, 0)
                            
                            if (scheduledCal.time.before(now)) {
                                status = LogStatus.MISSED
                            }
                        }
                    }

                    val details = if (schedule.type == TaskType.MEDICATION) {
                        "Take ${schedule.eventSnapshot.dose} ${schedule.eventSnapshot.unit ?: "dose"}"
                    } else {
                        schedule.eventSnapshot.instructions ?: ""
                    }

                    HomeTask(
                        scheduleId = schedule.id,
                        sourceId = schedule.eventSnapshot.sourceId,
                        title = schedule.eventSnapshot.title,
                        time = schedule.startTime,
                        doseDescription = details,
                        taskType = schedule.type,
                        status = status
                    )
                }.sortedBy { it.time }

                val completed = homeTasks.count { it.status == LogStatus.COMPLETED }
                val total = homeTasks.size
                
                HomeData(homeTasks, completed, total)
            }
    }
}
