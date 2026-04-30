package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.model.MedicationSupply
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.domain.usecase.LogTaskUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class TaskLogViewModel(
    private val logTaskUseCase: LogTaskUseCase,
    private val medicationRepository: MedicationRepository,
    private val profileId: String
) : ViewModel() {

    private val _isLogging = MutableStateFlow<Boolean>(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()

    private val _logResult = MutableStateFlow<Result<Unit>?>(null)
    val logResult: StateFlow<Result<Unit>?> = _logResult.asStateFlow()

    private val _availableSupplies = MutableStateFlow<List<MedicationSupply>>(emptyList())
    val availableSupplies: StateFlow<List<MedicationSupply>> = _availableSupplies.asStateFlow()

    fun fetchSupplies(medId: String) {
        viewModelScope.launch {
            medicationRepository.getMedicationSupplies(profileId, medId).onSuccess {
                _availableSupplies.value = it
            }
        }
    }

    fun logTask(
        sourceId: String, 
        scheduleId: String, 
        taskType: com.example.pillmate.domain.model.TaskType, 
        status: com.example.pillmate.domain.model.LogStatus, 
        scheduledTime: Date, 
        dose: Float = 1.0f,
        supplyId: String? = null
    ) {
        viewModelScope.launch {
            _isLogging.value = true
            val result = logTaskUseCase.execute(
                profileId = profileId,
                sourceId = sourceId,
                scheduleId = scheduleId,
                taskType = taskType,
                status = status,
                scheduledTime = scheduledTime,
                dose = dose,
                supplyId = supplyId
            )
            _logResult.value = result
            _isLogging.value = false
        }
    }

    fun onTakeClicked(sourceId: String, scheduleId: String, taskType: com.example.pillmate.domain.model.TaskType, scheduledTime: Date, dose: Float, supplyId: String? = null) {
        logTask(sourceId, scheduleId, taskType, com.example.pillmate.domain.model.LogStatus.COMPLETED, scheduledTime, dose, supplyId)
    }

    fun onSkipClicked(sourceId: String, scheduleId: String, taskType: TaskType, scheduledTime: Date) {
        logTask(sourceId, scheduleId, taskType, LogStatus.SKIPPED, scheduledTime)
    }

    fun onSnoozeClicked(sourceId: String, scheduleId: String, taskType: TaskType, scheduledTime: Date) {
        logTask(sourceId, scheduleId, taskType, LogStatus.SNOOZED, scheduledTime)
    }
}
