package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pillmate.domain.usecase.LogMedicationUseCase

class MedicationLogViewModelFactory(
    private val logMedicationUseCase: LogMedicationUseCase,
    private val profileId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicationLogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicationLogViewModel(logMedicationUseCase, profileId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
