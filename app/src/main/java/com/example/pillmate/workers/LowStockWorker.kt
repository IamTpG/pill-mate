package com.example.pillmate.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.domain.usecase.CalculateDailyIntakeUseCase
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
    private val calculateDailyIntakeUseCase: CalculateDailyIntakeUseCase by inject()

    override suspend fun doWork(): Result {
        val profileId = auth.currentUser?.uid ?: return Result.success()

        val medicationsResult = medicationRepository.getAllOnce(profileId)
        val medications = medicationsResult.getOrNull() ?: return Result.retry()
        
        // Calculate planned intake for today
        val todayIntake = calculateDailyIntakeUseCase.execute(profileId, java.util.Date())

        medications.forEach { medication ->
            val dailyRequirement = todayIntake[medication.id] ?: 0f
            
            // Check if quantity is enough for today's planned doses
            // If dailyRequirement is 0 (not in use today), we don't notify unless we want a static fallback (omitted per PR request)
            if (dailyRequirement > 0f && medication.quantity < dailyRequirement) {
                notificationManager.showLowStockNotification(
                    medName = medication.name,
                    remaining = medication.quantity
                )
            }
        }

        return Result.success()
    }
}
