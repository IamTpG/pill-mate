package com.example.pillmate.presentation.ui.activities

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.pillmate.R
import com.example.pillmate.data.service.*

class MediaServiceTestActivity: AppCompatActivity() {
	
	private lateinit var imageProvider: ImageProviderService
	private lateinit var cameraProvider: CameraProviderService
	private var galleryPhotoUri: Uri? = null
	private var cameraPhotoUri: Uri? = null
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_media_service_test)
		
		cameraProvider = CameraProviderService(this, this) { success ->
			if (success && cameraPhotoUri != null) {
				val imageView: ImageView = findViewById(R.id.cameraImageView)
				imageView.setImageURI(cameraPhotoUri)
			}
		}
		
		imageProvider = ImageProviderService(this, this) {
			uri -> galleryPhotoUri = uri
			val imageView: ImageView = findViewById(R.id.galleryImageView)
			imageView.setImageURI(galleryPhotoUri)
		}
		
		val galleryButton: Button = findViewById(R.id.gallerybutton)
		galleryButton.setOnClickListener {
			imageProvider.openGallery()
		}
		
		val cameraButton: Button = findViewById(R.id.camerabutton)
		cameraButton.setOnClickListener {
			cameraPhotoUri = cameraProvider.createPhotoUri()
			cameraProvider.openCamera(cameraPhotoUri!!)
		}
	}
}