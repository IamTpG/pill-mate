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

class ProfileViewModel(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val profileDao: ProfileDao
) : ViewModel() {

    val localProfiles: StateFlow<List<ProfileEntity>> = profileDao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val currentLocalProfile: StateFlow<ProfileEntity?> = profileDao.getCurrentProfileFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        // Automatically fetch Firestore details whenever the active profile changes!
        viewModelScope.launch {
            currentLocalProfile.collect { profile ->
                if (profile != null) {
                    loadProfileDetails(profile.id)
                }
            }
        }
    }

    fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("profiles").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val name = doc.getString("fullName") ?: "Unknown User"
                val dobMillis = doc.getLong("dateOfBirth")
                val healthInfo = doc.getString("healthInformation") ?: ""

                viewModelScope.launch(Dispatchers.IO) {
                    val existing = profileDao.getProfileById(uid)
                    val activeProfile = profileDao.getActiveProfile()

                    if (existing == null) {
                        profileDao.clearCurrentProfile()
                        profileDao.insertProfile(
                            ProfileEntity(
                                id = uid,
                                name = name,
                                role = "Primary User",
                                isCurrent = true,
                                dateOfBirth = dobMillis,
                                healthInformation = healthInfo
                            )
                        )
                    } else {
                        if (activeProfile == null) {
                            profileDao.insertProfile(existing.copy(
                                name = name,
                                isCurrent = true,
                                dateOfBirth = dobMillis,
                                healthInformation = healthInfo
                            ))
                        } else {
                            profileDao.insertProfile(existing.copy(
                                name = name,
                                dateOfBirth = dobMillis,
                                healthInformation = healthInfo
                            ))
                        }
                    }
                }
            }
        }
    }

    private fun loadProfileDetails(profileId: String) {
        db.collection("profiles").document(profileId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val name = doc.getString("fullName") ?: "Unknown User"
                val dobMillis = doc.getLong("dateOfBirth")
                val healthInfo = doc.getString("healthInformation") ?: ""

                viewModelScope.launch(Dispatchers.IO) {
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
            }
        }
    }

    fun saveProfile(name: String, dobMillis: Long?, healthInfo: String, onSuccess: () -> Unit) {
        val activeId = currentLocalProfile.value?.id ?: return

        val updates = mapOf(
            "fullName" to name,
            "dateOfBirth" to dobMillis,
            "healthInformation" to healthInfo
        )

        db.collection("profiles").document(activeId).set(updates, SetOptions.merge()).addOnSuccessListener {
            viewModelScope.launch(Dispatchers.IO) {
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
                launch(Dispatchers.Main) { onSuccess() }
            }
        }
    }

    fun switchActiveProfile(profileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            profileDao.switchProfile(profileId)
        }
    }
}