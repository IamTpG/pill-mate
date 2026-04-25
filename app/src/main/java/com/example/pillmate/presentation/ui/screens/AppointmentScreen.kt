//package com.example.pillmate.presentation.ui.screens
//
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Search
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.unit.dp
//import com.example.pillmate.R
//import com.example.pillmate.presentation.ui.components.AddAppointment
//import com.example.pillmate.presentation.ui.components.AppointmentAddOptions
//import com.example.pillmate.presentation.ui.components.AppointmentCard
//import com.example.pillmate.presentation.viewmodel.AppointmentViewModel
//import org.koin.androidx.compose.koinViewModel
//import com.example.pillmate.presentation.ui.components.AppointmentHeader
//import com.example.pillmate.presentation.ui.components.SearchBar
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AppointmentScreen(
//	viewModel: AppointmentViewModel,
//	profileId: String,
//	paddingValues: PaddingValues // Added to respect MainScaffold
//) {
//	val logs by viewModel.uiState.collectAsState()
//
//	var isShowAddOptions by remember { mutableStateOf(false) }
//
//	var isShowAddAppointment by remember { mutableStateOf(false)}
//
//	// Search States
//	var showSearchBar by remember { mutableStateOf(false) }
//	var searchQuery by remember { mutableStateOf("") }
//	var isSearchActive by remember { mutableStateOf(false) }
//
//// 1. Filter logs for the suggestions drop-down (returns unique names)
//	val suggestions = remember(searchQuery, logs) {
//		if (searchQuery.isBlank()) emptyList()
//		else logs.filter { it.name.contains(searchQuery, ignoreCase = true) }
//			.distinctBy { it.name }
//	}
//
//// 2. Filter logs for the main UI list
//	val displayedLogs = remember(searchQuery, logs) {
//		if (searchQuery.isBlank()) logs
//		else logs.filter { it.name.contains(searchQuery, ignoreCase = true) }
//	}
//
//
//	LaunchedEffect(profileId) {
//		if (profileId.isNotEmpty()) {
//			viewModel.fetchAppointments(profileId)
//		}
//	}
//
//	Box(modifier = Modifier.fillMaxSize()) {
//		// Background stays full screen
//		Image(
//			painter = painterResource(id = R.drawable.background),
//			contentDescription = null,
//			modifier = Modifier.fillMaxSize(),
//			contentScale = ContentScale.Crop
//		)
//
//		// Apply innerPadding (from Scaffold) and horizontal padding to the list
//		LazyColumn(
//			modifier = Modifier
//				.fillMaxSize()
//				.padding(paddingValues) // Use the padding from MainScaffold
//				.padding(horizontal = 16.dp),
//			horizontalAlignment = Alignment.CenterHorizontally
//		) {
//			item {
//				// Pass calculated counts for the "3/10" logic
////				AppointmentHeader(
////					completedCount = logs.count { it.status.name == "COMPLETED" },
////					totalCount = logs.size
////				)
//				AppointmentHeader(3, 10) {
//					showSearchBar = !showSearchBar
//				}
//			}
//
//			if (showSearchBar) {
//				item {
//					SearchBar(
//						query = searchQuery,
//						onQueryChange = { searchQuery = it },
//						onSearch = { isSearchActive = false }, // Triggers when user hits 'Enter' on keyboard
//						active = isSearchActive,
//						onActiveChange = { isSearchActive = it },
//						modifier = Modifier
//							.fillMaxWidth()
//							.padding(bottom = 16.dp),
//						placeholder = { Text("Search appointment name...") },
//						leadingIcon = {
//							Icon(Icons.Default.Search, contentDescription = "Search")
//						},
//						trailingIcon = {
//							if (searchQuery.isNotEmpty() || isSearchActive) {
//								IconButton(onClick = {
//									if (searchQuery.isNotEmpty()) {
//										searchQuery = "" // Clear text
//									} else {
//										isSearchActive = false
//										showSearchBar = false // Close search entirely
//									}
//								}) {
//									Icon(painterResource(id = R.drawable.ic_closing), contentDescription = "Close")
//								}
//							}
//						}
//					) {
//						// This block is the content of the SearchBar drop-down (Suggestions)
//						LazyColumn(
//							modifier = Modifier.fillMaxWidth(),
//							contentPadding = PaddingValues(16.dp),
//							verticalArrangement = Arrangement.spacedBy(8.dp)
//						) {
//							items(suggestions) { suggestion ->
//								Row(
//									modifier = Modifier
//										.fillMaxWidth()
//										.clickable {
//											// When clicked: set query, close suggestions, main list updates automatically
//											searchQuery = suggestion.name
//											isSearchActive = false
//										}
//										.padding(16.dp)
//								) {
//									Icon(
//										painter = painterResource(id = R.drawable.ic_history), // Use a history or search icon
//										contentDescription = null,
//										tint = Color.Gray,
//										modifier = Modifier.padding(end = 16.dp)
//									)
//									Text(text = suggestion.name)
//								}
//							}
//						}
//					}
//				}
//			}
//
//			items(displayedLogs) { log ->
//				AppointmentCard(log = log)
//			}
//
//			item {
//				Button(
//					onClick = { isShowAddOptions = true },
//					colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E5E52)),
//					shape = RoundedCornerShape(24.dp),
//					modifier = Modifier
//						.fillMaxWidth(0.6f)
//						.padding(vertical = 24.dp)
//				) {
//					Text("Add more", color = Color.White)
//				}
//			}
//		}
//	}
//
//	if (isShowAddOptions) {
//		Box(
//			modifier = Modifier
//				.fillMaxSize()
//				.background(Color.Black.copy(alpha = 0.5f)), // Dimmed background
//			contentAlignment = Alignment.Center // Centers the Card
//		) {
//			AppointmentAddOptions({ isShowAddOptions = false}) {
//				isShowAddAppointment = true
//			}
//		}
//	}
//
//	if (isShowAddAppointment) {
//		AddAppointment({ newAppointment ->
//			viewModel.postAppointment(profileId, newAppointment)
//			isShowAddAppointment = false
//		}) {
//			isShowAddAppointment = false
//		}
//	}
//}

package com.example.pillmate.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.pillmate.R
import com.example.pillmate.presentation.ui.components.AddAppointment
import com.example.pillmate.presentation.ui.components.AppointmentAddOptions
import com.example.pillmate.presentation.ui.components.AppointmentCard
import com.example.pillmate.presentation.ui.components.AppointmentHeader
import com.example.pillmate.presentation.ui.components.AppointmentSearchBar
import com.example.pillmate.presentation.viewmodel.AppointmentViewModel

@Composable
fun AppointmentScreen(
	viewModel: AppointmentViewModel,
	profileId: String,
	paddingValues: PaddingValues
) {
	val logs by viewModel.uiState.collectAsState()
	
	var isShowAddOptions by remember { mutableStateOf(false) }
	var isShowAddAppointment by remember { mutableStateOf(false) }
	
	// Hoisted here because:
	//   - showSearchBar is toggled by AppointmentHeader's Search button (parent concern)
	//   - searchQuery drives the filtered list below (parent concern)
	var showSearchBar by remember { mutableStateOf(false) }
	var searchQuery by remember { mutableStateOf("") }
	
	// Filter logs for the main list — driven by the hoisted query
	val displayedLogs = remember(searchQuery, logs) {
		if (searchQuery.isBlank()) logs
		else logs.filter { it.name.contains(searchQuery, ignoreCase = true) }
	}
	
	LaunchedEffect(profileId) {
		if (profileId.isNotEmpty()) {
			viewModel.fetchAppointments(profileId)
		}
	}
	
	Box(modifier = Modifier.fillMaxSize()) {
		Image(
			painter = painterResource(id = R.drawable.background),
			contentDescription = null,
			modifier = Modifier.fillMaxSize(),
			contentScale = ContentScale.Crop
		)
		
		Column(modifier = Modifier.fillMaxSize()) {
			
			AppointmentHeader(3, 10, { isShowAddOptions = true}) {
				if (showSearchBar) searchQuery = ""
				showSearchBar = !showSearchBar
			}
			
			if (showSearchBar) {
				AppointmentSearchBar(
					logs = logs,
					searchQuery = searchQuery,
					onQueryChange = { searchQuery = it },
					// Abort mechanism 2: close icon inside the bar hides it and clears query
					onDismiss = {
						searchQuery = ""
						showSearchBar = false
					}
				)
				
			}
			
			LazyColumn(
				modifier = Modifier
					.fillMaxWidth(1f)
					.padding(paddingValues)
					.padding(horizontal = 16.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				items(displayedLogs) { log ->
					AppointmentCard(log = log, {})
				}
				
			}
			
			
		}
		
		
	}
	
	if (isShowAddOptions) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(Color.Black.copy(alpha = 0.5f)),
			contentAlignment = Alignment.Center
		) {
			AppointmentAddOptions(
				onClickCloseButton = { isShowAddOptions = false },
				onClickAddAppointment = { isShowAddAppointment = true }
			)
		}
	}
	
	if (isShowAddAppointment) {
		AddAppointment(
			onSubmit = { newAppointment ->
				viewModel.postAppointment(profileId, newAppointment)
				isShowAddAppointment = false
			},
			onDismissRequest = { isShowAddAppointment = false }
		)
	}
}