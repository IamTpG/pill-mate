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
import com.example.pillmate.data.local.dao.ProfileDao
import com.example.pillmate.domain.usecase.UpdateHydrationGoalUseCase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update

data class VitalsUiState(
    val hydrationMl: Int = 0,
    val hydrationTarget: Int = 2500,
    val latestBloodPressure: String = "--/--",
    val bloodPressureStatus: String = "Normal",
    val latestWeight: String = "--",
    val weightStatus: String = "0kg today",
    val recentActivity: List<HealthMetric> = emptyList(),
    val weeklyStats: WeeklyStats = WeeklyStats(),
    val isLoading: Boolean = false,
    val showLogPanel: Boolean = false,
    val showWeeklyReport: Boolean = false
)

data class WeeklyStats(
    val avgBP: String = "--/--",
    val totalWater: String = "0 ml",
    val avgWeight: String = "0.0 kg",
    val activityCounts: Map<MetricType, Int> = emptyMap(),
    val dateRange: String = "",
    val totalLogs: Int = 0,
    val avgWaterPerDay: String = "0 ml/day",
    val hydrationGoalDays: Int = 0,
    val hydrationGoalSummary: String = "0/7 days",
    val dailyHydration: List<DailyHydration> = emptyList(),
    val bpReadings: Int = 0,
    val highBpReadings: Int = 0,
    val latestBpStatus: String = "No data",
    val weightReadings: Int = 0,
    val weightChange: String = "No change",
    val insights: List<String> = emptyList()
)

data class DailyHydration(
    val label: String,
    val amountMl: Int,
    val goalMet: Boolean
)

