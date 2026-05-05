package com.example.pillmate.data.repository

import com.example.pillmate.domain.model.InventoryLog
import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.model.MedicationSupply
import com.example.pillmate.domain.repository.MedicationRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class FirestoreMedicationRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val profileId: String
) :
    FirestoreRepositoryImpl<Medication>(
        getCollectionReference = { pid ->
            firestore.collection("profiles").document(pid).collection("medications")
        },
        modelClass = Medication::class.java,
        setId = { med, id -> med.copy(id = id) }
    ),
    MedicationRepository {

    override suspend fun getAllOnce(profileId: String): Result<List<Medication>> = runCatching {
        val meds = super.getAllOnce(profileId).getOrThrow()
        
        // Fetch all 'main' supplies for these meds
        // Note: Individual fetches are safer than collectionGroup if security rules are per-med
        meds.map { med ->
            val supply = getMedicationSupplies(profileId, med.id).getOrNull()?.find { it.id == "main" }
            med.copy(supply = supply)
        }
    }

    override suspend fun getById(profileId: String, id: String): Result<Medication?> = runCatching {
        val med = super.getById(profileId, id).getOrThrow()
        if (med != null) {
            val supply = getMedicationSupplies(profileId, id).getOrNull()?.find { it.id == "main" }
            med.copy(supply = supply)
        } else null
    }

    override fun getId(item: Medication): String {
        return item.id.ifEmpty { firestore.collection("tmp").document().id }
    }

    override suspend fun getMedicationWithSupply(profileId: String, id: String): Result<Medication?> = runCatching {
        val doc = firestore.collection("profiles").document(profileId)
            .collection("medications").document(id).get().await()

        if (doc.exists()) {
            val med = doc.toObject(Medication::class.java)?.copy(id = doc.id)
            val mainSupply = getMedicationSupplies(profileId, id).getOrNull()?.find { it.id == "main" }
            med?.copy(supply = mainSupply)
        } else {
            null
        }
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
        val logId = UUID.randomUUID().toString()
        val logData = hashMapOf(
            "id" to logId,
            "changeAmount" to amount,
            "reason" to reason,
            "timestamp" to Timestamp.now()
        )
        firestore.collection("profiles").document(profileId)
            .collection("medications").document(medicationId)
            .collection("supply").document("main")
            .collection("logs").document(logId)
            .set(logData).await()
    }

    override fun getLogsForMedication(medicationId: String): Flow<List<InventoryLog>> = callbackFlow {
        val listener = firestore.collection("profiles").document(profileId)
            .collection("medications").document(medicationId)
            .collection("supply").document("main")
            .collection("logs")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val logs = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        InventoryLog(
                            id = doc.id,
                            medId = medicationId,
                            changeAmount = (doc.getDouble("changeAmount") ?: 0.0).toFloat(),
                            reason = doc.getString("reason") ?: "",
                            timestamp = doc.getTimestamp("timestamp")?.toDate() ?: Date()
                        )
                    } catch (_: Exception) { null }
                } ?: emptyList()
                trySend(logs)
            }
        awaitClose { listener.remove() }
    }
}

