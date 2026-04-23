package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.repository.AppointmentRepository
import java.util.Date

class GetAppointmentsUseCase(private val repository: AppointmentRepository) {
	operator fun invoke(profileId: String, date: Date) =
		repository.getAppointmentLogs(profileId, date)
}