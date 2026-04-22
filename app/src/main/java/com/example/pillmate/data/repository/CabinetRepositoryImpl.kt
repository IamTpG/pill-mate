package com.example.pillmate.data.repository

import com.example.pillmate.data.local.dao.MedicationDao
import com.example.pillmate.data.local.dao.SupplyLogDao
import com.example.pillmate.data.local.entity.SupplyLogEntity
import com.example.pillmate.data.mapper.toDomainModel
import com.example.pillmate.data.mapper.toEntity
import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.repository.CabinetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.UUID

class CabinetRepositoryImpl(
    private val medicationDao: MedicationDao,
    private val supplyLogDao: SupplyLogDao
) : CabinetRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCabinetMedications(profileId: String): Flow<List<Medication>> {
        return medicationDao.getMedicationsForProfile(profileId).flatMapLatest { entities ->
            if (entities.isEmpty()) return@flatMapLatest flowOf(emptyList())

            val domainFlows = entities.map { entity ->
                supplyLogDao.getCurrentInventoryCount(entity.id).map { currentCount ->
                    entity.toDomainModel(inventory = currentCount ?: 0)
                }
            }
            combine(domainFlows) { it.toList() }
        }
    }

    override fun insertMedication(profileId: String, medication: Medication) {
        medicationDao.insertMedication(medication.toEntity(profileId))
    }

    override fun updateMedication(profileId: String, medication: Medication) {
        medicationDao.updateMedication(medication.toEntity(profileId))
    }

    override fun deleteMedication(profileId: String, medication: Medication) {
        medicationDao.deleteMedication(medication.toEntity(profileId))
    }

    override fun logInventoryChange(medicationId: String, amount: Int, reason: String) {
        val newLog = SupplyLogEntity(
            id = UUID.randomUUID().toString(),
            medicationId = medicationId,
            changeAmount = amount,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
        supplyLogDao.insertSupplyLog(newLog)
    }

    override fun getLogsForMedication(medicationId: String): Flow<List<SupplyLogEntity>> {
        return supplyLogDao.getLogsForMedication(medicationId)
    }
}