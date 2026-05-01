package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.repository.ScheduleRepository

class CreateScheduleUseCase(
    private val scheduleRepository: ScheduleRepository
) {
    suspend fun execute(profileId: String, schedule: Schedule): Result<Unit> {
        return scheduleRepository.add(profileId, schedule)
    }
}
