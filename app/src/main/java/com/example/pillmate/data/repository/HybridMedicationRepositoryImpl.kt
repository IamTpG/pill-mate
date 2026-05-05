package com.example.pillmate.data.repository

import com.example.pillmate.data.local.dao.SupplyLogDao
import com.example.pillmate.data.local.entity.SupplyLogEntity
import com.example.pillmate.domain.model.InventoryLog
import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.model.MedicationSupply
import com.example.pillmate.domain.repository.LocalRepository
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.domain.repository.RemoteRepository
import com.example.pillmate.util.NetworkChecker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class HybridMedicationRepositoryImpl(
    localRepo: LocalRepository<Medication>,
    remoteRepo: RemoteRepository<Medication>,
    private val supplyLogDao: SupplyLogDao,
    private val firestore: FirebaseFirestore,
    private val networkChecker: NetworkChecker
) : HybridRepositoryImpl<Medication>(
    localRepo = localRepo,
    remoteRepo = remoteRepo,
    networkChecker = { networkChecker.isOnline() },
    getId = { it.id },
    getUpdatedAt = { it.updatedAt },
    getDeletedAt = { it.deletedAt }, // Use soft delete for background sync worker
    copyWithUpdated = { item, date -> item.copy(updatedAt = date) },
    copyWithDeleted = { item, date -> item.copy(deletedAt = date) } // Enable soft delete
), MedicationRepository {
    override suspend fun add(profileId: String, item: Medication): Result<Unit> = runCatching {
        localRepo.add(profileId, item).getOrThrow()
        
        // Sync to Firestore
        if (networkChecker.isOnline()) {
            remoteRepo.add(profileId, item).getOrThrow()
            
            // Subcollection doc 'main' is source of truth for batch details
            item.supply?.let { s ->
                val mainSupplyRef = firestore.collection("profiles").document(profileId)
                    .collection("medications").document(item.id)
                    .collection("supply").document("main")
                
                mainSupplyRef.set(s.copy(id = "main")).await()
            }
        }
    }

    override suspend fun update(profileId: String, item: Medication): Result<Unit> = runCatching {
        // preserve external updatedAt if already set (e.g. from sync)
        val updatedItem = if (getDeletedAt(item) == null) {
            item.copy(updatedAt = Date())
        } else item
        localRepo.update(profileId, updatedItem).getOrThrow()
        
        // Sync to Firestore
        if (networkChecker.isOnline()) {
            remoteRepo.update(profileId, updatedItem).getOrThrow()
            
            // Subcollection 'main' source of truth
            updatedItem.supply?.let { s ->
                val mainSupplyRef = firestore.collection("profiles").document(profileId)
                    .collection("medications").document(updatedItem.id)
                    .collection("supply").document("main")
                
                mainSupplyRef.set(s.copy(id = "main")).await()
            }
        }
    }

    override suspend fun getMedicationWithSupply(profileId: String, id: String): Result<Medication?> = runCatching {
        // ...Existing heavy fetch logic is fine as a deep-dive...
        val med = getById(profileId, id).getOrThrow()
        if (med != null) {
            val mainSupply = getMedicationSupplies(profileId, id).getOrNull()?.find { it.id == "main" }
            med.copy(supply = mainSupply)
        } else null
    }

    override suspend fun getMedicationSupplies(profileId: String, medId: String): Result<List<MedicationSupply>> = runCatching {
        val supplyDocs = firestore.collection("profiles").document(profileId)
            .collection("medications").document(medId)
            .collection("supply").get().await()

        supplyDocs.documents.map { doc ->
            val inventoryLogs = doc.reference.collection("logs").get().await()
            val totalQty = inventoryLogs.documents.sumOf { (it.get("changeAmount") as? Number)?.toDouble() ?: 0.0 }.toFloat()

            doc.toObject(MedicationSupply::class.java)!!.copy(
                id = doc.id,
                quantity = totalQty
            )
        }
    }

    override suspend fun updateMedicationSupply(profileId: String, medId: String, changeAmount: Float, supplyId: String?): Result<Unit> = runCatching {
        val targetSupplyRef = firestore.collection("profiles").document(profileId)
            .collection("medications").document(medId)
            .collection("supply").document(supplyId ?: "main")

        val inventoryLog = hashMapOf(
            "changeAmount" to changeAmount,
            "reason" to if (changeAmount < 0) "TAKEN" else "REFILL",
            "timestamp" to Timestamp.now()
        )
        targetSupplyRef.collection("logs").add(inventoryLog).await()
    }

    override suspend fun logInventoryChange(profileId: String, medicationId: String, amount: Int, reason: String): Result<Unit> = runCatching {
        val newLog = SupplyLogEntity(
            id = UUID.randomUUID().toString(),
            medicationId = medicationId,
            changeAmount = amount,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
        supplyLogDao.insertSupplyLog(newLog)

        // Sync to Firestore
        try {
            val logData = hashMapOf(
                "id" to newLog.id,
                "changeAmount" to newLog.changeAmount,
                "reason" to newLog.reason,
                "timestamp" to newLog.timestamp
            )
            firestore.collection("profiles").document(profileId)
                .collection("medications").document(medicationId)
                .collection("supply").document("main")
                .collection("logs").document(newLog.id)
                .set(logData).await()
        } catch (_: Exception) {
            // Silently fail remote sync — local is source of truth for inventory
        }
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
