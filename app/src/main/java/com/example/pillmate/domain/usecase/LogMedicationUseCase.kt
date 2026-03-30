package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.model.TaskLog
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.domain.repository.LogRepository
import com.example.pillmate.domain.repository.MedicationRepository
import java.util.Date

class LogMedicationUseCase(
    private val medicationRepository: MedicationRepository,
    private val logRepository: LogRepository
) {
    suspend fun execute(
        profileId: String,
        medId: String,
        status: LogStatus,
        scheduledTime: Date,
        dose: Float = 1.0f,
        notes: String? = null
    ): Result<Unit> {
        val log = TaskLog(
            scheduleId = medId, // Simplified for now
            type = TaskType.MEDICATION,
            status = status,
            scheduledTime = scheduledTime,
            actualTime = if (status == LogStatus.COMPLETED) Date() else null,
            notes = notes
        )

        // 1. Save the log
        val logResult = logRepository.saveLog(profileId, log)
        if (logResult.isFailure) return logResult

        // 2. If completed (Taken), deduct from inventory
        if (status == LogStatus.COMPLETED) {
            val updateResult = medicationRepository.updateMedicationSupply(medId, -dose)
            if (updateResult.isFailure) {
                // Should we rollback the log? Or just log the error?
                // For now, return the error.
                return updateResult
            }
        }

        return Result.success(Unit)
    }
}
