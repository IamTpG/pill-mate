package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.domain.repository.LogRepository
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.presentation.model.HomeTask
import com.example.pillmate.util.RecurrenceEvaluator
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
        return scheduleRepository.getAll(profileId)
            .combine(logRepository.getLogsForDayFlow(profileId, date)) { schedules, logs ->
                val now = Date()
                
                val cal = Calendar.getInstance().apply {
                    time = date
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val targetDateStart = cal.time
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val targetDateEnd = cal.time
                
                val activeSchedules = schedules.filter { schedule ->
                    RecurrenceEvaluator.isOccurringOn(
                        targetDate = date,
                        rrule = schedule.recurrenceRule,
                        startTimeIso = schedule.startTime,
                        endDate = schedule.endDate
                    )
                }

                val homeTasks = activeSchedules.flatMap { schedule ->
                    schedule.doseTimes.map { doseTime ->
                        val displayTime: String
                        
                        // SimpleDateFormat for ISO parsing
                        val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                        val displayFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        
                        val scheduledTimeDate: Date? = try {
                            if (doseTime.time.contains("T")) {
                                isoFormat.parse(doseTime.time)
                            } else {
                                val timeParts = doseTime.time.split(":")
                                if (timeParts.size >= 2) {
                                    val hrStr = timeParts[0]
                                    val cleanMin = timeParts[1].filter { it.isDigit() }.toInt()
                                    var hr = hrStr.filter { it.isDigit() }.toInt()
                                    if (doseTime.time.contains("PM", ignoreCase = true) && hr < 12) hr += 12
                                    if (doseTime.time.contains("AM", ignoreCase = true) && hr == 12) hr = 0

                                    val cal = Calendar.getInstance().apply {
                                        time = date
                                        set(Calendar.HOUR_OF_DAY, hr)
                                        set(Calendar.MINUTE, cleanMin)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    cal.time
                                } else null
                            }
                        } catch (e: Exception) {
                            null
                        }

                        // Match against both schedule ID and the explicit scheduled time
                        val matchingLog = logs.find { log -> 
                            log.scheduleId == schedule.id && 
                            scheduledTimeDate != null && 
                            Math.abs(log.scheduledTime.time - scheduledTimeDate.time) < 60000 
                        }
                        
                        var status = matchingLog?.status

                        if (status == null && scheduledTimeDate != null) {
                            if (scheduledTimeDate.before(now)) {
                                status = LogStatus.MISSED
                            }
                        }
                        
                        displayTime = scheduledTimeDate?.let { displayFormat.format(it) } ?: doseTime.time

                        val details = if (schedule.type == TaskType.MEDICATION) {
                            doseTime.doseContext.ifBlank { "Take ${schedule.eventSnapshot.dose} ${schedule.eventSnapshot.unit ?: "dose"}" }
                        } else {
                            schedule.eventSnapshot.instructions ?: ""
                        }

                        val fallbackDose = doseTime.doseContext.split(" ").firstOrNull()?.toFloatOrNull() ?: doseTime.dose

                        HomeTask(
                            scheduleId = schedule.id,
                            sourceId = schedule.eventSnapshot.sourceId,
                            title = schedule.eventSnapshot.title,
                            time = displayTime,
                            doseDescription = details,
                            dose = fallbackDose,
                            taskType = schedule.type,
                            status = status,
                            frequency = schedule.frequency
                        )
                    }
                }.sortedBy { it.time }

                val completed = homeTasks.count { it.status == LogStatus.COMPLETED }
                val total = homeTasks.size
                
                HomeData(homeTasks, completed, total)
            }
    }
}
