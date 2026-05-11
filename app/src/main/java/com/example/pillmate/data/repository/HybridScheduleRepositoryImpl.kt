package com.example.pillmate.data.repository

import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.repository.LocalRepository
import com.example.pillmate.domain.repository.RemoteRepository
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.util.NetworkChecker

class HybridScheduleRepositoryImpl(
    private val localScheduleRepo: ScheduleRepository,
    private val remoteScheduleRepo: ScheduleRepository,
    internal val networkChecker: NetworkChecker
) : HybridRepositoryImpl<Schedule>(
    localRepo = localScheduleRepo as LocalRepository<Schedule>,
    remoteRepo = remoteScheduleRepo as RemoteRepository<Schedule>,
    networkChecker = { networkChecker.isOnline() },
    getId = { it.id },
    getUpdatedAt = { it.updatedAt },
    getDeletedAt = { it.deletedAt },
    copyWithUpdated = { item, date -> item.copy(updatedAt = date) },
    copyWithDeleted = { item, date -> item.copy(deletedAt = date) }
), ScheduleRepository {
}
