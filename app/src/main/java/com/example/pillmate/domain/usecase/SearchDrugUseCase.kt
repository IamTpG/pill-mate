package com.example.pillmate.domain.usecase

import com.example.pillmate.domain.model.DrugInfo
import com.example.pillmate.domain.repository.DrugLibraryRepository
import kotlinx.coroutines.flow.Flow

class SearchDrugUseCase(private val repository: DrugLibraryRepository) {
    operator fun invoke(query: String): Flow<Result<List<DrugInfo>>> {
        TODO("Implementation to be migrated from repo")
    }
}
