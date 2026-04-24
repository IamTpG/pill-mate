package com.example.pillmate.data.repository

import com.example.pillmate.domain.model.Appointment
import com.example.pillmate.domain.model.AppointmentLog
import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.repository.AppointmentRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class AppointmentRepositoryImpl(private val firestore: FirebaseFirestore) : AppointmentRepository {
	override fun getAppointmentLogs(profileId: String, date: Date): Flow<List<AppointmentLog>> = callbackFlow {
		// 1. Path check: Ensure "profiles" and "appointments" match your Firebase Console exactly
		val query = firestore.collection("profiles")
			.document(profileId)
			.collection("appointments")
		
		val listener = query.addSnapshotListener { snapshot, error ->
			if (error != null) {
				// Log the error - critical for debugging Firestore
				close(error)
				return@addSnapshotListener
			}
			
			val logs = snapshot?.documents?.mapNotNull { doc ->
				try {
					// If data is at the root of the document:
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
		
		// Clean up the listener when the Flow is cancelled or ViewModel is cleared
		awaitClose { listener.remove() }
	}
	
	override suspend fun addAppointment(profileId: String, appointment: Appointment): Result<Unit> {
		return try {
			// Path: profiles/{profileId}/appointment
			firestore.collection("profiles")
				.document(profileId)
				.collection("appointments")
				.add(appointment) // Firebase generates a unique ID for the document
				.await() // From kotlinx-coroutines-play-services
			
			Result.success(Unit)
		} catch (e: Exception) {
			Result.failure(e)
		}
	}
}