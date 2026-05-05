package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.TaskLog
import com.example.pillmate.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import java.util.Date

class GetLogsForDayUseCase(private val repository: LogRepository) {
    operator fun invoke(profileId: String, date: Date): Flow<List<TaskLog>> {
        TODO("Implementation to be migrated from repo")
    }
}
