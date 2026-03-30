package com.example.pillmate.data.remote.firebase

import com.example.pillmate.domain.model.TaskLog
import com.example.pillmate.domain.repository.LogRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

    override suspend fun getLogsForDay(profileId: String, date: Date): Result<List<TaskLog>> {
        return try {
            // Simple date range for the query (Start of day to End of day)
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

            val snapshot = db.collection("profiles").document(profileId)
                .collection("logs")
                .whereGreaterThanOrEqualTo("scheduledTime", start)
                .whereLessThanOrEqualTo("scheduledTime", end)
                .get().await()
            
            val logs = snapshot.documents.mapNotNull { it.toObject(TaskLog::class.java)?.copy(id = it.id) }
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
