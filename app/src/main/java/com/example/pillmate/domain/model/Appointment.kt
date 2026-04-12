package com.example.pillmate.domain.model

data class Appointment(
	val id: String = "",
	val name: String = "",
	val location: String = "",
	val doctorName: String = "",
	val category: String = "",
	val description: String = "",
)
