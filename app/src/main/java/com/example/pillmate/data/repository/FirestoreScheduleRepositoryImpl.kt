package com.example.pillmate.data.repository

import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.repository.ScheduleRepository
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

class FirestoreScheduleRepositoryImpl(private val firestore: FirebaseFirestore) :
    FirestoreRepositoryImpl<Schedule>(
        getCollectionReference = { profileId ->
            firestore.collection("profiles").document(profileId).collection("schedules")
        },
        modelClass = Schedule::class.java,
        setId = { schedule, id -> schedule.copy(id = id) }
    ),
    ScheduleRepository {

    override fun getId(item: Schedule): String {
        return item.id.ifEmpty { firestore.collection("tmp").document().id }
    }

    override fun getAll(profileId: String): Flow<List<Schedule>> {
        return super.getAll(profileId).map { list ->
            list.filter { it.deletedAt == null }
        }
    }

    override suspend fun getAllOnce(profileId: String): Result<List<Schedule>> = runCatching {
        val list = super.getAllOnce(profileId).getOrThrow()
        list.filter { it.deletedAt == null }
    }

    override suspend fun remove(profileId: String, id: String): Result<Unit> = runCatching {
        val item = getById(profileId, id).getOrNull() ?: return@runCatching
        update(profileId, item.copy(deletedAt = java.util.Date())).getOrThrow()
    }
}
