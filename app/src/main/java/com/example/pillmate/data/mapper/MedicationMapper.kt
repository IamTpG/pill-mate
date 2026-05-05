package com.example.pillmate.data.mapper

import com.example.pillmate.data.local.entity.MedicationEntity
import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.model.MedicationSupply
import java.util.Date

fun MedicationEntity.toDomainModel(inventory: Int = 0): Medication {
    return Medication(
        id = id,
        name = name,
        description = description,
        unit = unit,
        photoUrl = photoUrl,
        supply = MedicationSupply(
            id = "main",
            batchName = "Main Batch",
            quantity = inventory.toFloat(),
            expirationDate = if (expirationDate > 0) Date(expirationDate) else null
        ),
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
        expirationDate = supply?.expirationDate?.time ?: 0L,
        createdAt = createdAt.time,
        updatedAt = updatedAt.time,
        deletedAt = deletedAt?.time
    )
}