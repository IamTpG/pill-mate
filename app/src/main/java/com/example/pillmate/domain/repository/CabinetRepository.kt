package com.example.pillmate.domain.repository

import com.example.pillmate.domain.model.Medication
import kotlinx.coroutines.flow.Flow

interface CabinetRepository {
    fun getCabinetMedications(profileId: String): Flow<List<Medication>>

    fun insertMedication(profileId: String, medication: Medication)
    fun updateMedication(profileId: String, medication: Medication)
    fun deleteMedication(profileId: String, medication: Medication)

    // Log a dose or refill
    fun logInventoryChange(medicationId: String, amount: Int, reason: String)
    
    // Get logs for history
    fun getLogsForMedication(medicationId: String): Flow<List<com.example.pillmate.data.local.entity.SupplyLogEntity>>
}