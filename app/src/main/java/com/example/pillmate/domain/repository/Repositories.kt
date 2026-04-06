package com.example.pillmate.domain.repository

import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.model.TaskLog
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface MedicationRepository {
    suspend fun getMedication(id: String): Result<Medication?>
    suspend fun updateMedicationSupply(medId: String, changeAmount: Float): Result<Unit>
    suspend fun getAllMedications(profileId: String): Result<List<Medication>>
    suspend fun saveMedication(profileId: String, medication: Medication): Result<String>
}

interface LogRepository {
    suspend fun saveLog(profileId: String, log: TaskLog): Result<Unit>
    fun getLogsForDayFlow(profileId: String, date: Date): Flow<List<TaskLog>>
}
