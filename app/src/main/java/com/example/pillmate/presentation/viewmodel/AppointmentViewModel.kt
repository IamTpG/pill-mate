package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.model.Appointment
import com.example.pillmate.domain.model.AppointmentLog
import com.example.pillmate.domain.usecase.AddAppointmentUseCase
import com.example.pillmate.domain.usecase.DeleteAppointmentUseCase
import com.example.pillmate.domain.usecase.GetAppointmentsUseCase
import com.example.pillmate.domain.usecase.UpdateAppointmentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

class AppointmentViewModel(
	private val getAppointmentsUseCase: GetAppointmentsUseCase,
	private val addAppointmentUseCase: AddAppointmentUseCase,
	private val updateAppointmentUseCase: UpdateAppointmentUseCase,
	private val deleteAppointmentUseCase: DeleteAppointmentUseCase
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
	
	fun updateAppointment(profileId: String, appointmentId: String, appointment: Appointment) {
		viewModelScope.launch {
			val result = updateAppointmentUseCase(profileId, appointmentId, appointment)
			
			result.onSuccess {
			
			}.onFailure {
			
			}
		}
	}
	
	fun deleteAppoinment(profileId: String, appointmentId: String) {
		viewModelScope.launch {
			val result = deleteAppointmentUseCase(profileId, appointmentId)
			
			result.onSuccess {
			
			}.onFailure {
			
			}
		}
	}
	
}