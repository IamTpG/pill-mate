package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.usecase.CreateScheduleUseCase
import com.example.pillmate.domain.usecase.ManageReminderUseCase
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.model.ScheduleEvent
import com.example.pillmate.domain.model.Reminder
import com.example.pillmate.domain.model.ReminderType
import com.example.pillmate.util.DataGenerator
import com.example.pillmate.notification.TaskNotificationManager
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DebugViewModel(
    private val generator: DataGenerator,
    private val createScheduleUseCase: CreateScheduleUseCase,
    private val manageReminderUseCase: ManageReminderUseCase,
    private val profileId: String,
    private val db: FirebaseFirestore,
    private val notificationManager: TaskNotificationManager,
    private val alarmTracker: com.example.pillmate.util.AlarmTracker
) : ViewModel() {

    fun getScheduledIds(): Set<Int> = alarmTracker.getScheduledIds()

    fun generateSampleData(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                generator.generateSampleData(profileId)
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun clearUserData(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                generator.clearUserData(profileId)
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun triggerRandomAlarm(onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val snapshot = db
                    .collection("profiles").document(profileId)
                    .collection("schedules").get().await()

                if (!snapshot.isEmpty) {
                    val randomDoc = snapshot.documents.random()
                    val scheduleId = randomDoc.id
                    val medId = randomDoc.get("eventSnapshot.sourceId") as? String ?: "debug_pill_id"
                    val medName = randomDoc.getString("eventSnapshot.title") ?: "Medication"
                    val dose = randomDoc.get("eventSnapshot.dose")?.toString() ?: "1.0"
                    val unit = randomDoc.getString("eventSnapshot.unit") ?: "dose"
                    val doseText = "$dose $unit"

                    val wasExact = notificationManager.scheduleTaskNotification(
                        sourceId = medId,
                        scheduleId = scheduleId,
                        title = medName,
                        details = doseText,
                        delaySeconds = 5,
                        requestCode = medId.hashCode(),
                        profileId = profileId, // Added profileId
                        taskType = "MEDICATION"
                    )
                    
                    if (wasExact) {
                        onSuccess("Exact Alarm scheduled (5s)!")
                    } else {
                        onSuccess("Alarm scheduled but might be delayed (Missing Permission)")
                    }
                } else {
                    onError(Exception("No schedules found! Generate data first."))
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun createTestScheduleIn1Min(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val futureTime = Date(System.currentTimeMillis() + 10000) // 10s in future
                val startTime = dateFormat.format(futureTime)

                // Try to find a real medication to link to
                val medsSnapshot = db.collection("profiles").document(profileId)
                    .collection("medications").limit(1).get().await()
                val realMedId = if (!medsSnapshot.isEmpty) medsSnapshot.documents[0].id else "debug_med_id"

                val newSchedule = Schedule(
                    id = "debug_test_1m_alarm",
                    startTime = startTime,
                    recurrenceRule = "FREQ=DAILY;COUNT=10",
                    reminders = listOf(
                        Reminder(minutesBefore = 0, type = ReminderType.ALARM)
                    ),
                    eventSnapshot = ScheduleEvent(
                        sourceId = realMedId,
                        title = "Test Medicine",
                        instructions = "Take 2.0 pills now",
                        dose = 2.0f,
                        unit = "pills"
                    )
                )

                // ManageReminderUseCase handles both saving to repo and setting exactly the AlarmManager alarms
                val result = manageReminderUseCase(profileId, newSchedule)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    onError((result.exceptionOrNull() ?: Exception("Unknown error")) as Exception)
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun getSchedulesList(onSuccess: (List<Schedule>) -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("profiles").document(profileId)
                    .collection("schedules").get().await()
                val schedules = snapshot.documents.mapNotNull { it.toObject(Schedule::class.java)?.copy(id = it.id) }
                onSuccess(schedules)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun triggerSpecificReminder(schedule: Schedule, reminder: Reminder, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val medId = schedule.eventSnapshot.sourceId
                val scheduleId = schedule.id
                val medName = schedule.eventSnapshot.title
                val dose = schedule.eventSnapshot.dose.toString()
                val unit = schedule.eventSnapshot.unit ?: "dose"
                val taskType = schedule.type.name
                
                val wasExact = notificationManager.scheduleTaskNotification(
                    sourceId = medId,
                    scheduleId = scheduleId,
                    title = "$medName (Manual)",
                    details = "Triggered ${reminder.type} at offset ${reminder.minutesBefore}m. Dose: $dose $unit",
                    delaySeconds = 5,
                    requestCode = (schedule.id + reminder.type.name).hashCode(),
                    profileId = profileId, // Added profileId
                    taskType = taskType,
                    reminderType = reminder.type.name,
                    rrule = schedule.recurrenceRule,
                    startTime = schedule.startTime,
                    instructions = schedule.eventSnapshot.instructions
                )
                
                if (wasExact) {
                    onSuccess("Scheduled in 5s: $medName (${reminder.type})")
                } else {
                    onSuccess("Scheduled but might be delayed: $medName")
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}
