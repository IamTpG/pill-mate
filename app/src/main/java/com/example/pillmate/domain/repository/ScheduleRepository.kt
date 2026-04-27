package com.example.pillmate.domain.repository

import com.example.pillmate.domain.model.Schedule
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface ScheduleRepository {
    suspend fun getSchedules(profileId: String): Result<List<Schedule>>
    suspend fun saveSchedule(profileId: String, schedule: Schedule): Result<Unit>
    suspend fun deleteSchedule(profileId: String, scheduleId: String): Result<Unit>
    fun getSchedulesFlow(profileId: String): Flow<List<Schedule>>
}
