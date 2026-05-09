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
    private val localMedRepo: MedicationRepository,
    private val remoteMedRepo: MedicationRepository,
    private val firestore: FirebaseFirestore,
    internal val networkChecker: NetworkChecker
) : HybridRepositoryImpl<Medication>(
    localRepo = localMedRepo as LocalRepository<Medication>,
    remoteRepo = remoteMedRepo as RemoteRepository<Medication>,
    networkChecker = { networkChecker.isOnline() },
    getId = { it.id },
    getUpdatedAt = { it.updatedAt },
    getDeletedAt = { it.deletedAt }, // Use soft delete for background sync worker
    copyWithUpdated = { item, date -> item.copy(updatedAt = date) },
    copyWithDeleted = { item, date -> item.copy(deletedAt = date) } // Enable soft delete
), MedicationRepository {
    override suspend fun add(profileId: String, item: Medication): Result<Unit> = runCatching {
        localMedRepo.add(profileId, item).getOrThrow()
        
        // Sync to Firestore
        if (networkChecker.isOnline()) {
            remoteMedRepo.add(profileId, item).getOrThrow()
            
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
        localMedRepo.update(profileId, updatedItem).getOrThrow()
        
        // Sync to Firestore
        if (networkChecker.isOnline()) {
            remoteMedRepo.update(profileId, updatedItem).getOrThrow()
            
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
        if (networkChecker.isOnline()) {
            remoteMedRepo.getMedicationWithSupply(profileId, id).getOrNull()
        } else {
            localMedRepo.getMedicationWithSupply(profileId, id).getOrNull()
        }
    }

    override suspend fun getMedicationSupplies(profileId: String, medId: String): Result<List<MedicationSupply>> = runCatching {
        if (networkChecker.isOnline()) {
            remoteMedRepo.getMedicationSupplies(profileId, medId).getOrThrow()
        } else {
            localMedRepo.getMedicationSupplies(profileId, medId).getOrThrow()
        }
    }

    override suspend fun updateMedicationSupply(profileId: String, medId: String, changeAmount: Float, supplyId: String?): Result<Unit> = runCatching {
        localMedRepo.updateMedicationSupply(profileId, medId, changeAmount, supplyId).getOrThrow()
        if (networkChecker.isOnline()) {
            remoteMedRepo.updateMedicationSupply(profileId, medId, changeAmount, supplyId).getOrThrow()
        }
    }

    override suspend fun logInventoryChange(profileId: String, medicationId: String, amount: Int, reason: String): Result<Unit> = runCatching {
        localMedRepo.logInventoryChange(profileId, medicationId, amount, reason).getOrThrow()

        // Sync to Firestore
        if (networkChecker.isOnline()) {
            try {
                remoteMedRepo.logInventoryChange(profileId, medicationId, amount, reason).getOrThrow()
            } catch (_: Exception) {
                // Silently fail remote sync — local is source of truth for inventory
            }
        }
    }

    override fun getLogsForMedication(medicationId: String): Flow<List<InventoryLog>> {
        return localMedRepo.getLogsForMedication(medicationId)
    }
}
