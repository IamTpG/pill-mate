package com.example.pillmate.data.repository

import com.example.pillmate.domain.model.Appointment
import com.example.pillmate.domain.model.AppointmentLog
import com.example.pillmate.domain.repository.AppointmentRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.*

class FirestoreAppointmentRepositoryImpl(private val firestore: FirebaseFirestore) :
    FirestoreRepositoryImpl<Appointment>(
        getCollectionReference = { profileId ->
            firestore.collection("profiles").document(profileId).collection("appointments")
        },
        modelClass = Appointment::class.java,
        setId = { appointment, id -> appointment.copy(id = id) }
    ),
    AppointmentRepository {

    override fun getId(item: Appointment): String {
        return item.id.ifEmpty { firestore.collection("tmp").document().id }
    }

    override fun getAppointmentLogs(profileId: String, date: Date): Flow<List<AppointmentLog>> = callbackFlow {
        // Path check: Ensure "profiles" and "appointments" match your Firebase Console exactly
        val query = firestore.collection("profiles")
            .document(profileId)
            .collection("appointments")
        
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            
            val logs = snapshot?.documents?.mapNotNull { doc ->
                try {
                    AppointmentLog(
                        id = doc.id,
                        name = doc.getString("name") ?: "Unnamed",
                        location = doc.getString("location") ?: "",
                        doctorName = doc.getString("doctorName") ?: "",
                        description = doc.getString("description") ?: ""
                    )
                } catch (e: Exception) {
                    null // Skip documents that don't match the model
                }
            } ?: emptyList()
            
            trySend(logs)
        }
        
        awaitClose { listener.remove() }
    }
}