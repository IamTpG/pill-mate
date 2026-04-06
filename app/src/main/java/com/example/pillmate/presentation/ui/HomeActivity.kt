package com.example.pillmate.presentation.ui

import android.content.Intent
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
import com.example.pillmate.data.remote.firebase.FirestoreScheduleRepository
import com.example.pillmate.databinding.ActivityHomeBinding
import com.example.pillmate.presentation.ui.adapter.CalendarAdapter
import com.example.pillmate.presentation.ui.adapter.TaskAdapter
import com.example.pillmate.presentation.viewmodel.HomeViewModel
import com.example.pillmate.presentation.viewmodel.HomeViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var calendarAdapter: CalendarAdapter

    private val viewModel: HomeViewModel by viewModels {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val profileId = auth.currentUser?.uid ?: ""
        val medicationRepo = FirestoreMedicationRepository(db, profileId)
        val logRepo = FirestoreLogRepository(db)
        val scheduleRepo = FirestoreScheduleRepository(db)
        HomeViewModelFactory(medicationRepo, logRepo, scheduleRepo, profileId)
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

        setupRecyclerView()
        setupObservers()
        viewModel.loadData()

        binding.btnDebug.setOnClickListener {
            val auth = FirebaseAuth.getInstance()
            val profileId = auth.currentUser?.uid ?: "xY1SqtnTwiQqDkQDaZHGsZ6gHrh2" // Use provider's UID as fallback
            DebugMenuFragment(profileId).show(supportFragmentManager, "DebugMenu")
        }

        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
            }
        }
    }

    private fun setupRecyclerView() {
        calendarAdapter = CalendarAdapter { day ->
            viewModel.selectDate(day.date)
        }
        binding.rvCalendar.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                this@HomeActivity,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = calendarAdapter
        }

        taskAdapter = TaskAdapter { task ->
            val intent = Intent(this, TaskAlarmActivity::class.java).apply {
                putExtra("MED_ID", task.medId)
                putExtra("SCHEDULE_ID", task.scheduleId)
                putExtra("MED_NAME", task.title)
                putExtra("DOSE_TEXT", task.doseDescription)
            }
            startActivity(intent)
        }
        binding.rvTasks.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@HomeActivity)
            adapter = taskAdapter
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

        viewModel.todayTasks.observe(this) { tasks ->
            taskAdapter.submitList(tasks)
        }

        viewModel.calendarDays.observe(this) { days ->
            calendarAdapter.submitList(days) {
                if (isFirstCalendarLoad) {
                    val selectedIndex = days.indexOfFirst { it.isSelected }
                    if (selectedIndex != -1) {
                        scrollToCalendarPosition(selectedIndex)
                    }
                    isFirstCalendarLoad = false
                }
            }
        }
    }

    private var isFirstCalendarLoad = true

    private fun scrollToCalendarPosition(position: Int) {
        val layoutManager = binding.rvCalendar.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
        val screenWidth = resources.displayMetrics.widthPixels
        
        val itemWidthPx = resources.getDimensionPixelSize(R.dimen.calendar_item_width)
        val marginPx = resources.getDimensionPixelSize(R.dimen.calendar_item_margin)
        val totalWidthPx = itemWidthPx + (marginPx * 2)
        
        val offset = (screenWidth / 2) - (totalWidthPx / 2)
        layoutManager?.scrollToPositionWithOffset(position, offset)
    }
}
