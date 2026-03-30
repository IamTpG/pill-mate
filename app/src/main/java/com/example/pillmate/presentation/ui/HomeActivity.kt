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
import com.example.pillmate.databinding.ActivityHomeBinding
import com.example.pillmate.presentation.viewmodel.HomeViewModel
import com.example.pillmate.presentation.viewmodel.HomeViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val viewModel: HomeViewModel by viewModels {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val profileId = auth.currentUser?.uid ?: ""
        val medicationRepo = FirestoreMedicationRepository(db, profileId)
        val logRepo = FirestoreLogRepository(db)
        HomeViewModelFactory(medicationRepo, logRepo, profileId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupObservers()
        viewModel.loadData()

        binding.btnDebug.setOnClickListener {
            val auth = FirebaseAuth.getInstance()
            val profileId = auth.currentUser?.uid ?: "xY1SqtnTwiQqDkQDaZHGsZ6gHrh2" // Use provider's UID as fallback
            DebugMenuFragment(profileId).show(supportFragmentManager, "DebugMenu")
        }
    }

    private fun setupObservers() {
        viewModel.todayProgress.observe(this) { (completed, total) ->
            binding.tvProgressCount.text = "$completed/$total"
            if (total > 0) {
                binding.pbWeekly.progress = (completed.toFloat() / total * 100).toInt()
            } else {
                binding.pbWeekly.progress = 0
            }
        }
    }
}
