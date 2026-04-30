package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.AppointmentLog
import com.example.pillmate.domain.repository.AppointmentRepository
import kotlinx.coroutines.flow.Flow
import java.util.Date

class GetAppointmentLogsUseCase(private val repository: AppointmentRepository) {
    operator fun invoke(profileId: String, date: Date): Flow<List<AppointmentLog>> {
        TODO("Implementation to be migrated from repo")
    }
}
