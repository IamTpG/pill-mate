package com.example.pillmate.data.remote.firebase

import com.example.pillmate.domain.model.TaskLog
import com.example.pillmate.domain.repository.LogRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreLogRepository(
    private val db: FirebaseFirestore
) : LogRepository {

    override suspend fun saveLog(profileId: String, log: TaskLog): Result<Unit> {
        return try {
            db.collection("profiles").document(profileId)
                .collection("logs").add(log).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getLogsForDayFlow(profileId: String, date: Date): Flow<List<TaskLog>> = callbackFlow {
        val cal = java.util.Calendar.getInstance()
        cal.time = date
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        val start = cal.time

        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        val end = cal.time

        val registration = db.collection("profiles").document(profileId)
            .collection("logs")
            .whereGreaterThanOrEqualTo("scheduledTime", start)
            .whereLessThanOrEqualTo("scheduledTime", end)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val logs = snapshot.documents.mapNotNull {
                        it.toObject(TaskLog::class.java)?.copy(id = it.id)
                    }
                    trySend(logs)
                }
            }
        
        awaitClose { registration.remove() }
    }

    override suspend fun getLogsForDay(profileId: String, date: Date): Result<List<TaskLog>> {
        val cal = java.util.Calendar.getInstance()
        cal.time = date
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        val start = cal.time

        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        val end = cal.time

        return try {
            val snapshot = db.collection("profiles").document(profileId)
                .collection("logs")
                .whereGreaterThanOrEqualTo("scheduledTime", start)
                .whereLessThanOrEqualTo("scheduledTime", end)
                .get()
                .await()
            val logs = snapshot.documents.mapNotNull { it.toObject(TaskLog::class.java)?.copy(id = it.id) }
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
