package com.example.pillmate.notification

import android.content.Context
import androidx.work.*
import com.example.pillmate.data.local.dao.ProfileDao
import com.example.pillmate.data.local.entity.ProfileEntity
import com.example.pillmate.workers.HealthReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class HealthReminderManager(
    private val context: Context,
    private val profileDao: ProfileDao
) {
    private val workManager = WorkManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val TAG_HYDRATION = "HEALTH_REMINDER_HYDRATION"
        const val TAG_BP = "HEALTH_REMINDER_BP"
        const val TAG_WEIGHT = "HEALTH_REMINDER_WEIGHT"
    }

    init {
        scope.launch {
            profileDao.getCurrentProfileFlow().collect { profile ->
                profile?.let { syncWithProfile(it) }
            }
        }
    }

    private fun syncWithProfile(profile: ProfileEntity) {
        // Hydration
        updateWork("HYDRATION", TAG_HYDRATION, profile.hydrationReminderEnabled, profile.hydrationInterval)
        // BP
        updateWork("BLOOD_PRESSURE", TAG_BP, profile.bpReminderEnabled, profile.bpInterval)
        // Weight
        updateWork("WEIGHT", TAG_WEIGHT, profile.weightReminderEnabled, profile.weightInterval)
    }

    private fun updateWork(type: String, tag: String, enabled: Boolean, intervalMinutes: Int) {
        if (enabled) {
            scheduleWork(type, tag, intervalMinutes.toLong())
        } else {
            workManager.cancelUniqueWork(tag)
        }
    }

    private fun scheduleWork(type: String, tag: String, intervalMinutes: Long) {
        val workRequest = PeriodicWorkRequestBuilder<HealthReminderWorker>(
            intervalMinutes, TimeUnit.MINUTES
        )
            .addTag(tag)
            .setInputData(workDataOf("METRIC_TYPE" to type))
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            tag,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
