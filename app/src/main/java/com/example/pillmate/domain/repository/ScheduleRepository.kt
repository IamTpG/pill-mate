package com.example.pillmate.domain.repository

import com.example.pillmate.domain.model.Schedule

interface ScheduleRepository : Repository<Schedule> {
    suspend fun getSchedulesBySourceId(profileId: String, sourceId: String): Result<List<Schedule>>
}
