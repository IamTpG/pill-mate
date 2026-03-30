package com.example.pillmate.domain.model

import java.util.Date

data class Schedule(
    val id: String = "",
    val type: TaskType = TaskType.MEDICATION,
    val startTime: String = "08:00", // "HH:mm"
    val recurrenceRule: String? = null,
    val enabled: Boolean = true,
    val eventSnapshot: ScheduleEvent = ScheduleEvent(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

data class ScheduleEvent(
    val sourceId: String = "", // Ref to medId, apptId, etc.
    val title: String = "",
    val instructions: String = "",
    val dose: Float = 1.0f
)
