package com.example.pillmate.domain.usecase

import com.example.pillmate.data.local.entity.SupplyLogEntity
import com.example.pillmate.domain.repository.CabinetRepository
import kotlinx.coroutines.flow.Flow

class GetMedicationLogsUseCase(private val repository: CabinetRepository) {
    operator fun invoke(medicationId: String): Flow<List<SupplyLogEntity>> {
        TODO("Implementation to be migrated from repo")
    }
}
