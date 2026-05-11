package com.example.pillmate.presentation.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.usecase.CreateScheduleUseCase
import com.example.pillmate.domain.usecase.ManageReminderUseCase
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.model.ScheduleEvent
import com.example.pillmate.domain.model.Reminder
import com.example.pillmate.domain.model.ReminderType
import com.example.pillmate.domain.model.HealthMetric
import com.example.pillmate.domain.model.MetricType
import com.example.pillmate.domain.usecase.LogHealthMetricUseCase
import com.example.pillmate.util.DataGenerator
import com.example.pillmate.notification.TaskNotificationManager
import com.example.pillmate.util.AlarmTracker
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DebugViewModel(
    private val generator: DataGenerator,
    private val createScheduleUseCase: CreateScheduleUseCase,
    private val manageReminderUseCase: ManageReminderUseCase,
    private val profileId: String,
    private val db: FirebaseFirestore,
    private val syncAlarmsUseCase: com.example.pillmate.domain.usecase.SyncAlarmsUseCase,
    private val notificationManager: TaskNotificationManager,
    private val alarmTracker: AlarmTracker,
    private val syncFcmTokenUseCase: com.example.pillmate.domain.usecase.SyncFcmTokenUseCase,
    private val logHealthMetricUseCase: LogHealthMetricUseCase
) : ViewModel() {

    fun copyFcmTokenToClipboard(context: Context) {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("FCM Token", token)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Token copied!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to get token", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun syncFcmToken(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                syncFcmTokenUseCase(profileId)
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun forceSync(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                syncAlarmsUseCase(profileId)
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun getScheduledIds(): Set<Int> = alarmTracker.getScheduledIds()

    fun clearAlarmTracker() {
        alarmTracker.clear()
    }

    fun triggerLowStockWorker(context: Context) {
        val request = androidx.work.OneTimeWorkRequestBuilder<com.example.pillmate.workers.LowStockWorker>().build()
        androidx.work.WorkManager.getInstance(context).enqueue(request)
        Toast.makeText(context, "LowStockWorker triggered manually!", Toast.LENGTH_SHORT).show()
    }

    fun triggerHealthNotification(type: String) {
        notificationManager.showHealthNotification(type)
    }

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

    fun generateSampleVitals(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val base = Calendar.getInstance().apply {
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val bpSamples = listOf(
                    118 to 76,
                    122 to 78,
                    129 to 82,
                    136 to 86,
                    124 to 79,
                    116 to 74,
                    130 to 84
                )
                val weightSamples = listOf(72.4, 72.2, 72.1, 71.9)
                val waterSamples = listOf(1800, 2200, 2500, 1650, 2800, 2100, 2600)

                for (dayOffset in 6 downTo 0) {
                    val dayIndex = 6 - dayOffset

                    val bpTime = base.withDayOffset(dayOffset, hour = 8, minute = 15)
                    val (sys, dia) = bpSamples[dayIndex]
                    logHealthMetricUseCase.execute(
                        profileId,
                        HealthMetric(
                            type = MetricType.BLOOD_PRESSURE,
                            valuePrimary = sys.toDouble(),
                            valueSecondary = dia.toDouble(),
                            unit = "mmHg",
                            recordedAt = bpTime
                        )
                    )

                    if (dayIndex % 2 == 0) {
                        val weightTime = base.withDayOffset(dayOffset, hour = 7, minute = 45)
                        logHealthMetricUseCase.execute(
                            profileId,
                            HealthMetric(
                                type = MetricType.WEIGHT,
                                valuePrimary = weightSamples[dayIndex / 2],
                                unit = "kg",
                                recordedAt = weightTime
                            )
                        )
                    }

                    val waterTotal = waterSamples[dayIndex]
                    listOf(0.35, 0.30, 0.20, 0.15).forEachIndexed { index, share ->
                        val waterTime = base.withDayOffset(dayOffset, hour = 9 + (index * 3), minute = 0)
                        logHealthMetricUseCase.execute(
                            profileId,
                            HealthMetric(
                                type = MetricType.WATER,
                                valuePrimary = waterTotal * share,
                                unit = "ml",
                                recordedAt = waterTime
                            )
                        )
                    }
                }

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
                val doseTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val futureTime = Date(System.currentTimeMillis() + 60000) // 20s in future
                val startTime = dateFormat.format(futureTime)
                val doseTimeStr = doseTimeFormat.format(futureTime)

                val hour = SimpleDateFormat("H", Locale.getDefault()).format(futureTime)
                val min = SimpleDateFormat("m", Locale.getDefault()).format(futureTime)
                val rrule = "FREQ=DAILY;BYHOUR=$hour;BYMINUTE=$min"

                // Try to find a real medication to link to
                val medsSnapshot = db.collection("profiles").document(profileId)
                    .collection("medications").limit(1).get().await()
                val realMedId = if (!medsSnapshot.isEmpty) medsSnapshot.documents[0].id else "debug_med_id"

                val newSchedule = Schedule(
                    id = "debug_test_1m_alarm",
                    doseTimes = listOf(com.example.pillmate.domain.model.DoseTime(time = doseTimeStr, dose = 1.0f, doseContext = "")),
                    startTime = startTime,
                    frequency = "Daily",
                    recurrenceRule = rrule,
                    reminders = listOf(
                        Reminder(minutesBefore = 0, type = ReminderType.ALARM)
                    ),
                    eventSnapshot = ScheduleEvent(
                        sourceId = realMedId,
                        title = "Test Medicine",
                        instructions = "Take 1.0 pills now",
                        dose = 1.0f,
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
                    startTime = schedule.doseTimes.firstOrNull()?.time ?: "",
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

    private fun Calendar.withDayOffset(daysAgo: Int, hour: Int, minute: Int): Date {
        return (clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }.time
    }
}
