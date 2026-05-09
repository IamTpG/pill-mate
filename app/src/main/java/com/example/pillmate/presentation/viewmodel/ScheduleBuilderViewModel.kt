package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.data.local.dao.ProfileDao
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.model.ScheduleEvent
import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.model.Reminder
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.domain.usecase.ManageReminderUseCase
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
    
    val selectedDaysOfWeek: Set<Int> = emptySet(), // 1=Mon, ..., 7=Sun
    val intervalValue: String = "1",
    val intervalUnit: String = "Days",
    val intervalUnits: List<String> = listOf("Hours", "Days", "Weeks", "Months"),
    
    val reminderTimes: List<ReminderTime> = emptyList(),
    
    val startDate: Date? = null,
    val endDate: Date? = null,
    
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

class ScheduleBuilderViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val manageReminderUseCase: ManageReminderUseCase,
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
            val result = scheduleRepository.getAllOnce(userId)
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

    fun toggleDayOfWeek(day: Int) {
        _uiState.update { state ->
            val current = state.selectedDaysOfWeek.toMutableSet()
            if (current.contains(day)) {
                current.remove(day)
            } else {
                if (current.size < 6) { // Cannot select 7 days
                    current.add(day)
                }
            }
            state.copy(selectedDaysOfWeek = current)
        }
    }

    fun setIntervalValue(value: String) {
        _uiState.update { it.copy(intervalValue = value) }
    }

    fun setIntervalUnit(unit: String) {
        _uiState.update { it.copy(intervalUnit = unit) }
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

    private fun extract24Hour(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.isEmpty()) return 8
        var hr = parts[0].filter { it.isDigit() }.toIntOrNull() ?: 8
        if (timeStr.contains("PM", ignoreCase = true) && hr < 12) hr += 12
        if (timeStr.contains("AM", ignoreCase = true) && hr == 12) hr = 0
        return hr
    }

    private fun extractMinute(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size < 2) return 0
        return parts[1].filter { it.isDigit() }.toIntOrNull() ?: 0
    }

    private fun formatToIsoString(date: Date, timeStr: String): String {
        return try {
            val dateCal = java.util.Calendar.getInstance().apply { time = date }
            val timeParts = timeStr.split(":")
            val cleanMin = if (timeParts.size > 1) timeParts[1].filter { it.isDigit() }.toIntOrNull() ?: 0 else 0
            val hr = extract24Hour(timeStr)

            dateCal.set(java.util.Calendar.HOUR_OF_DAY, hr)
            dateCal.set(java.util.Calendar.MINUTE, cleanMin)
            dateCal.set(java.util.Calendar.SECOND, 0)
            
            val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            isoFormat.format(dateCal.time)
        } catch (e: Exception) {
            val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            isoFormat.format(date)
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

        if (state.repeatFrequency == "Weekly" && state.selectedDaysOfWeek.isEmpty()) {
            _uiState.update { it.copy(error = "Please select at least one day for weekly frequency.") }
            return
        }

        if (state.repeatFrequency == "Interval") {
            val num = state.intervalValue.toIntOrNull()
            if (num == null || num <= 0) {
                _uiState.update { it.copy(error = "Please enter a valid interval number.") }
                return
            }
        }
        
        _uiState.update { it.copy(isSaving = true, error = null) }
        
        viewModelScope.launch {
            val userId = getEffectiveProfileId()
            if (userId == null) {
                _uiState.update { it.copy(isSaving = false, error = "No valid profile id. Please sign in again.") }
                return@launch
            }

            try {
                // 1. Format his Start Time (The Anchor)
                val firstTime = state.reminderTimes.firstOrNull()?.timeTitle ?: "08:00"
                val startTimeIso = formatToIsoString(state.startDate ?: Date(), firstTime) 

                // 2. Build his RRULE String
                val hours = state.reminderTimes.joinToString(",") { extract24Hour(it.timeTitle).toString() }
                val minutes = state.reminderTimes.joinToString(",") { extractMinute(it.timeTitle).toString() }
                
                val rruleString = when (state.repeatFrequency) {
                    "Daily" -> {
                        "FREQ=DAILY;BYHOUR=$hours;BYMINUTE=$minutes"
                    }
                    "Weekly" -> {
                        val daysMap = mapOf(1 to "MO", 2 to "TU", 3 to "WE", 4 to "TH", 5 to "FR", 6 to "SA", 7 to "SU")
                        val byDay = state.selectedDaysOfWeek.mapNotNull { daysMap[it] }.joinToString(",")
                        if (byDay.isNotEmpty()) {
                            "FREQ=WEEKLY;BYDAY=$byDay;BYHOUR=$hours;BYMINUTE=$minutes"
                        } else null
                    }
                    "Interval" -> {
                        val intervalNum = state.intervalValue.toIntOrNull() ?: 1
                        val freq = when (state.intervalUnit) {
                            "Hours" -> "HOURLY"
                            "Days" -> "DAILY"
                            "Weeks" -> "WEEKLY"
                            "Months" -> "MONTHLY"
                            else -> "DAILY"
                        }
                        if (freq == "HOURLY") {
                            "FREQ=HOURLY;INTERVAL=$intervalNum"
                        } else {
                            "FREQ=$freq;INTERVAL=$intervalNum;BYHOUR=$hours;BYMINUTE=$minutes"
                        }
                    }
                    else -> null
                }

                val schedule = Schedule(
                    id = state.existingScheduleId ?: "",
                    name = state.scheduleName,
                    type = com.example.pillmate.domain.model.TaskType.MEDICATION,
                    doseTimes = state.reminderTimes.map { 
                        com.example.pillmate.domain.model.DoseTime(time = it.timeTitle, doseContext = it.doseContext, dose = it.dose) 
                    },
                    frequency = state.repeatFrequency,
                    startTime = startTimeIso,
                    recurrenceRule = rruleString,
                    reminders = listOf(
                        com.example.pillmate.domain.model.Reminder(
                            type = com.example.pillmate.domain.model.ReminderType.ALARM, 
                            minutesBefore = 0
                        )
                    ),
                    endDate = state.endDate,
                    createdAt = state.startDate ?: Date(),
                    eventSnapshot = ScheduleEvent(
                        sourceId = state.selectedMedication!!.id,
                        title = state.selectedMedication.name,
                        instructions = "${state.selectedMedication.supply?.quantity ?: 0} ${state.selectedMedication.unit}",
                        dose = 1.0f
                    )
                )
                
                scheduleRepository.add(userId, schedule)
                
                // Refresh existing schedules to align with updated DB state
                val refreshedList = scheduleRepository.getAllOnce(userId).getOrNull()?.filter { it.eventSnapshot.sourceId == state.selectedMedication!!.id } ?: emptyList()
                val refreshedDoc = refreshedList.find { it.id == schedule.id || (state.existingScheduleId.isNullOrBlank() && it.createdAt == schedule.createdAt) }
                // Update home screen widget
                try {
                    val widgetIntent = android.content.Intent("com.example.pillmate.ACTION_UPDATE_WIDGET")
                    val context = org.koin.core.context.GlobalContext.get().get<android.content.Context>()
                    widgetIntent.setPackage(context.packageName)
                    context.sendBroadcast(widgetIntent)
                } catch (e: Exception) {}

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
                scheduleRepository.remove(userId, idToDelete)
                // Refresh existing schedules to align with updated DB state
                val refreshedList = scheduleRepository.getAllOnce(userId).getOrNull()?.filter { it.eventSnapshot.sourceId == state.selectedMedication?.id } ?: emptyList()
                // Update home screen widget
                try {
                    val widgetIntent = android.content.Intent("com.example.pillmate.ACTION_UPDATE_WIDGET")
                    val context = org.koin.core.context.GlobalContext.get().get<android.content.Context>()
                    widgetIntent.setPackage(context.packageName)
                    context.sendBroadcast(widgetIntent)
                } catch (e: Exception) {}

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

    fun addReminderToExistingSchedule(scheduleId: String, reminder: Reminder) {
        viewModelScope.launch {
            val profileId = getEffectiveProfileId() ?: return@launch
            val state = _uiState.value
            val schedule = state.existingSchedules.find { it.id == scheduleId } ?: return@launch
            val updatedSchedule = schedule.copy(reminders = schedule.reminders + reminder)
            
            manageReminderUseCase(profileId, updatedSchedule)
            refreshSchedules(profileId)
        }
    }

    fun removeReminderFromExistingSchedule(scheduleId: String, reminder: Reminder) {
        viewModelScope.launch {
            val profileId = getEffectiveProfileId() ?: return@launch
            val state = _uiState.value
            val schedule = state.existingSchedules.find { it.id == scheduleId } ?: return@launch
            val updatedSchedule = schedule.copy(reminders = schedule.reminders - reminder)
            
            manageReminderUseCase.cancelReminder(scheduleId, reminder.minutesBefore)
            manageReminderUseCase(profileId, updatedSchedule)
            refreshSchedules(profileId)
        }
    }

    fun updateReminderInExistingSchedule(scheduleId: String, oldReminder: Reminder, newReminder: Reminder) {
        viewModelScope.launch {
            val profileId = getEffectiveProfileId() ?: return@launch
            val state = _uiState.value
            val schedule = state.existingSchedules.find { it.id == scheduleId } ?: return@launch
            val updatedReminders = schedule.reminders.map { if (it == oldReminder) newReminder else it }
            val updatedSchedule = schedule.copy(reminders = updatedReminders)
            
            manageReminderUseCase.cancelReminder(scheduleId, oldReminder.minutesBefore)
            manageReminderUseCase(profileId, updatedSchedule)
            refreshSchedules(profileId)
        }
    }

    private suspend fun refreshSchedules(profileId: String) {
        val medicationId = _uiState.value.selectedMedication?.id ?: return
        val result = scheduleRepository.getAllOnce(profileId)
        val existing = result.getOrNull()?.filter { it.eventSnapshot.sourceId == medicationId } ?: emptyList()
        _uiState.update { it.copy(existingSchedules = existing) }
    }
}
