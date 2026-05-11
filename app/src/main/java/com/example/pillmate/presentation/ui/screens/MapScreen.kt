//package com.example.pillmate.presentation.ui.screens
//
//import android.Manifest
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.interaction.MutableInteractionSource
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.KeyboardActions
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import com.example.pillmate.data.remote.api.NominatimService
//import com.example.pillmate.data.remote.dto.PlaceResult
//import com.example.pillmate.presentation.ui.components.OsmMapView
//import kotlinx.coroutines.launch
//import org.osmdroid.util.GeoPoint
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import androidx.compose.material.icons.filled.Clear
//import androidx.compose.material.icons.filled.Search
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.input.ImeAction
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.text.input.TextFieldValue
//import com.example.pillmate.util.getCurrentLocation
//import okhttp3.OkHttpClient
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MapScreen(
//	navController: NavController
//) {
//	val coroutineScope = rememberCoroutineScope()
//	var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
//	val interactionSource = remember { MutableInteractionSource() }
//	// Mặc định ở TP.HCM
//	var currentLocation by remember { mutableStateOf(GeoPoint(10.8231, 106.6297)) }
//	var searchResults by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
//	var isSearchActive by remember { mutableStateOf(false) }
//	// Khởi tạo Retrofit (Trong thực tế nên dùng Hilt/Dagger để inject)
//	val context = LocalContext.current
//
//	var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
//
//	// Permission launcher
//	val locationPermissionLauncher = rememberLauncherForActivityResult(
//		contract = ActivityResultContracts.RequestMultiplePermissions()
//	) { permissions ->
//		val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
//				|| permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
//		if (granted) {
//			coroutineScope.launch {
//				val location = getCurrentLocation(context)
//				if (location != null) {
//					userLocation = GeoPoint(location.first, location.second)
//				}
//			}
//		}
//	}
//
//	LaunchedEffect(Unit) {
//		locationPermissionLauncher.launch(
//			arrayOf(
//				Manifest.permission.ACCESS_FINE_LOCATION,
//				Manifest.permission.ACCESS_COARSE_LOCATION
//			)
//		)
//	}
//
//	val retrofit = remember {
//		val okHttpClient = OkHttpClient.Builder()
//			.addInterceptor { chain ->
//				val request = chain.request().newBuilder()
//					.header("User-Agent", context.packageName)
//					.build()
//				chain.proceed(request)
//			}
//			.build()
//
//		Retrofit.Builder()
//			.baseUrl("https://nominatim.openstreetmap.org/")
//			.client(okHttpClient)
//			.addConverterFactory(GsonConverterFactory.create())
//			.build()
//			.create(NominatimService::class.java)
//	}
//
//	val performSearch = {
//		if (searchQuery.text.isNotBlank()) {
//			coroutineScope.launch {
//				try {
//					val results = retrofit.searchPlace(searchQuery.text)
//					println("DEBUG: Found ${results.size} results")  // ✅ Kiểm tra có data không
//					results.forEach { println("DEBUG: ${it.display_name} -> ${it.lat}, ${it.lon}") }
//					searchResults = results
//				} catch (e: Exception) {
//					println("DEBUG Error: ${e.message}")
//				}
//			}
//			isSearchActive = false
//		}
//	}
//
//	Scaffold(
//		topBar = {
//			TopAppBar(
//				title = { Text("Map") },
//				navigationIcon = {
//					IconButton(onClick = { navController.popBackStack() }) {
//						Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//					}
//				}
//			)
//		}
//	) { paddingValues ->
//		Column(modifier = Modifier.fillMaxSize()) {
//			// Thanh tìm kiếm
//			Row(
//				modifier = Modifier.fillMaxWidth().padding(paddingValues),
//				horizontalArrangement = Arrangement.spacedBy(8.dp),
//				verticalAlignment = Alignment.CenterVertically
//			) {
//				OutlinedTextField(
//					value = searchQuery,
//					onValueChange = { searchQuery = it },
//					modifier = Modifier
//						.fillMaxWidth()
//						.padding(horizontal = 16.dp),
//					placeholder = { Text("Nhập địa điểm (vd: Quận 1, TP.HCM)") },
//					leadingIcon = {
//						IconButton(onClick = { performSearch() }) {
//							Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
//						}
//					},
//					trailingIcon = {
//						if (searchQuery.text.isNotEmpty()) {
//							IconButton(onClick = {
//								searchQuery = TextFieldValue("")
//								searchResults = emptyList()
//							}) {
//								Icon(Icons.Default.Clear, contentDescription = "Xóa")
//							}
//						}
//					},
//					keyboardOptions = KeyboardOptions(
//						keyboardType = KeyboardType.Text,
//						imeAction = ImeAction.Search
//					),
//					keyboardActions = KeyboardActions(
//						onSearch = { performSearch() }
//					),
//					singleLine = true,
//					shape = RoundedCornerShape(24.dp)
//				)
//			}
//			Spacer(modifier = Modifier.height(16.dp))
//			// Bản đồ
//			OsmMapView(
//				modifier = Modifier
//					.fillMaxSize()
//					.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
//				searchResults,
//				userLocation
//			)
//		}
//	}
//}


package com.example.pillmate.presentation.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pillmate.data.remote.api.NominatimService
import com.example.pillmate.data.remote.dto.PlaceResult
import com.example.pillmate.presentation.ui.components.OsmMapView
import com.example.pillmate.util.getCurrentLocation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL

// ── Data class cho kết quả phòng khám ──────────────────────────────────────
data class ClinicRecord(
	val tenCoSo: String,
	val soGiayPhep: String,
	val diaChi: String,
	val ngayCap: String,
	val tinhTrang: String,
	val tenHinhThuc: String,
	val soCCHN: String
)

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
	
	val context = LocalContext.current
	var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
	
	// ── Dialog state ──────────────────────────────────────────────────────────
	var showClinicDialog by remember { mutableStateOf(false) }
	var clinicList by remember { mutableStateOf<List<ClinicRecord>>(emptyList()) }
	var isLoadingClinics by remember { mutableStateOf(false) }
	var clinicError by remember { mutableStateOf<String?>(null) }
	
	// ── Permission launcher ───────────────────────────────────────────────────
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
	
	// ── Retrofit cho Nominatim ────────────────────────────────────────────────
	val nominatimRetrofit = remember {
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
	
	// ── Fallback geocoding: tách token theo dấu phẩy, thử từng suffix ────────
	/**
	 * Thử tìm kiếm địa chỉ trên Nominatim bằng cách lần lượt bỏ bớt phần đầu
	 * của địa chỉ (phân tách bởi dấu phẩy) cho đến khi có kết quả.
	 *
	 * Ví dụ: "SI-09 Khu phố Garden Court 2, Phường Tân Phong, Quận 7, TP Hồ Chí Minh"
	 * → thử "SI-09 Khu phố Garden Court 2, Phường Tân Phong, Quận 7, TP Hồ Chí Minh"
	 * → thử "Phường Tân Phong, Quận 7, TP Hồ Chí Minh"
	 * → thử "Quận 7, TP Hồ Chí Minh"
	 * → thử "TP Hồ Chí Minh"
	 */
	suspend fun geocodeWithFallback(address: String): List<PlaceResult> {
		val parts = address.split(",").map { it.trim() }.filter { it.isNotEmpty() }
		for (i in 0 until parts.size - 1) {
			val query = parts.drop(i).joinToString(", ")
			try {
				val results = nominatimRetrofit.searchPlace(query)
				if (results.isNotEmpty()) {
					println("DEBUG geocode fallback success at i=$i query='$query'")
					return results
				}
			} catch (e: Exception) {
				println("DEBUG geocode fallback error at i=$i: ${e.message}")
			}
		}
		// Thử toàn bộ địa chỉ lần cuối phòng trường hợp không có phẩy
		return try {
			nominatimRetrofit.searchPlace(address)
		} catch (e: Exception) {
			emptyList()
		}
	}
	
	// ── Search từ OutlinedTextField ───────────────────────────────────────────
	val performSearch = {
		if (searchQuery.text.isNotBlank()) {
			coroutineScope.launch {
				try {
					val results = geocodeWithFallback(searchQuery.text)
					println("DEBUG: Found ${results.size} results")
					results.forEach { println("DEBUG: ${it.display_name} -> ${it.lat}, ${it.lon}") }
					searchResults = results
				} catch (e: Exception) {
					println("DEBUG Error: ${e.message}")
				}
			}
			isSearchActive = false
		}
	}
	
	// ── Fetch danh sách phòng khám từ Open Data HCM ──────────────────────────
	fun fetchClinics() {
		coroutineScope.launch {
			isLoadingClinics = true
			clinicError = null
			try {
				val url =
					"https://opendata.hochiminhcity.gov.vn/api/action/datastore/search.json?resource_id=c00e92e9-5f0a-4bb9-b536-c35d65c1ec74&limit=30"
				val responseText = withContext(kotlinx.coroutines.Dispatchers.IO) {
					URL(url).readText()
				}
				val json = JSONObject(responseText)
				val records = json.getJSONObject("result").getJSONArray("records")
				val list = mutableListOf<ClinicRecord>()
				for (i in 0 until records.length()) {
					val obj = records.getJSONObject(i)
					list.add(
						ClinicRecord(
							tenCoSo = obj.optString("TenCoSo", ""),
							soGiayPhep = obj.optString("SoGiayPhep", ""),
							diaChi = obj.optString("DiaChi", ""),
							ngayCap = obj.optString("NgayCap", ""),
							tinhTrang = obj.optString("TinhTrang", ""),
							tenHinhThuc = obj.optString("TenHinhThuc", ""),
							soCCHN = obj.optString("SoCCHN_NDD", "")
						)
					)
				}
				clinicList = list
			} catch (e: Exception) {
				clinicError = "Không thể tải dữ liệu: ${e.message}"
				println("DEBUG fetchClinics error: ${e.message}")
			} finally {
				isLoadingClinics = false
			}
		}
	}
	
	// ── Clinic Dialog ─────────────────────────────────────────────────────────
	if (showClinicDialog) {
		AlertDialog(
			onDismissRequest = { showClinicDialog = false },
			title = { Text("Danh sách phòng khám", fontWeight = FontWeight.Bold) },
			text = {
				Box(modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 420.dp)) {
					when {
						isLoadingClinics -> {
							Box(
								modifier = Modifier.fillMaxWidth().padding(32.dp),
								contentAlignment = Alignment.Center
							) {
								CircularProgressIndicator()
							}
						}
						
						clinicError != null -> {
							Text(
								text = clinicError!!,
								color = MaterialTheme.colorScheme.error,
								modifier = Modifier.padding(8.dp)
							)
						}
						
						clinicList.isEmpty() -> {
							Text(
								text = "Không có dữ liệu.",
								modifier = Modifier.padding(8.dp)
							)
						}
						
						else -> {
							LazyColumn(
								modifier = Modifier.fillMaxWidth(),
								verticalArrangement = Arrangement.spacedBy(4.dp)
							) {
								items(clinicList) { clinic ->
									ClinicItem(
										clinic = clinic,
										onClick = {
											// Điền địa chỉ vào search field
											searchQuery = TextFieldValue(clinic.diaChi)
											showClinicDialog = false
											// Geocode với fallback rồi cập nhật map
											coroutineScope.launch {
												try {
													val results = geocodeWithFallback(clinic.diaChi)
													searchResults = results
												} catch (e: Exception) {
													println("DEBUG item geocode error: ${e.message}")
												}
											}
										}
									)
									HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
								}
							}
						}
					}
				}
			},
			confirmButton = {
				TextButton(onClick = { showClinicDialog = false }) {
					Text("Đóng")
				}
			},
			shape = RoundedCornerShape(16.dp)
		)
	}
	
	// ── UI chính ──────────────────────────────────────────────────────────────
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Map") },
				navigationIcon = {
					IconButton(onClick = { navController.popBackStack() }) {
						Icon(Icons.Default.ArrowBack, contentDescription = "Back")
					}
				},
				actions = {
					// Nút danh sách phòng khám ở vị trí end của TopAppBar
					IconButton(
						onClick = {
							showClinicDialog = true
							fetchClinics()
						}
					) {
						Icon(
							imageVector = Icons.Default.List,
							contentDescription = "Danh sách phòng khám"
						)
					}
				}
			)
		}
	) { paddingValues ->
		Column(modifier = Modifier.fillMaxSize()) {
			// Thanh tìm kiếm
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(paddingValues),
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

// ── Composable cho từng item phòng khám ──────────────────────────────────────
@Composable
private fun ClinicItem(
	clinic: ClinicRecord,
	onClick: () -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.clickable { onClick() }
			.padding(vertical = 10.dp, horizontal = 4.dp)
	) {
		Text(
			text = clinic.tenCoSo,
			fontWeight = FontWeight.Bold,
			fontSize = 15.sp,
			color = Color.Black,
			maxLines = 2,
			overflow = TextOverflow.Ellipsis
		)
		if (clinic.diaChi.isNotEmpty()) {
			Spacer(modifier = Modifier.height(2.dp))
			Text(
				text = clinic.diaChi,
				fontSize = 13.sp,
				color = Color.Gray,
				fontWeight = FontWeight.Normal,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}