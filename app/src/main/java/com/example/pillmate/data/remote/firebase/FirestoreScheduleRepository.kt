package com.example.pillmate.data.remote.firebase

import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.repository.ScheduleRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreScheduleRepository(
    private val db: FirebaseFirestore
) : ScheduleRepository {

    override suspend fun getSchedules(profileId: String): Result<List<Schedule>> {
        return try {
            val snapshot = db.collection("profiles").document(profileId)
                .collection("schedules").get().await()
            val schedules = snapshot.documents.mapNotNull { it.toObject(Schedule::class.java)?.copy(id = it.id) }
            Result.success(schedules)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveSchedule(profileId: String, schedule: Schedule): Result<Unit> {
        return try {
            db.collection("profiles").document(profileId)
                .collection("schedules").add(schedule).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
