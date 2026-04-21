package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.MedicationSupply
import com.example.pillmate.domain.repository.MedicationRepository

class GetSupplyStockUseCase(
    private val medicationRepository: MedicationRepository
) {
    suspend fun execute(medId: String): Result<List<MedicationSupply>> {
        return medicationRepository.getMedicationSupplies(medId)
    }
}
