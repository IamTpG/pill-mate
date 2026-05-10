package com.example.pillmate.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.data.local.dao.ProfileDao
import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.model.SupplyLog
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.domain.repository.SupplyLogRepository
import com.example.pillmate.domain.usecase.CalculateDailyIntakeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.firstOrNull
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.UUID

import com.example.pillmate.domain.repository.LogRepository
import com.example.pillmate.domain.usecase.DeleteMedicationUseCase
import java.util.Date

data class CabinetUiState(
    val isLoading: Boolean = true,
    val activeMedsCount: Int = 0,
    val lowStockCount: Int = 0,
    val searchQuery: String = "",
    val activeMedications: List<Medication> = emptyList(),
    val expiredMedications: List<Medication> = emptyList(),
    val dailyRequirements: Map<String, Float> = emptyMap()
)

class CabinetViewModel(
    private val medicationRepository: MedicationRepository,
    private val supplyLogRepository: SupplyLogRepository,
    private val deleteMedicationUseCase: DeleteMedicationUseCase,
    private val calculateDailyIntakeUseCase: CalculateDailyIntakeUseCase,
    private val scheduleRepository: ScheduleRepository,
    private val logRepository: LogRepository,
    private val profileDao: ProfileDao,
    private val auth: FirebaseAuth,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CabinetUiState())
    val uiState: StateFlow<CabinetUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        observeCabinet()
    }

    private suspend fun getEffectiveProfileId(): String? {
        val active = profileDao.getActiveProfile()?.id
        if (active != null) return active
        
        val anyLocal = profileDao.getAllProfiles().firstOrNull()?.firstOrNull()?.id
        if (anyLocal != null) return anyLocal
        
        return auth.currentUser?.uid
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeCabinet() {
        viewModelScope.launch {
            profileDao.getCurrentProfileFlow().flatMapLatest { profile ->
                val effectiveId = profile?.id ?: getEffectiveProfileId()
                
                if (effectiveId != null) {
                    val today = java.util.Date()
                    combine(
                        medicationRepository.getAll(effectiveId),
                        _searchQuery,
                        scheduleRepository.getAll(effectiveId),
                        logRepository.getLogsForDayFlow(effectiveId, today)
                    ) { allMedications, query, schedules, logs ->
                        val todayIntake = calculateDailyIntakeUseCase.computeIntake(schedules, today, logs)
                        android.util.Log.d("CabinetVM", "Today Intake: $todayIntake, Schedules: ${schedules.size}, Logs: ${logs.size}")

                        val filteredMeds = if (query.isBlank()) {
                            allMedications
                        } else {
                            allMedications.filter { it.name.contains(query, ignoreCase = true) }
                        }

                        val expired = filteredMeds.filter {
                            it.expirationDate?.before(today) == true
                        }
                        val active = filteredMeds.filter {
                            !(it.expirationDate?.before(today) ?: false)
                        }

                        val lowStock = filteredMeds.count { 
                            val requirement = todayIntake[it.id] ?: 0f
                            val isLow = requirement > 0f && it.quantity < requirement
                            if (requirement > 0f) {
                                android.util.Log.d("CabinetVM", "Checking Med: ${it.name}, Qty: ${it.quantity}, Req: $requirement, IsLow: $isLow")
                            }
                            isLow
                        }

                        CabinetUiState(
                            isLoading = false,
                            activeMedsCount = active.size,
                            lowStockCount = lowStock,
                            searchQuery = query,
                            activeMedications = active,
                            expiredMedications = expired,
                            dailyRequirements = todayIntake
                        )
                    }
                } else {
                    // Fallback empty state
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

    fun addMedication(name: String, unit: String, initialCount: Int, description: String, expirationDate: Long?, imageUriStr: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeProfileId = getEffectiveProfileId() ?: return@launch

            val photoUrl = saveImageToInternalStorage(imageUriStr)
            val newId = UUID.randomUUID().toString()

            val newMedication = Medication(
                id = newId,
                name = name,
                description = description,
                unit = unit,
                photoUrl = photoUrl,
                expirationDate = if (expirationDate != null && expirationDate > 0) java.util.Date(expirationDate) else null
            )

            medicationRepository.add(activeProfileId, newMedication)

            if (initialCount > 0) {
                supplyLogRepository.add(
                    activeProfileId,
                    SupplyLog(
                        id = UUID.randomUUID().toString(),
                        medId = newId,
                        changeAmount = initialCount.toFloat(),
                        reason = "INITIAL_STOCK",
                        createdAt = Date(),
                        updatedAt = Date()
                    )
                )
            }
        }
    }

    fun logDose(medicationId: String, amountTaken: Int, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeProfileId = getEffectiveProfileId() ?: return@launch

            val currentStock = _uiState.value.activeMedications
                .find { it.id == medicationId }?.quantity?.toInt()
                ?: _uiState.value.expiredMedications
                    .find { it.id == medicationId }?.quantity?.toInt()
                ?: 0
            val clampedAmount = amountTaken.coerceAtMost(currentStock.coerceAtLeast(0))
            if (clampedAmount <= 0) return@launch
            
            supplyLogRepository.add(
                activeProfileId,
                SupplyLog(
                    id = UUID.randomUUID().toString(),
                    medId = medicationId,
                    changeAmount = (-clampedAmount).toFloat(),
                    reason = reason.ifBlank { "Taken" },
                    createdAt = Date(),
                    updatedAt = Date()
                )
            )
        }
    }

    fun refillMedication(medicationId: String, amount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeProfileId = getEffectiveProfileId() ?: return@launch
            if (amount <= 0) return@launch

            supplyLogRepository.add(
                activeProfileId,
                SupplyLog(
                    id = UUID.randomUUID().toString(),
                    medId = medicationId,
                    changeAmount = amount.toFloat(),
                    reason = "REFILL",
                    createdAt = Date(),
                    updatedAt = Date()
                )
            )
        }
    }

    fun getLogsForMedication(medicationId: String): Flow<List<SupplyLog>> {
        return supplyLogRepository.getLogsForMedication(medicationId)
    }

    fun updateMedication(
        existingMedication: Medication,
        newName: String,
        newUnit: String,
        newCount: Int,
        newDescription: String,
        newExpirationDate: Long?,
        imageUriStr: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeProfileId = getEffectiveProfileId() ?: return@launch

            val photoUrl = saveImageToInternalStorage(imageUriStr) ?: existingMedication.photoUrl
            val updatedMedication = existingMedication.copy(
                name = newName,
                unit = newUnit,
                description = newDescription,
                photoUrl = photoUrl,
                expirationDate = if (newExpirationDate != null && newExpirationDate > 0) java.util.Date(newExpirationDate) else null
            )

            medicationRepository.update(activeProfileId, updatedMedication)

            val currentQuantity = existingMedication.quantity.toInt()
            if (newCount != currentQuantity) {
                val difference = newCount - currentQuantity
                supplyLogRepository.add(
                    activeProfileId,
                    SupplyLog(
                        id = UUID.randomUUID().toString(),
                        medId = existingMedication.id,
                        changeAmount = difference.toFloat(),
                        reason = "ADJUSTMENT",
                        createdAt = Date(),
                        updatedAt = Date()
                    )
                )
            }
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeProfileId = getEffectiveProfileId() ?: return@launch
            deleteMedicationUseCase(activeProfileId, medication.id)
        }
    }
}