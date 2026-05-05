package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.repository.MedicationRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DeleteMedicationUseCase(
    private val medicationRepository: MedicationRepository,
    private val scheduleRepository: com.example.pillmate.domain.repository.ScheduleRepository,
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(profileId: String, medicationId: String): Result<Unit> = runCatching {
        // 1. Soft-delete the medication itself
        medicationRepository.remove(profileId, medicationId).getOrThrow()

        // 2. Cascade soft-delete related schedules
        try {
            val schedules = scheduleRepository.getAllOnce(profileId).getOrNull() ?: emptyList()
            val relatedSchedules = schedules.filter { it.eventSnapshot.sourceId == medicationId }
            for (schedule in relatedSchedules) {
                scheduleRepository.remove(profileId, schedule.id)
            }
        } catch (_: Exception) {
            // Silently fail cascade
        }
    }
}
