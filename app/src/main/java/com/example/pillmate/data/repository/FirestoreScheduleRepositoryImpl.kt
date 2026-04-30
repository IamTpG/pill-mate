package com.example.pillmate.data.repository

import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.repository.ScheduleRepository
import com.google.firebase.firestore.FirebaseFirestore

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
}
