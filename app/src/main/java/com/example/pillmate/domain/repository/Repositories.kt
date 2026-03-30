package com.example.pillmate.domain.repository

import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.model.TaskLog
import java.util.Date

interface MedicationRepository {
    suspend fun getMedication(id: String): Result<Medication?>
    suspend fun updateMedicationSupply(medId: String, changeAmount: Float): Result<Unit>
    suspend fun getAllMedications(profileId: String): Result<List<Medication>>
}

interface LogRepository {
    suspend fun saveLog(profileId: String, log: TaskLog): Result<Unit>
    suspend fun getLogsForDay(profileId: String, date: Date): Result<List<TaskLog>>
}
