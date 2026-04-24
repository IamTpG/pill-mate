package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.model.Appointment
import com.example.pillmate.domain.model.AppointmentLog
import com.example.pillmate.domain.usecase.AddAppointmentUseCase
import com.example.pillmate.domain.usecase.GetAppointmentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

class AppointmentViewModel(
	private val getAppointmentsUseCase: GetAppointmentsUseCase,
	private val addAppointmentUseCase: AddAppointmentUseCase
) : ViewModel() {
	private val _uiState = MutableStateFlow<List<AppointmentLog>>(emptyList())
	val uiState: StateFlow<List<AppointmentLog>> = _uiState
	
	fun fetchAppointments(profileId: String) {
		viewModelScope.launch {
			getAppointmentsUseCase(profileId, Date()).collect { logs ->
				_uiState.value = logs
			}
		}
	}
	
	fun postAppointment(profileId: String, appointment: Appointment) {
		viewModelScope.launch {
			val result = addAppointmentUseCase(profileId, appointment)
			
			result.onSuccess {
			
			}.onFailure {
			
			}
		}
	}
}