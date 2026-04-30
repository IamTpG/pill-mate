package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.repository.AppointmentRepository

class DeleteAppointmentUseCase(
	private val repository: AppointmentRepository
) {
	suspend operator fun invoke(profileId: String, appointmentId: String) =
		repository.remove(profileId, appointmentId)
}