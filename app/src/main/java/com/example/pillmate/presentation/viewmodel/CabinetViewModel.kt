package com.example.pillmate.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.data.local.dao.ProfileDao
import com.example.pillmate.domain.model.InventoryLog
import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.domain.usecase.CheckLowStockUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.UUID

import com.example.pillmate.domain.usecase.DeleteMedicationUseCase
import kotlin.math.abs

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
    private val medicationRepository: MedicationRepository,
    private val deleteMedicationUseCase: DeleteMedicationUseCase,
    private val profileDao: ProfileDao,
    private val checkLowStockUseCase: CheckLowStockUseCase,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CabinetUiState())
    val uiState: StateFlow<CabinetUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        observeCabinet()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeCabinet() {
        viewModelScope.launch {
            profileDao.getCurrentProfileFlow().flatMapLatest { profile ->
                if (profile != null) {
                    combine(
                        medicationRepository.getAll(profile.id),
                        _searchQuery
                    ) { allMedications, query ->

                        val filteredMeds = if (query.isBlank()) {
                            allMedications
                        } else {
                            allMedications.filter { it.name.contains(query, ignoreCase = true) }
                        }

                        val expired = filteredMeds.filter {
                            it.supply?.expirationDate?.before(java.util.Date()) == true
                        }
                        val active = filteredMeds.filter {
                            !(it.supply?.expirationDate?.before(java.util.Date()) ?: false)
                        }

                        val penalty = expired.size * 5
                        val score = (100 - penalty).coerceIn(0, 100)

                        val lowStockResults = filteredMeds.map { med ->
                            checkLowStockUseCase.execute(profile.id, med.id)
                        }

                        CabinetUiState(
                            isLoading = false,
                            healthScore = score,
                            activeMedsCount = active.size,
                            lowStockCount = lowStockResults.count { it.isLow },
                            searchQuery = query,
                            activeMedications = active,
                            expiredMedications = expired
                        )
                    }
                } else {
                    flowOf(CabinetUiState(isLoading = false))
                }
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

    fun addMedication(name: String, unit: String, initialCount: Float, description: String, expirationDate: Long, imageUriStr: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeProfileId = profileDao.getCurrentProfileFlow().firstOrNull()?.id ?: return@launch

            val photoUrl = saveImageToInternalStorage(imageUriStr)
            val newId = UUID.randomUUID().toString()

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

            medicationRepository.add(activeProfileId, newMedication)

            if (initialCount > 0) {
                medicationRepository.logInventoryChange(
                    profileId = activeProfileId,
                    medicationId = newId,
                    amount = initialCount,
                    reason = "INITIAL_STOCK"
                )
            }
        }
    }

    fun logDose(medicationId: String, amountTaken: Float, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeProfileId = profileDao.getCurrentProfileFlow().firstOrNull()?.id ?: return@launch

            val currentStock = _uiState.value.activeMedications
                .find { it.id == medicationId }?.supply?.quantity
                ?: _uiState.value.expiredMedications
                    .find { it.id == medicationId }?.supply?.quantity
                ?: 0f
            val clampedAmount = amountTaken.coerceAtMost(currentStock)
            if (clampedAmount <= 0) return@launch
            medicationRepository.logInventoryChange(
                profileId = activeProfileId,
                medicationId = medicationId,
                amount = -clampedAmount,
                reason = reason.ifBlank { "Taken" }
            )
        }
    }

    fun getLogsForMedication(medicationId: String): Flow<List<InventoryLog>> {
        return medicationRepository.getLogsForMedication(medicationId)
    }

    fun updateMedication(
        existingMedication: Medication,
        newName: String,
        newUnit: String,
        newCount: Float,
        newDescription: String,
        newExpirationDate: Long,
        imageUriStr: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeProfileId = profileDao.getCurrentProfileFlow().firstOrNull()?.id ?: return@launch

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

            medicationRepository.update(activeProfileId, updatedMedication)

            val currentQuantity = existingMedication.supply?.quantity ?: 0f
            if (abs(newCount - currentQuantity) > 0.001f) {
                val difference = newCount - currentQuantity
                medicationRepository.logInventoryChange(
                    profileId = activeProfileId,
                    medicationId = existingMedication.id,
                    amount = difference,
                    reason = "ADJUSTMENT"
                )
            }
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeProfileId = profileDao.getCurrentProfileFlow().firstOrNull()?.id ?: return@launch
            deleteMedicationUseCase(activeProfileId, medication.id)
        }
    }
}