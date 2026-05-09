package com.example.pillmate.domain.usecase

import com.example.pillmate.data.local.dao.ProfileDao
import com.example.pillmate.domain.model.MetricType
import com.example.pillmate.domain.repository.HealthMetricRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.*

data class WidgetData(
    val nextTask: NextTaskInfo?,
    val hydrationMl: Int,
    val hydrationGoal: Int
)

class GetWidgetDataUseCase(
    private val getNextTaskUseCase: GetNextTaskUseCase,
    private val healthMetricRepository: HealthMetricRepository,
    private val profileDao: ProfileDao
) {
    suspend fun execute(profileId: String): WidgetData {
        val nextTask = getNextTaskUseCase.execute(profileId)
        
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val metrics = healthMetricRepository.getRecentMetrics(profileId, 50).firstOrNull() ?: emptyList()
        val hydrationMl = metrics.filter { 
            it.type == MetricType.WATER && isSameDay(it.recordedAt, Date()) 
        }.sumOf { it.valuePrimary }.toInt()
        
        val profile = profileDao.getProfileById(profileId)
        val goal = profile?.hydrationGoal ?: 2500
        
        return WidgetData(nextTask, hydrationMl, goal)
    }

    private fun isSameDay(d1: Date, d2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = d1 }
        val cal2 = Calendar.getInstance().apply { time = d2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
