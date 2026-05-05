package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.MedicationSupply
import com.example.pillmate.domain.repository.MedicationRepository

class GetMedicationSuppliesUseCase(private val repository: MedicationRepository) {
    suspend operator fun invoke(medId: String): Result<List<MedicationSupply>> {
        TODO("Implementation to be migrated from repo")
    }
}
