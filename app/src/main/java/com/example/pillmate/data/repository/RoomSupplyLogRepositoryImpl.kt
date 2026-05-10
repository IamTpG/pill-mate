package com.example.pillmate.data.repository

import com.example.pillmate.data.local.dao.SupplyLogDao
import com.example.pillmate.data.local.entity.SupplyLogEntity
import com.example.pillmate.data.mapper.toDomainModel
import com.example.pillmate.data.mapper.toEntity
import com.example.pillmate.domain.model.SupplyLog
import com.example.pillmate.domain.repository.LocalRepository
import com.example.pillmate.domain.repository.SupplyLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RoomSupplyLogRepositoryImpl(
    private val supplyLogDao: SupplyLogDao
) : RoomRepositoryImpl<SupplyLog, SupplyLogEntity>(
    getAllFlow = { _ -> supplyLogDao.observeAllLogs() },
    getAllOnceFunc = { _ -> supplyLogDao.observeAllLogs().first() },
    getByIdFunc = { _, id -> null }, // supply logs usually not fetched by id
    insert = { entity -> supplyLogDao.insertSupplyLog(entity) },
    updateFunc = { entity -> supplyLogDao.insertSupplyLog(entity) },
    deleteById = { _, id -> /* Soft delete logic here if needed, or handled via update */ },
    toDomain = { entity -> entity.toDomainModel() },
    toEntity = { _, domain -> domain.toEntity() },
    getId = { it.id }
), SupplyLogRepository, LocalRepository<SupplyLog> {

    override fun getLogsForMedication(medicationId: String): Flow<List<SupplyLog>> {
        return supplyLogDao.getLogsForMedication(medicationId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun deleteLogsForMedication(profileId: String, medicationId: String): Result<Unit> = runCatching {
        supplyLogDao.deleteLogsForMedication(medicationId)
    }
}
