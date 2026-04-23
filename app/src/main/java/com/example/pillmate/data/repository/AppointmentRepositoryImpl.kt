package com.example.pillmate.data.repository

import com.example.pillmate.domain.model.AppointmentLog
import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.repository.AppointmentRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.*

class AppointmentRepositoryImpl(private val firestore: FirebaseFirestore) : AppointmentRepository {
	override fun getAppointmentLogs(profileId: String, date: Date): Flow<List<AppointmentLog>> = callbackFlow {
		val calendar = Calendar.getInstance().apply { time = date }
		calendar.set(Calendar.HOUR_OF_DAY, 0)
		val start = calendar.time
		calendar.set(Calendar.HOUR_OF_DAY, 23)
		val end = calendar.time
		
		val listener = firestore.collection("profiles")
			.document(profileId)
			.collection("appointments")
			.whereGreaterThanOrEqualTo("scheduledTime", Timestamp(start))
			.whereLessThanOrEqualTo("scheduledTime", Timestamp(end))
			.addSnapshotListener { snapshot, _ ->
				val logs = snapshot?.documents?.mapNotNull { doc ->
					val event = doc.get("eventSnapshot") as? Map<*, *>
					AppointmentLog(
						id = doc.id,
						status = LogStatus.valueOf(doc.getString("status") ?: "PENDING"),
						scheduledTime = doc.getTimestamp("scheduledTime")?.toDate() ?: Date(),
						title = event?.get("title") as? String ?: "No Title",
						instructions = event?.get("instructions") as? String ?: "",
						location = event?.get("location") as? String,
						doctorName = event?.get("doctorName") as? String
					)
				} ?: emptyList()
				trySend(logs)
			}
		awaitClose { listener.remove() }
	}
}