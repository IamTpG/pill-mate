package com.example.pillmate.domain.repository

import com.example.pillmate.domain.model.Appointment
import com.example.pillmate.domain.model.AppointmentLog
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface AppointmentRepository {
	fun getAppointmentLogs(profileId: String, date: Date): Flow<List<AppointmentLog>>
	
	suspend fun addAppointment(profileId: String, appointment: Appointment): Result<Unit>
	
	suspend fun updateApponitment(profileId: String, appointmentId: String, appointment: Appointment): Result<Unit>
	
	suspend fun deleteAppointment(profileId: String, appointmentId: String): Result<Unit>
}