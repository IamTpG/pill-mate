package com.example.pillmate.data.repository

import com.example.pillmate.data.local.dao.MedicationDao
import com.example.pillmate.data.local.entity.MedicationEntity
import com.example.pillmate.data.mapper.toDomainModel
import com.example.pillmate.data.mapper.toEntity
import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.repository.LocalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMedicationRepositoryImpl(
    private val dao: MedicationDao
) : RoomRepositoryImpl<Medication, MedicationEntity>(
    getAllFlow = { profileId -> dao.getMedicationsForProfile(profileId) },
    getAllOnceFunc = { profileId -> dao.getAllMedicationsOnce(profileId) },
    getByIdFunc = { profileId, id -> dao.getMedicationByIdAndProfile(profileId, id) },
    insert = { entity -> dao.insertMedication(entity) },
    updateFunc = { entity -> dao.updateMedication(entity) },
    deleteById = { profileId, id -> dao.deleteById(profileId, id) },
    toDomain = { entity -> entity.toDomainModel() },
    toEntity = { profileId, domain -> domain.toEntity(profileId) },
    getId = { it.id }
), LocalRepository<Medication>
