package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.domain.usecase.GetHomeTasksUseCase
import com.example.pillmate.presentation.model.HomeTask
import com.example.pillmate.domain.model.LogStatus
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date

class HomeViewModel(
    private val medicationRepository: MedicationRepository,
    private val getHomeTasksUseCase: GetHomeTasksUseCase,
    private val profileId: String
) : ViewModel() {

    private val _todayProgress = MutableLiveData<Pair<Int, Int>>(0 to 0)
    val todayProgress: LiveData<Pair<Int, Int>> = _todayProgress

    private val _todayTasks = MutableLiveData<List<HomeTask>>()
    val todayTasks: LiveData<List<HomeTask>> = _todayTasks

    private val _selectedDate = MutableLiveData<Date>(Date())
    val selectedDate: LiveData<Date> = _selectedDate

    private val _calendarDays = MutableLiveData<List<com.example.pillmate.presentation.model.CalendarDay>>()
    val calendarDays: LiveData<List<com.example.pillmate.presentation.model.CalendarDay>> = _calendarDays

    private var loadDataJob: kotlinx.coroutines.Job? = null

    private val calendarStartDate: Date by lazy {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_MONTH, -15)
        cal.time
    }

    init {
        generateCalendarDays()
    }

    private fun generateCalendarDays() {
        val days = mutableListOf<com.example.pillmate.presentation.model.CalendarDay>()
        val cal = java.util.Calendar.getInstance()
        cal.time = calendarStartDate
        
        val selected = _selectedDate.value ?: Date()
        
        // Show 31 days (15 past, today, 15 future)
        for (i in 0 until 31) {
            val date = cal.time
            val dayOfWeek = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(date).uppercase()
            val dayOfMonth = java.text.SimpleDateFormat("dd", java.util.Locale.getDefault()).format(date)
            
            val isSelected = isSameDay(date, selected)
            
            days.add(com.example.pillmate.presentation.model.CalendarDay(date, dayOfWeek, dayOfMonth, isSelected))
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        _calendarDays.value = days
    }

    private fun isSameDay(d1: Date, d2: Date): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { time = d1 }
        val cal2 = java.util.Calendar.getInstance().apply { time = d2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    fun selectDate(date: Date) {
        _selectedDate.value = date
        generateCalendarDays()
        loadData(date)
    }

    fun loadData(date: Date = _selectedDate.value ?: Date()) {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            getHomeTasksUseCase.execute(profileId, date)
                .collect { data ->
                    _todayTasks.postValue(data.tasks)
                    _todayProgress.postValue(data.completedCount to data.totalCount)
                }
        }
    }
}
