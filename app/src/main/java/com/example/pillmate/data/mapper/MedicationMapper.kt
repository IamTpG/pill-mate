package com.example.pillmate.data.mapper

import com.example.pillmate.data.local.entity.MedicationEntity
import com.example.pillmate.domain.model.Medication
import java.util.Date

fun MedicationEntity.toDomainModel(inventory: Float = 0f): Medication {
    return Medication(
        id = id,
        name = name,
        description = description,
        unit = unit,
        photoUrl = photoUrl,
        expirationDate = if (expirationDate > 0) Date(expirationDate) else null,
        quantity = inventory,
        createdAt = Date(createdAt),
        updatedAt = Date(updatedAt),
        deletedAt = deletedAt?.let { Date(it) }
    )
}

fun Medication.toEntity(profileId: String): MedicationEntity {
    return MedicationEntity(
        id = id,
        profileId = profileId,
        name = name,
        description = description ?: "",
        unit = unit,
        photoUrl = photoUrl,
        expirationDate = expirationDate?.time ?: 0L,
        createdAt = createdAt.time,
        updatedAt = updatedAt.time,
        deletedAt = deletedAt?.time
    )
}