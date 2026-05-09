package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.HealthMetric
import com.example.pillmate.domain.repository.HealthMetricRepository
import kotlinx.coroutines.flow.Flow

class GetHealthMetricsUseCase(private val repository: HealthMetricRepository) {
    fun execute(profileId: String, limit: Int = 20): Flow<List<HealthMetric>> {
        return repository.getRecentMetrics(profileId, limit)
    }
}
