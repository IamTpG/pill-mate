package com.example.pillmate.data.repository

import com.example.pillmate.domain.model.Appointment
import com.example.pillmate.domain.model.AppointmentLog
import com.example.pillmate.domain.repository.AppointmentRepository
import com.example.pillmate.domain.repository.LocalRepository
import com.example.pillmate.domain.repository.RemoteRepository
import com.example.pillmate.util.NetworkChecker
import kotlinx.coroutines.flow.Flow
import java.util.Date

class HybridAppointmentRepositoryImpl(
	private val localAppointmentRepo: AppointmentRepository,
	private val remoteAppointmentRepo: AppointmentRepository,
	internal val networkChecker: NetworkChecker
) : HybridRepositoryImpl<Appointment>(
	localRepo = localAppointmentRepo as LocalRepository<Appointment>,
	remoteRepo = remoteAppointmentRepo as RemoteRepository<Appointment>,
	networkChecker = { networkChecker.isOnline() },
	getId = { it.id },
	getUpdatedAt = { it.updatedAt },
	getDeletedAt = { it.deletedAt }, // Kích hoạt cơ chế Soft Delete (Xóa mềm) để Sync
	copyWithUpdated = { item, date -> item.copy(updatedAt = date) },
	copyWithDeleted = { item, date -> item.copy(deletedAt = date) } // Cập nhật thời gian khi user bấm xóa
), AppointmentRepository {
	override fun getAppointmentLogs(profileId: String, date: Date): Flow<List<AppointmentLog>> {
		// Delegate (giao phó) việc lấy log cho Local Repository để đảm bảo tốc độ và có thể chạy offline
		return localAppointmentRepo.getAppointmentLogs(profileId, date)
	}
}