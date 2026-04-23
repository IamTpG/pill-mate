package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.model.TaskLog
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.domain.repository.CabinetRepository
import com.example.pillmate.domain.repository.LogRepository
import com.example.pillmate.domain.repository.MedicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class LogTaskUseCase(
    private val medicationRepository: MedicationRepository,
    private val logRepository: LogRepository,
    private val cabinetRepository: CabinetRepository
) {
    suspend fun execute(
        profileId: String,
        sourceId: String,
        scheduleId: String,
        taskType: TaskType,
        status: LogStatus,
        scheduledTime: Date,
        dose: Float = 1.0f,
        notes: String? = null
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

        // 2. If completed medication, deduct from inventory (Room + Firestore via CabinetRepository)
        if (status == LogStatus.COMPLETED && taskType == TaskType.MEDICATION) {
            withContext(Dispatchers.IO) {
                cabinetRepository.logInventoryChange(
                    profileId = profileId,
                    medicationId = sourceId,
                    amount = -dose.toInt(),
                    reason = "Taken"
                )
            }
        }

        return Result.success(Unit)
    }
}
