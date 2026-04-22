package com.example.pillmate.domain.usecase

import android.util.Log
import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.notification.TaskNotificationManager
import com.example.pillmate.util.AlarmTracker
import java.util.Calendar
import kotlinx.coroutines.flow.first
import com.example.pillmate.domain.model.TaskLog
import com.example.pillmate.domain.repository.LogRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncAlarmsUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val logRepository: LogRepository,
    private val notificationManager: TaskNotificationManager,
    private val alarmTracker: AlarmTracker
) {
    private fun isSnoozeId(requestCode: Int, schedules: List<Schedule>): Boolean {
        // A snooze ID is generated from "SNOOZE_" + schedule.id
        return schedules.any { ("SNOOZE_" + it.id).hashCode() == requestCode }
    }

    suspend operator fun invoke(profileId: String) {
        Log.d("SyncAlarmsUseCase", "Starting sync for $profileId")
        val result = scheduleRepository.getSchedules(profileId)
        val schedules = result.getOrNull() ?: return
        
        // Fetch logs for today to avoid scheduling already completed tasks
        val todayLogsSnapshot: List<TaskLog> = try {
            logRepository.getLogsForDayFlow(profileId, Date()).first()
        } catch (e: Exception) {
            Log.e("SyncAlarmsUseCase", "Failed to fetch logs for sync", e)
            emptyList()
        }
        val completedScheduleIds = todayLogsSnapshot.filter { it.status == LogStatus.COMPLETED || it.status == LogStatus.SKIPPED }.map { it.scheduleId }.toSet()

        val previouslyScheduledIds = alarmTracker.getScheduledIds()
        val desiredScheduledIds = mutableSetOf<Int>()
        
        schedules.forEach { schedule ->
            schedule.reminders.forEach { reminder ->
                val requestCode = "${schedule.id}_${reminder.minutesBefore}_${reminder.type.name}".hashCode()
                
                // ROBUST TIME PARSING (Matches TaskAlarmScreen)
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val displayFormat = SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val fallbackFormat = SimpleDateFormat("H:m", java.util.Locale.getDefault())

                val parsedStart: java.util.Date? = try {
                    if (completedScheduleIds.contains(schedule.id)) {
                        android.util.Log.d("SyncAlarmsUseCase", "Skipping completed schedule: ${schedule.id}")
                        null
                    } else {
                        when {
                            schedule.startTime.contains("T") -> isoFormat.parse(schedule.startTime)
                            schedule.startTime.isNotBlank() -> {
                                try { displayFormat.parse(schedule.startTime) } 
                                catch (e: Exception) { fallbackFormat.parse(schedule.startTime) }
                            }
                            else -> null
                        }
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
                    
                    // Buffer of 2 seconds to account for parsing precision loss (milliseconds)
                    while (target.timeInMillis < System.currentTimeMillis() - 2000) {
                        target.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    val delaySeconds = ((target.timeInMillis - System.currentTimeMillis()) / 1000).coerceAtLeast(0L).toInt()
                    
                    val scheduled = notificationManager.scheduleTaskNotification(
                        sourceId = schedule.eventSnapshot.sourceId,
                        scheduleId = schedule.id,
                        title = schedule.eventSnapshot.title,
                        details = schedule.eventSnapshot.instructions ?: "",
                        delaySeconds = delaySeconds,
                        requestCode = requestCode,
                        profileId = profileId,
                        taskType = schedule.type.name,
                        reminderType = reminder.type.name,
                        rrule = schedule.recurrenceRule,
                        startTime = schedule.startTime
                    )
                    
                    // Track ANY successfully scheduled alarm (Exact or not)
                    if (scheduled) {
                        desiredScheduledIds.add(requestCode)
                    }
                }
            }
        }
        
        // ORPHAN CANCELLATION: IDs that are on the device but no longer in the DB
        val orphans = previouslyScheduledIds - desiredScheduledIds
        
        // Protect snoozes: If a snooze is active but not in Firestore (obviously), don't kill it
        val trueOrphans = orphans.filter { !isSnoozeId(it, schedules) }
        
        trueOrphans.forEach { orphanId ->
            notificationManager.cancelNotification(orphanId)
        }
        
        // Update local state: Keep the desired ones AND any preserved snoozes
        val preservedSnoozes = orphans.filter { isSnoozeId(it, schedules) }
        alarmTracker.updateScheduledIds(desiredScheduledIds + preservedSnoozes)
    }
}
