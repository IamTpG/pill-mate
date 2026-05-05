package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.repository.MedicationRepository

class UpdateMedicationSupplyUseCase(private val repository: MedicationRepository) {
    suspend operator fun invoke(medId: String, changeAmount: Float, supplyId: String? = null): Result<Unit> {
        TODO("Implementation to be migrated from repo")
    }
}
