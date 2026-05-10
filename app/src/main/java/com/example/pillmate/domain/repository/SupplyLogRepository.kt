package com.example.pillmate.domain.repository

import com.example.pillmate.domain.model.SupplyLog
import kotlinx.coroutines.flow.Flow

interface SupplyLogRepository : Repository<SupplyLog> {
    fun getLogsForMedication(medicationId: String): Flow<List<SupplyLog>>
    suspend fun deleteLogsForMedication(profileId: String, medicationId: String): Result<Unit>
}
