package com.example.pillmate.data.repository

import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.repository.LocalRepository
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.domain.repository.RemoteRepository
import com.example.pillmate.util.NetworkChecker

class HybridMedicationRepositoryImpl(
    private val localMedRepo: MedicationRepository,
    private val remoteMedRepo: MedicationRepository,
    internal val networkChecker: NetworkChecker
) : HybridRepositoryImpl<Medication>(
    localRepo = localMedRepo as LocalRepository<Medication>,
    remoteRepo = remoteMedRepo as RemoteRepository<Medication>,
    networkChecker = { networkChecker.isOnline() },
    getId = { it.id },
    getUpdatedAt = { it.updatedAt },
    getDeletedAt = { it.deletedAt }, // Use soft delete for background sync worker
    copyWithUpdated = { item, date -> item.copy(updatedAt = date) },
    copyWithDeleted = { item, date -> item.copy(deletedAt = date) } // Enable soft delete
), MedicationRepository {
}
