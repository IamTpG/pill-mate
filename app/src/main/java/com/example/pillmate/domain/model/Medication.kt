package com.example.pillmate.domain.model

import java.util.Date

data class Medication(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val unit: String = "tablet",
    val photoUrl: String? = null,
    @get:com.google.firebase.firestore.Exclude
    val supply: MedicationSupply? = null,
    val createdAt: java.util.Date = java.util.Date(),
    val updatedAt: java.util.Date = java.util.Date(),
    val deletedAt: java.util.Date? = null
)

data class MedicationSupply(
    val id: String = "",
    val batchName: String = "Main Bottle",
    val quantity: Float = 0f, // Inferred from logs in repository
    val expirationDate: Date? = null,
    val updatedAt: Date = Date()
)

data class InventoryLog(
    val id: String = "",
    val medId: String = "",
    val changeAmount: Float = 0f,
    val reason: String = "TAKEN", // TAKEN, REFILL, ADJUSTMENT
    val timestamp: Date = Date()
)
