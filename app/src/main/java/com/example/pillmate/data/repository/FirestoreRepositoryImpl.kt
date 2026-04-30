package com.example.pillmate.data.repository

import com.example.pillmate.domain.repository.RemoteRepository
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

abstract class FirestoreRepositoryImpl<T : Any>(
    private val getCollectionReference: (profileId: String) -> CollectionReference,
    private val modelClass: Class<T>,
    private val setId: (T, String) -> T
) : RemoteRepository<T> {

    override fun getAll(profileId: String): Flow<List<T>> = callbackFlow {
        val listener = getCollectionReference(profileId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val items = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(modelClass)?.let { setId(it, doc.id) }
            } ?: emptyList()

            trySend(items)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getAllOnce(profileId: String): Result<List<T>> = runCatching {
        val snapshot = getCollectionReference(profileId).get().await()
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(modelClass)?.let { setId(it, doc.id) }
        }
    }

    override suspend fun getById(profileId: String, id: String): Result<T?> = runCatching {
        val snapshot = getCollectionReference(profileId).document(id).get().await()
        snapshot.toObject(modelClass)?.let { setId(it, snapshot.id) }
    }

    override suspend fun add(profileId: String, item: T): Result<Unit> = runCatching {
        val id = getId(item) // use the domain ID
        val itemWithId = setId(item, id)
        getCollectionReference(profileId).document(id).set(itemWithId).await()
    }

    override suspend fun update(profileId: String, item: T): Result<Unit> = runCatching {
        getCollectionReference(profileId).document(getId(item)).set(item).await()
    }

    override suspend fun remove(profileId: String, id: String): Result<Unit> = runCatching {
        getCollectionReference(profileId).document(id).delete().await()
    }

    override fun getCollection(profileId: String): CollectionReference {
        return getCollectionReference(profileId)
    }

    protected abstract fun getId(item: T): String
}
