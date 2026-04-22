package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.data.local.dao.ProfileDao
import com.example.pillmate.data.local.entity.ProfileEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val profileDao: ProfileDao
) : ViewModel() {

    private val _profileData = MutableStateFlow<Map<String, String>>(emptyMap())
    val profileData = _profileData.asStateFlow()

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

                viewModelScope.launch(Dispatchers.IO) {
                    val existing = profileDao.getProfileById(uid)
                    val activeProfile = profileDao.getActiveProfile()

                    if (existing == null) {
                        profileDao.clearCurrentProfile()
                        profileDao.insertProfile(
                            ProfileEntity(id = uid, name = name, role = "Primary User", isCurrent = true)
                        )
                    } else {
                        if (activeProfile == null) {
                            profileDao.insertProfile(existing.copy(name = name, isCurrent = true))
                        } else {
                            profileDao.insertProfile(existing.copy(name = name))
                        }
                    }
                }
            }
        }
    }

    // Loads specific data for the Editor Screen
    private fun loadProfileDetails(profileId: String) {
        db.collection("profiles").document(profileId).get().addOnSuccessListener { doc ->
            val name = doc.getString("fullName") ?: "Unknown User"
            _profileData.value = mapOf(
                "fullName" to name,
                "dateOfBirth" to (doc.getString("dateOfBirth") ?: ""),
                "healthInformation" to (doc.getString("healthInformation") ?: "")
            )
        }
    }

    fun saveProfile(name: String, dob: String, healthInfo: String, onSuccess: () -> Unit) {
        // 🟢 Use the ACTIVE profile ID, not auth.currentUser
        val activeId = currentLocalProfile.value?.id ?: return
        val updates = mapOf(
            "fullName" to name,
            "dateOfBirth" to dob,
            "healthInformation" to healthInfo
        )

        // Use set with merge in case this caregiver profile doesn't have a Firestore document yet
        db.collection("profiles").document(activeId).set(updates, SetOptions.merge()).addOnSuccessListener {
            // Also update the Room database name so the Switch screen updates instantly
            viewModelScope.launch(Dispatchers.IO) {
                val currentEntity = profileDao.getProfileById(activeId)
                if (currentEntity != null) {
                    profileDao.insertProfile(currentEntity.copy(name = name))
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