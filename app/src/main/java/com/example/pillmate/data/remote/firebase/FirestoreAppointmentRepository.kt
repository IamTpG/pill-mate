package com.example.pillmate.data.remote.firebase

import androidx.annotation.Nullable
import com.example.pillmate.domain.model.Appointment
import com.example.pillmate.domain.repository.AppointmentRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await

class FirestoreAppointmentRepository(
	val profileId: String,
	val db: FirebaseFirestore
): AppointmentRepository {
	
	private val collectionRef = db.collection("profiles").document(profileId)
		.collection("appointments")
	
	override suspend fun saveAppointment(
		appointment: Appointment
	): Result<String> {
		return try {
			val appointmentDocRef = collectionRef.add(appointment).await()
			
			Result.success(appointmentDocRef.id)
			} catch (e: Exception) {
			Result.failure(e)
		}
	}
	
	override suspend fun getAppointment(appointmentId: String): Result<Appointment?> {
		return try {
			val appointmentDocRef = collectionRef.document(appointmentId).get().await()
			
			if (appointmentDocRef.exists()) {
				val appointment = appointmentDocRef.toObject(Appointment::class.java)?.copy(id = appointmentDocRef.id)
				
				Result.success(appointment)
			} else {
				Result.success(null)
			}
		} catch (e: Exception) {
			Result.failure(e)
		}
	}
	
	override suspend fun getAllAppointments(): Result<List<Appointment>> {
		return try {
			val snapshot = collectionRef.get().await()
			
			val appointments = snapshot.documents.mapNotNull { doc ->
				try {
					Appointment(
						id = doc.id,
						name = doc.getString("name") ?: "",
						location = doc.getString("location") ?: "",
						doctorName = doc.getString("doctorName") ?: "",
						description = doc.getString("description") ?: ""
					)
				} catch (e: Exception) {
					null
				}
			}
			
			Result.success(appointments)
		} catch(e: Exception) {
			Result.failure(e)
		}
	}
}