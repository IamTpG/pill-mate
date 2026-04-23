package com.example.pillmate.util

import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.model.ScheduleEvent
import com.example.pillmate.domain.model.TaskType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class DataGenerator(private val db: FirebaseFirestore) {

    suspend fun generateSampleData(profileId: String) {
        val profileRef = db.collection("profiles").document(profileId)
        val cal = Calendar.getInstance()
        val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())

        val categories = listOf(
            Triple("medications", TaskType.MEDICATION, listOf("Lisinopril", "Multivitamin", "Metformin")),
            Triple("meals", TaskType.MEAL, listOf("Breakfast", "Lunch", "Dinner")),
            Triple("appointments", TaskType.APPOINTMENT, listOf("Dentist", "General Checkup", "Therapy")),
            Triple("exercises", TaskType.EXERCISE, listOf("Pushups", "Run", "Yoga")),
            Triple("tasks", TaskType.OTHER, listOf("Drink Water", "Walk Dog", "Read Book"))
        )

        for ((collectionPath, type, names) in categories) {
            for ((index, name) in names.withIndex()) {
                val itemData = mapOf("name" to name, "description" to "Sample $name")
                val itemRef = profileRef.collection(collectionPath).add(itemData).await()

                if (collectionPath == "medications") {
                    val supplyRef = itemRef.collection("supply").add(mapOf("updatedAt" to Timestamp.now())).await()
                    supplyRef.collection("logs").add(mapOf(
                        "changeAmount" to 30.0,
                        "reason" to "INITIAL",
                        "timestamp" to Timestamp.now()
                    )).await()
                }

                // Distribute times: 8am, 12pm, 4pm, 8pm across items
                val hour = 8 + (index * 4) + (type.ordinal % 3)
                cal.set(Calendar.HOUR_OF_DAY, hour % 24)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val isoStartTime = isoFormat.format(cal.time)

                val schedule = Schedule(
                    type = type,
                    doseTimes = listOf(com.example.pillmate.domain.model.DoseTime(time = isoStartTime)),
                    recurrenceRule = "FREQ=DAILY",
                    reminders = listOf(
                        com.example.pillmate.domain.model.Reminder(0, com.example.pillmate.domain.model.ReminderType.ALARM),
                        com.example.pillmate.domain.model.Reminder(10, com.example.pillmate.domain.model.ReminderType.NOTIFICATION)
                    ),
                    eventSnapshot = ScheduleEvent(
                        sourceId = itemRef.id,
                        title = name,
                        dose = 1.0f,
                        unit = if (collectionPath == "medications") "pill" else null
                    )
                )
                profileRef.collection("schedules").add(schedule).await()
            }
        }
    }

    suspend fun clearUserData(profileId: String) {
        val profileRef = db.collection("profiles").document(profileId)
        
        // Delete Categories
        deleteCollection(profileRef.collection("medications"))
        deleteCollection(profileRef.collection("meals"))
        deleteCollection(profileRef.collection("appointments"))
        deleteCollection(profileRef.collection("exercises"))
        deleteCollection(profileRef.collection("tasks"))
        
        // Delete Schedules
        deleteCollection(profileRef.collection("schedules"))
        
        // Delete Logs
        deleteCollection(profileRef.collection("logs"))
    }

    private suspend fun deleteCollection(collectionRef: com.google.firebase.firestore.CollectionReference) {
        val snapshot = collectionRef.get().await()
        for (doc in snapshot.documents) {
            // Firestore doesn't delete subcollections automatically, 
            // but for a simple debug tool, we just delete the top docs.
            doc.reference.delete().await()
        }
    }
}
