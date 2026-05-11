package com.example.pillmate.presentation.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImage
import com.example.pillmate.presentation.viewmodel.ImageVaultViewModel
import org.koin.androidx.compose.koinViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageVaultScreen(
    viewModel: ImageVaultViewModel,
    profileViewModel: com.example.pillmate.presentation.viewmodel.ProfileViewModel = koinViewModel(),
    paddingValues: PaddingValues,
    onBack: () -> Unit
) {
    val images by viewModel.images.collectAsState()

    val currentLocalProfile by profileViewModel.currentLocalProfile.collectAsState()
    val isCaregiver = currentLocalProfile?.role == "Caregiver_View"
    val activeProfileId = currentLocalProfile?.id ?: return

    LaunchedEffect(activeProfileId) {
        viewModel.loadImages(activeProfileId)
    }

    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.uploadImage(context, it, activeProfileId) }
    }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        containerColor = Color(0xFF1B1B1B),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Thư viện y tế", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (!isCaregiver) {
                FloatingActionButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    containerColor = Color(0xFF1c5f55)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Image", tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        if (images.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chưa có hình ảnh nào.", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(images) { img ->
                    val imageSource = if (!img.remoteUrl.isNullOrEmpty()) {
                        img.remoteUrl
                    } else {
                        img.localUri?.let { File(it) }
                    }

                    AsyncImage(
                        model = imageSource,
                        contentDescription = "Medical Document",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.DarkGray)
                    )
                }
            }
        }
    }
}