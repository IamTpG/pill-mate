package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.data.local.dao.ProfileDao
import com.example.pillmate.domain.model.Appointment
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.domain.model.ScheduleEvent
import com.example.pillmate.domain.model.Reminder
import com.example.pillmate.domain.repository.ScheduleRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

// Tái sử dụng ReminderTime từ domain của bạn
data class AppointmentReminderTime(val id: Int, val timeTitle: String, val note: String = "")

data class AppointmentScheduleUiState(
	val existingSchedules: List<Schedule> = emptyList(),
	val existingScheduleId: String? = null,
	val readOnly: Boolean = false,
	val selectedAppointment: Appointment? = null,
	
	val scheduleName: String = "",
	val repeatFrequency: String = "No Repeat",
	val availableFrequencies: List<String> = listOf( "Daily", "Weekly", "Interval", "As Needed"),
	
	val selectedDaysOfWeek: Set<Int> = emptySet(),
	val intervalValue: String = "1",
	val intervalUnit: String = "Days",
	val intervalUnits: List<String> = listOf("Hours", "Days", "Weeks", "Months"),
	
	val reminderTimes: List<AppointmentReminderTime> = emptyList(),
	
	val startDate: Date? = null,
	val endDate: Date? = null,
	
	val isSaving: Boolean = false,
	val saveSuccess: Boolean = false,
	val error: String? = null
)

