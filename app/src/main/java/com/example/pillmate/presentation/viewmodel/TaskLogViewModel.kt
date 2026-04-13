package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.domain.usecase.LogTaskUseCase
import kotlinx.coroutines.launch
import java.util.Date

class TaskLogViewModel(
    private val logTaskUseCase: LogTaskUseCase,
    private val profileId: String
) : ViewModel() {

    private val _isLogging = MutableLiveData<Boolean>(false)
    val isLogging: LiveData<Boolean> = _isLogging

    private val _logResult = MutableLiveData<Result<Unit>?>()
    val logResult: LiveData<Result<Unit>?> = _logResult

    fun logTask(sourceId: String, scheduleId: String, taskType: TaskType, status: LogStatus, scheduledTime: Date, dose: Float = 1.0f) {
        viewModelScope.launch {
            _isLogging.value = true
            val result = logTaskUseCase.execute(
                profileId = profileId,
                sourceId = sourceId,
                scheduleId = scheduleId,
                taskType = taskType,
                status = status,
                scheduledTime = scheduledTime,
                dose = dose
            )
            _logResult.value = result
            _isLogging.value = false
        }
    }

    fun onTakeClicked(sourceId: String, scheduleId: String, taskType: TaskType, scheduledTime: Date, dose: Float) {
        logTask(sourceId, scheduleId, taskType, LogStatus.COMPLETED, scheduledTime, dose)
    }

    fun onSkipClicked(sourceId: String, scheduleId: String, taskType: TaskType, scheduledTime: Date) {
        logTask(sourceId, scheduleId, taskType, LogStatus.SKIPPED, scheduledTime)
    }

    fun onSnoozeClicked(sourceId: String, scheduleId: String, taskType: TaskType, scheduledTime: Date) {
        logTask(sourceId, scheduleId, taskType, LogStatus.SNOOZED, scheduledTime)
    }
}
