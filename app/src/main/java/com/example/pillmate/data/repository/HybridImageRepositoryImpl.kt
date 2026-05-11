package com.example.pillmate.data.repository

import android.net.Uri
import com.example.pillmate.data.local.dao.ImageDao
import com.example.pillmate.data.local.entity.MedicalImageEntity
import com.example.pillmate.domain.model.MedicalImage
import com.example.pillmate.domain.repository.ImageRepository
import com.example.pillmate.domain.repository.LocalRepository
import com.example.pillmate.domain.repository.RemoteRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

class RoomImageRepositoryImpl(private val dao: ImageDao) : RoomRepositoryImpl<MedicalImage, MedicalImageEntity>(
    getAllFlow = { dao.getAllImages(it) },
    getAllOnceFunc = { dao.getAllImagesOnce(it) },
    getByIdFunc = { pId, id -> dao.getImageById(pId, id) },
    insert = { dao.insertImage(it) },
    updateFunc = { dao.insertImage(it) },
    deleteById = { pId, id -> dao.deleteImage(pId, id) },
    toDomain = { MedicalImage(it.id, it.localUri, it.remoteUrl, Date(it.createdAt), Date(it.updatedAt), it.deletedAt?.let { d -> Date(d) }) },
    toEntity = { pId, dom -> MedicalImageEntity(dom.id, pId, dom.localUri, dom.remoteUrl, dom.createdAt.time, dom.updatedAt.time, dom.deletedAt?.time) },
    getId = { it.id }
)

class FirestoreImageRepositoryImpl(firestore: FirebaseFirestore) : FirestoreRepositoryImpl<MedicalImage>(
    getCollectionReference = { firestore.collection("profiles").document(it).collection("medical_images") },
    modelClass = MedicalImage::class.java,
    setId = { item, id -> item.copy(id = id) }
) { override fun getId(item: MedicalImage) = item.id }

class HybridImageRepositoryImpl(
    localRepo: LocalRepository<MedicalImage>,
    remoteRepo: RemoteRepository<MedicalImage>,
    private val storage: FirebaseStorage,
    private val networkChecker: () -> Boolean
) : HybridRepositoryImpl<MedicalImage>(
    localRepo = localRepo, remoteRepo = remoteRepo, networkChecker = networkChecker,
    getId = { it.id }, getUpdatedAt = { it.updatedAt }, getDeletedAt = { it.deletedAt },
    copyWithUpdated = { item, date -> item.copy(updatedAt = date) },
    copyWithDeleted = { item, date -> item.copy(deletedAt = date) }
), ImageRepository {

    override suspend fun add(profileId: String, item: MedicalImage): Result<Unit> = runCatching {
        localRepo.add(profileId, item).getOrThrow()

        if (networkChecker() && item.localUri != null) {
            try {
                val uri = if (item.localUri.startsWith("content://") || item.localUri.startsWith("file://")) {
                    android.net.Uri.parse(item.localUri)
                } else {
                    android.net.Uri.fromFile(java.io.File(item.localUri))
                }

                val storageRef = storage.reference.child("profiles/$profileId/images/${item.id}.jpg")
                storageRef.putFile(uri).await()

                val downloadUrl = storageRef.downloadUrl.await().toString()
                val updatedItem = item.copy(remoteUrl = downloadUrl, updatedAt = Date())

                localRepo.update(profileId, updatedItem)
                remoteRepo.add(profileId, updatedItem).getOrThrow()
            } catch (e: Exception) {
                android.util.Log.e("ImageVault", "Upload Storage Error: ${e.message}", e)
            }
        }
    }
}