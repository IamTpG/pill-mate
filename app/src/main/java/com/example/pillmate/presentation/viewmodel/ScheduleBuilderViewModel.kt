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
import com.google.firebase.auth.FirebaseAuth
import java.util.Date

data class ReminderTime(val id: Int, val timeTitle: String, val doseContext: String)

data class ScheduleBuilderUiState(
    val existingSchedules: List<Schedule> = emptyList(),
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
        _uiState.update { state -> 
            state.copy(
                selectedMedication = medication,
                existingSchedules = emptyList(),
                reminderTimes = emptyList(),
                startDate = null,
                endDate = null,
                repeatFrequency = "Daily",
                saveSuccess = false,
                error = null
            )
        }
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val result = scheduleRepository.getSchedules(userId)
            val existing = result.getOrNull()?.filter { it.eventSnapshot.sourceId == medication.id } ?: emptyList()
            if (existing.isNotEmpty()) {
                val first = existing.first()
                _uiState.update { state ->
                    state.copy(
                        existingSchedules = existing,
                        repeatFrequency = first.frequency ?: "Daily",
                        endDate = first.endDate,
                        startDate = first.createdAt,
                        reminderTimes = existing.mapIndexed { idx, sched ->
                            ReminderTime(id = reminderIdCounter++, timeTitle = sched.startTime, doseContext = sched.eventSnapshot.instructions ?: "")
                        }
                    )
                }
            }
        }
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
            state.copy(reminderTimes = updated, saveSuccess = false)
        }
    }

    fun removeReminderTime(id: Int) {
        _uiState.update { state ->
            state.copy(reminderTimes = state.reminderTimes.filter { it.id != id }, saveSuccess = false)
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
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                _uiState.update { it.copy(isSaving = false, error = "User not logged in") }
                return@launch
            }

            try {
                // Update or Create
                for (i in state.reminderTimes.indices) {
                    val reminder = state.reminderTimes[i]
                    val existingId = if (i < state.existingSchedules.size) state.existingSchedules[i].id else ""
                    
                    val schedule = Schedule(
                        id = existingId,
                        type = com.example.pillmate.domain.model.TaskType.MEDICATION,
                        startTime = reminder.timeTitle,
                        frequency = state.repeatFrequency,
                        endDate = state.endDate,
                        createdAt = state.startDate ?: Date(),
                        eventSnapshot = ScheduleEvent(
                            sourceId = state.selectedMedication!!.id,
                            title = state.selectedMedication.name,
                            instructions = reminder.doseContext.ifBlank { "${state.selectedMedication.supply?.quantity ?: 0} ${state.selectedMedication.unit}" },
                            dose = 1.0f
                        )
                    )
                    scheduleRepository.saveSchedule(userId, schedule)
                }
                
                // Delete orphaned records
                for (i in state.reminderTimes.size until state.existingSchedules.size) {
                    val orphanId = state.existingSchedules[i].id
                    scheduleRepository.deleteSchedule(userId, orphanId)
                }
                
                // Refresh existing schedules to align with updated DB state
                val refreshed = scheduleRepository.getSchedules(userId).getOrNull()?.filter { it.eventSnapshot.sourceId == state.selectedMedication!!.id } ?: emptyList()
                _uiState.update { it.copy(isSaving = false, saveSuccess = true, existingSchedules = refreshed) }
            } catch (ex: Exception) {
                _uiState.update { it.copy(isSaving = false, error = ex.message) }
            }
        }
    }
}
