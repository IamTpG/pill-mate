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
        val logResult = logRepository.add(profileId, log)
        if (logResult.isFailure) return logResult

        // 2. If completed medication, deduct from inventory (Room + Firestore via CabinetRepository)
        if (status == LogStatus.COMPLETED && taskType == TaskType.MEDICATION) {
            withContext(Dispatchers.IO) {
                medicationRepository.logInventoryChange(
                    profileId = profileId,
                    medicationId = sourceId,
                    amount = -dose.toInt(),
                    reason = "Taken"
                )
            }

            // 3. IMMEDIATE LOW STOCK ALERT
            try {
                val supplies = medicationRepository.getMedicationSupplies(profileId, sourceId).getOrNull() ?: emptyList()
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

        // 4. Update widget
        try {
            val widgetIntent = android.content.Intent("com.example.pillmate.ACTION_UPDATE_WIDGET")
            // Note: This needs Context. Since LogTaskUseCase is in domain, I should probably 
            // trigger it from a listener or pass context.
            // But this is a simple project, I'll pass Context or use GlobalContext's androidContext.
            val context = org.koin.core.context.GlobalContext.get().get<android.content.Context>()
            widgetIntent.setPackage(context.packageName)
            context.sendBroadcast(widgetIntent)
        } catch (e: Exception) {}

        return Result.success(Unit)
    }
}
