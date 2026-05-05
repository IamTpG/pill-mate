package com.example.pillmate.domain.repository

import com.example.pillmate.domain.model.TaskLog
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface LogRepository : Repository<TaskLog> {
    fun getLogsForDayFlow(profileId: String, date: Date): Flow<List<TaskLog>>
}
