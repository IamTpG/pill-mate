package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.model.Reminder
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.domain.usecase.ManageReminderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReminderUiState(
    val schedules: List<Schedule> = emptyList(),
    val isLoading: Boolean = false
)

class ReminderViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val manageReminderUseCase: ManageReminderUseCase,
    private val profileId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    init {
        loadSchedules()
    }

    private fun loadSchedules() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            scheduleRepository.getAll(profileId).collect {
                _uiState.value = _uiState.value.copy(schedules = it, isLoading = false)
            }
        }
    }

    fun addReminder(scheduleId: String, reminder: Reminder) {
        val currentSchedules = _uiState.value.schedules
        val schedule = currentSchedules.find { it.id == scheduleId } ?: return
        val newReminders = schedule.reminders + reminder
        val updatedSchedule = schedule.copy(reminders = newReminders)

        viewModelScope.launch {
            manageReminderUseCase(profileId, updatedSchedule)
        }
    }

    fun removeReminder(scheduleId: String, reminder: Reminder) {
        val currentSchedules = _uiState.value.schedules
        val schedule = currentSchedules.find { it.id == scheduleId } ?: return
        val newReminders = schedule.reminders - reminder
        val updatedSchedule = schedule.copy(reminders = newReminders)

        manageReminderUseCase.cancelReminder(scheduleId, reminder.minutesBefore)

        viewModelScope.launch {
            manageReminderUseCase(profileId, updatedSchedule)
        }
    }

    fun updateReminder(scheduleId: String, oldReminder: Reminder, newReminder: Reminder) {
        val currentSchedules = _uiState.value.schedules
        val schedule = currentSchedules.find { it.id == scheduleId } ?: return
        val newReminders = schedule.reminders.map { if (it == oldReminder) newReminder else it }
        val updatedSchedule = schedule.copy(reminders = newReminders)

        manageReminderUseCase.cancelReminder(scheduleId, oldReminder.minutesBefore)

        viewModelScope.launch {
            manageReminderUseCase(profileId, updatedSchedule)
        }
    }
}
