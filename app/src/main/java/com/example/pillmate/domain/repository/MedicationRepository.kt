package com.example.pillmate.domain.repository

import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.model.MedicationSupply

interface MedicationRepository : Repository<Medication> {
    suspend fun getMedicationWithSupply(profileId: String, id: String): Result<Medication?>
    suspend fun getMedicationSupplies(profileId: String, medId: String): Result<List<MedicationSupply>>
    suspend fun updateMedicationSupply(profileId: String, medId: String, changeAmount: Float, supplyId: String? = null): Result<Unit>
}
