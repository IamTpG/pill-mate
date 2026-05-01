package com.example.pillmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.pillmate.data.local.entity.SupplyLogEntity

@Dao
interface SupplyLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSupplyLog(log: SupplyLogEntity): Long

    @Query("SELECT * FROM supply_logs WHERE medicationId = :medicationId ORDER BY timestamp DESC")
    fun getLogsForMedication(medicationId: String): Flow<List<SupplyLogEntity>>

    @Query("SELECT SUM(changeAmount) FROM supply_logs WHERE medicationId = :medicationId")
    fun getCurrentInventoryCount(medicationId: String): Flow<Int?>

    @Query("SELECT * FROM supply_logs ORDER BY timestamp DESC")
    fun observeAllLogs(): Flow<List<SupplyLogEntity>>

    @Query("DELETE FROM supply_logs WHERE medicationId = :medicationId")
    suspend fun deleteLogsForMedication(medicationId: String)
}
