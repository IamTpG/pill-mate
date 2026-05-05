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
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedications(medications: List<MedicationEntity>): List<Long>

    @Update
    suspend fun updateMedication(medication: MedicationEntity): Int

    @Delete
    suspend fun deleteMedication(medication: MedicationEntity): Int

    @Query("DELETE FROM medications WHERE id = :id AND profileId = :profileId")
    suspend fun deleteById(profileId: String, id: String)

    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE profileId = :profileId ORDER BY name ASC")
    suspend fun getAllMedicationsOnce(profileId: String): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getMedicationById(id: String): MedicationEntity?

    @Query("SELECT * FROM medications WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun getMedicationByIdAndProfile(profileId: String, id: String): MedicationEntity?

    @Query("SELECT * FROM medications WHERE profileId = :profileId ORDER BY name ASC")
    fun getMedicationsForProfile(profileId: String): Flow<List<MedicationEntity>>
}
