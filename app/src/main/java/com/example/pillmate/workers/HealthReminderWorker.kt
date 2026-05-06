package com.example.pillmate.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pillmate.notification.TaskNotificationManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HealthReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val notificationManager: TaskNotificationManager by inject()

    override suspend fun doWork(): Result {
        val type = inputData.getString("METRIC_TYPE") ?: return Result.failure()
        notificationManager.showHealthNotification(type)
        return Result.success()
    }
}
