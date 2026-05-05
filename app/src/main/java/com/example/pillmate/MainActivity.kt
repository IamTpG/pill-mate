package com.example.pillmate

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.domain.usecase.SyncAlarmsUseCase
import com.example.pillmate.domain.usecase.SyncFcmTokenUseCase
import com.example.pillmate.presentation.ui.PillMateApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val syncAlarmsUseCase: SyncAlarmsUseCase by inject()
    private val syncFcmTokenUseCase: SyncFcmTokenUseCase by inject()
    private val syncManager: com.example.pillmate.util.SyncManager by inject()
    private val medicationRepository: MedicationRepository by inject()
    
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

        // Initialize SyncManager with Hybrid Repositories
        if (medicationRepository is com.example.pillmate.data.repository.HybridMedicationRepositoryImpl) {
            syncManager.register(medicationRepository as com.example.pillmate.data.repository.HybridMedicationRepositoryImpl)
        }
        syncManager.startMonitoring(this)

        // Setup Real-time Sync (with debounce to avoid clobbering ManageReminderUseCase)
        if (profileId.isNotBlank()) {
            var isInitialSnapshot = true
            scheduleListener = db.collection("profiles").document(profileId)
                .collection("schedules")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshot != null) {
                        // Skip the initial snapshot (existing data on registration)
                        if (isInitialSnapshot) {
                            isInitialSnapshot = false
                            Log.d("MainActivity", "Firestore listener: skipping initial snapshot")
                            return@addSnapshotListener
                        }
                        // Cancel any pending sync to debounce rapid-fire events
                        syncJob?.cancel()
                        syncJob = lifecycleScope.launch {
                            // Wait 3s so ManageReminderUseCase's alarm isn't immediately overwritten
                            kotlinx.coroutines.delay(3000)
                            Log.d("MainActivity", "Firestore listener: running debounced sync")
                                try {
                                    syncAlarmsUseCase(profileId)
                                    // Update home screen widget
                                    val widgetIntent = android.content.Intent("com.example.pillmate.ACTION_UPDATE_WIDGET")
                                    widgetIntent.setPackage(packageName)
                                    sendBroadcast(widgetIntent)
                                } catch (e: Exception) {
                                Log.e("MainActivity", "Sync from listener failed", e)
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