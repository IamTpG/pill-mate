package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.domain.repository.ScheduleRepository

class UpdateScheduleUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val checkLowStockUseCase: CheckLowStockUseCase
) {
    suspend operator fun invoke(profileId: String, schedule: Schedule): Result<Unit> {
        val result = scheduleRepository.add(profileId, schedule)
        if (result.isSuccess && schedule.type == TaskType.MEDICATION) {
            checkLowStockUseCase.execute(profileId, schedule.eventSnapshot.sourceId)
        }
        return result
    }
}
