package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.repository.CabinetRepository

class LogInventoryChangeUseCase(private val repository: CabinetRepository) {
    operator fun invoke(profileId: String, medicationId: String, amount: Int, reason: String) {
        TODO("Implementation to be migrated from repo")
    }
}
