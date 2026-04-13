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

        // 1. Create Medications
        val meds = listOf(
            mapOf("name" to "Lisinopril", "unit" to "pill", "description" to "Blood pressure"),
            mapOf("name" to "Multivitamin", "unit" to "capsule", "description" to "Daily health"),
            mapOf("name" to "Metformin", "unit" to "pill", "description" to "Diabetes")
        )

        val cal = Calendar.getInstance()
        val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())

        for (medData in meds) {
            val medRef = profileRef.collection("medications").add(medData).await()
            
            // 2. Create Supply & Initial Log (REFILL +30)
            val supplyRef = medRef.collection("supply").add(mapOf("updatedAt" to Timestamp.now())).await()
            supplyRef.collection("logs").add(mapOf(
                "changeAmount" to 30.0,
                "reason" to "REFILL",
                "timestamp" to Timestamp.now()
            )).await()

            // 3. Create Schedules
            val scheduleHour = when(medData["name"]) {
                "Lisinopril" -> 8
                "Multivitamin" -> 9
                else -> 13
            }
            
            cal.set(Calendar.HOUR_OF_DAY, scheduleHour)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val isoStartTime = isoFormat.format(cal.time)

            val schedule = Schedule(
                type = TaskType.MEDICATION,
                startTime = isoStartTime,
                recurrenceRule = "FREQ=DAILY",
                eventSnapshot = ScheduleEvent(
                    sourceId = medRef.id,
                    title = medData["name"] as String,
                    dose = 1.0f,
                    unit = medData["unit"] as? String
                )
            )
            profileRef.collection("schedules").add(schedule).await()
        }
    }

    suspend fun clearUserData(profileId: String) {
        val profileRef = db.collection("profiles").document(profileId)
        
        // Delete Medications (and subcollections)
        deleteCollection(profileRef.collection("medications"))
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
