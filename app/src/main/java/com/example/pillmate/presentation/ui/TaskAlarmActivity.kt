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
        binding.tvMedDosage.text = details

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
