package com.example.pillmate.presentation.model

import com.example.pillmate.domain.model.LogStatus

data class HomeTask(
    val scheduleId: String,
    val sourceId: String,
    val title: String,
    val time: String,
    val doseDescription: String,
    val dose: Float = 1.0f,
    val taskType: com.example.pillmate.domain.model.TaskType,
    val status: LogStatus? = null // null means upcoming
)
