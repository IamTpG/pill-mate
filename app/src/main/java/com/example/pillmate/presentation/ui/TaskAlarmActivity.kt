package com.example.pillmate.presentation.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pillmate.R

import android.widget.Toast
import androidx.activity.viewModels
import com.example.pillmate.data.remote.firebase.FirestoreLogRepository
import com.example.pillmate.data.remote.firebase.FirestoreMedicationRepository
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.presentation.viewmodel.TaskLogViewModel
import com.example.pillmate.databinding.ActivityTaskAlarmBinding
import com.example.pillmate.notification.TaskNotificationManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Date

class TaskAlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskAlarmBinding
    
    private val viewModel: TaskLogViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTaskAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Wake screen and show over lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Set UI from Intent
        val title = intent.getStringExtra("TITLE") ?: "Task"
        val details = intent.getStringExtra("DETAILS") ?: ""
        val taskTypeString = intent.getStringExtra("TASK_TYPE") ?: "OTHER"
        val taskType = try { TaskType.valueOf(taskTypeString) } catch (e: Exception) { TaskType.OTHER }

        binding.tvMedName.text = title
        val instructions = intent.getStringExtra("EXTRA_INSTRUCTIONS") ?: ""
        val startTimeStr = intent.getStringExtra("EXTRA_START_TIME")
        val rrule = intent.getStringExtra("EXTRA_RRULE")

        binding.tvMedCategory.text = instructions.ifBlank { "no description" }
        binding.tvMedDosage.text = details

        // Time calculations
        val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val displayFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val fallbackFormat = java.text.SimpleDateFormat("H:m", java.util.Locale.getDefault())

        val parsedStart: java.util.Date? = try {
            when {
                startTimeStr?.contains("T") == true -> isoFormat.parse(startTimeStr)
                !startTimeStr.isNullOrBlank() -> {
                    try { displayFormat.parse(startTimeStr) } 
                    catch (e: Exception) { fallbackFormat.parse(startTimeStr) }
                }
                else -> null
            }
        } catch (e: Exception) {
            android.util.Log.e("TaskAlarmActivity", "Parse error for $startTimeStr", e)
            null
        }

        binding.tvScheduledTime.text = if (parsedStart != null) "Scheduled: ${displayFormat.format(parsedStart)}" else "Scheduled: ${startTimeStr ?: "--:--"}"

        if (!rrule.isNullOrBlank() && parsedStart != null) {
            val cal = java.util.Calendar.getInstance()
            cal.time = parsedStart
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1) // Default to +24h for simple daily display
            binding.tvNextTime.text = "Next: ${displayFormat.format(cal.time)}"
            binding.ivArrowNext.visibility = android.view.View.VISIBLE
        } else {
            binding.tvNextTime.text = ""
            binding.ivArrowNext.visibility = android.view.View.GONE
        }
        
        android.util.Log.d("TaskAlarmActivity", "Data: $title, $startTimeStr, $rrule")

        // Change button text based on task type
        binding.btnLogDose.text = when (taskType) {
            TaskType.MEDICATION -> "TAKE NOW"
            TaskType.APPOINTMENT -> "ATTEND"
            TaskType.EXERCISE -> "START"
            else -> "COMPLETE"
        }

        // Setup observers
        viewModel.logResult.observe(this) { result ->
            if (result != null) {
                if (result.isSuccess) {
                    Toast.makeText(this, "Task completed!", Toast.LENGTH_SHORT).show()
                    TaskNotificationManager(this).dismissNotification()
                    finish()
                } else {
                    Toast.makeText(this, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Handle Click - Action
        binding.btnLogDose.setOnClickListener {
            val sourceId = intent.getStringExtra("SOURCE_ID") ?: "mock_id"
            val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: sourceId
            val scheduledTime = Date()
            val dose = 1.0f
            
            viewModel.onTakeClicked(sourceId, scheduleId, taskType, scheduledTime, dose)
        }

        // Handle Click - Skip
        binding.btnSkip.setOnClickListener {
            val sourceId = intent.getStringExtra("SOURCE_ID") ?: "mock_id"
            val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: sourceId
            val scheduledTime = Date()
            
            viewModel.onSkipClicked(sourceId, scheduleId, taskType, scheduledTime)
        }

        // Handle Click - Snooze
        binding.btnSnooze.setOnClickListener {
            val sourceId = intent.getStringExtra("SOURCE_ID") ?: "mock_id"
            val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: sourceId
            val scheduledTime = Date()
            
            viewModel.onSnoozeClicked(sourceId, scheduleId, taskType, scheduledTime)
        }
    }
}
