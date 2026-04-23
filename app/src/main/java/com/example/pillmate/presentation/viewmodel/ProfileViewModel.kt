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

    fun syncCurrentProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Lấy người ĐANG ACTIVE trong máy (Ví dụ: A2)
            val activeProfile = profileDao.getActiveProfile()

            // 2. Nếu đã có người Active (A2), dùng ID của A2.
            // Nếu chưa có ai (app mới mở lần đầu), mới dùng ID của tài khoản gốc (A4)
            val targetUid = activeProfile?.id ?: auth.currentUser?.uid ?: return@launch

            // 3. Tải đúng người đó về và cập nhật
            db.collection("profiles").document(targetUid).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("fullName") ?: "Unknown User"
                    val dobMillis = doc.getLong("dateOfBirth")
                    val healthInfo = doc.getString("healthInformation") ?: ""

                    viewModelScope.launch(Dispatchers.IO) {
                        val existing = profileDao.getProfileById(targetUid)
                        if (existing == null) {
                            // Nếu chưa có trong Room, tạo mới và đánh dấu Active
                            profileDao.clearCurrentProfile()
                            profileDao.insertProfile(
                                ProfileEntity(targetUid, name, dobMillis, healthInfo, "Primary User", true)
                            )
                        } else {
                            // Cập nhật dữ liệu và LUÔN GIỮ NGƯỜI NÀY LÀM ACTIVE
                            profileDao.insertProfile(
                                existing.copy(name = name, isCurrent = true, dateOfBirth = dobMillis, healthInformation = healthInfo)
                            )
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
            profileDao.clearCurrentProfile()

            val profile = profileDao.getProfileById(profileId)
            if (profile != null) {
                profileDao.insertProfile(profile.copy(isCurrent = true))
            }

            loadProfileDetails(profileId)
        }
    }
}