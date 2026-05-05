package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.HealthMetric
import com.example.pillmate.domain.repository.HealthMetricRepository

class LogHealthMetricUseCase(private val repository: HealthMetricRepository) {
    suspend fun execute(profileId: String, metric: HealthMetric): Result<Unit> {
        return repository.add(profileId, metric)
    }
}
