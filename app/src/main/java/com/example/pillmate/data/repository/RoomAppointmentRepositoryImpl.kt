package com.example.pillmate.data.repository

import com.example.pillmate.data.local.dao.AppointmentDao
import com.example.pillmate.data.local.entity.AppointmentEntity
import com.example.pillmate.data.mapper.toDomainModel
import com.example.pillmate.data.mapper.toEntity
import com.example.pillmate.domain.model.Appointment
import com.example.pillmate.domain.model.AppointmentLog
import com.example.pillmate.domain.repository.AppointmentRepository
import com.example.pillmate.domain.repository.LocalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class RoomAppointmentRepositoryImpl(
	private val dao: AppointmentDao
) : RoomRepositoryImpl<Appointment, AppointmentEntity>(
	getAllFlow = { profileId -> dao.getAppointmentsForProfile(profileId) },
	getAllOnceFunc = { profileId -> dao.getAppointmentsOnce(profileId) },
	getByIdFunc = { profileId, id -> dao.getAppointmentByIdAndProfile(profileId, id) },
	insert = { entity -> dao.insertAppointment(entity) },
	updateFunc = { entity -> dao.updateAppointment(entity) },
	deleteById = { profileId, id -> dao.deleteById(profileId, id) },
	toDomain = { entity -> entity.toDomainModel() },
	toEntity = { profileId, domain -> domain.toEntity(profileId) },
	getId = { it.id }
), AppointmentRepository, LocalRepository<Appointment> {
	
	override fun getAppointmentLogs(profileId: String, date: Date): Flow<List<AppointmentLog>> {
		// Lấy Flow dữ liệu từ Room và map nó sang model AppointmentLog
		return dao.getAppointmentsForProfile(profileId).map { entities ->
			entities.map { entity ->
				AppointmentLog(
					id = entity.id,
					name = entity.name,
					location = entity.location,
					doctorName = entity.doctorName,
					description = entity.description
				)
			}
		}
	}
	// Khác với Medication, Appointment không cần join với bảng SupplyLog.
	// Nên ta chỉ việc lấy Flow từ DAO và map thẳng sang Domain Model.
	override fun getAll(profileId: String): Flow<List<Appointment>> {
		return dao.getAppointmentsForProfile(profileId).map { entities ->
			entities.map { it.toDomainModel() }
		}
	}
	
	override suspend fun getAllOnce(profileId: String): Result<List<Appointment>> = runCatching {
		dao.getAppointmentsOnce(profileId).map { it.toDomainModel() }
	}
	
	override suspend fun getById(profileId: String, id: String): Result<Appointment?> = runCatching {
		dao.getAppointmentByIdAndProfile(profileId, id)?.toDomainModel()
	}
	
	override suspend fun remove(profileId: String, id: String): Result<Unit> = runCatching {
		dao.deleteById(profileId, id)
	}
}