package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.data.local.dao.ProfileDao
import com.example.pillmate.data.local.entity.ProfileEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileViewModel(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val profileDao: ProfileDao
) : ViewModel() {

    val localProfiles: StateFlow<List<ProfileEntity>> = profileDao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val currentLocalProfile: StateFlow<ProfileEntity?> = profileDao.getCurrentProfileFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun syncCurrentProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            val activeProfile = profileDao.getActiveProfile()
            val targetUid = activeProfile?.id ?: auth.currentUser?.uid ?: return@launch

            try {
                val doc = db.collection("profiles").document(targetUid).get().await()
                if (doc.exists()) {
                    val name = doc.getString("fullName") ?: "Unknown User"
                    val dobMillis = doc.getLong("dateOfBirth")
                    val healthInfo = doc.getString("healthInformation") ?: ""

                    val existing = profileDao.getProfileById(targetUid)
                    if (existing == null) {
                        profileDao.clearCurrentProfile()
                        profileDao.insertProfile(
                            ProfileEntity(targetUid, name, dobMillis, healthInfo, "Primary User", true)
                        )
                    } else {
                        profileDao.insertProfile(
                            existing.copy(name = name, isCurrent = true, dateOfBirth = dobMillis, healthInformation = healthInfo)
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadProfileDetails(profileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val doc = db.collection("profiles").document(profileId).get().await()
                if (doc.exists()) {
                    val name = doc.getString("fullName") ?: "Unknown User"
                    val dobMillis = doc.getLong("dateOfBirth")
                    val healthInfo = doc.getString("healthInformation") ?: ""

                    val existing = profileDao.getProfileById(profileId)
                    if (existing != null) {
                        profileDao.insertProfile(
                            existing.copy(
                                name = name,
                                dateOfBirth = dobMillis,
                                healthInformation = healthInfo
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveProfile(name: String, dobMillis: Long?, healthInfo: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeId = currentLocalProfile.value?.id ?: return@launch

            val updates = mapOf(
                "fullName" to name,
                "dateOfBirth" to dobMillis,
                "healthInformation" to healthInfo
            )

            try {
                db.collection("profiles").document(activeId).set(updates, SetOptions.merge()).await()
                val currentEntity = profileDao.getProfileById(activeId)
                if (currentEntity != null) {
                    profileDao.insertProfile(
                        currentEntity.copy(
                            name = name,
                            dateOfBirth = dobMillis,
                            healthInformation = healthInfo
                        )
                    )
                }
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun switchActiveProfile(profileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            profileDao.clearCurrentProfile()

            val profile = profileDao.getProfileById(profileId)
            if (profile != null) {
                profileDao.insertProfile(profile.copy(isCurrent = true))
            }

            loadProfileDetails(profileId)
        }
    }
}