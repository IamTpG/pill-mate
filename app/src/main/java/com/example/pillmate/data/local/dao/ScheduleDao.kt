package com.example.pillmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pillmate.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE profileId = :profileId")
    fun getSchedulesForProfile(profileId: String): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE profileId = :profileId")
    suspend fun getAllSchedulesOnce(profileId: String): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE profileId = :profileId AND id = :id LIMIT 1")
    suspend fun getScheduleByIdAndProfile(profileId: String, id: String): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity)

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE profileId = :profileId AND id = :id")
    suspend fun deleteById(profileId: String, id: String)
}
