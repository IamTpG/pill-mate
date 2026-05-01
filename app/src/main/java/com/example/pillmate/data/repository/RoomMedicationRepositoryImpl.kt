package com.example.pillmate.data.repository

import com.example.pillmate.data.local.dao.MedicationDao
import com.example.pillmate.data.local.dao.SupplyLogDao
import com.example.pillmate.data.local.entity.MedicationEntity
import com.example.pillmate.data.mapper.toDomainModel
import com.example.pillmate.data.mapper.toEntity
import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.repository.LocalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull

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
), LocalRepository<Medication> {

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
}
