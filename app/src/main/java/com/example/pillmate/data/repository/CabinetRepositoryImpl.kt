package com.example.pillmate.data.repository

import com.example.pillmate.data.local.dao.MedicationDao
import com.example.pillmate.data.local.dao.SupplyLogDao
import com.example.pillmate.data.local.entity.MedicationEntity
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
        // Keep Room in sync with Firestore so cabinet still loads after local DB resets.
        syncScope.launch { syncMedicationsFromRemote(profileId) }
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

    private suspend fun syncMedicationsFromRemote(profileId: String) {
        try {
            val snapshot = firestore.collection("profiles").document(profileId)
                .collection("medications")
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                val data = doc.data ?: return@forEach
                val expirationRaw = (data["supply"] as? Map<*, *>)?.get("expirationDate")
                val expirationMillis = when (expirationRaw) {
                    is com.google.firebase.Timestamp -> expirationRaw.toDate().time
                    is java.util.Date -> expirationRaw.time
                    is Long -> expirationRaw
                    else -> 0L
                }

                medicationDao.insertMedication(
                    MedicationEntity(
                        id = doc.id,
                        profileId = profileId,
                        name = data["name"] as? String ?: "",
                        description = data["description"] as? String ?: "",
                        unit = data["unit"] as? String ?: "tablet",
                        photoUrl = data["photoUrl"] as? String,
                        expirationDate = expirationMillis
                    )
                )
            }
        } catch (_: Exception) {
            // Ignore sync errors; UI still shows local cache.
        }
    }
}