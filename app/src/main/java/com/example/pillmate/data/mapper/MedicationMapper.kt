package com.example.pillmate.data.mapper

import com.example.pillmate.data.local.entity.MedicationEntity
import com.example.pillmate.domain.model.Medication

import com.example.pillmate.domain.model.MedicationSupply
import java.util.Date

fun MedicationEntity.toDomainModel(inventory: Int): Medication {
    return Medication(
        id = id,
        name = name,
        description = description,
        unit = unit,
        photoUrl = photoUrl,
        supply = MedicationSupply(
            quantity = inventory.toFloat(),
            expirationDate = Date(expirationDate)
        )
    )
}

fun Medication.toEntity(): MedicationEntity {
    return MedicationEntity(
        id = id,
        profileId = "", // Legacy field for local DB
        name = name,
        description = description ?: "",
        unit = unit,
        photoUrl = photoUrl,
        expirationDate = supply?.expirationDate?.time ?: 0L
    )
}