package com.example.pillmate.domain.model
import java.util.Date

data class MedicalImage(
    val id: String = "",
    val localUri: String? = null,
    val remoteUrl: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val deletedAt: Date? = null
)