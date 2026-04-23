package com.example.pillmate.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.repository.CabinetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import com.example.pillmate.domain.model.Medication
import kotlinx.coroutines.Dispatchers
import java.util.UUID

data class CabinetUiState(
    val isLoading: Boolean = true,
    val healthScore: Int = 100,
    val activeMedsCount: Int = 0,
    val lowStockCount: Int = 0,
    val searchQuery: String = "",
    val activeMedications: List<Medication> = emptyList(),
    val expiredMedications: List<Medication> = emptyList()
)

class CabinetViewModel(
    private val cabinetRepository: CabinetRepository,
    application: Application
) : AndroidViewModel(application) {

    // The single source of truth for the UI
    private val _uiState = MutableStateFlow(CabinetUiState())
    val uiState: StateFlow<CabinetUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        observeCabinet()
    }

    private fun observeCabinet() {
        viewModelScope.launch {
            combine(
                cabinetRepository.getCabinetMedications(),
                _searchQuery
            ) { allMedications, query ->
                
                // 1. Global counts (unaffected by search)
                val allExpired = allMedications.filter {
                    it.supply?.expirationDate?.before(java.util.Date()) == true
                }
                val allActive = allMedications.filter {
                    !(it.supply?.expirationDate?.before(java.util.Date()) ?: false)
                }
                val penalty = allExpired.size * 5
                val score = (100 - penalty).coerceIn(0, 100)

                // 2. Handle the Search Bar for display list
                val filteredMeds = if (query.isBlank()) {
                    allMedications
                } else {
                    allMedications.filter { it.name.contains(query, ignoreCase = true) }
                }

                // 3. Split filtered into Expired vs Active for display
                val expired = filteredMeds.filter {
                    it.supply?.expirationDate?.before(java.util.Date()) == true
                }
                val active = filteredMeds.filter {
                    !(it.supply?.expirationDate?.before(java.util.Date()) ?: false)
                }

                // 4. Output state
                CabinetUiState(
                    isLoading = false,
                    healthScore = score,
                    activeMedsCount = allActive.size,
                    lowStockCount = allMedications.count { (it.supply?.quantity ?: 0f) < 10f },
                    searchQuery = query,
                    activeMedications = active,
                    expiredMedications = expired
                )
            }.collect { finalState ->
                _uiState.value = finalState
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private fun saveImageToInternalStorage(imageUriStr: String?): String? {
        if (imageUriStr == null) return null
        val context = getApplication<Application>()
        val imageUri = android.net.Uri.parse(imageUriStr)
        var photoUrl: String? = null
        try {
            val fileName = "med_${System.currentTimeMillis()}.jpg"
            val file = java.io.File(context.filesDir, fileName)
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                java.io.FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            photoUrl = file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return photoUrl
    }

    fun addMedication(name: String, unit: String, initialCount: Int, description: String, expirationDate: Long, imageUriStr: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val photoUrl = saveImageToInternalStorage(imageUriStr)
            val newId = UUID.randomUUID().toString()
            
            // 1. Create base Medication
            val newMedication = Medication(
                id = newId,
                name = name,
                description = description,
                unit = unit,
                photoUrl = photoUrl,
                supply = com.example.pillmate.domain.model.MedicationSupply(
                    quantity = 0f,
                    expirationDate = if (expirationDate > 0) java.util.Date(expirationDate) else null
                )
            )
            
            cabinetRepository.insertMedication(newMedication)

            // 2. Log initial stock
            if (initialCount > 0) {
                cabinetRepository.logInventoryChange(
                    medicationId = newId,
                    amount = initialCount,
                    reason = "INITIAL_STOCK"
                )
            }
        }
    }

    fun logDose(medicationId: String, amountTaken: Int, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Find current stock to enforce floor of 0
            val currentStock = _uiState.value.activeMedications
                .find { it.id == medicationId }?.supply?.quantity?.toInt()
                ?: _uiState.value.expiredMedications
                    .find { it.id == medicationId }?.supply?.quantity?.toInt()
                ?: 0
            val clampedAmount = amountTaken.coerceAtMost(currentStock.coerceAtLeast(0))
            if (clampedAmount <= 0) return@launch
            cabinetRepository.logInventoryChange(
                medicationId = medicationId,
                amount = -clampedAmount,
                reason = reason.ifBlank { "Taken" }
            )
        }
    }

    fun getLogsForMedication(medicationId: String): Flow<List<com.example.pillmate.data.local.entity.SupplyLogEntity>> {
        return cabinetRepository.getLogsForMedication(medicationId)
    }

    fun updateMedication(
        existingMedication: Medication,
        newName: String,
        newUnit: String,
        newCount: Int,
        newDescription: String,
        newExpirationDate: Long,
        imageUriStr: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val photoUrl = saveImageToInternalStorage(imageUriStr) ?: existingMedication.photoUrl
            val updatedMedication = existingMedication.copy(
                name = newName,
                unit = newUnit,
                description = newDescription,
                photoUrl = photoUrl,
                supply = existingMedication.supply?.copy(
                    expirationDate = if (newExpirationDate > 0) java.util.Date(newExpirationDate) else null
                ) ?: com.example.pillmate.domain.model.MedicationSupply(
                    expirationDate = if (newExpirationDate > 0) java.util.Date(newExpirationDate) else null,
                    quantity = 0f
                )
            )
            
            cabinetRepository.updateMedication(updatedMedication)

            val currentQuantity = (existingMedication.supply?.quantity ?: 0f).toInt()
            if (newCount != currentQuantity) {
                val difference = newCount - currentQuantity
                cabinetRepository.logInventoryChange(
                    medicationId = existingMedication.id,
                    amount = difference,
                    reason = "ADJUSTMENT"
                )
            }
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch(Dispatchers.IO) {
            cabinetRepository.deleteMedication(medication)
        }
    }
}