package com.example.pillmate

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.pillmate.domain.usecase.SyncAlarmsUseCase
import com.example.pillmate.domain.usecase.SyncFcmTokenUseCase
import com.example.pillmate.presentation.ui.PillMateApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val syncAlarmsUseCase: SyncAlarmsUseCase by inject()
    private val syncFcmTokenUseCase: SyncFcmTokenUseCase by inject()
    private val profileId: String by inject()
    private val db: FirebaseFirestore by inject()
    private var scheduleListener: ListenerRegistration? = null
    private var syncJob: Job? = null
    private var fcmJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure FCM token and topic are synced for current profile
        if (profileId.isNotBlank()) {
            fcmJob = lifecycleScope.launch {
                try {
                    syncFcmTokenUseCase(profileId)
                } catch (e: Exception) {
                    Log.e("MainActivity", "FCM Sync failed", e)
                }
            }
        }

        // Setup Real-time Sync
        if (profileId.isNotBlank()) {
            scheduleListener = db.collection("profiles").document(profileId)
                .collection("schedules")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshot != null) {
                        // Debounce/Cancel previous sync if multiple fire quickly
                        syncJob?.cancel()
                        syncJob = lifecycleScope.launch {
                            try {
                                syncAlarmsUseCase(profileId)
                            } catch (e: Exception) {
                                // Silent fail for sync
                            }
                        }
                    }
                }
        }

        setContent {
            org.koin.compose.KoinContext {
                PillMateApp(
                    onSignOutComplete = {
                        // Navigation handled internally in PillMateApp
                    }
                )
            }
        }
    }
}