package com.example.pillmate.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.pillmate.databinding.FragmentDebugMenuBinding
import com.example.pillmate.presentation.viewmodel.DebugViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class DebugMenuFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentDebugMenuBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DebugViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDebugMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGenerateData.setOnClickListener {
            binding.btnGenerateData.isEnabled = false
            viewModel.generateSampleData(
                onSuccess = {
                    Toast.makeText(requireContext(), "Data Generated!", Toast.LENGTH_SHORT).show()
                    binding.btnGenerateData.isEnabled = true
                    dismiss()
                },
                onError = { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnGenerateData.isEnabled = true
                }
            )
        }

        binding.btnClearData.setOnClickListener {
            viewModel.clearUserData(
                onSuccess = {
                    Toast.makeText(requireContext(), "Data Cleared!", Toast.LENGTH_SHORT).show()
                    dismiss()
                },
                onError = { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        }

        binding.btnTriggerAlarm.setOnClickListener {
            viewModel.triggerRandomAlarm(
                onSuccess = { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    dismiss()
                },
                onError = { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        }

        binding.btnCreateTestSchedule.setOnClickListener {
            binding.btnCreateTestSchedule.isEnabled = false
            viewModel.createTestScheduleIn1Min(
                onSuccess = {
                    val alarmManager = requireContext().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                    val next = alarmManager.nextAlarmClock?.triggerTime
                    val timeMsg = if (next != null) {
                        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(next))
                    } else "None"
                    
                    Toast.makeText(requireContext(), "Next alarm: $timeMsg", Toast.LENGTH_LONG).show()
                    binding.btnCreateTestSchedule.isEnabled = true
                    dismiss()
                },
                onError = { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnCreateTestSchedule.isEnabled = true
                }
            )
        }

        binding.btnTriggerSpecific.setOnClickListener {
            showScheduleSelectionDialog()
        }
    }

    private fun showScheduleSelectionDialog() {
        viewModel.getSchedulesList(
            onSuccess = { schedules ->
                if (schedules.isEmpty()) {
                    Toast.makeText(requireContext(), "No schedules found!", Toast.LENGTH_SHORT).show()
                    return@getSchedulesList
                }
                
                val titles = schedules.map { "${it.eventSnapshot.title} (${it.startTime})" }.toTypedArray()
                
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Select Schedule")
                    .setItems(titles) { _, which ->
                        showReminderSelectionDialog(schedules[which])
                    }
                    .show()
            },
            onError = { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun showReminderSelectionDialog(schedule: com.example.pillmate.domain.model.Schedule) {
        if (schedule.reminders.isEmpty()) {
            Toast.makeText(requireContext(), "No reminders in this schedule!", Toast.LENGTH_SHORT).show()
            return
        }
        
        val reminderTexts = schedule.reminders.map { "${it.type} at ${it.minutesBefore}m before" }.toTypedArray()
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Reminder to Trigger")
            .setItems(reminderTexts) { _, which ->
                viewModel.triggerSpecificReminder(
                    schedule, 
                    schedule.reminders[which],
                    onSuccess = { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        dismiss()
                    },
                    onError = { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
