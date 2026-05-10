package com.example.pillmate.data.repository

import com.example.pillmate.data.local.dao.ScheduleDao
import com.example.pillmate.data.local.entity.ScheduleEntity
import com.example.pillmate.data.mapper.toDomainModel
import com.example.pillmate.data.mapper.toEntity
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.repository.LocalRepository
import com.example.pillmate.domain.repository.ScheduleRepository

class RoomScheduleRepositoryImpl(
    private val dao: ScheduleDao
) : RoomRepositoryImpl<Schedule, ScheduleEntity>(
    getAllFlow = { profileId -> dao.getSchedulesForProfile(profileId) },
    getAllOnceFunc = { profileId -> dao.getAllSchedulesOnce(profileId) },
    getByIdFunc = { profileId, id -> dao.getScheduleByIdAndProfile(profileId, id) },
    insert = { entity -> dao.insertSchedule(entity) },
    updateFunc = { entity -> dao.updateSchedule(entity) },
    deleteById = { profileId, id -> dao.deleteById(profileId, id) },
    toDomain = { entity -> entity.toDomainModel() },
    toEntity = { profileId, domain -> domain.toEntity(profileId) },
    getId = { it.id }
), ScheduleRepository, LocalRepository<Schedule>
