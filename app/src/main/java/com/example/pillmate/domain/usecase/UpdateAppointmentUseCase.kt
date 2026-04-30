package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.Appointment
import com.example.pillmate.domain.repository.AppointmentRepository

class UpdateAppointmentUseCase(
	private val repository: AppointmentRepository
) {
	suspend operator fun invoke(profileId: String, appointmentId: String, appointment: Appointment) =
		repository.update(profileId, appointment)
}