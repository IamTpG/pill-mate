package com.example.pillmate.domain.repository

import com.example.pillmate.domain.model.Schedule
import java.util.Date

interface ScheduleRepository {
    suspend fun getSchedules(profileId: String): Result<List<Schedule>>
    suspend fun saveSchedule(profileId: String, schedule: Schedule): Result<Unit>
}
