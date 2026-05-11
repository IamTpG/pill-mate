package com.example.pillmate.data.local.dao
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.pillmate.data.local.entity.MedicalImageEntity

@Dao
interface ImageDao {
    @Query("SELECT * FROM medical_images WHERE profileId = :profileId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllImages(profileId: String): Flow<List<MedicalImageEntity>>

    @Query("SELECT * FROM medical_images WHERE profileId = :profileId AND deletedAt IS NULL")
    suspend fun getAllImagesOnce(profileId: String): List<MedicalImageEntity>
    @Query("SELECT * FROM medical_images WHERE profileId = :profileId AND id = :id LIMIT 1")
    suspend fun getImageById(profileId: String, id: String): MedicalImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: MedicalImageEntity)

    @Query("DELETE FROM medical_images WHERE profileId = :profileId AND id = :id")
    suspend fun deleteImage(profileId: String, id: String)
}