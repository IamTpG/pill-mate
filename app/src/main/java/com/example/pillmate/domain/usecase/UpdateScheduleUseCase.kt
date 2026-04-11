package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.repository.ScheduleRepository

class UpdateScheduleUseCase(
    private val scheduleRepository: ScheduleRepository
) {
    suspend operator fun invoke(profileId: String, schedule: Schedule): Result<Unit> {
        return scheduleRepository.saveSchedule(profileId, schedule)
    }
}
