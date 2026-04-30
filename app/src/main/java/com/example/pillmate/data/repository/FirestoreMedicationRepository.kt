package com.example.pillmate.data.repository

import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.model.MedicationSupply
import com.example.pillmate.domain.repository.MedicationRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreMedicationRepository(
    private val db: FirebaseFirestore,
    private val profileId: String
) : MedicationRepository {

    override suspend fun getMedication(id: String): Result<Medication?> {
        return try {
            val doc = db.collection("profiles").document(profileId)
                .collection("medications").document(id).get().await()
            
            if (doc.exists()) {
                val med = doc.toObject(Medication::class.java)?.copy(id = doc.id)
                
                // Fetch all supplies and calculate combined total
                val suppliesResult = getMedicationSupplies(id)
                val supplies = suppliesResult.getOrNull() ?: emptyList()
                val totalQty = supplies.sumOf { it.quantity.toDouble() }.toFloat()

                // For the "primary" supply displayed on the main med object,
                // we'll pick the one with lowest stock > 0 as the "active" one,
                // or just the first if all 0.
                val activeSupply = supplies.filter { it.quantity > 0 }.minByOrNull { it.quantity }
                    ?: supplies.firstOrNull()
                
                Result.success(med?.copy(supply = activeSupply?.copy(quantity = totalQty)))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMedicationSupply(medId: String, changeAmount: Float, supplyId: String?): Result<Unit> {
        return try {
            val targetSupplyRef = if (supplyId != null) {
                // Use specific supply provided (User manual choice)
                db.collection("profiles").document(profileId)
                    .collection("medications").document(medId)
                    .collection("supply").document(supplyId)
            } else {
                // SMART AUTO-SELECTION: Use supply with lowest stock > 0
                val supplies = getMedicationSupplies(medId).getOrNull() ?: emptyList()
                val target = supplies.filter { it.quantity > 0 }.minByOrNull { it.quantity }
                    ?: supplies.firstOrNull() // Fallback to first if all empty

                if (target != null) {
                    db.collection("profiles").document(profileId)
                        .collection("medications").document(medId)
                        .collection("supply").document(target.id)
                } else null
            }
            
            if (targetSupplyRef != null) {
                // Add inventory log
                val inventoryLog = hashMapOf(
                    "changeAmount" to changeAmount,
                    "reason" to if (changeAmount < 0) "TAKEN" else "REFILL",
                    "timestamp" to Timestamp.now()
                )
                targetSupplyRef.collection("logs").add(inventoryLog).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No supply record found for medication"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMedicationSupplies(medId: String): Result<List<MedicationSupply>> {
        return try {
            val supplyDocs = db.collection("profiles").document(profileId)
                .collection("medications").document(medId)
                .collection("supply").get().await()

            val supplies = supplyDocs.documents.map { doc ->
                val inventoryLogs = doc.reference.collection("logs").get().await()
                val totalQty = inventoryLogs.documents.sumOf { it.getDouble("changeAmount") ?: 0.0 }.toFloat()

                doc.toObject(MedicationSupply::class.java)!!.copy(
                    id = doc.id,
                    quantity = totalQty
                )
            }
            Result.success(supplies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllMedications(profileId: String): Result<List<Medication>> {
        // Implementation for listing
        return Result.success(emptyList()) // Placeholder
    }

    override suspend fun saveMedication(profileId: String, medication: Medication): Result<String> {
        return try {
            val docRef = db.collection("profiles").document(profileId)
                .collection("medications").add(medication).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
