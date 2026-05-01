package com.example.pillmate.domain.model

import java.util.Date

data class DoseTime(
    val time: String = "08:00", // "HH:mm"
    val doseContext: String = "",
    val dose: Float = 1.0f
)

data class Schedule(
    val id: String = "",
    val name: String = "",
    val type: TaskType = TaskType.MEDICATION,
    val doseTimes: List<DoseTime> = emptyList(),
    val recurrenceRule: String? = null,
    val frequency: String? = null,
    val enabled: Boolean = true,
    val reminders: List<Reminder> = emptyList(),
    val eventSnapshot: ScheduleEvent = ScheduleEvent(),
    val endDate: Date? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val deletedAt: Date? = null
)

enum class ReminderType {
    NOTIFICATION, ALARM
}

data class Reminder(
    val minutesBefore: Int = 0,
    val type: ReminderType = ReminderType.NOTIFICATION
)

data class ScheduleEvent(
    val sourceId: String = "", // Ref to medId, apptId, etc.
    val title: String = "",
    val instructions: String = "",
    val dose: Float = 1.0f,
    val unit: String? = null
)
