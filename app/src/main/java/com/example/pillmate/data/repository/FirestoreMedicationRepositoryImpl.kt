package com.example.pillmate.data.repository

import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.repository.MedicationRepository
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreMedicationRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val profileId: String
) :
    FirestoreRepositoryImpl<Medication>(
        getCollectionReference = { pid ->
            firestore.collection("profiles").document(pid).collection("medications")
        },
        modelClass = Medication::class.java,
        setId = { med, id -> med.copy(id = id) }
    ),
    MedicationRepository {

    override fun getId(item: Medication): String {
        return item.id.ifEmpty { firestore.collection("tmp").document().id }
    }
}

