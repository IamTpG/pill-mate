package com.example.pillmate.presentation.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.model.Reminder
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.domain.usecase.UpdateScheduleUseCase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ReminderViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val updateScheduleUseCase: UpdateScheduleUseCase,
    private val profileId: String
) : ViewModel() {

    private val _schedules = MutableLiveData<List<Schedule>>()
    val schedules: LiveData<List<Schedule>> = _schedules

    init {
        loadSchedules()
    }

    private fun loadSchedules() {
        viewModelScope.launch {
            scheduleRepository.getSchedulesFlow(profileId).collect {
                _schedules.postValue(it)
            }
        }
    }

    fun addReminder(scheduleId: String, reminder: Reminder) {
        val currentSchedules = _schedules.value ?: return
        val schedule = currentSchedules.find { it.id == scheduleId } ?: return
        val newReminders = schedule.reminders + reminder
        val updatedSchedule = schedule.copy(reminders = newReminders)
        
        viewModelScope.launch {
            updateScheduleUseCase(profileId, updatedSchedule)
        }
    }

    fun removeReminder(scheduleId: String, reminder: Reminder) {
        val currentSchedules = _schedules.value ?: return
        val schedule = currentSchedules.find { it.id == scheduleId } ?: return
        val newReminders = schedule.reminders - reminder
        val updatedSchedule = schedule.copy(reminders = newReminders)
        
        viewModelScope.launch {
            updateScheduleUseCase(profileId, updatedSchedule)
        }
    }

    fun updateReminder(scheduleId: String, oldReminder: Reminder, newReminder: Reminder) {
        val currentSchedules = _schedules.value ?: return
        val schedule = currentSchedules.find { it.id == scheduleId } ?: return
        val newReminders = schedule.reminders.map { if (it == oldReminder) newReminder else it }
        val updatedSchedule = schedule.copy(reminders = newReminders)
        
        viewModelScope.launch {
            updateScheduleUseCase(profileId, updatedSchedule)
        }
    }
}
