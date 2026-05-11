package com.example.pillmate.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pillmate.data.local.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
	
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertAppointment(appointment: AppointmentEntity): Long
	
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertAppointments(appointments: List<AppointmentEntity>): List<Long>
	
	@Update
	suspend fun updateAppointment(appointment: AppointmentEntity): Int
	
	@Delete
	suspend fun deleteAppointment(appointment: AppointmentEntity): Int
	
	// Xóa vật lý một bản ghi cụ thể theo id và profileId
	@Query("DELETE FROM appointments WHERE id = :id AND profileId = :profileId")
	suspend fun deleteById(profileId: String, id: String)
	
	// Lấy luồng dữ liệu (Flow) để UI tự động cập nhật
	@Query("SELECT * FROM appointments WHERE profileId = :profileId ORDER BY createdAt DESC")
	fun getAppointmentsForProfile(profileId: String): Flow<List<AppointmentEntity>>
	
	// Lấy dữ liệu 1 lần (dùng cho các logic ngầm, không cần UI phản ứng)
	@Query("SELECT * FROM appointments WHERE profileId = :profileId ORDER BY createdAt DESC")
	suspend fun getAppointmentsOnce(profileId: String): List<AppointmentEntity>
	
	@Query("SELECT * FROM appointments WHERE id = :id AND profileId = :profileId LIMIT 1")
	suspend fun getAppointmentByIdAndProfile(profileId: String, id: String): AppointmentEntity?
}