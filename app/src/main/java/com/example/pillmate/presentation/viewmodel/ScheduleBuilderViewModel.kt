package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.model.ScheduleEvent
import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

data class ReminderTime(val id: Int, val timeTitle: String, val doseContext: String)

data class ScheduleBuilderUiState(
    val selectedMedication: Medication? = null,
    
    val repeatFrequency: String = "Daily",
    val availableFrequencies: List<String> = listOf("Daily", "Weekly", "Interval", "As Needed"),
    
    val reminderTimes: List<ReminderTime> = emptyList(),
    
    val startDate: Date? = null,
    val endDate: Date? = null,
    
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

class ScheduleBuilderViewModel(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleBuilderUiState())
    val uiState: StateFlow<ScheduleBuilderUiState> = _uiState.asStateFlow()
    
    private var reminderIdCounter = 0

    fun setSelectedMedication(medication: Medication) {
        _uiState.update { it.copy(selectedMedication = medication) }
    }

    fun setStartDate(date: Date) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun setEndDate(date: Date?) {
        _uiState.update { it.copy(endDate = date) }
    }

    fun setFrequency(freq: String) {
        _uiState.update { it.copy(repeatFrequency = freq) }
    }

    fun addReminderTime(timeTitle: String, doseContext: String) {
        _uiState.update { state ->
            val updated = state.reminderTimes + ReminderTime(reminderIdCounter++, timeTitle, doseContext)
            state.copy(reminderTimes = updated)
        }
    }

    fun removeReminderTime(id: Int) {
        _uiState.update { state ->
            state.copy(reminderTimes = state.reminderTimes.filter { it.id != id })
        }
    }

    fun saveSchedule() {
        val state = _uiState.value
        
        if (state.selectedMedication == null) {
            _uiState.update { it.copy(error = "Please select a medication first.") }
            return
        }
        
        if (state.reminderTimes.isEmpty()) {
            _uiState.update { it.copy(error = "Please add at least one reminder time.") }
            return
        }
        
        if (state.startDate == null) {
            _uiState.update { it.copy(error = "Please select a start date.") }
            return
        }
        
        _uiState.update { it.copy(isSaving = true, error = null) }
        
        viewModelScope.launch {
            val schedule = Schedule(
                type = com.example.pillmate.domain.model.TaskType.MEDICATION,
                startTime = state.reminderTimes.firstOrNull()?.timeTitle ?: "08:00 AM",
                frequency = state.repeatFrequency,
                endDate = state.endDate,
                eventSnapshot = ScheduleEvent(
                    sourceId = state.selectedMedication!!.id,
                    title = state.selectedMedication.name,
                    instructions = "${state.selectedMedication.supply?.quantity ?: 0} ${state.selectedMedication.unit}",
                    dose = 1.0f
                )
            )
            
            // Hardcoded user auth wrapper to demonstrate storage logic locally.
            val result = scheduleRepository.saveSchedule("demo_profile_id", schedule)
            
            result.onSuccess {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            }.onFailure { ex ->
                _uiState.update { it.copy(isSaving = false, error = ex.message) }
            }
        }
    }
}
