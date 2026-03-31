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
import com.example.pillmate.presentation.viewmodel.MedicationLogViewModelFactory
import com.example.pillmate.databinding.ActivityTaskAlarmBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class TaskAlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskAlarmBinding
    
    // In a real app, these would come from DI
    private val viewModel: MedicationLogViewModel by viewModels {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val profileId = auth.currentUser?.uid ?: ""
        val medicationRepo = FirestoreMedicationRepository(db, profileId)
        val logRepo = FirestoreLogRepository(db)
        val useCase = LogMedicationUseCase(medicationRepo, logRepo)
        MedicationLogViewModelFactory(useCase, profileId)
    }

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

        // Handle Click
        binding.btnLogDose.setOnClickListener {
            // Mocking medId and scheduledTime for now
            // In reality, these are passed via Intent
            val medId = intent.getStringExtra("MED_ID") ?: "mock_med_id"
            val scheduledTime = Date() // Mock
            val dose = 1.0f // Mock
            
            viewModel.onTakeClicked(medId, scheduledTime, dose)
        }
    }
}
