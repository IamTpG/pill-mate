package com.example.pillmate.domain.repository

import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.model.MedicationSupply
import com.example.pillmate.domain.model.InventoryLog
import kotlinx.coroutines.flow.Flow

interface MedicationRepository : Repository<Medication> {
    suspend fun getMedicationWithSupply(profileId: String, id: String): Result<Medication?>
    suspend fun getMedicationSupplies(profileId: String, medId: String): Result<List<MedicationSupply>>
    suspend fun updateMedicationSupply(profileId: String, medId: String, changeAmount: Float, supplyId: String? = null): Result<Unit>
    suspend fun logInventoryChange(profileId: String, medicationId: String, amount: Int, reason: String): Result<Unit>
    fun getLogsForMedication(medicationId: String): Flow<List<InventoryLog>>
}
