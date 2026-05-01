package com.example.pillmate.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.domain.usecase.CheckLowStockUseCase
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
    private val checkLowStockUseCase: CheckLowStockUseCase by inject()

    override suspend fun doWork(): Result {
        val profileId = auth.currentUser?.uid ?: return Result.success()

        val medicationsResult = medicationRepository.getAllOnce(profileId)
        val medications = medicationsResult.getOrNull() ?: return Result.retry()

        medications.forEach { medication ->
            val result = checkLowStockUseCase.execute(profileId, medication.id)
            if (result.isLow) {
                notificationManager.showLowStockNotification(
                    medName = medication.name,
                    remaining = result.remainingStock
                )
            }
        }

        return Result.success()
    }
}
