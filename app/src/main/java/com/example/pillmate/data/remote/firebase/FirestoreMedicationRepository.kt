package com.example.pillmate.data.remote.firebase

import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.model.MedicationSupply
import com.example.pillmate.domain.repository.MedicationRepository
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
                // Fetch supply (assuming only one for now for simplicity)
                val supplyDocs = db.collection("profiles").document(profileId)
                    .collection("medications").document(id)
                    .collection("supply").limit(1).get().await()
                
                val supply = if (!supplyDocs.isEmpty) {
                    val supplyDoc = supplyDocs.documents[0]
                    // Sum up all logs to get total quantity
                    val inventoryLogs = supplyDoc.reference.collection("logs").get().await()
                    val totalQty = inventoryLogs.documents.sumOf { it.getDouble("changeAmount") ?: 0.0 }.toFloat()
                    
                    supplyDoc.toObject(MedicationSupply::class.java)?.copy(
                        id = supplyDoc.id,
                        quantity = totalQty
                    )
                } else null
                
                Result.success(med?.copy(supply = supply))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMedicationSupply(medId: String, changeAmount: Float): Result<Unit> {
        return try {
            // Find the supply document
            val supplyDocs = db.collection("profiles").document(profileId)
                .collection("medications").document(medId)
                .collection("supply").limit(1).get().await()
            
            if (!supplyDocs.isEmpty) {
                val supplyDoc = supplyDocs.documents[0]
                
                // Add inventory log (Quantity is inferred from these logs)
                val inventoryLog = hashMapOf(
                    "changeAmount" to changeAmount,
                    "reason" to if (changeAmount < 0) "TAKEN" else "REFILL",
                    "timestamp" to com.google.firebase.Timestamp.now()
                )
                supplyDoc.reference.collection("logs").add(inventoryLog).await()
                
                Result.success(Unit)
            } else {
                Result.failure(Exception("No supply record found for medication"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllMedications(profileId: String): Result<List<Medication>> {
        // Implementation for listing
        return Result.success(emptyList()) // Placeholder
    }
}
