package com.example.pillmate.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.notification.TaskNotificationManager
import com.google.firebase.auth.FirebaseAuth
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LowStockWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val medicationRepository: MedicationRepository by inject()
    private val auth: FirebaseAuth by inject()
    private val notificationManager: TaskNotificationManager by inject()

    override suspend fun doWork(): Result {
        val profileId = auth.currentUser?.uid ?: return Result.success()

        val medicationsResult = medicationRepository.getAllMedications(profileId)
        val medications = medicationsResult.getOrNull() ?: return Result.retry()

        medications.forEach { medication ->
            val supply = medication.supply
            if (supply != null && supply.quantity < 5.0f) {
                notificationManager.showLowStockNotification(
                    medName = medication.name,
                    remaining = supply.quantity
                )
            }
        }

        return Result.success()
    }
}
