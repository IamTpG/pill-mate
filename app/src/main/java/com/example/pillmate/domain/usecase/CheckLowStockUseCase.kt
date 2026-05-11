package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.notification.TaskNotificationManager
import java.util.Date

class CheckLowStockUseCase(
    private val medicationRepository: MedicationRepository,
    private val calculateDailyIntakeUseCase: CalculateDailyIntakeUseCase,
    private val notificationManager: TaskNotificationManager
) {
    suspend fun execute(profileId: String, medicationId: String? = null): Result<Unit> {
        return runCatching {
            if (profileId.isBlank()) return@runCatching

            val todayIntake = calculateDailyIntakeUseCase.execute(profileId, Date())
            val medications = if (medicationId.isNullOrBlank()) {
                medicationRepository.getAllOnce(profileId).getOrNull().orEmpty()
            } else {
                listOfNotNull(medicationRepository.getById(profileId, medicationId).getOrNull())
            }

            medications.forEach { medication ->
                val dailyRequirement = todayIntake[medication.id] ?: 0f
                if (dailyRequirement > 0f && medication.quantity < dailyRequirement) {
                    notificationManager.showLowStockNotification(
                        medName = medication.name,
                        remaining = medication.quantity
                    )
                }
            }
        }
    }
}
