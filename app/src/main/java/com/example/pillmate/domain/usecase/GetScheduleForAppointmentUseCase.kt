package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.domain.repository.ScheduleRepository

class GetScheduleForAppointmentUseCase(
	private val scheduleRepository: ScheduleRepository
) {
	suspend operator fun invoke(profileId: String, appointmentId: String): Schedule? {
		// Lấy tất cả schedule của user này (từ Room cho cực nhanh nhờ Offline-first)
		val result = scheduleRepository.getAllOnce(profileId)
		
		// Lọc ra schedule thỏa mãn: type là APPOINTMENT và sourceId trùng với appointmentId
		return result.getOrNull()?.find { schedule ->
			schedule.type == TaskType.APPOINTMENT &&
					schedule.eventSnapshot.sourceId == appointmentId
		}
	}
}