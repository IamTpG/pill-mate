package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.usecase.LogMedicationUseCase
import kotlinx.coroutines.launch
import java.util.Date

class MedicationLogViewModel(
    private val logMedicationUseCase: LogMedicationUseCase,
    private val profileId: String
) : ViewModel() {

    private val _isLogging = MutableLiveData<Boolean>(false)
    val isLogging: LiveData<Boolean> = _isLogging

    private val _logResult = MutableLiveData<Result<Unit>?>()
    val logResult: LiveData<Result<Unit>?> = _logResult

    fun logMedication(medId: String, scheduleId: String, status: LogStatus, scheduledTime: Date, dose: Float = 1.0f) {
        viewModelScope.launch {
            _isLogging.value = true
            val result = logMedicationUseCase.execute(
                profileId = profileId,
                medId = medId,
                scheduleId = scheduleId,
                status = status,
                scheduledTime = scheduledTime,
                dose = dose
            )
            _logResult.value = result
            _isLogging.value = false
        }
    }

    fun onTakeClicked(medId: String, scheduleId: String, scheduledTime: Date, dose: Float) {
        logMedication(medId, scheduleId, LogStatus.COMPLETED, scheduledTime, dose)
    }

    fun onSkipClicked(medId: String, scheduleId: String, scheduledTime: Date) {
        logMedication(medId, scheduleId, LogStatus.SKIPPED, scheduledTime)
    }

    fun onSnoozeClicked(medId: String, scheduleId: String, scheduledTime: Date) {
        logMedication(medId, scheduleId, LogStatus.SNOOZED, scheduledTime)
    }
}
