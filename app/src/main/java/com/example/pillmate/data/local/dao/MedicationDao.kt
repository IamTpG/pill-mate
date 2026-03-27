package com.example.pillmate.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.example.pillmate.data.local.entity.MedicationEntity

@Dao
interface MedicationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMedication(medication: MedicationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMedications(medications: List<MedicationEntity>): List<Long>

    @Update
    fun updateMedication(medication: MedicationEntity): Int

    @Delete
    fun deleteMedication(medication: MedicationEntity): Int

    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    fun getMedicationById(id: String): MedicationEntity?
}
