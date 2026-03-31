package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.repository.LogRepository
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.presentation.model.HomeTask
import com.example.pillmate.domain.model.LogStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date

class HomeViewModel(
    private val medicationRepository: MedicationRepository,
    private val logRepository: LogRepository,
    private val scheduleRepository: ScheduleRepository,
    private val profileId: String
) : ViewModel() {

    private val _todayProgress = MutableLiveData<Pair<Int, Int>>(0 to 0)
    val todayProgress: LiveData<Pair<Int, Int>> = _todayProgress

    private val _todayTasks = MutableLiveData<List<HomeTask>>()
    val todayTasks: LiveData<List<HomeTask>> = _todayTasks

    fun loadData(date: Date = Date()) {
        viewModelScope.launch {
            scheduleRepository.getSchedulesFlow(profileId)
                .combine(logRepository.getLogsForDayFlow(profileId, date)) { schedules, logs ->
                    schedules to logs
                }
                .collect { (schedules, logs) ->
                    val now = Date()
                    val homeTasks = schedules.map { schedule ->
                        val matchingLog = logs.find { it.scheduleId == schedule.id }
                        
                        var status = matchingLog?.status
                        if (status == null) {
                            val timeParts = schedule.startTime.split(":")
                            if (timeParts.size == 2) {
                                val hour = timeParts[0].toInt()
                                val min = timeParts[1].toInt()
                                val scheduledCal = java.util.Calendar.getInstance()
                                scheduledCal.time = date
                                scheduledCal.set(java.util.Calendar.HOUR_OF_DAY, hour)
                                scheduledCal.set(java.util.Calendar.MINUTE, min)
                                scheduledCal.set(java.util.Calendar.SECOND, 0)
                                if (scheduledCal.time.before(now)) {
                                    status = LogStatus.MISSED
                                }
                            }
                        }

                        HomeTask(
                            scheduleId = schedule.id,
                            medId = schedule.eventSnapshot.sourceId,
                            title = schedule.eventSnapshot.title,
                            time = schedule.startTime,
                            doseDescription = "Take ${schedule.eventSnapshot.dose} ${schedule.eventSnapshot.unit ?: "dose"}",
                            status = status
                        )
                    }.sortedBy { it.time }

                    _todayTasks.postValue(homeTasks)
                    
                    val completed = logs.count { it.status == LogStatus.COMPLETED }
                    val total = schedules.size
                    _todayProgress.postValue(completed to total)
                }
        }
    }
}
