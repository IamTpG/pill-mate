package com.example.pillmate.domain.repository

import com.google.firebase.firestore.CollectionReference

interface RemoteRepository<T> : Repository<T> {
    fun getCollection(profileId: String): CollectionReference? = null
}
