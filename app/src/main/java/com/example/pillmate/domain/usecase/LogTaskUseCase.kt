package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.model.TaskLog
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.domain.repository.LogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.pillmate.domain.model.SupplyLog
import com.example.pillmate.domain.repository.SupplyLogRepository
import java.util.Date
import java.util.UUID

class LogTaskUseCase(
    private val supplyLogRepository: SupplyLogRepository,
    private val logRepository: LogRepository,
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

        // 2. If completed medication, deduct from inventory
        if (status == LogStatus.COMPLETED && taskType == TaskType.MEDICATION) {
            withContext(Dispatchers.IO) {
                supplyLogRepository.add(
                    profileId = profileId,
                    item = SupplyLog(
                        id = UUID.randomUUID().toString(),
                        medId = sourceId,
                        changeAmount = (-dose).toFloat(),
                        reason = "Taken",
                        createdAt = Date(),
                        updatedAt = Date()
                    )
                )
            }

            // 3. IMMEDIATE LOW STOCK ALERT
            checkLowStockUseCase.execute(profileId, sourceId)
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
