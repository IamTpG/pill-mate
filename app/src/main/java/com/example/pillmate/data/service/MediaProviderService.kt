package com.example.pillmate.data.service

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import kotlin.io.path.createTempFile


class CameraProviderService(
	private val context: Context,
	caller: ActivityResultCaller,
	private val onResult: (Boolean) -> Unit) {
	
	private val takePictureLauncher = caller.registerForActivityResult(ActivityResultContracts.TakePicture()) {
		isSuccess -> onResult(isSuccess)
	}

	fun createPhotoUri(): Uri {
		val tempFile = File.createTempFile("IMG_", ".jpg", context.cacheDir).apply{
			createNewFile()
		}
		
		return FileProvider.getUriForFile(
			context,
			"${context.packageName}.fileprovider",
			tempFile
		)
	}
	
	fun openCamera(photoUri: Uri) {
		takePictureLauncher.launch(photoUri)
	}
}

class ImageProviderService(
	private val context: Context,
	caller: ActivityResultCaller,
	private val onResult: (Uri?) -> Unit) {
	
	private val takeGalleryLauncher = caller.registerForActivityResult(ActivityResultContracts.GetContent()) {
		uri: Uri ? ->
		if (uri != null) {
			onResult(uri)
		}
	}
	
	fun openGallery() {
		takeGalleryLauncher.launch("images/*")
	}
}