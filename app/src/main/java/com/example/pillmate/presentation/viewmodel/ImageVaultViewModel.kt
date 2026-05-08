package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.data.repository.HybridRepositoryImpl
import com.example.pillmate.domain.model.MedicalImage
import com.example.pillmate.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

import android.util.Log

class ImageVaultViewModel(
    private val imageRepository: ImageRepository,
) : ViewModel() {

    private val _images = MutableStateFlow<List<MedicalImage>>(emptyList())
    val images = _images.asStateFlow()

    fun loadImages(profileId: String) {
        viewModelScope.launch {
            imageRepository.getAll(profileId).collect { list ->
                _images.value = list
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                (imageRepository as? com.example.pillmate.data.repository.HybridRepositoryImpl<*>)?.syncAll(profileId)
            } catch (e: Exception) {
                Log.e("ImageVault", "Sync errors: ${e.message}", e)
            }
        }
    }

    fun uploadImage(context: Context, uri: Uri, profileId: String) {
        val newId = UUID.randomUUID().toString()
        val safeLocalPath = copyToInternalStorage(context, uri, newId)
        val newImage = MedicalImage(
            id = newId,
            localUri = safeLocalPath ?: uri.toString(),
            remoteUrl = null
        )

        viewModelScope.launch {
            imageRepository.add(profileId, newImage)
        }
    }

    private fun copyToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)

            val file = File(context.filesDir, "$fileName.jpg")
            val outputStream = FileOutputStream(file)

            inputStream?.copyTo(outputStream)

            inputStream?.close()
            outputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}