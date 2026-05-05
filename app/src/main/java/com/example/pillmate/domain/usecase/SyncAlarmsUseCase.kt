package com.example.pillmate.domain.usecase

import android.util.Log
import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.notification.TaskNotificationManager
import com.example.pillmate.util.AlarmTracker
import com.example.pillmate.util.RecurrenceEvaluator
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
        val result = scheduleRepository.getAllOnce(profileId)
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
            schedule.doseTimes.forEach { doseTime ->
                schedule.reminders.forEach { reminder ->
                    val requestCode = "${schedule.id}_${doseTime.time}_${reminder.minutesBefore}_${reminder.type.name}".hashCode()

                    // ROBUST TIME PARSING (Matches TaskAlarmScreen)
                    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val displayFormat = SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    val fallbackFormat = SimpleDateFormat("H:m", java.util.Locale.getDefault())

                    if (completedScheduleIds.contains(schedule.id)) {
                        Log.d("SyncAlarmsUseCase", "Cancelling completed schedule: ${schedule.id}")
                        notificationManager.cancelNotification(requestCode)
                        notificationManager.dismissNotification(schedule.id)
                    } else {
                        val nextOccurrence = RecurrenceEvaluator.getNextOccurrence(
                            fromDate = Date(),
                            rrule = schedule.recurrenceRule,
                            startTimeIso = schedule.startTime,
                            endDate = schedule.endDate,
                            doseTime = doseTime.time
                        )

                        if (nextOccurrence != null) {
                            val target = Calendar.getInstance().apply {
                                time = nextOccurrence
                                add(Calendar.MINUTE, -reminder.minutesBefore)
                            }

                            // Buffer of 2 seconds to account for parsing precision loss
                            while (target.timeInMillis < System.currentTimeMillis() - 2000) {
                                target.add(Calendar.DAY_OF_YEAR, 1) // Should rarely happen with getNextOccurrence
                            }

                            val delaySeconds = ((target.timeInMillis - System.currentTimeMillis()) / 1000).coerceAtLeast(0L).toInt()

                            val scheduled = notificationManager.scheduleTaskNotification(
                                sourceId = schedule.eventSnapshot.sourceId,
                                scheduleId = schedule.id,
                                title = schedule.eventSnapshot.title,
                                details = doseTime.doseContext.ifBlank { schedule.eventSnapshot.instructions ?: "" },
                                delaySeconds = delaySeconds,
                                requestCode = requestCode,
                                profileId = profileId,
                                taskType = schedule.type.name,
                                reminderType = reminder.type.name,
                                rrule = schedule.recurrenceRule,
                                startTime = doseTime.time,
                                instructions = schedule.eventSnapshot.instructions
                            )

                            if (scheduled) {
                                desiredScheduledIds.add(requestCode)
                            }
                        }
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
