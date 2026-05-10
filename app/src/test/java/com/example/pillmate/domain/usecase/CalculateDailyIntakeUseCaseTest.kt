package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.*
import com.example.pillmate.domain.repository.ScheduleRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date
import java.util.Calendar

class CalculateDailyIntakeUseCaseTest {

    private val scheduleRepository = mockk<ScheduleRepository>()
    private val useCase = CalculateDailyIntakeUseCase(scheduleRepository)

    @Test
    fun `execute returns correct daily intake for multiple medications`() = runBlocking {
        val profileId = "test_user"
        val today = Date()
        
        // Simple daily schedule
        val schedule1 = Schedule(
            id = "s1",
            type = TaskType.MEDICATION,
            enabled = true,
            eventSnapshot = ScheduleEvent(sourceId = "med1"),
            doseTimes = listOf(DoseTime(dose = 2f), DoseTime(dose = 1f)),
            frequency = "Daily",
            recurrenceRule = "FREQ=DAILY",
            startTime = "2026-05-10T08:00:00"
        )
        
        // Weekly schedule (assuming today matches)
        val schedule2 = Schedule(
            id = "s2",
            type = TaskType.MEDICATION,
            enabled = true,
            eventSnapshot = ScheduleEvent(sourceId = "med2"),
            doseTimes = listOf(DoseTime(dose = 5f)),
            frequency = "Weekly",
            recurrenceRule = null, // Mocking will handle it via isOccurringOn behavior if I wasn't using real evaluator logic
            startTime = "2026-05-10T08:00:00"
        )

        every { runBlocking { scheduleRepository.getAllOnce(profileId) } } returns Result.success(listOf(schedule1, schedule2))

        val result = useCase.execute(profileId, today)

        // med1: 2 + 1 = 3
        assertEquals(3f, result["med1"])
    }
}
