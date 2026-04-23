package com.example.pillmate.domain.model

import java.util.Date

// enum class LogStatus { COMPLETED, MISSED, SKIPPED, LATE, PENDING } // PENDING for 'Upcoming'

data class AppointmentLog(
	val id: String,
	val status: LogStatus,
	val scheduledTime: Date,
	val title: String,        // From eventSnapshot.title
	val instructions: String, // From eventSnapshot.instructions
	val location: String?,    // From eventSnapshot.location
	val doctorName: String?   // Extracted from title or notes
)