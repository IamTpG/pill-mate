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
    protected val remoteRepo: RemoteRepository<T>,
    private val networkChecker: () -> Boolean,
    protected val getId: (T) -> String,
    protected val getUpdatedAt: (T) -> Date,
    protected val getDeletedAt: (T) -> Date?,
    protected val copyWithUpdated: (T, Date) -> T,
    protected val copyWithDeleted: (T, Date?) -> T
) : Repository<T> {
    
    private val threshold = 1000L // 1 second threshold for sync stability
    
    private fun isSignificantlyDifferent(d1: Date, d2: Date): Boolean {
        return Math.abs(d1.time - d2.time) > threshold
    }

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
                    if (remoteDel == null || isSignificantlyDifferent(localDel, remoteDel)) {
                        remoteRepo.update(profileId, item) // propagate deletion
                    }
                } else if (remoteDel != null) {
                    if (isSignificantlyDifferent(remoteDel, localUpdated)) {
                        localRepo.update(profileId, remoteItem) // propagate remote deletion
                    }
                } else {
                    // No deletions, pick latest updated
                    if (isSignificantlyDifferent(localUpdated, remoteUpdated)) {
                        if (localUpdated.after(remoteUpdated)) remoteRepo.update(profileId, item)
                        else localRepo.update(profileId, remoteItem)
                    }
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
                        localDel != null && (remoteDel == null || isSignificantlyDifferent(localDel, remoteDel)) ->
                            remoteRepo.update(profileId, local)
                        remoteDel != null && (localDel == null || isSignificantlyDifferent(remoteDel, localDel)) ->
                            localRepo.update(profileId, remote)
                        isSignificantlyDifferent(localUpdated, remoteUpdated) -> {
                            if (localUpdated.after(remoteUpdated)) remoteRepo.update(profileId, local)
                            else localRepo.update(profileId, remote)
                        }
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
