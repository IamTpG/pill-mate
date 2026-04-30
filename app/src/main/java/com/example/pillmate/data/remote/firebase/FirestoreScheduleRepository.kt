package com.example.pillmate.data.remote.firebase

import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.repository.ScheduleRepository
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
            val profileRef = db.collection("profiles").document(profileId)
            profileRef.set(mapOf("accountId" to profileId), SetOptions.merge()).await()
            val collection = profileRef.collection("schedules")
            if (schedule.id.isNotBlank()) {
                collection.document(schedule.id).set(schedule).await()
            } else {
                collection.add(schedule).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSchedule(profileId: String, scheduleId: String): Result<Unit> {
        return try {
            db.collection("profiles").document(profileId)
                .collection("schedules").document(scheduleId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSchedulesFlow(profileId: String): Flow<List<Schedule>> = callbackFlow {
        val registration = db.collection("profiles").document(profileId)
            .collection("schedules")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val schedules = snapshot.documents.mapNotNull { it.toObject(Schedule::class.java)?.copy(id = it.id) }
                    trySend(schedules)
                }
            }
        
        awaitClose { registration.remove() }
    }
}
