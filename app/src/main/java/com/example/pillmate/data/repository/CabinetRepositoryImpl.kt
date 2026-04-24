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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class CabinetRepositoryImpl(
    private val medicationDao: MedicationDao,
    private val supplyLogDao: SupplyLogDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CabinetRepository {

    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
        syncScope.launch {
            try {
                firestore.collection("profiles").document(profileId)
                    .collection("medications").document(medication.id)
                    .set(medication).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun updateMedication(profileId: String, medication: Medication) {
        medicationDao.updateMedication(medication.toEntity(profileId))
        syncScope.launch {
            try {
                firestore.collection("profiles").document(profileId)
                    .collection("medications").document(medication.id)
                    .set(medication).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun deleteMedication(profileId: String, medication: Medication) {
        medicationDao.deleteMedication(medication.toEntity(profileId))
        syncScope.launch {
            try {
                firestore.collection("profiles").document(profileId)
                    .collection("medications").document(medication.id)
                    .delete().await()
                    
                // Cascade delete schedules
                val schedules = firestore.collection("profiles").document(profileId)
                    .collection("schedules")
                    .whereEqualTo("eventSnapshot.sourceId", medication.id)
                    .get().await()
                for (doc in schedules.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun logInventoryChange(profileId: String, medicationId: String, amount: Int, reason: String) {
        val newLog = SupplyLogEntity(
            id = UUID.randomUUID().toString(),
            medicationId = medicationId,
            changeAmount = amount,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
        supplyLogDao.insertSupplyLog(newLog)
        
        syncScope.launch {
            try {
                val logData = hashMapOf(
                    "id" to newLog.id,
                    "changeAmount" to newLog.changeAmount,
                    "reason" to newLog.reason,
                    "timestamp" to newLog.timestamp
                )
                firestore.collection("profiles").document(profileId)
                    .collection("medications").document(medicationId)
                    .collection("logs").document(newLog.id)
                    .set(logData).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getLogsForMedication(medicationId: String): Flow<List<SupplyLogEntity>> {
        return supplyLogDao.getLogsForMedication(medicationId)
    }
}