package com.example.pillmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supply_logs")
data class SupplyLogEntity(
    @PrimaryKey
    val id: String, 
    val medicationId: String, 
    val changeAmount: Float,
    val reason: String,
    val timestamp: Long 
)