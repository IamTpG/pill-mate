package com.example.pillmate.presentation.ui

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
import com.example.pillmate.domain.usecase.LogMedicationUseCase
import com.example.pillmate.presentation.viewmodel.MedicationLogViewModel
import com.example.pillmate.databinding.ActivityTaskAlarmBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Date

class TaskAlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskAlarmBinding
    
    private val viewModel: MedicationLogViewModel by viewModel()

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

        // Set UI from Intent
        binding.tvMedName.text = intent.getStringExtra("MED_NAME") ?: "Medication"
        binding.tvMedDosage.text = intent.getStringExtra("DOSE_TEXT") ?: "1.0 Dose"

        // Setup observers
        viewModel.logResult.observe(this) { result ->
            if (result != null) {
                if (result.isSuccess) {
                    Toast.makeText(this, "Dose logged successfully!", Toast.LENGTH_SHORT).show()
                    com.example.pillmate.notification.MedicationNotificationManager(this).dismissNotification()
                    finish()
                } else {
                    Toast.makeText(this, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Handle Click - Take
        binding.btnLogDose.setOnClickListener {
            val medId = intent.getStringExtra("MED_ID") ?: "mock_med_id"
            val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: medId
            val scheduledTime = Date()
            val dose = 1.0f
            
            viewModel.onTakeClicked(medId, scheduleId, scheduledTime, dose)
        }

        // Handle Click - Skip
        binding.btnSkip.setOnClickListener {
            val medId = intent.getStringExtra("MED_ID") ?: "mock_med_id"
            val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: medId
            val scheduledTime = Date()
            
            viewModel.onSkipClicked(medId, scheduleId, scheduledTime)
        }

        // Handle Click - Snooze
        binding.btnSnooze.setOnClickListener {
            val medId = intent.getStringExtra("MED_ID") ?: "mock_med_id"
            val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: medId
            val scheduledTime = Date()
            
            viewModel.onSnoozeClicked(medId, scheduleId, scheduledTime)
        }
    }
}
