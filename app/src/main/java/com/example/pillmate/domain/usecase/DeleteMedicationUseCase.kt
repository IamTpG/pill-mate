package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.repository.MedicationRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DeleteMedicationUseCase(
    private val medicationRepository: MedicationRepository,
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(profileId: String, medicationId: String): Result<Unit> = runCatching {
        // 1. Delete the medication itself
        medicationRepository.remove(profileId, medicationId).getOrThrow()

        // 2. Cascade delete related schedules from Firestore
        try {
            val schedules = firestore.collection("profiles").document(profileId)
                .collection("schedules")
                .whereEqualTo("eventSnapshot.sourceId", medicationId)
                .get().await()
            for (doc in schedules.documents) {
                doc.reference.delete().await()
            }
        } catch (_: Exception) {
            // Silently fail cascade — medication is already deleted
        }
    }
}
