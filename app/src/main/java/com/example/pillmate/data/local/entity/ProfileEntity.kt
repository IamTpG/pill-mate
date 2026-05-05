package com.example.pillmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dateOfBirth: Long? = null,
    val healthInformation: String = "",
    val role: String,
    val isCurrent: Boolean = false,
    val hydrationGoal: Int = 2500
)
