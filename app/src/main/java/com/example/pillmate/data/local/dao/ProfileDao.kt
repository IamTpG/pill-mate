package com.example.pillmate.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.pillmate.data.local.entity.ProfileEntity
import com.example.pillmate.data.local.entity.SavedAccountEntity

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

    @Query("DELETE FROM profiles WHERE role = :role")
    suspend fun deleteProfilesByRole(role: String)

    // --- Quản lý tài khoản ghi nhớ ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedAccount(account: SavedAccountEntity)

    @Query("SELECT * FROM saved_accounts")
    fun getSavedAccounts(): Flow<List<SavedAccountEntity>>

    @Query("DELETE FROM saved_accounts WHERE id = :id")
    suspend fun deleteSavedAccount(id: String)

    // Hàm xóa sạch dữ liệu profiles nhưng giữ lại saved_accounts
    @Query("DELETE FROM profiles")
    suspend fun clearAllProfiles()

    @Transaction
    suspend fun switchProfile(profileId: String) {
        clearCurrentProfile()
        setCurrentProfile(profileId)
    }
}