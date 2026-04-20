package com.example.pillmate

import android.app.Application
import com.example.pillmate.di.appModule
import com.example.pillmate.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule, viewModelModule)
        }
        
        scheduleWorkers()
    }

    private fun scheduleWorkers() {
        val workManager = androidx.work.WorkManager.getInstance(this)
        val lowStockRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.pillmate.workers.LowStockWorker>(
            1, java.util.concurrent.TimeUnit.DAYS
        ).setConstraints(
            androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "LowStockCheck",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            lowStockRequest
        )
    }
}
