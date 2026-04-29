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
import android.util.Log

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
        Log.d("GetHomeTasks", "Executing for profileId: $profileId, date: $date")
        return scheduleRepository.getSchedulesFlow(profileId)
            .combine(logRepository.getLogsForDayFlow(profileId, date)) { schedules, logs ->
                Log.d("GetHomeTasks", "Flow emitted. Schedules: ${schedules.size}, Logs: ${logs.size}")
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
                    // Schedule must start on or before the end of the target day
                    val startsBeforeOrOnTarget = (schedule.createdAt ?: Date(0)).before(targetDateEnd)
                    // Schedule must end on or after the start of the target day
                    val endsAfterOrOnTarget = schedule.endDate == null || schedule.endDate.after(targetDateStart)
                    
                    startsBeforeOrOnTarget && endsAfterOrOnTarget
                }
                Log.d("GetHomeTasks", "Active schedules count: ${activeSchedules.size}")

                val homeTasks = activeSchedules.flatMap { schedule ->
                    Log.d("GetHomeTasks", "Schedule ${schedule.id} has ${schedule.doseTimes.size} doseTimes")
                    schedule.doseTimes.map { doseTime ->

                        val displayTime: String
                        
                        // SimpleDateFormat for ISO parsing
                        val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                        val displayFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        
                        val scheduledTimeDate: Date? = try {
                            val cal = Calendar.getInstance().apply {
                                time = date // Use the target day
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            if (doseTime.time.contains("T")) {
                                // Extract HH:mm from ISO string but apply to 'date' (target day)
                                val parsed = isoFormat.parse(doseTime.time)
                                if (parsed != null) {
                                    val parsedCal = Calendar.getInstance().apply { time = parsed }
                                    cal.set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY))
                                    cal.set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE))
                                    cal.time
                                } else null
                            } else {
                                val timeParts = doseTime.time.split(":")
                                if (timeParts.size >= 2) {
                                    val hrStr = timeParts[0]
                                    val minPart = timeParts[1]
                                    val cleanMin = minPart.filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.toInt() ?: 0
                                    var hr = hrStr.filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.toInt() ?: 0
                                    
                                    if (doseTime.time.contains("PM", ignoreCase = true) && hr < 12) hr += 12
                                    if (doseTime.time.contains("AM", ignoreCase = true) && hr == 12) hr = 0

                                    cal.set(Calendar.HOUR_OF_DAY, hr)
                                    cal.set(Calendar.MINUTE, cleanMin)
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
                            status = status
                        )
                    }
                }.sortedBy { it.time }

                val completed = homeTasks.count { it.status == LogStatus.COMPLETED }
                val total = homeTasks.size
                
                HomeData(homeTasks, completed, total)
            }
    }
}
