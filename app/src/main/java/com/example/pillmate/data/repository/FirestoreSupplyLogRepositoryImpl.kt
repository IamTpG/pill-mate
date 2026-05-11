package com.example.pillmate.data.repository

import com.example.pillmate.domain.model.SupplyLog
import com.example.pillmate.domain.repository.SupplyLogRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreSupplyLogRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val profileId: String
) : FirestoreRepositoryImpl<SupplyLog>(
    getCollectionReference = { pid ->
        firestore.collection("profiles").document(pid).collection("supplyLogs")
    },
    modelClass = SupplyLog::class.java,
    setId = { log, id -> log.copy(id = id) }
), SupplyLogRepository {

    override fun getId(item: SupplyLog): String {
        return item.id.ifEmpty { firestore.collection("tmp").document().id }
    }

    override fun getLogsForMedication(medicationId: String): Flow<List<SupplyLog>> = callbackFlow {
        val listener = firestore.collection("profiles").document(profileId)
            .collection("supplyLogs")
            .whereEqualTo("medId", medicationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val logs = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        SupplyLog(
                            id = doc.id,
                            medId = doc.getString("medId") ?: "",
                            changeAmount = (doc.getDouble("changeAmount") ?: 0.0).toFloat(),
                            reason = doc.getString("reason") ?: "",
                            createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date(),
                            updatedAt = doc.getTimestamp("updatedAt")?.toDate() ?: Date(),
                            deletedAt = doc.getTimestamp("deletedAt")?.toDate()
                        )
                    } catch (_: Exception) { null }
                } ?: emptyList()
                trySend(logs)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun deleteLogsForMedication(profileId: String, medicationId: String): Result<Unit> = runCatching {
        val logs = firestore.collection("profiles").document(profileId)
            .collection("supplyLogs")
            .whereEqualTo("medId", medicationId)
            .get()
            .await()

        if (logs.documents.isNotEmpty()) {
            val batch = firestore.batch()
            logs.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }
}
