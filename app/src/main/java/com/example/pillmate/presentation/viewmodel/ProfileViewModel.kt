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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class ProfileViewModel(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val profileDao: ProfileDao
) : ViewModel() {

    val localProfiles: StateFlow<List<ProfileEntity>> = profileDao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val currentLocalProfile: StateFlow<ProfileEntity?> = profileDao.getCurrentProfileFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _shareCode = MutableStateFlow<String?>(null)
    val shareCode = _shareCode.asStateFlow()

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

    // Hàm tạo mã chia sẻ an toàn
    fun generateSecureShareCode() {
        val activeId = currentLocalProfile.value?.id ?: return

        // 1. Tạo mã 6 ký tự ngẫu nhiên (Chữ hoa và Số)
        val allowedChars = ('A'..'Z') + ('0'..'9')
        val newCode = (1..6).map { allowedChars.random() }.joinToString("")

        // 2. Lưu mã này lên Firestore của người bệnh
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updates = mapOf(
                    "shareCode" to newCode,
                    // Tùy chọn: Thêm thời gian hết hạn (VD: 24h sau)
                    "shareCodeExpiresAt" to System.currentTimeMillis() + (24 * 60 * 60 * 1000)
                )
                db.collection("profiles").document(activeId).set(updates, SetOptions.merge()).await()

                // 3. Cập nhật UI
                _shareCode.value = newCode
            } catch (e: Exception) {
                // Xử lý lỗi (Log hoặc Toast)
            }
        }
    }

    // Hàm dọn dẹp mã khi không dùng nữa (Bảo mật)
    fun clearShareCode() {
        _shareCode.value = null
    }

    // Hàm nhập mã và liên kết người bệnh
    fun linkCaregiverProfile(shareCode: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Tìm kiếm trên Firestore bằng shareCode
                val snapshot = db.collection("profiles").whereEqualTo("shareCode", shareCode).get().await()

                if (snapshot.isEmpty) {
                    launch(Dispatchers.Main) { onResult(false, "Mã không hợp lệ hoặc đã hết hạn.") }
                    return@launch
                }

                val doc = snapshot.documents.first()
                val targetUid = doc.id

                // 2. Chặn việc tự nhập mã của chính mình
                if (targetUid == auth.currentUser?.uid) {
                    launch(Dispatchers.Main) { onResult(false, "Không thể tự theo dõi chính mình.") }
                    return@launch
                }

                val name = doc.getString("fullName") ?: "Unknown Patient"
                val dobMillis = doc.getLong("dateOfBirth")
                val healthInfo = doc.getString("healthInformation") ?: ""

                // 3. Lưu vào Room DB trên máy người chăm sóc với role = "Caregiver_View"
                val existing = profileDao.getProfileById(targetUid)
                if (existing == null) {
                    profileDao.insertProfile(
                        ProfileEntity(targetUid, name, dobMillis, healthInfo, "Caregiver_View", false)
                    )
                } else {
                    profileDao.insertProfile(
                        existing.copy(name = name, dateOfBirth = dobMillis, healthInformation = healthInfo, role = "Caregiver_View")
                    )
                }

                launch(Dispatchers.Main) { onResult(true, "Đã liên kết thành công với $name!") }
            } catch (e: Exception) {
                launch(Dispatchers.Main) { onResult(false, "Lỗi kết nối: ${e.message}") }
            }
        }
    }
}