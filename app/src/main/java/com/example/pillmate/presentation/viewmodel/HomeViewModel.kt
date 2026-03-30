package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.model.TaskLog
import com.example.pillmate.domain.repository.LogRepository
import com.example.pillmate.domain.repository.MedicationRepository
import kotlinx.coroutines.launch
import java.util.Date

class HomeViewModel(
    private val medicationRepository: MedicationRepository,
    private val logRepository: LogRepository,
    private val profileId: String
) : ViewModel() {

    private val _todayProgress = MutableLiveData<Pair<Int, Int>>(0 to 0)
    val todayProgress: LiveData<Pair<Int, Int>> = _todayProgress

    private val _dailyTasks = MutableLiveData<List<TaskLog>>()
    val dailyTasks: LiveData<List<TaskLog>> = _dailyTasks

    fun loadData(date: Date = Date()) {
        viewModelScope.launch {
            val logsResult = logRepository.getLogsForDay(profileId, date)
            if (logsResult.isSuccess) {
                val logs = logsResult.getOrDefault(emptyList())
                _dailyTasks.value = logs
                
                // Calculate progress (this is a simple example)
                val completed = logs.count { it.status.name == "COMPLETED" }
                val total = logs.size
                _todayProgress.value = completed to total
            }
        }
    }
}
