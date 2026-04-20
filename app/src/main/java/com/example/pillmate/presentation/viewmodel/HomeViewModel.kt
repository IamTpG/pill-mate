package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.domain.usecase.GetHomeTasksUseCase
import com.example.pillmate.presentation.model.CalendarDay
import com.example.pillmate.presentation.model.HomeTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date

data class HomeUiState(
    val todayProgress: Pair<Int, Int> = 0 to 0,
    val todayTasks: List<HomeTask> = emptyList(),
    val selectedDate: Date = Date(),
    val calendarDays: List<CalendarDay> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val medicationRepository: MedicationRepository,
    private val getHomeTasksUseCase: GetHomeTasksUseCase,
    private val syncAlarmsUseCase: com.example.pillmate.domain.usecase.SyncAlarmsUseCase,
    private val profileId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadDataJob: kotlinx.coroutines.Job? = null

    init {
        generateCalendarDays()
        loadData()
        
        // Self-Healing: Sync alarms on launch to catch missed ones
        viewModelScope.launch {
            syncAlarmsUseCase(profileId)
        }
    }

    private fun generateCalendarDays() {
        val days = mutableListOf<CalendarDay>()
        val cal = java.util.Calendar.getInstance()
        cal.time = calendarStartDate
        
        val selected = _uiState.value.selectedDate
        
        // Show 31 days (15 past, today, 15 future)
        for (i in 0 until 31) {
            val date = cal.time
            val dayOfWeek = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(date).uppercase()
            val dayOfMonth = java.text.SimpleDateFormat("dd", java.util.Locale.getDefault()).format(date)
            
            val isSelected = isSameDay(date, selected)
            
            days.add(CalendarDay(date, dayOfWeek, dayOfMonth, isSelected))
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        _uiState.value = _uiState.value.copy(calendarDays = days)
    }

    private fun isSameDay(d1: Date, d2: Date): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { time = d1 }
        val cal2 = java.util.Calendar.getInstance().apply { time = d2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    fun selectDate(date: Date) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        generateCalendarDays()
        loadData(date)
    }

    fun loadData(date: Date = _uiState.value.selectedDate) {
        loadDataJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadDataJob = viewModelScope.launch {
            getHomeTasksUseCase.execute(profileId, date)
                .collect { data ->
                    _uiState.value = _uiState.value.copy(
                        todayTasks = data.tasks,
                        todayProgress = data.completedCount to data.totalCount,
                        isLoading = false
                    )
                }
        }
    }
}
