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
					AppointmentCard(log = log, { swipedAppointmentId ->
						 viewModel.deleteAppoinment(profileId, swipedAppointmentId)
					})
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