class AppointmentScheduleViewModel(
	private val scheduleRepository: ScheduleRepository,
	private val profileDao: ProfileDao,
	private val auth: FirebaseAuth
) : ViewModel() {
	
	private val _uiState = MutableStateFlow(AppointmentScheduleUiState())
	val uiState: StateFlow<AppointmentScheduleUiState> = _uiState.asStateFlow()
	
	private var reminderIdCounter = 0
	
	private suspend fun getEffectiveProfileId(): String? {
		val active = profileDao.getActiveProfile()?.id?.takeIf { it.isNotBlank() }
		if (active != null) return active
		val anyLocal = profileDao.getAllProfiles().firstOrNull()?.firstOrNull()?.id?.takeIf { it.isNotBlank() }
		if (anyLocal != null) return anyLocal
		return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
	}
	
	fun setSelectedAppointment(appointment: Appointment) {
		_uiState.update { state ->
			state.copy(
				selectedAppointment = appointment,
				existingSchedules = emptyList(),
				existingScheduleId = null,
				readOnly = false,
				reminderTimes = emptyList(),
				startDate = null,
				endDate = null,
				repeatFrequency = "No Repeat",
				saveSuccess = false,
				error = null
			)
		}
		// Fetch existing schedules for this appointment if needed
		viewModelScope.launch {
			val userId = getEffectiveProfileId() ?: return@launch
			val result = scheduleRepository.getAllOnce(userId)
			val existing = result.getOrNull()?.filter { it.eventSnapshot.sourceId == appointment.id } ?: emptyList()
			_uiState.update { it.copy(existingSchedules = existing) }
		}
	}
	
	fun openScheduleBuilder(scheduleId: String?) {
		val state = _uiState.value
		if (scheduleId == null) {
			_uiState.update {
				it.copy(
					existingScheduleId = null, readOnly = false, scheduleName = "",
					reminderTimes = emptyList(), startDate = null, endDate = null,
					repeatFrequency = "No Repeat", saveSuccess = false, error = null
				)
			}
		} else {
			val existing = state.existingSchedules.find { it.id == scheduleId }
			if (existing != null) {
				_uiState.update { it.copy(
					existingScheduleId = existing.id, readOnly = true, scheduleName = existing.name,
					repeatFrequency = existing.frequency ?: "No Repeat", endDate = existing.endDate,
					startDate = existing.createdAt,
					reminderTimes = existing.doseTimes.map { doseTime ->
						AppointmentReminderTime(id = reminderIdCounter++, timeTitle = doseTime.time, note = doseTime.doseContext)
					},
					saveSuccess = false, error = null
				)}
			}
		}
	}
	
	fun setEditMode() { _uiState.update { it.copy(readOnly = false) } }
	fun setScheduleName(name: String) { _uiState.update { it.copy(scheduleName = name) } }
	fun setStartDate(date: Date) { _uiState.update { it.copy(startDate = date) } }
	fun setEndDate(date: Date?) { _uiState.update { it.copy(endDate = date) } }
	fun setFrequency(freq: String) { _uiState.update { it.copy(repeatFrequency = freq) } }
	fun setIntervalValue(value: String) { _uiState.update { it.copy(intervalValue = value) } }
	fun setIntervalUnit(unit: String) { _uiState.update { it.copy(intervalUnit = unit) } }
	fun toggleDayOfWeek(day: Int) {
		_uiState.update { state ->
			val current = state.selectedDaysOfWeek.toMutableSet()
			if (current.contains(day)) current.remove(day) else current.add(day)
			state.copy(selectedDaysOfWeek = current)
		}
	}
	
	fun addReminderTime(timeTitle: String, note: String = "") {
		_uiState.update { state ->
			state.copy(reminderTimes = state.reminderTimes + AppointmentReminderTime(reminderIdCounter++, timeTitle), saveSuccess = false)
		}
	}
	
	fun removeReminderTime(id: Int) {
		_uiState.update { state -> state.copy(reminderTimes = state.reminderTimes.filter { it.id != id }, saveSuccess = false) }
	}
	
	// (Giữ nguyên các hàm extract24Hour, extractMinute, formatToIsoString từ ScheduleBuilderViewModel)
	private fun extract24Hour(timeStr: String): Int { /* ... */ return 8 }
	private fun extractMinute(timeStr: String): Int { /* ... */ return 0 }
	private fun formatToIsoString(date: Date, timeStr: String): String { /* ... */ return "" }
	
	fun saveSchedule() {
		val state = _uiState.value
//		if (state.selectedAppointment == null) {
//			_uiState.update { it.copy(error = "Please select an appointment.") }
//			return
//		}
//		if (state.reminderTimes.isEmpty()) {
//			_uiState.update { it.copy(error = "Please add at least one reminder time.") }
//			return
//		}
//		if (state.startDate == null) {
//			_uiState.update { it.copy(error = "Please select a date.") }
//			return
//		}
		
		_uiState.update { it.copy(isSaving = true, error = null) }
		
		viewModelScope.launch {
			val userId = getEffectiveProfileId() ?: return@launch
			try {
				val firstTime = state.reminderTimes.firstOrNull()?.timeTitle ?: "08:00"
				val startTimeIso = formatToIsoString(state.startDate ?: Date(), firstTime)
				
				val hours = state.reminderTimes.joinToString(",") { extract24Hour(it.timeTitle).toString() }
				val minutes = state.reminderTimes.joinToString(",") { extractMinute(it.timeTitle).toString() }
				
				// Xử lý RRULE tương tự ScheduleBuilder
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
					name = state.scheduleName.ifBlank { state.selectedAppointment!!.name },
					type = com.example.pillmate.domain.model.TaskType.APPOINTMENT, // Đổi type
					doseTimes = state.reminderTimes.map {
						com.example.pillmate.domain.model.DoseTime(time = it.timeTitle, doseContext = "", dose = 1f)
					},
					frequency = state.repeatFrequency,
					startTime = startTimeIso,
					recurrenceRule = rruleString,
					reminders = listOf(com.example.pillmate.domain.model.Reminder(type = com.example.pillmate.domain.model.ReminderType.ALARM, minutesBefore = 30)), // Nhắc trước 30p
					endDate = state.endDate,
					createdAt = state.startDate ?: Date(),
					eventSnapshot = ScheduleEvent(
						sourceId = state.selectedAppointment!!.id,
						title = state.selectedAppointment.name,
						instructions = "Location: ${state.selectedAppointment.location}",
						dose = 1.0f
					)
				)
				
				scheduleRepository.add(userId, schedule)
				_uiState.update { it.copy(isSaving = false, saveSuccess = true, readOnly = true) }
			} catch (ex: Exception) {
				_uiState.update { it.copy(isSaving = false, error = ex.message) }
			}
		}
	}
}