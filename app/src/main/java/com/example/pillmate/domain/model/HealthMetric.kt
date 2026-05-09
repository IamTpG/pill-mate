package com.example.pillmate.domain.model

import java.util.Date

enum class MetricType {
    WEIGHT, BLOOD_PRESSURE, WATER, HEART_RATE
}

data class HealthMetric(
    val id: String = "",
    val type: MetricType = MetricType.WEIGHT,
    val valuePrimary: Double = 0.0,
    val valueSecondary: Double? = null,
    val unit: String = "",
    val recordedAt: Date = Date()
)
