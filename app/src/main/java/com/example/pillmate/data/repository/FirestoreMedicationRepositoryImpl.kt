package com.example.pillmate.data.repository

import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.model.MedicationSupply
import com.example.pillmate.domain.repository.MedicationRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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

    override fun getId(item: Medication): String {
        return item.id.ifEmpty { firestore.collection("tmp").document().id }
    }

    override suspend fun getMedicationWithSupply(profileId: String, id: String): Result<Medication?> = runCatching {
        val doc = firestore.collection("profiles").document(profileId)
            .collection("medications").document(id).get().await()

        if (doc.exists()) {
            val med = doc.toObject(Medication::class.java)?.copy(id = doc.id)

            val suppliesResult = getMedicationSupplies(profileId, id)
            val supplies = suppliesResult.getOrNull() ?: emptyList()
            val totalQty = supplies.sumOf { it.quantity.toDouble() }.toFloat()

            val activeSupply = supplies.filter { it.quantity > 0 }.minByOrNull { it.quantity }
                ?: supplies.firstOrNull()

            med?.copy(supply = activeSupply?.copy(quantity = totalQty))
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
            val totalQty = inventoryLogs.documents.sumOf { it.getDouble("changeAmount") ?: 0.0 }.toFloat()

            doc.toObject(MedicationSupply::class.java)!!.copy(
                id = doc.id,
                quantity = totalQty
            )
        }
    }

    override suspend fun updateMedicationSupply(profileId: String, medId: String, changeAmount: Float, supplyId: String?): Result<Unit> = runCatching {
        val targetSupplyRef = if (supplyId != null) {
            firestore.collection("profiles").document(profileId)
                .collection("medications").document(medId)
                .collection("supply").document(supplyId)
        } else {
            val supplies = getMedicationSupplies(profileId, medId).getOrNull() ?: emptyList()
            val target = supplies.filter { it.quantity > 0 }.minByOrNull { it.quantity }
                ?: supplies.firstOrNull()

            if (target != null) {
                firestore.collection("profiles").document(profileId)
                    .collection("medications").document(medId)
                    .collection("supply").document(target.id)
            } else null
        }

        if (targetSupplyRef != null) {
            val inventoryLog = hashMapOf(
                "changeAmount" to changeAmount,
                "reason" to if (changeAmount < 0) "TAKEN" else "REFILL",
                "timestamp" to Timestamp.now()
            )
            targetSupplyRef.collection("logs").add(inventoryLog).await()
        } else {
            throw Exception("No supply record found for medication")
        }
    }
}
