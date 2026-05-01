package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.repository.MedicationRepository
import java.util.Date

class CheckLowStockUseCase(
    private val medicationRepository: MedicationRepository,
    private val getMedicationUsageUseCase: GetMedicationUsageUseCase
) {
    /**
     * Checks if stock is low for a specific medication.
     * Considered low if it is empty at the end of the day of the scan.
     */
    suspend fun execute(profileId: String, medicationId: String): LowStockResult {
        // 1. Get current total supply for non-expired batches
        val suppliesResult = medicationRepository.getMedicationSupplies(profileId, medicationId)
        val supplies = suppliesResult.getOrNull() ?: emptyList()
        
        val now = Date()
        val totalValidSupply = supplies
            .filter { it.expirationDate == null || it.expirationDate.after(now) }
            .sumOf { it.quantity.toDouble() }.toFloat()

        // 2. Get usage requirement for today
        val usageToday = getMedicationUsageUseCase.execute(profileId, medicationId, now)

        // 3. Compare. If remaining supply minus today's usage < 0, it's low.
        val isLow = (totalValidSupply - usageToday) < 0.001f // Using epsilon for float comparison

        return LowStockResult(
            isLow = isLow,
            remainingStock = totalValidSupply,
            predictedUsage = usageToday
        )
    }
}

data class LowStockResult(
    val isLow: Boolean,
    val remainingStock: Float,
    val predictedUsage: Float
)
