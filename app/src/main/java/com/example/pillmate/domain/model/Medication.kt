package com.example.pillmate.domain.model

import com.google.firebase.firestore.Exclude
import java.util.Date

data class Medication(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val unit: String = "tablet",
    val photoUrl: String? = null,
    val expirationDate: Date? = null,
    @get:Exclude
    val quantity: Float = 0f, // Inferred from logs in repository
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val deletedAt: Date? = null
)

data class SupplyLog(
    val id: String = "",
    val medId: String = "",
    val changeAmount: Float = 0f,
    val reason: String = "TAKEN", // TAKEN, REFILL, ADJUSTMENT
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val deletedAt: Date? = null
)
