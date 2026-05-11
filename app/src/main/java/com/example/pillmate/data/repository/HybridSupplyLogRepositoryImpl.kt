package com.example.pillmate.data.repository

import com.example.pillmate.domain.model.SupplyLog
import com.example.pillmate.domain.repository.LocalRepository
import com.example.pillmate.domain.repository.RemoteRepository
import com.example.pillmate.domain.repository.SupplyLogRepository
import com.example.pillmate.util.NetworkChecker
import kotlinx.coroutines.flow.Flow

class HybridSupplyLogRepositoryImpl(
    private val localLogRepo: SupplyLogRepository,
    private val remoteLogRepo: SupplyLogRepository,
    private val networkChecker: NetworkChecker
) : HybridRepositoryImpl<SupplyLog>(
    localRepo = localLogRepo as LocalRepository<SupplyLog>,
    remoteRepo = remoteLogRepo as RemoteRepository<SupplyLog>,
    networkChecker = { networkChecker.isOnline() },
    getId = { it.id },
    getUpdatedAt = { it.updatedAt },
    getDeletedAt = { it.deletedAt },
    copyWithUpdated = { item, date -> item.copy(updatedAt = date) },
    copyWithDeleted = { item, date -> item.copy(deletedAt = date) }
), SupplyLogRepository {

    override fun getLogsForMedication(medicationId: String): Flow<List<SupplyLog>> {
        return localLogRepo.getLogsForMedication(medicationId)
    }

    override suspend fun deleteLogsForMedication(profileId: String, medicationId: String): Result<Unit> = runCatching {
        localLogRepo.deleteLogsForMedication(profileId, medicationId).getOrThrow()
        if (networkChecker.isOnline()) {
            remoteLogRepo.deleteLogsForMedication(profileId, medicationId).getOrThrow()
        }
    }
}
