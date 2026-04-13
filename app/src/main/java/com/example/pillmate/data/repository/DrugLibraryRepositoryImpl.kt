package com.example.pillmate.data.repository

import com.example.pillmate.data.remote.api.OpenFdaApi
import com.example.pillmate.domain.model.DrugInfo
import com.example.pillmate.domain.repository.DrugLibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DrugLibraryRepositoryImpl(
    private val api: OpenFdaApi
) : DrugLibraryRepository {
    override fun searchDrug(query: String): Flow<Result<List<DrugInfo>>> = flow {
        try {
            // Build FDA query syntax
            val safeQuery = query.trim().replace(" ", "+")
            // Search either the brand name OR the generic name for maximum coverage
            val fdaQuery = "openfda.brand_name:\"$safeQuery\"+OR+openfda.generic_name:\"$safeQuery\""
            
            val response = api.searchDrugInfo(searchQuery = fdaQuery, limit = 10)
            
            val drugList = response.results?.mapNotNull { dto ->
                val brandName = dto.openFda?.brandName?.firstOrNull()
                val genericName = dto.openFda?.genericName?.firstOrNull()
                
                if (brandName == null && genericName == null) return@mapNotNull null
                
                val sections = mutableListOf<Pair<String, String>>()
                dto.indicationsAndUsage?.let { sections.add("Indications & Usage" to it.joinToString("\n\n")) }
                dto.purpose?.let { sections.add("Purpose" to it.joinToString("\n\n")) }
                dto.activeIngredient?.let { sections.add("Active Ingredient" to it.joinToString("\n\n")) }
                dto.dosageAndAdministration?.let { sections.add("Dosage & Administration" to it.joinToString("\n\n")) }
                dto.doNotUse?.let { sections.add("Do Not Use" to it.joinToString("\n\n")) }
                dto.stopUse?.let { sections.add("Stop Use" to it.joinToString("\n\n")) }
                dto.contraindications?.let { sections.add("Contraindications" to it.joinToString("\n\n")) }
                dto.pregnancyOrBreastFeeding?.let { sections.add("Pregnancy or Breast Feeding" to it.joinToString("\n\n")) }
                dto.questions?.let { sections.add("Questions" to it.joinToString("\n\n")) }
                dto.keepOutOfReachOfChildren?.let { sections.add("Keep Out of Reach of Children" to it.joinToString("\n\n")) }
                dto.storageAndHandling?.let { sections.add("Storage & Handling" to it.joinToString("\n\n")) }
                dto.inactiveIngredient?.let { sections.add("Inactive Ingredient" to it.joinToString("\n\n")) }

                DrugInfo(
                    brandName = brandName ?: genericName ?: "Unknown",
                    genericName = genericName ?: brandName ?: "Unknown",
                    warnings = dto.warnings?.joinToString("\n\n"),
                    infoSections = sections
                )
            } ?: emptyList()
            
            emit(Result.success(drugList))
            
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404) {
                // OpenFDA returns 404 when no results are found
                emit(Result.success(emptyList()))
            } else {
                emit(Result.failure(e))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
