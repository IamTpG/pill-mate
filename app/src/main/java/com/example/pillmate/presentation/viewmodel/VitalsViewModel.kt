package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.model.HealthMetric
import com.example.pillmate.domain.model.MetricType
import com.example.pillmate.domain.usecase.GetHealthMetricsUseCase
import com.example.pillmate.domain.usecase.LogHealthMetricUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

data class VitalsUiState(
    val hydrationMl: Int = 0,
    val hydrationTarget: Int = 2500,
    val latestBloodPressure: String = "--/--",
    val bloodPressureStatus: String = "Normal",
    val latestWeight: String = "--",
    val weightStatus: String = "0kg today",
    val recentActivity: List<HealthMetric> = emptyList(),
    val isLoading: Boolean = false,
    val showLogPanel: Boolean = false
)

class VitalsViewModel(
    private val getHealthMetricsUseCase: GetHealthMetricsUseCase,
    private val logHealthMetricUseCase: LogHealthMetricUseCase,
    private val profileId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(VitalsUiState())
    val uiState: StateFlow<VitalsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getHealthMetricsUseCase.execute(profileId, 20).collect { metrics ->
                val today = Date()
                val hydration = metrics.filter { 
                    it.type == MetricType.WATER && isSameDay(it.recordedAt, today) 
                }.sumOf { it.valuePrimary }.toInt()

                val latestBP = metrics.find { it.type == MetricType.BLOOD_PRESSURE }
                val latestWeight = metrics.find { it.type == MetricType.WEIGHT }

                _uiState.update { state ->
                    state.copy(
                        hydrationMl = hydration,
                        latestBloodPressure = latestBP?.let { "${it.valuePrimary.toInt()}/${it.valueSecondary?.toInt() ?: "--"}" } ?: "--/--",
                        bloodPressureStatus = latestBP?.let { getBpStatus(it.valuePrimary.toInt(), it.valueSecondary?.toInt() ?: 0) } ?: "No data",
                        latestWeight = latestWeight?.let { String.format("%.1f", it.valuePrimary) } ?: "--",
                        weightStatus = "", // Removed "0kg today"
                        recentActivity = metrics,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun getBpStatus(sys: Int, dia: Int): String {
        return when {
            sys >= 180 || dia >= 120 -> "Crisis"
            sys >= 140 || dia >= 90 -> "Stage 2"
            sys >= 130 || dia >= 80 -> "Stage 1"
            sys >= 120 && dia < 80 -> "Elevated"
            sys > 0 && dia > 0 -> "Normal"
            else -> "Unknown"
        }
    }

    fun logMetric(type: MetricType, value1: Double, value2: Double? = null, unit: String) {
        viewModelScope.launch {
            val metric = HealthMetric(
                type = type,
                valuePrimary = value1,
                valueSecondary = value2,
                unit = unit,
                recordedAt = Date()
            )
            logHealthMetricUseCase.execute(profileId, metric)
            toggleLogPanel(false)
        }
    }

    fun toggleLogPanel(show: Boolean) {
        _uiState.update { it.copy(showLogPanel = show) }
    }

    private fun isSameDay(d1: Date, d2: Date): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { time = d1 }
        val cal2 = java.util.Calendar.getInstance().apply { time = d2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }
}

// Extension for MutableStateFlow update
private inline fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
    val prevValue = value
    val nextValue = function(prevValue)
    value = nextValue
}
