package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.HealthMetric
import com.example.pillmate.domain.repository.HealthMetricRepository

class LogHealthMetricUseCase(private val repository: HealthMetricRepository) {
    suspend fun execute(profileId: String, metric: HealthMetric): Result<Unit> {
        val result = repository.add(profileId, metric)
        if (result.isSuccess) {
            triggerWidgetUpdate()
        }
        return result
    }

    private fun triggerWidgetUpdate() {
        try {
            val context = org.koin.core.context.GlobalContext.get().get<android.content.Context>()
            val intent = android.content.Intent("com.example.pillmate.ACTION_UPDATE_WIDGET")
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        } catch (e: Exception) {}
    }
}
