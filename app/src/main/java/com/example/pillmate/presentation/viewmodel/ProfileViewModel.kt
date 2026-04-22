package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _profileData = MutableStateFlow<Map<String, String>>(emptyMap())
    val profileData = _profileData.asStateFlow()

    fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("profiles").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                _profileData.value = mapOf(
                    "fullName" to (doc.getString("fullName") ?: ""),
                    "dateOfBirth" to (doc.getString("dateOfBirth") ?: ""),
                    "healthInformation" to (doc.getString("healthInformation") ?: "")
                )
            }
        }
    }

    fun saveProfile(name: String, dob: String, healthInfo: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "fullName" to name,
            "dateOfBirth" to dob,
            "healthInformation" to healthInfo
        )
        db.collection("profiles").document(uid).update(updates).addOnSuccessListener {
            onSuccess()
        }
    }
}