package com.example.pillmate.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pillmate.R
import com.example.pillmate.databinding.FragmentReminderBinding
import com.example.pillmate.domain.model.Reminder
import com.example.pillmate.domain.model.ReminderType
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.presentation.ui.adapter.ScheduleReminderAdapter
import com.example.pillmate.presentation.viewmodel.ReminderViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReminderFragment : Fragment() {

    private var _binding: FragmentReminderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderViewModel by viewModel()
    private lateinit var adapter: ScheduleReminderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            // Top padding for header
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            
            // Bottom padding for RecyclerView to account for both System Nav and App Bottom Nav
            // App Bottom Nav is 60dp in other layouts, let's use that as a safe margin
            val density = resources.displayMetrics.density
            val bottomNavHeight = (60 * density).toInt()
            binding.rvSchedules.setPadding(0, 0, 0, systemBars.bottom + bottomNavHeight)
            
            insets
        }

        adapter = ScheduleReminderAdapter(
            onAddReminderClick = { schedule ->
                showReminderDialog(schedule, null)
            },
            onEditReminderClick = { schedule, reminder ->
                showReminderDialog(schedule, reminder)
            },
            onRemoveReminderClick = { schedule, reminder ->
                viewModel.removeReminder(schedule.id, reminder)
            }
        )

        binding.rvSchedules.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSchedules.adapter = adapter

        viewModel.schedules.observe(viewLifecycleOwner) { schedules ->
            adapter.submitList(schedules)
        }
    }

    private fun showReminderDialog(schedule: Schedule, reminderToEdit: Reminder?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reminder, null)
        val etMinutes: EditText = dialogView.findViewById(R.id.etMinutes)
        val rbNotification: RadioButton = dialogView.findViewById(R.id.rbNotification)
        val rbAlarm: RadioButton = dialogView.findViewById(R.id.rbAlarm)

        if (reminderToEdit != null) {
            etMinutes.setText(reminderToEdit.minutesBefore.toString())
            if (reminderToEdit.type == ReminderType.ALARM) {
                rbAlarm.isChecked = true
            } else {
                rbNotification.isChecked = true
            }
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val minutesStr = etMinutes.text.toString()
                val minutes = if (minutesStr.isNotBlank()) minutesStr.toIntOrNull() ?: 0 else 0
                val type = if (rbAlarm.isChecked) ReminderType.ALARM else ReminderType.NOTIFICATION
                
                val newReminder = Reminder(minutesBefore = minutes, type = type)
                if (reminderToEdit != null) {
                    viewModel.updateReminder(schedule.id, reminderToEdit, newReminder)
                } else {
                    viewModel.addReminder(schedule.id, newReminder)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
