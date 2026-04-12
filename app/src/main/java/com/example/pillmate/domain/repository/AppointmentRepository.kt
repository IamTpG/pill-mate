package com.example.pillmate.domain.repository

import com.example.pillmate.domain.model.Appointment

interface AppointmentRepository {
	suspend fun saveAppointment(appointment: Appointment): Result<String>
	
	suspend fun getAppointment(appointmentId: String): Result<Appointment?>
	
	suspend fun getAllAppointments(): Result<List<Appointment>>
}