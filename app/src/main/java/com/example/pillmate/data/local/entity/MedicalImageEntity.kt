package com.example.pillmate.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medical_images")
data class MedicalImageEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val localUri: String?,
    val remoteUrl: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)