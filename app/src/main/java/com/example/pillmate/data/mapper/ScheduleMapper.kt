package com.example.pillmate.data.mapper

import com.example.pillmate.data.local.entity.ScheduleEntity
import com.example.pillmate.domain.model.DoseTime
import com.example.pillmate.domain.model.Reminder
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.model.ScheduleEvent
import com.example.pillmate.domain.model.TaskType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

private val gson = Gson()

fun ScheduleEntity.toDomainModel(): Schedule {
    val doseTimesType = object : TypeToken<List<DoseTime>>() {}.type
    val doseTimesList: List<DoseTime> = try {
        gson.fromJson(doseTimesJson, doseTimesType) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    val remindersType = object : TypeToken<List<Reminder>>() {}.type
    val remindersList: List<Reminder> = try {
        gson.fromJson(remindersJson, remindersType) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    val parsedEvent: ScheduleEvent = try {
        gson.fromJson(eventSnapshotJson, ScheduleEvent::class.java) ?: ScheduleEvent()
    } catch (e: Exception) {
        ScheduleEvent()
    }

    return Schedule(
        id = id,
        name = name,
        type = try { TaskType.valueOf(type) } catch (e: Exception) { TaskType.MEDICATION },
        doseTimes = doseTimesList,
        recurrenceRule = recurrenceRule,
        frequency = frequency,
        enabled = enabled,
        reminders = remindersList,
        eventSnapshot = parsedEvent,
        startTime = startTime,
        endDate = endDate?.let { Date(it) },
        createdAt = Date(createdAt),
        updatedAt = Date(updatedAt),
        deletedAt = deletedAt?.let { Date(it) }
    )
}

fun Schedule.toEntity(profileId: String): ScheduleEntity {
    return ScheduleEntity(
        id = id,
        profileId = profileId,
        name = name,
        type = type.name,
        doseTimesJson = gson.toJson(doseTimes),
        recurrenceRule = recurrenceRule,
        frequency = frequency,
        enabled = enabled,
        remindersJson = gson.toJson(reminders),
        eventSnapshotJson = gson.toJson(eventSnapshot),
        startTime = startTime,
        endDate = endDate?.time,
        createdAt = createdAt.time,
        updatedAt = updatedAt.time,
        deletedAt = deletedAt?.time
    )
}
