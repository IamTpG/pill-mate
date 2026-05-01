package com.example.pillmate.domain.repository

import com.example.pillmate.domain.model.Appointment
import com.example.pillmate.domain.model.AppointmentLog
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface AppointmentRepository : Repository<Appointment> {
    fun getAppointmentLogs(profileId: String, date: Date): Flow<List<AppointmentLog>>
}