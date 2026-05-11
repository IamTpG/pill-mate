package com.example.pillmate.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pillmate.domain.usecase.CheckLowStockUseCase
import com.google.firebase.auth.FirebaseAuth
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LowStockWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val auth: FirebaseAuth by inject()
    private val checkLowStockUseCase: CheckLowStockUseCase by inject()

    override suspend fun doWork(): Result {
        val profileId = auth.currentUser?.uid ?: return Result.success()
        return if (checkLowStockUseCase.execute(profileId).isSuccess) Result.success() else Result.retry()
    }
}
