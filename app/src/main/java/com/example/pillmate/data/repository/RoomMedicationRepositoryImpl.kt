package com.example.pillmate.data.repository

import com.example.pillmate.data.local.dao.MedicationDao
import com.example.pillmate.data.local.dao.SupplyLogDao
import com.example.pillmate.data.local.entity.MedicationEntity
import com.example.pillmate.data.local.entity.SupplyLogEntity
import com.example.pillmate.data.mapper.toDomainModel
import com.example.pillmate.data.mapper.toEntity
import com.example.pillmate.domain.model.InventoryLog
import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.model.MedicationSupply
import com.example.pillmate.domain.repository.LocalRepository
import com.example.pillmate.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID

class RoomMedicationRepositoryImpl(
    private val dao: MedicationDao,
    private val supplyLogDao: SupplyLogDao
) : RoomRepositoryImpl<Medication, MedicationEntity>(
    getAllFlow = { profileId -> dao.getMedicationsForProfile(profileId) },
    getAllOnceFunc = { profileId -> dao.getAllMedicationsOnce(profileId) },
    getByIdFunc = { profileId, id -> dao.getMedicationByIdAndProfile(profileId, id) },
    insert = { entity -> dao.insertMedication(entity) },
    updateFunc = { entity -> dao.updateMedication(entity) },
    deleteById = { profileId, id -> dao.deleteById(profileId, id) },
    toDomain = { entity -> entity.toDomainModel() },
    toEntity = { profileId, domain -> domain.toEntity(profileId) },
    getId = { it.id }
), MedicationRepository, LocalRepository<Medication> {

    // Combine medications + supply logs so UI reacts when either table changes
    override fun getAll(profileId: String): Flow<List<Medication>> =
        combine(
            dao.getMedicationsForProfile(profileId),
            supplyLogDao.observeAllLogs()
        ) { entities, allLogs ->
            entities.map { entity ->
                val inventory = allLogs
                    .filter { it.medicationId == entity.id }
                    .sumOf { it.changeAmount }
                entity.toDomainModel(inventory)
            }
        }

    override suspend fun getAllOnce(profileId: String): Result<List<Medication>> = runCatching {
        dao.getAllMedicationsOnce(profileId).map { entity ->
            val inventory = supplyLogDao.getCurrentInventoryCount(entity.id)
                .firstOrNull() ?: 0
            entity.toDomainModel(inventory)
        }
    }

    override suspend fun getById(profileId: String, id: String): Result<Medication?> = runCatching {
        dao.getMedicationByIdAndProfile(profileId, id)?.let { entity ->
            val inventory = supplyLogDao.getCurrentInventoryCount(entity.id)
                .firstOrNull() ?: 0
            entity.toDomainModel(inventory)
        }
    }

    override suspend fun remove(profileId: String, id: String): Result<Unit> = runCatching {
        supplyLogDao.deleteLogsForMedication(id)
        dao.deleteById(profileId, id)
    }

    override suspend fun getMedicationWithSupply(profileId: String, id: String): Result<Medication?> = getById(profileId, id)

    override suspend fun getMedicationSupplies(profileId: String, medId: String): Result<List<MedicationSupply>> = runCatching {
        val medication = getById(profileId, medId).getOrThrow()
        medication?.supply?.let { listOf(it) } ?: emptyList()
    }

    override suspend fun updateMedicationSupply(profileId: String, medId: String, changeAmount: Float, supplyId: String?): Result<Unit> = logInventoryChange(
        profileId = profileId,
        medicationId = medId,
        amount = changeAmount.toInt(),
        reason = if (changeAmount < 0) "TAKEN" else "REFILL"
    )

    override suspend fun logInventoryChange(profileId: String, medicationId: String, amount: Int, reason: String): Result<Unit> = runCatching {
        val newLog = SupplyLogEntity(
            id = UUID.randomUUID().toString(),
            medicationId = medicationId,
            changeAmount = amount,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
        supplyLogDao.insertSupplyLog(newLog)
    }

    override fun getLogsForMedication(medicationId: String): Flow<List<InventoryLog>> {
        return supplyLogDao.getLogsForMedication(medicationId).map { entities ->
            entities.map { entity ->
                InventoryLog(
                    id = entity.id,
                    medId = entity.medicationId,
                    changeAmount = entity.changeAmount.toFloat(),
                    reason = entity.reason,
                    timestamp = Date(entity.timestamp)
                )
            }
        }
    }
}
