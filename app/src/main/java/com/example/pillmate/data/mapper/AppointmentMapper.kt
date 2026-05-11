package com.example.pillmate.data.mapper

import com.example.pillmate.data.local.entity.AppointmentEntity
import com.example.pillmate.domain.model.Appointment
import java.util.Date

/**
 * Chuyển đổi từ Room Entity (Local Database) sang Domain Model (UI/Business Logic)
 */
fun AppointmentEntity.toDomainModel(): Appointment {
	return Appointment(
		id = id,
		name = name,
		location = location,
		doctorName = doctorName,
		description = description,
		createdAt = Date(createdAt),
		updatedAt = Date(updatedAt),
		deletedAt = deletedAt?.let { Date(it) } // Xử lý an toàn cho nullable field
	)
}

/**
 * Chuyển đổi từ Domain Model sang Room Entity để lưu vào Local Database.
 * @param profileId: Truyền từ bên ngoài vào vì Domain Model thường không (hoặc không nên)
 * chứa thông tin định danh của user/profile đang đăng nhập để tái sử dụng tốt hơn.
 */
fun Appointment.toEntity(profileId: String): AppointmentEntity {
	return AppointmentEntity(
		id = id,
		profileId = profileId,
		name = name,
		location = location,
		doctorName = doctorName,
		description = description ?: "", // Fallback empty string nếu description null
		createdAt = createdAt.time,      // Chuyển Date thành Long (milliseconds)
		updatedAt = updatedAt.time,
		deletedAt = deletedAt?.time
	)
}