package com.example.pillmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val name: String,
    val type: String, // TaskType enum as String
    val doseTimesJson: String, // List<DoseTime> as JSON
    val recurrenceRule: String?,
    val frequency: String?,
    val enabled: Boolean,
    val remindersJson: String, // List<Reminder> as JSON
    val eventSnapshotJson: String, // ScheduleEvent as JSON
    val startTime: String?,
    val endDate: Long?, // Date as timestamp
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)
