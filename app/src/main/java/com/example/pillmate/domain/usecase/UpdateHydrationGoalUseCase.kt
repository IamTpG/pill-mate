package com.example.pillmate.domain.usecase

import com.example.pillmate.data.local.dao.ProfileDao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UpdateHydrationGoalUseCase(
    private val db: FirebaseFirestore,
    private val profileDao: ProfileDao
) {
    suspend fun execute(profileId: String, goal: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Update Firestore
            db.collection("profiles").document(profileId).update("hydrationGoal", goal).await()
            
            // Update Local
            val current = profileDao.getProfileById(profileId)
            if (current != null) {
                profileDao.insertProfile(current.copy(hydrationGoal = goal))
            }
        }
    }
}
