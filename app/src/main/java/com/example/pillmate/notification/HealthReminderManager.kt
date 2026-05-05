package com.example.pillmate.notification

import android.content.Context
import android.content.SharedPreferences
import androidx.work.*
import com.example.pillmate.workers.HealthReminderWorker
import java.util.concurrent.TimeUnit

class HealthReminderManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("health_reminder_prefs", Context.MODE_PRIVATE)
    private val workManager = WorkManager.getInstance(context)

    companion object {
        const val KEY_HYDRATION_ENABLED = "hydration_enabled"
        const val KEY_HYDRATION_INTERVAL = "hydration_interval"
        const val KEY_BP_ENABLED = "bp_enabled"
        const val KEY_BP_INTERVAL = "bp_interval"
        const val KEY_WEIGHT_ENABLED = "weight_enabled"
        const val KEY_WEIGHT_INTERVAL = "weight_interval"

        const val TAG_HYDRATION = "HEALTH_REMINDER_HYDRATION"
        const val TAG_BP = "HEALTH_REMINDER_BP"
        const val TAG_WEIGHT = "HEALTH_REMINDER_WEIGHT"
    }

    fun isEnabled(type: String): Boolean = prefs.getBoolean("${type.lowercase()}_enabled", false)
    fun getInterval(type: String): Int = prefs.getInt("${type.lowercase()}_interval", 240) // Default 4 hours

    fun updateSetting(type: String, enabled: Boolean, intervalMinutes: Int) {
        prefs.edit().apply {
            putBoolean("${type.lowercase()}_enabled", enabled)
            putInt("${type.lowercase()}_interval", intervalMinutes)
            apply()
        }
        
        val tag = when (type) {
            "HYDRATION" -> TAG_HYDRATION
            "BLOOD_PRESSURE" -> TAG_BP
            "WEIGHT" -> TAG_WEIGHT
            else -> return
        }

        if (enabled) {
            scheduleWork(type, tag, intervalMinutes.toLong())
        } else {
            workManager.cancelAllWorkByTag(tag)
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
