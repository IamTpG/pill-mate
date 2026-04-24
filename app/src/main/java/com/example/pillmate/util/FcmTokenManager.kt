package com.example.pillmate.util

import android.os.Build
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class FcmTokenManager(private val db: FirebaseFirestore) {

    suspend fun registerCurrentToken(profileId: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            saveToken(profileId, token)
            
            // Subscribe to profile-specific topic for broadcasts
            FirebaseMessaging.getInstance().subscribeToTopic("profile_$profileId").await()
            Log.d("FcmTokenManager", "Subscribed to topic: profile_$profileId")
        } catch (e: Exception) {
            Log.e("FcmTokenManager", "Failed to get/save FCM token", e)
        }
    }

    private suspend fun saveToken(profileId: String, token: String) {
        val tokenData = hashMapOf(
            "deviceName" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "platform" to "ANDROID",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("profiles").document(profileId)
            .collection("fcmTokens").document(token)
            .set(tokenData)
            .await()

        Log.d("FcmTokenManager", "FCM token registered for profile $profileId")
    }
}
