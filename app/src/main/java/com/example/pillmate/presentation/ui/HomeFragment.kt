package com.example.pillmate.presentation.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pillmate.R

import androidx.fragment.app.Fragment
import com.example.pillmate.databinding.FragmentHomeBinding
import com.example.pillmate.presentation.ui.adapter.CalendarAdapter
import com.example.pillmate.presentation.ui.adapter.TaskAdapter
import com.example.pillmate.presentation.viewmodel.HomeViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var calendarAdapter: CalendarAdapter
    
    private val viewModel: HomeViewModel by viewModel()

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                Toast.makeText(requireContext(), "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupObservers()
        viewModel.loadData()

        binding.btnDebug.setOnClickListener {
            DebugMenuFragment().show(parentFragmentManager, "DebugMenu")
        }

        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS

            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

    private fun setupRecyclerView() {
        calendarAdapter = CalendarAdapter { day ->
            viewModel.selectDate(day.date)
        }
        binding.rvCalendar.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                requireContext(),
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = calendarAdapter
        }

        taskAdapter = TaskAdapter { task ->
            val intent = Intent(requireContext(), TaskAlarmActivity::class.java).apply {
                putExtra("SOURCE_ID", task.sourceId)
                putExtra("SCHEDULE_ID", task.scheduleId)
                putExtra("TITLE", task.title)
                putExtra("DETAILS", task.doseDescription)
                putExtra("TASK_TYPE", task.taskType.name)
            }
            startActivity(intent)
        }
        binding.rvTasks.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = taskAdapter
            isNestedScrollingEnabled = false
            setHasFixedSize(false)
        }
    }

    private fun setupObservers() {
        viewModel.todayProgress.observe(viewLifecycleOwner) { (completed, total) ->
            binding.tvProgressCount.text = "$completed/$total"
            if (total > 0) {
                binding.pbWeekly.progress = (completed.toFloat() / total * 100).toInt()
            } else {
                binding.pbWeekly.progress = 0
            }
        }

        viewModel.todayTasks.observe(viewLifecycleOwner) { tasks ->
            taskAdapter.submitList(tasks)
        }

        viewModel.calendarDays.observe(viewLifecycleOwner) { days ->
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
