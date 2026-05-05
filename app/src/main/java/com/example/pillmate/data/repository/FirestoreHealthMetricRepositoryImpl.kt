package com.example.pillmate.data.repository

import com.example.pillmate.domain.model.HealthMetric
import com.example.pillmate.domain.repository.HealthMetricRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreHealthMetricRepositoryImpl(private val firestore: FirebaseFirestore) :
    FirestoreRepositoryImpl<HealthMetric>(
        getCollectionReference = { profileId ->
            firestore.collection("profiles").document(profileId).collection("healthMetrics")
        },
        modelClass = HealthMetric::class.java,
        setId = { metric, id -> metric.copy(id = id) }
    ),
    HealthMetricRepository {

    override fun getId(item: HealthMetric): String {
        return item.id.ifEmpty { firestore.collection("tmp").document().id }
    }

    override fun getRecentMetrics(profileId: String, limit: Int): Flow<List<HealthMetric>> = callbackFlow {
        val query = getCollection(profileId)
            .orderBy("recordedAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val items = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(HealthMetric::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            trySend(items)
        }
        awaitClose { listener.remove() }
    }
}
