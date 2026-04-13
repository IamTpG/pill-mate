package com.example.pillmate.domain.usecase

import com.example.pillmate.util.FcmTokenManager

class SyncFcmTokenUseCase(
    private val fcmTokenManager: FcmTokenManager
) {
    suspend operator fun invoke(profileId: String) {
        fcmTokenManager.registerCurrentToken(profileId)
    }
}
