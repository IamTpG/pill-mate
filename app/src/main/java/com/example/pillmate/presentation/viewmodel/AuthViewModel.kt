package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.usecase.SyncAlarmsUseCase
import com.example.pillmate.domain.usecase.SyncFcmTokenUseCase
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class AuthViewModel(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val syncFcmTokenUseCase: SyncFcmTokenUseCase,
    private val syncAlarmsUseCase: SyncAlarmsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun signInWithGoogle(credential: AuthCredential) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = auth.signInWithCredential(credential).await()
                val user = result.user
                if (user != null) {
                    ensureProfileCreated(user.uid, user.displayName, user.email)
                    syncData(user.uid)
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "User is null") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    syncData(user.uid)
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun signUpWithEmail(fullname: String, email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(fullname)
                        .build()
                    user.updateProfile(profileUpdates).await()
                    
                    ensureProfileCreated(user.uid, fullname, email)
                    syncData(user.uid)
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun ensureProfileCreated(uid: String, fullName: String?, email: String?) {
        val docRef = db.collection("profiles").document(uid)
        val doc = docRef.get().await()
        if (!doc.exists()) {
            val userProfile = hashMapOf(
                "accountId" to uid,
                "fullName" to fullName,
                "email" to email,
                "type" to "USER",
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(userProfile).await()
        }
    }

    private fun syncData(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            syncFcmTokenUseCase(uid)
            syncAlarmsUseCase(uid)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