class VitalsViewModel(
    private val getHealthMetricsUseCase: GetHealthMetricsUseCase,
    private val logHealthMetricUseCase: LogHealthMetricUseCase,
    private val updateHydrationGoalUseCase: UpdateHydrationGoalUseCase,
    private val profileDao: ProfileDao,
    private val auth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(VitalsUiState())
    val uiState: StateFlow<VitalsUiState> = _uiState.asStateFlow()

    init {
        observeProfile()
        watchProfile()
    }

    private suspend fun getEffectiveProfileId(): String? {
        val active = profileDao.getActiveProfile()?.id
        if (active != null) return active
        
        val anyLocal = profileDao.getAllProfiles().firstOrNull()?.firstOrNull()?.id
        if (anyLocal != null) return anyLocal
        
        return auth.currentUser?.uid
    }

    private fun observeProfile() {
        viewModelScope.launch {
            profileDao.getCurrentProfileFlow().collect { profile ->
                val effectiveId = profile?.id ?: getEffectiveProfileId()
                effectiveId?.let { id ->
                    loadData(id)
                }
            }
        }
    }

    private fun watchProfile() {
        viewModelScope.launch {
            profileDao.getCurrentProfileFlow().collect { profile ->
                profile?.let { p ->
                    _uiState.update { it.copy(hydrationTarget = p.hydrationGoal) }
                }
            }
        }
    }

    fun loadData(profileId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getHealthMetricsUseCase.execute(profileId, 100).collect { metrics ->
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
                        weeklyStats = calculateWeeklyStats(metrics, state.hydrationTarget),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun calculateWeeklyStats(metrics: List<HealthMetric>, hydrationTarget: Int): WeeklyStats {
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }
        val weekStart = java.util.Calendar.getInstance().apply {
            time = today.time
            add(java.util.Calendar.DAY_OF_YEAR, -6)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val weekMetrics = metrics.filter { !it.recordedAt.before(weekStart.time) && !it.recordedAt.after(today.time) }
        
        val bpList = weekMetrics.filter { it.type == MetricType.BLOOD_PRESSURE }
        val avgSys = if (bpList.isNotEmpty()) bpList.averageOf { it.valuePrimary } else 0.0
        val avgDia = if (bpList.isNotEmpty()) bpList.averageOf { it.valueSecondary ?: 0.0 } else 0.0
        val highBpCount = bpList.count {
            getBpStatus(it.valuePrimary.toInt(), it.valueSecondary?.toInt() ?: 0) != "Normal"
        }
        val latestBpStatus = bpList.maxByOrNull { it.recordedAt }?.let {
            getBpStatus(it.valuePrimary.toInt(), it.valueSecondary?.toInt() ?: 0)
        } ?: "No data"
        
        val weightList = weekMetrics.filter { it.type == MetricType.WEIGHT }.sortedBy { it.recordedAt }
        val avgWeight = if (weightList.isNotEmpty()) weightList.averageOf { it.valuePrimary } else 0.0
        val weightChange = if (weightList.size >= 2) {
            val delta = weightList.last().valuePrimary - weightList.first().valuePrimary
            when {
                delta > 0.05 -> "+${String.format("%.1f", delta)} kg"
                delta < -0.05 -> "${String.format("%.1f", delta)} kg"
                else -> "Stable"
            }
        } else {
            "Need 2 logs"
        }
        
        val waterTotal = weekMetrics.filter { it.type == MetricType.WATER }.sumOf { it.valuePrimary }
        val dailyHydration = (0..6).map { index ->
            val day = java.util.Calendar.getInstance().apply {
                time = weekStart.time
                add(java.util.Calendar.DAY_OF_YEAR, index)
            }
            val amount = weekMetrics.filter {
                it.type == MetricType.WATER && isSameDay(it.recordedAt, day.time)
            }.sumOf { it.valuePrimary }.toInt()
            DailyHydration(
                label = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(day.time),
                amountMl = amount,
                goalMet = hydrationTarget > 0 && amount >= hydrationTarget
            )
        }
        val goalDays = dailyHydration.count { it.goalMet }

        val insights = buildList {
            if (weekMetrics.isEmpty()) {
                add("No vitals logged in the last 7 days yet.")
            } else {
                add("You logged ${weekMetrics.size} vital ${if (weekMetrics.size == 1) "entry" else "entries"} this week.")
            }
            if (waterTotal > 0.0) {
                add("Hydration averaged ${String.format("%,d", (waterTotal / 7.0).toInt())} ml per day.")
            }
            if (bpList.isNotEmpty()) {
                add(if (highBpCount == 0) "All blood pressure readings were in the normal range." else "$highBpCount blood pressure reading${if (highBpCount == 1) "" else "s"} were above normal.")
            }
            if (weightList.size >= 2) {
                add("Weight changed $weightChange across the week.")
            }
        }
        
        return WeeklyStats(
            avgBP = if (avgSys > 0) "${avgSys.toInt()}/${avgDia.toInt()}" else "--/--",
            totalWater = String.format("%,d ml", waterTotal.toInt()),
            avgWeight = String.format("%.1f kg", avgWeight),
            activityCounts = weekMetrics.groupBy { it.type }.mapValues { it.value.size },
            dateRange = "${java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(weekStart.time)} - ${java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(today.time)}",
            totalLogs = weekMetrics.size,
            avgWaterPerDay = "${String.format("%,d", (waterTotal / 7.0).toInt())} ml/day",
            hydrationGoalDays = goalDays,
            hydrationGoalSummary = "$goalDays/7 days",
            dailyHydration = dailyHydration,
            bpReadings = bpList.size,
            highBpReadings = highBpCount,
            latestBpStatus = latestBpStatus,
            weightReadings = weightList.size,
            weightChange = weightChange,
            insights = insights
        )
    }

    private fun List<HealthMetric>.averageOf(selector: (HealthMetric) -> Double): Double {
        if (this.isEmpty()) return 0.0
        return this.sumOf(selector) / this.size
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
            val effectiveId = getEffectiveProfileId() ?: return@launch
            logHealthMetricUseCase.execute(effectiveId, metric)
            toggleLogPanel(false)
        }
    }

    fun updateHydrationTarget(target: Int) {
        viewModelScope.launch {
            val effectiveId = getEffectiveProfileId() ?: return@launch
            updateHydrationGoalUseCase.execute(effectiveId, target)
        }
    }

    fun toggleLogPanel(show: Boolean) {
        _uiState.update { it.copy(showLogPanel = show) }
    }

    fun toggleWeeklyReport(show: Boolean) {
        _uiState.update { it.copy(showWeeklyReport = show) }
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
