package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pillmate.domain.repository.LogRepository
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.domain.repository.ScheduleRepository

class HomeViewModelFactory(
    private val medicationRepository: MedicationRepository,
    private val logRepository: LogRepository,
    private val scheduleRepository: ScheduleRepository,
    private val profileId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(medicationRepository, logRepository, scheduleRepository, profileId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
