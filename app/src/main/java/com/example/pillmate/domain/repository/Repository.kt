package com.example.pillmate.domain.repository

import kotlinx.coroutines.flow.Flow

interface Repository<T> {
    fun getAll(profileId: String): Flow<List<T>>
    suspend fun getAllOnce(profileId: String): Result<List<T>>
    suspend fun getById(profileId: String, id: String): Result<T?>
    suspend fun add(profileId: String, item: T): Result<Unit>
    suspend fun remove(profileId: String, id: String): Result<Unit>
    suspend fun update(profileId: String, item: T): Result<Unit>
}
