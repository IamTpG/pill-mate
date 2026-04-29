package com.example.pillmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_accounts")
data class SavedAccountEntity(
    @PrimaryKey val id: String, // Firebase UID
    val email: String,
    val name: String,
    val loginMethod: String, // "GOOGLE" hoặc "EMAIL"
    val password: String? = null
)
