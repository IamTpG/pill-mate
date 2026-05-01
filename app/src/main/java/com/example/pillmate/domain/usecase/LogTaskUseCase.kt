package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.model.TaskLog
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.domain.repository.LogRepository
import com.example.pillmate.domain.repository.MedicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.pillmate.notification.TaskNotificationManager
import java.util.Date

class LogTaskUseCase(
    private val medicationRepository: MedicationRepository,
    private val logRepository: LogRepository,
    private val notificationManager: TaskNotificationManager,
    private val checkLowStockUseCase: CheckLowStockUseCase
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
        val logResult = logRepository.add(profileId, log)
        if (logResult.isFailure) return logResult

        // 2. If completed medication, deduct from inventory (Room + Firestore via CabinetRepository)
        if (status == LogStatus.COMPLETED && taskType == TaskType.MEDICATION) {
            withContext(Dispatchers.IO) {
                medicationRepository.logInventoryChange(
                    profileId = profileId,
                    medicationId = sourceId,
                    amount = -dose,
                    reason = "Taken"
                )
            }

            // 3. DYNAMIC LOW STOCK ALERT
            try {
                val lowStockResult = checkLowStockUseCase.execute(profileId, sourceId)
                if (lowStockResult.isLow) {
                    notificationManager.showLowStockNotification(sourceId, lowStockResult.remainingStock)
                }
            } catch (e: Exception) {
                // Non-fatal
            }
        }

        return Result.success(Unit)
    }
}
