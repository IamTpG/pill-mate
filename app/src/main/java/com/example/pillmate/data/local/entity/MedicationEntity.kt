package com.example.pillmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val name: String,
    val description: String,
    val unit: String,
    val photoUrl: String? = null,
    val expirationDate: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
