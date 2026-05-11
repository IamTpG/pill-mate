package com.example.pillmate.data.mapper

import com.example.pillmate.data.local.entity.SupplyLogEntity
import com.example.pillmate.domain.model.SupplyLog
import java.util.Date

fun SupplyLogEntity.toDomainModel(): SupplyLog {
    return SupplyLog(
        id = id,
        medId = medicationId,
        changeAmount = changeAmount,
        reason = reason,
        createdAt = Date(createdAt),
        updatedAt = Date(updatedAt),
        deletedAt = deletedAt?.let { Date(it) }
    )
}

fun SupplyLog.toEntity(): SupplyLogEntity {
    return SupplyLogEntity(
        id = id,
        medicationId = medId,
        changeAmount = changeAmount,
        reason = reason,
        createdAt = createdAt.time,
        updatedAt = updatedAt.time,
        deletedAt = deletedAt?.time
    )
}
