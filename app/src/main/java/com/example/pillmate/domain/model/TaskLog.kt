package com.example.pillmate.domain.model

import java.util.Date

//enum class LogStatus {
//    COMPLETED, MISSED, SKIPPED, LATE, SNOOZED
//}

enum class TaskType {
    MEDICATION, MEAL, APPOINTMENT, TASK, EXERCISE, OTHER
}

data class TaskLog(
    val id: String = "",
    val scheduleId: String = "",
    val type: TaskType = TaskType.MEDICATION,
    val status: LogStatus = LogStatus.MISSED,
    val scheduledTime: Date = Date(),
    val actualTime: Date? = null,
    val eventSnapshot: Map<String, Any> = emptyMap(),
    val notes: String? = null,
    val createdAt: Date = Date()
)
