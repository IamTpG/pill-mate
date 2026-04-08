package com.example.pillmate.presentation.model

import com.example.pillmate.domain.model.LogStatus

data class HomeTask(
    val scheduleId: String,
    val medId: String,
    val title: String,
    val time: String,
    val doseDescription: String,
    val status: LogStatus? = null // null means upcoming
)
