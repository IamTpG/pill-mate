package com.example.pillmate.presentation.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pillmate.data.remote.api.NominatimService
import com.example.pillmate.data.remote.dto.PlaceResult
import com.example.pillmate.presentation.ui.components.OsmMapView
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.example.pillmate.util.getCurrentLocation
import okhttp3.OkHttpClient
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
	navController: NavController
) {
	val coroutineScope = rememberCoroutineScope()
	var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
	val interactionSource = remember { MutableInteractionSource() }
	// Mặc định ở TP.HCM
	var currentLocation by remember { mutableStateOf(GeoPoint(10.8231, 106.6297)) }
	var searchResults by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
	var isSearchActive by remember { mutableStateOf(false) }
	// Khởi tạo Retrofit (Trong thực tế nên dùng Hilt/Dagger để inject)
	val context = LocalContext.current

	var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
	
	// Permission launcher
	val locationPermissionLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestMultiplePermissions()
	) { permissions ->
		val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
				|| permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
		if (granted) {
			coroutineScope.launch {
				val location = getCurrentLocation(context)
				if (location != null) {
					userLocation = GeoPoint(location.first, location.second)
				}
			}
		}
	}
	
	LaunchedEffect(Unit) {
		locationPermissionLauncher.launch(
			arrayOf(
				Manifest.permission.ACCESS_FINE_LOCATION,
				Manifest.permission.ACCESS_COARSE_LOCATION
			)
		)
	}
	
	val retrofit = remember {
		val okHttpClient = OkHttpClient.Builder()
			.addInterceptor { chain ->
				val request = chain.request().newBuilder()
					.header("User-Agent", context.packageName)
					.build()
				chain.proceed(request)
			}
			.build()
		
		Retrofit.Builder()
			.baseUrl("https://nominatim.openstreetmap.org/")
			.client(okHttpClient)
			.addConverterFactory(GsonConverterFactory.create())
			.build()
			.create(NominatimService::class.java)
	}
	
	val performSearch = {
		if (searchQuery.text.isNotBlank()) {
			coroutineScope.launch {
				try {
					val results = retrofit.searchPlace(searchQuery.text)
					println("DEBUG: Found ${results.size} results")  // ✅ Kiểm tra có data không
					results.forEach { println("DEBUG: ${it.display_name} -> ${it.lat}, ${it.lon}") }
					searchResults = results
				} catch (e: Exception) {
					println("DEBUG Error: ${e.message}")
				}
			}
			isSearchActive = false
		}
	}
	
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Map") },
				navigationIcon = {
					IconButton(onClick = { navController.popBackStack() }) {
						Icon(Icons.Default.ArrowBack, contentDescription = "Back")
					}
				}
			)
		}
	) { paddingValues ->
		Column(modifier = Modifier.fillMaxSize()) {
			// Thanh tìm kiếm
			Row(
				modifier = Modifier.fillMaxWidth().padding(paddingValues),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				OutlinedTextField(
					value = searchQuery,
					onValueChange = { searchQuery = it },
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 16.dp),
					placeholder = { Text("Nhập địa điểm (vd: Quận 1, TP.HCM)") },
					leadingIcon = {
						IconButton(onClick = { performSearch() }) {
							Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
						}
					},
					trailingIcon = {
						if (searchQuery.text.isNotEmpty()) {
							IconButton(onClick = {
								searchQuery = TextFieldValue("")
								searchResults = emptyList()
							}) {
								Icon(Icons.Default.Clear, contentDescription = "Xóa")
							}
						}
					},
					keyboardOptions = KeyboardOptions(
						keyboardType = KeyboardType.Text,
						imeAction = ImeAction.Search
					),
					keyboardActions = KeyboardActions(
						onSearch = { performSearch() }
					),
					singleLine = true,
					shape = RoundedCornerShape(24.dp)
				)
			}
			Spacer(modifier = Modifier.height(16.dp))
			// Bản đồ
			OsmMapView(
				modifier = Modifier
					.fillMaxSize()
					.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
				searchResults,
				userLocation
			)
		}
	}
}