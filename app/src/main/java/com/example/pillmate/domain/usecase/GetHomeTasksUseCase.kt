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
                    val displayTime: String
                    
                    // SimpleDateFormat for ISO parsing
                    val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                    val displayFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    
                    val scheduledTimeDate: Date? = try {
                        val cal = Calendar.getInstance().apply { 
                            time = date
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        
                        if (schedule.startTime.contains("T")) {
                            val parsed = isoFormat.parse(schedule.startTime)!!
                            val tempCal = Calendar.getInstance().apply { time = parsed }
                            cal.set(Calendar.HOUR_OF_DAY, tempCal.get(Calendar.HOUR_OF_DAY))
                            cal.set(Calendar.MINUTE, tempCal.get(Calendar.MINUTE))
                            cal.time
                        } else {
                            val timeParts = schedule.startTime.split(":")
                            if (timeParts.size >= 2) {
                                cal.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                                cal.set(Calendar.MINUTE, timeParts[1].toInt())
                                cal.time
                            } else null
                        }
                    } catch (e: Exception) {
                        null
                    }

                    if (status == null && scheduledTimeDate != null) {
                        if (scheduledTimeDate.before(now)) {
                            status = LogStatus.MISSED
                        }
                    }
                    
                    displayTime = scheduledTimeDate?.let { displayFormat.format(it) } ?: schedule.startTime

                    val details = if (schedule.type == TaskType.MEDICATION) {
                        "Take ${schedule.eventSnapshot.dose} ${schedule.eventSnapshot.unit ?: "dose"}"
                    } else {
                        schedule.eventSnapshot.instructions ?: ""
                    }

                    HomeTask(
                        scheduleId = schedule.id,
                        sourceId = schedule.eventSnapshot.sourceId,
                        title = schedule.eventSnapshot.title,
                        time = displayTime,
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
