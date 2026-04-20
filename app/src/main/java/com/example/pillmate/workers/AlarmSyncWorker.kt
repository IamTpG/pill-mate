package com.example.pillmate.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pillmate.domain.usecase.SyncAlarmsUseCase
import com.google.firebase.auth.FirebaseAuth
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AlarmSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val syncAlarmsUseCase: SyncAlarmsUseCase by inject()
    private val auth: FirebaseAuth by inject()

    override suspend fun doWork(): Result {
        val profileId = auth.currentUser?.uid ?: return Result.success()

        return try {
            syncAlarmsUseCase(profileId)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
