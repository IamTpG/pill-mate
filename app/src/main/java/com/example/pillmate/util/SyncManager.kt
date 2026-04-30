package com.example.pillmate.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import com.example.pillmate.data.repository.HybridRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SyncManager(
    private val firebaseAuth: FirebaseAuth
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isMonitoring = false
    private val hybridRepositories = mutableListOf<HybridRepositoryImpl<*>>()

    private val profileId: String
        get() = firebaseAuth.currentUser?.uid ?: ""

    fun register(vararg repos: HybridRepositoryImpl<*>) {
        repos.filterNotNull().forEach { repo ->
            hybridRepositories.add(repo)
        }
    }

    fun startMonitoring(context: Context) {
        if (isMonitoring) return
        isMonitoring = true
        startNetworkMonitoring(context)
        startProfileMonitoring()
    }

    private fun startNetworkMonitoring(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch {
                    delay(500)
                    if (profileId.isNotEmpty()) {
                        hybridRepositories.forEach { it.syncAll(profileId) }
                    }
                }
            }
        }
        val request = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(request, callback)
    }

    private fun startProfileMonitoring() {
        firebaseAuth.addAuthStateListener {
            scope.launch {
                val currentProfile = it.currentUser?.uid
                if (!currentProfile.isNullOrEmpty()) {
                    hybridRepositories.forEach { repo ->
                        repo.syncAll(currentProfile)
                        repo.startRemoteListener(currentProfile, scope)
                    }
                }
            }
        }
    }

    suspend fun syncAllNow() {
        if (profileId.isNotEmpty()) {
            hybridRepositories.forEach { it.syncAll(profileId) }
        }
    }
}
