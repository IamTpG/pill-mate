package com.example.pillmate.data.repository

import com.example.pillmate.domain.repository.LocalRepository
import com.example.pillmate.domain.repository.RemoteRepository
import com.example.pillmate.domain.repository.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date

open class HybridRepositoryImpl<T>(
    protected val localRepo: LocalRepository<T>,
    private val remoteRepo: RemoteRepository<T>,
    private val networkChecker: () -> Boolean,
    private val getId: (T) -> String,
    private val getUpdatedAt: (T) -> Date,
    private val getDeletedAt: (T) -> Date?,
    private val copyWithUpdated: (T, Date) -> T,
    private val copyWithDeleted: (T, Date?) -> T
) : Repository<T> {

    override fun getAll(profileId: String): Flow<List<T>> =
        localRepo.getAll(profileId).map { list -> list.filter { getDeletedAt(it) == null } }

    override suspend fun getAllOnce(profileId: String): Result<List<T>> = runCatching {
        val list = localRepo.getAllOnce(profileId).getOrThrow()
        list.filter { getDeletedAt(it) == null }
    }

    override suspend fun getById(profileId: String, id: String): Result<T?> = runCatching {
        val item = localRepo.getById(profileId, id).getOrThrow()
        item?.takeIf { getDeletedAt(it) == null }
    }

    override suspend fun add(profileId: String, item: T): Result<Unit> = runCatching {
        localRepo.add(profileId, item).getOrThrow()
        syncItem(profileId, item)
    }

    override suspend fun remove(profileId: String, id: String): Result<Unit> = runCatching {
        val item = localRepo.getById(profileId, id).getOrNull() ?: return@runCatching
        val deletedItem = copyWithDeleted(item, Date())
        localRepo.update(profileId, deletedItem).getOrThrow()
        syncItem(profileId, deletedItem)
    }

    override suspend fun update(profileId: String, item: T): Result<Unit> = runCatching {
        val updatedItem = copyWithUpdated(item, Date())
        localRepo.update(profileId, updatedItem).getOrThrow()
        syncItem(profileId, updatedItem)
    }

    private suspend fun syncItem(profileId: String, item: T) {
        if (!networkChecker()) return

        val remoteItem = remoteRepo.getById(profileId, getId(item)).getOrNull()

        when {
            remoteItem == null && getDeletedAt(item) == null -> remoteRepo.add(profileId, item)
            remoteItem != null -> {
                val localDel = getDeletedAt(item)
                val remoteDel = getDeletedAt(remoteItem)
                val localUpdated = getUpdatedAt(item)
                val remoteUpdated = getUpdatedAt(remoteItem)

                // If local deleted
                if (localDel != null) {
                    if (remoteDel == null || localDel.after(remoteDel)) {
                        remoteRepo.update(profileId, item) // propagate deletion
                    }
                } else if (remoteDel != null) {
                    if (remoteDel.after(localUpdated)) {
                        localRepo.update(profileId, remoteItem) // propagate remote deletion
                    }
                } else {
                    // No deletions, pick latest updated
                    if (!localUpdated.before(remoteUpdated)) remoteRepo.update(profileId, item)
                    else localRepo.update(profileId, remoteItem)
                }
            }
        }
    }

    suspend fun syncAll(profileId: String) {
        if (!networkChecker()) return

        val localItems = localRepo.getAllOnce(profileId).getOrNull() ?: emptyList()
        val remoteItems = remoteRepo.getAllOnce(profileId).getOrNull() ?: emptyList()

        val localMap = localItems.associateBy(getId)
        val remoteMap = remoteItems.associateBy(getId)

        val allIds = localMap.keys + remoteMap.keys

        for (id in allIds) {
            val local = localMap[id]
            val remote = remoteMap[id]

            when {
                local == null && remote != null && getDeletedAt(remote) == null -> localRepo.add(profileId, remote)
                local != null && remote == null && getDeletedAt(local) == null -> remoteRepo.add(profileId, local)
                local != null && remote != null -> {
                    val localDel = getDeletedAt(local)
                    val remoteDel = getDeletedAt(remote)
                    val localUpdated = getUpdatedAt(local)
                    val remoteUpdated = getUpdatedAt(remote)

                    when {
                        localDel != null && (remoteDel == null || localDel.after(remoteDel)) ->
                            remoteRepo.update(profileId, local)
                        remoteDel != null && (localDel == null || remoteDel.after(localDel)) ->
                            localRepo.update(profileId, remote)
                        localUpdated.after(remoteUpdated) -> remoteRepo.update(profileId, local)
                        remoteUpdated.after(localUpdated) -> localRepo.update(profileId, remote)
                    }
                }
            }
        }
    }

    /** Listen to remote changes and update Room accordingly */
    fun startRemoteListener(profileId: String, scope: CoroutineScope) {
        val ref = remoteRepo.getCollection(profileId) ?: return
        
        ref.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            scope.launch { syncAll(profileId) }
        }
    }
}
