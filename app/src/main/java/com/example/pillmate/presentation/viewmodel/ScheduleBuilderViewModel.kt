package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.data.local.dao.ProfileDao
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.model.ScheduleEvent
import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.repository.ScheduleRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

data class ReminderTime(val id: Int, val timeTitle: String, val doseContext: String, val dose: Float = 1.0f)

data class ScheduleBuilderUiState(
    val existingSchedules: List<Schedule> = emptyList(),
    val existingScheduleId: String? = null,
    val readOnly: Boolean = false,
    val selectedMedication: Medication? = null,
    
    val scheduleName: String = "",
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
    private val scheduleRepository: ScheduleRepository,
    private val profileDao: ProfileDao,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleBuilderUiState())
    val uiState: StateFlow<ScheduleBuilderUiState> = _uiState.asStateFlow()
    
    private var reminderIdCounter = 0
    
    private suspend fun getEffectiveProfileId(): String? {
        val active = profileDao.getActiveProfile()?.id?.takeIf { it.isNotBlank() }
        if (active != null) return active

        val anyLocal = profileDao.getAllProfiles().firstOrNull()?.firstOrNull()?.id?.takeIf { it.isNotBlank() }
        if (anyLocal != null) return anyLocal

        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
    }

    fun setSelectedMedication(medication: Medication) {
        _uiState.update { state -> 
            state.copy(
                selectedMedication = medication,
                existingSchedules = emptyList(),
                existingScheduleId = null,
                readOnly = false,
                reminderTimes = emptyList(),
                startDate = null,
                endDate = null,
                repeatFrequency = "Daily",
                saveSuccess = false,
                error = null
            )
        }
        viewModelScope.launch {
            val userId = getEffectiveProfileId() ?: return@launch
            val result = scheduleRepository.getSchedules(userId)
            val existing = result.getOrNull()?.filter { it.eventSnapshot.sourceId == medication.id } ?: emptyList()
            _uiState.update { it.copy(existingSchedules = existing) }
        }
    }

    fun openScheduleBuilder(scheduleId: String?) {
        val state = _uiState.value
        if (scheduleId == null) {
            _uiState.update { 
                it.copy(
                    existingScheduleId = null,
                    readOnly = false,
                    scheduleName = "",
                    reminderTimes = emptyList(),
                    startDate = null,
                    endDate = null,
                    repeatFrequency = "Daily",
                    saveSuccess = false,
                    error = null
                )
            }
        } else {
            val existing = state.existingSchedules.find { it.id == scheduleId }
            if (existing != null) {
                _uiState.update { it.copy(
                    existingScheduleId = existing.id,
                    readOnly = true,
                    scheduleName = existing.name,
                    repeatFrequency = existing.frequency ?: "Daily",
                    endDate = existing.endDate,
                    startDate = existing.createdAt,
                    reminderTimes = existing.doseTimes.map { doseTime ->
                        ReminderTime(id = reminderIdCounter++, timeTitle = doseTime.time, doseContext = doseTime.doseContext, dose = doseTime.dose)
                    },
                    saveSuccess = false,
                    error = null
                )}
            }
        }
    }

    fun setEditMode() {
        _uiState.update { it.copy(readOnly = false) }
    }

    fun setScheduleName(name: String) {
        _uiState.update { it.copy(scheduleName = name) }
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

    fun addReminderTime(timeTitle: String, doseContext: String, dose: Float = 1.0f) {
        _uiState.update { state ->
            val updated = state.reminderTimes + ReminderTime(reminderIdCounter++, timeTitle, doseContext, dose)
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
            val userId = getEffectiveProfileId()
            if (userId == null) {
                _uiState.update { it.copy(isSaving = false, error = "No valid profile id. Please sign in again.") }
                return@launch
            }

            try {
                val schedule = Schedule(
                    id = state.existingScheduleId ?: "",
                    name = state.scheduleName,
                    type = com.example.pillmate.domain.model.TaskType.MEDICATION,
                    doseTimes = state.reminderTimes.map { 
                        com.example.pillmate.domain.model.DoseTime(time = it.timeTitle, doseContext = it.doseContext, dose = it.dose) 
                    },
                    frequency = state.repeatFrequency,
                    endDate = state.endDate,
                    createdAt = state.startDate ?: Date(),
                    eventSnapshot = ScheduleEvent(
                        sourceId = state.selectedMedication!!.id,
                        title = state.selectedMedication.name,
                        instructions = "${state.selectedMedication.supply?.quantity ?: 0} ${state.selectedMedication.unit}",
                        dose = 1.0f
                    )
                )
                
                scheduleRepository.saveSchedule(userId, schedule).getOrThrow()
                
                // Refresh existing schedules to align with updated DB state
                val refreshedList = scheduleRepository.getSchedules(userId).getOrThrow().filter { it.eventSnapshot.sourceId == state.selectedMedication!!.id }
                val refreshedDoc = refreshedList.find { it.id == schedule.id || (state.existingScheduleId.isNullOrBlank() && it.createdAt == schedule.createdAt) }
                _uiState.update { it.copy(isSaving = false, saveSuccess = true, existingSchedules = refreshedList, existingScheduleId = refreshedDoc?.id, readOnly = true) }
            } catch (ex: Exception) {
                _uiState.update { it.copy(isSaving = false, error = ex.message) }
            }
        }
    }
    
    fun deleteSchedule(scheduleId: String? = null) {
        val state = _uiState.value
        val idToDelete = scheduleId ?: state.existingScheduleId ?: return
        
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val userId = getEffectiveProfileId() ?: return@launch
            try {
                scheduleRepository.deleteSchedule(userId, idToDelete).getOrThrow()
                // Refresh existing schedules to align with updated DB state
                val refreshedList = scheduleRepository.getSchedules(userId).getOrThrow().filter { it.eventSnapshot.sourceId == state.selectedMedication?.id }
                _uiState.update { it.copy(
                    isSaving = false,
                    saveSuccess = true,
                    existingScheduleId = null,
                    existingSchedules = refreshedList,
                    reminderTimes = emptyList()
                )}
            } catch (ex: Exception) {
                _uiState.update { it.copy(isSaving = false, error = ex.message) }
            }
        }
    }
}
