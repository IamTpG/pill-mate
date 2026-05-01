package com.example.pillmate.presentation.viewmodel

import com.example.pillmate.R
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.data.local.dao.ProfileDao
import com.example.pillmate.data.local.entity.ProfileEntity
import com.example.pillmate.data.local.entity.SavedAccountEntity
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map

class ProfileViewModel(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val profileDao: ProfileDao
) : ViewModel() {
    val savedAccounts: StateFlow<List<SavedAccountEntity>> = profileDao.getSavedAccounts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 1. Hồ sơ đang hoạt động (Dùng để hiển thị tên, lấy ID để tải thuốc/lịch hẹn...)
    val currentLocalProfile: StateFlow<ProfileEntity?> = profileDao.getCurrentProfileFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // 2. Danh sách các tài khoản đã đăng nhập (Dùng cho SwitchProfileScreen)
    // Lọc bỏ những người chỉ được theo dõi (Caregiver_View)
    val localProfiles: StateFlow<List<ProfileEntity>> = profileDao.getAllProfiles()
        .map { list -> list.filter { it.role != "Caregiver_View" } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 3. Danh sách những người đang theo dõi (Dùng cho FollowedListScreen)
    // Chỉ lấy những hồ sơ có vai trò là Caregiver_View
    val followedProfiles: StateFlow<List<ProfileEntity>> = profileDao.getAllProfiles()
        .map { list -> list.filter { it.role == "Caregiver_View" } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _shareCode = MutableStateFlow<String?>(null)
    val shareCode = _shareCode.asStateFlow()


    fun syncCurrentProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            val firebaseUser = auth.currentUser ?: return@launch
            val firebaseUid = firebaseUser.uid

            val activeProfile = profileDao.getActiveProfile()
            val targetUid = activeProfile?.id ?: firebaseUid

            try {
                if (targetUid == firebaseUid) {
                    val doc = db.collection("profiles").document(targetUid).get().await()
                    if (doc.exists()) {
                        val name = doc.getString("fullName") ?: "Unknown User"
                        val dobMillis = doc.getLong("dateOfBirth")
                        val healthInfo = doc.getString("healthInformation") ?: ""

                        profileDao.insertSavedAccount(
                            SavedAccountEntity(
                                id = firebaseUid,
                                email = firebaseUser.email ?: "",
                                name = name,
                                loginMethod = if (firebaseUser.providerData.any { it.providerId == "google.com" }) "GOOGLE" else "EMAIL"
                            )
                        )

                        profileDao.clearAllProfiles()
                        profileDao.insertProfile(ProfileEntity(firebaseUid, name, dobMillis, healthInfo, "Primary User", true))

                        syncFollowedProfiles(firebaseUid)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadProfileDetails(profileId: String, roleIfNew: String = "Caregiver_View") {
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
                    } else {
                        profileDao.insertProfile(
                            ProfileEntity(
                                id = profileId,
                                name = name,
                                dateOfBirth = dobMillis,
                                healthInformation = healthInfo,
                                role = roleIfNew,
                                isCurrent = false
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
            val targetProfile = profileDao.getProfileById(profileId) ?: return@launch

            profileDao.clearCurrentProfile()
            profileDao.insertProfile(targetProfile.copy(isCurrent = true))

            // 🟢 Nếu chuyển sang tài khoản chính, hãy đồng bộ danh sách người theo dõi của người đó
            if (targetProfile.role == "Primary User") {
                syncFollowedProfiles(profileId)
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
                // 1. Tìm Patient (User 1) qua mã code
                val snapshot = db.collection("profiles").whereEqualTo("shareCode", shareCode).get().await()
                if (snapshot.isEmpty) {
                    launch(Dispatchers.Main) { onResult(false, "Mã không hợp lệ.") }
                    return@launch
                }

                val patientDoc = snapshot.documents.first()
                val patientId = patientDoc.id
                val myUid = auth.currentUser?.uid ?: return@launch

                // 🟢 2. LƯU MỐI QUAN HỆ LÊN CLOUD (Dùng để đồng bộ sang máy khác/tài khoản khác)
                val connectionData = mapOf(
                    "caregiverId" to myUid,
                    "patientId" to patientId
                )
                db.collection("connections").document("${myUid}_$patientId").set(connectionData).await()

                // 3. Lưu vào Room để hiển thị ngay tại máy này
                val name = patientDoc.getString("fullName") ?: "Unknown"
                profileDao.insertProfile(
                    ProfileEntity(patientId, name, patientDoc.getLong("dateOfBirth"),
                        patientDoc.getString("healthInformation") ?: "", "Caregiver_View", false)
                )

                launch(Dispatchers.Main) { onResult(true, "Kết nối thành công!") }
            } catch (e: Exception) {
                launch(Dispatchers.Main) { onResult(false, "Lỗi: ${e.message}") }
            }
        }
    }

    private fun syncFollowedProfiles(myUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 2. Lấy danh sách connections của TÔI từ Firestore
                val connections = db.collection("connections")
                    .whereEqualTo("caregiverId", myUid).get().await()

                // 3. Tải thông tin chi tiết của từng người bệnh về máy
                connections.documents.forEach { doc ->
                    val patientId = doc.getString("patientId") ?: return@forEach
                    loadProfileDetails(patientId) // Hàm này sẽ gọi Firestore và save vào Room
                }
            } catch (e: Exception) { /* Log lỗi sync */ }
        }
    }
}