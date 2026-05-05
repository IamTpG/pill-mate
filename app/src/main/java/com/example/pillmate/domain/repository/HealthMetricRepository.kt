package com.example.pillmate.domain.repository

import com.example.pillmate.domain.model.HealthMetric
import kotlinx.coroutines.flow.Flow

interface HealthMetricRepository : RemoteRepository<HealthMetric> {
    fun getRecentMetrics(profileId: String, limit: Int): Flow<List<HealthMetric>>
}
