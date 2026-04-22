package com.example.pillmate.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.pillmate.data.local.entity.ProfileEntity

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE isCurrent = 1 LIMIT 1")
    fun getCurrentProfileFlow(): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("UPDATE profiles SET isCurrent = 0")
    suspend fun clearCurrentProfile()

    @Query("UPDATE profiles SET isCurrent = 1 WHERE id = :profileId")
    suspend fun setCurrentProfile(profileId: String)

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Query("DELETE FROM profiles WHERE id = :profileId")
    suspend fun deleteProfileById(profileId: String)

    @Query("SELECT * FROM profiles WHERE isCurrent = 1 LIMIT 1")
    suspend fun getActiveProfile(): ProfileEntity?

    @Transaction
    suspend fun switchProfile(profileId: String) {
        clearCurrentProfile()
        setCurrentProfile(profileId)
    }
}