package com.example.pillmate.data.repository

import com.example.pillmate.domain.repository.LocalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

abstract class RoomRepositoryImpl<TDomain, TEntity>(
    private val getAllFlow: (profileId: String) -> Flow<List<TEntity>>,
    private val getAllOnceFunc: suspend (profileId: String) -> List<TEntity>,
    private val getByIdFunc: suspend (profileId: String, id: String) -> TEntity?,
    private val insert: suspend (TEntity) -> Unit,
    private val updateFunc: suspend (TEntity) -> Unit,
    private val deleteById: suspend (profileId: String, id: String) -> Unit,
    protected val toDomain: (TEntity) -> TDomain,
    protected val toEntity: (profileId: String, domain: TDomain) -> TEntity,
    protected val getId: (TDomain) -> String
) : LocalRepository<TDomain> {

    override fun getAll(profileId: String): Flow<List<TDomain>> =
        getAllFlow(profileId).map { list -> list.map(toDomain) }

    override suspend fun getAllOnce(profileId: String): Result<List<TDomain>> = runCatching {
        getAllOnceFunc(profileId).map(toDomain)
    }

    override suspend fun getById(profileId: String, id: String): Result<TDomain?> = runCatching {
        getByIdFunc(profileId, id)?.let(toDomain)
    }

    override suspend fun add(profileId: String, item: TDomain): Result<Unit> = runCatching {
        insert(toEntity(profileId, item))
    }

    override suspend fun update(profileId: String, item: TDomain): Result<Unit> = runCatching {
        updateFunc(toEntity(profileId, item))
    }

    override suspend fun remove(profileId: String, id: String): Result<Unit> = runCatching {
        deleteById(profileId, id)
    }
}
