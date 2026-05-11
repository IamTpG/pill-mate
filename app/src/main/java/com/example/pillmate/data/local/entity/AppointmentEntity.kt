package com.example.pillmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class AppointmentEntity(
	@PrimaryKey
	val id: String,
	val profileId: String,
	val name: String,
	val location: String,
	val doctorName: String,
	val description: String,
	val createdAt: Long,
	val updatedAt: Long,
	val deletedAt: Long? = null
)
