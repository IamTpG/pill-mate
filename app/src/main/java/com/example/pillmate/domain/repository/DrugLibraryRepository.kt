package com.example.pillmate.domain.repository

import com.example.pillmate.domain.model.DrugInfo
import kotlinx.coroutines.flow.Flow

interface DrugLibraryRepository {
    fun searchDrug(query: String): Flow<Result<List<DrugInfo>>>
}
