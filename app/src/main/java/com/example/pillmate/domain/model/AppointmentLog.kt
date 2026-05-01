package com.example.pillmate.domain.model

import kotlinx.serialization.descriptors.SerialDescriptor
import java.util.Date

// enum class LogStatus { COMPLETED, MISSED, SKIPPED, LATE, PENDING } // PENDING for 'Upcoming'

data class AppointmentLog(
	val id: String,
	val name: String = "",
	val location: String = "",
	val doctorName: String = "",
	val description: String = ""
)

data class Appointment(
	val id: String = "",
	val name: String = "",
	val location: String = "",
	val doctorName: String = "",
	val description: String = "",
	val updatedAt: Date = Date(),
	val deletedAt: Date? = null
)