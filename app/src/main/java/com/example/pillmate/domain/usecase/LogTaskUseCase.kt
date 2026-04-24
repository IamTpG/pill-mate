package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.model.TaskLog
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.domain.repository.LogRepository
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.notification.TaskNotificationManager
import java.util.Date

class LogTaskUseCase(
    private val medicationRepository: MedicationRepository,
    private val logRepository: LogRepository,
    private val notificationManager: TaskNotificationManager
) {
    suspend fun execute(
        profileId: String,
        sourceId: String,
        scheduleId: String,
        taskType: TaskType,
        status: LogStatus,
        scheduledTime: Date,
        dose: Float = 1.0f,
        notes: String? = null,
        supplyId: String? = null
    ): Result<Unit> {
        val log = TaskLog(
            scheduleId = scheduleId,
            type = taskType,
            status = status,
            scheduledTime = scheduledTime,
            actualTime = if (status == LogStatus.COMPLETED) Date() else null,
            notes = notes
        )

        // 1. Save the log
        val logResult = logRepository.saveLog(profileId, log)
        if (logResult.isFailure) return logResult

        // 2. If completed medication, deduct from inventory
        if (status == LogStatus.COMPLETED && taskType == TaskType.MEDICATION) {
            val updateResult = medicationRepository.updateMedicationSupply(sourceId, -dose, supplyId)
            if (updateResult.isFailure) {
                return updateResult
            }

            // 3. IMMEDIATE LOW STOCK ALERT
            try {
                // Fetch supplies directly to check stock levels
                val supplies = medicationRepository.getMedicationSupplies(sourceId).getOrNull() ?: emptyList()
                val currentStock = if (supplyId != null) {
                    supplies.find { it.id == supplyId }?.quantity ?: 0f
                } else {
                    // If no specific supply, check the lowest remaining batch
                    supplies.filter { it.quantity > 0 }.minOfOrNull { it.quantity } ?: 0f
                }

                if (currentStock < 5.0f) {
                    notificationManager.showLowStockNotification(sourceId, currentStock)
                }
            } catch (e: Exception) {
                // Non-fatal
            }
        }

        return Result.success(Unit)
    }
}
