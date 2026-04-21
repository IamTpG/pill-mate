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
                    val batchNames = listOf("Main Bottle", "Travel Pack", "Medicine Cabinet")
                    val numBatches = (2..3).random()
                    
                    for (i in 0 until numBatches) {
                        val supplyData = mapOf(
                            "batchName" to batchNames[i],
                            "updatedAt" to Timestamp.now()
                        )
                        val supplyRef = itemRef.collection("supply").add(supplyData).await()
                        
                        // Different stock levels to test smart selection
                        val initialStock = if (i == 0) 20.0 else 5.0 * (i + 1)
                        supplyRef.collection("logs").add(mapOf(
                            "changeAmount" to initialStock,
                            "reason" to "INITIAL",
                            "timestamp" to Timestamp.now()
                        )).await()
                    }
                }

                // Distribute times: 8am, 12pm, 4pm, 8pm across items
                val hour = 8 + (index * 4) + (type.ordinal % 3)
                cal.set(Calendar.HOUR_OF_DAY, hour % 24)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val isoStartTime = isoFormat.format(cal.time)

                val schedule = Schedule(
                    type = type,
                    startTime = isoStartTime,
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
        
        // Delete Medications and their nested subcollections (supply -> logs)
        val medications = profileRef.collection("medications").get().await()
        for (medDoc in medications.documents) {
            val supplies = medDoc.reference.collection("supply").get().await()
            for (supplyDoc in supplies.documents) {
                // Delete logs under each supply
                deleteCollection(supplyDoc.reference.collection("logs"))
                supplyDoc.reference.delete().await()
            }
            medDoc.reference.delete().await()
        }

        // Delete other top-level subcollections
        deleteCollection(profileRef.collection("meals"))
        deleteCollection(profileRef.collection("appointments"))
        deleteCollection(profileRef.collection("exercises"))
        deleteCollection(profileRef.collection("tasks"))
        deleteCollection(profileRef.collection("schedules"))
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
