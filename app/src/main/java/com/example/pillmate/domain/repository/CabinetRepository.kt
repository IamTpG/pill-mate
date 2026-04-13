package com.example.pillmate.domain.repository

import com.example.pillmate.domain.model.Medication
import kotlinx.coroutines.flow.Flow

interface CabinetRepository {
    fun getCabinetMedications(): Flow<List<Medication>>
    
    // Standard operations
    fun insertMedication(medication: Medication)
    fun updateMedication(medication: Medication)
    fun deleteMedication(medication: Medication)
    
    // Log a dose or refill
    fun logInventoryChange(medicationId: String, amount: Int, reason: String)
    
    // Get logs for history
    fun getLogsForMedication(medicationId: String): Flow<List<com.example.pillmate.data.local.entity.SupplyLogEntity>>
}