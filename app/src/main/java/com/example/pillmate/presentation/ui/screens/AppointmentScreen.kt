package com.example.pillmate.presentation.ui.screens

import android.graphics.drawable.Icon
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.pillmate.domain.model.Appointment
import com.example.pillmate.domain.model.AppointmentLog
import com.example.pillmate.presentation.ui.components.AddAppointment
import com.example.pillmate.presentation.ui.components.AppointmentAddOptions
import com.example.pillmate.presentation.ui.components.AppointmentCard
import com.example.pillmate.presentation.ui.components.AppointmentDetailBottomSheet
import com.example.pillmate.presentation.ui.components.AppointmentHeader
import com.example.pillmate.presentation.ui.components.AppointmentSearchBar
import com.example.pillmate.presentation.ui.components.EditAppointment
import com.example.pillmate.presentation.viewmodel.AppointmentViewModel
import com.example.pillmate.presentation.viewmodel.ProfileViewModel
import org.koin.androidx.compose.koinViewModel
@Composable
fun AppointmentScreen(
	viewModel: AppointmentViewModel,
	profileId: String,
	paddingValues: PaddingValues,
	onNavigateToScheduleBuilder: (Appointment) -> Unit
) {
	val logs by viewModel.uiState.collectAsState()
	
	var showAddAppointment by remember { mutableStateOf(false) }
	var showEditAppointment by remember { mutableStateOf(false) }
	var editAppointment by remember {mutableStateOf(Appointment())}
	
	var showSearchBar by remember { mutableStateOf(false) }
	var searchQuery by remember { mutableStateOf("") }
	
	// Filter logs for the main list — driven by the hoisted query
	val displayedLogs = remember(searchQuery, logs) {
		if (searchQuery.isBlank()) logs
		else logs.filter { it.name.contains(searchQuery, ignoreCase = true) }
	}
	
	val profileViewModel: ProfileViewModel = koinViewModel()
	val currentLocalProfile by profileViewModel.currentLocalProfile.collectAsState()
	val isCaregiver = currentLocalProfile?.role == "Caregiver_View"
	
	var selectedAppointmentForDetails by remember { mutableStateOf<AppointmentLog?>(null) }
	
	LaunchedEffect(profileId) {
		if (profileId.isNotEmpty()) {
			viewModel.fetchAppointments(profileId)
		}
	}
	
	Box(modifier = Modifier.fillMaxSize().padding()) {
		Image(
			painter = painterResource(id = R.drawable.background),
			contentDescription = null,
			modifier = Modifier.fillMaxSize(),
			contentScale = ContentScale.Crop
		)
		Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
		
		Column(modifier = Modifier.fillMaxSize().padding(paddingValues), verticalArrangement = Arrangement.spacedBy(0.dp)) {
			
			AppointmentHeader(logs.size)
			
			AppointmentSearchBar(
				logs = logs,
				searchQuery = searchQuery,
				onQueryChange = { searchQuery = it },
				// Abort mechanism 2: close icon inside the bar hides it and clears query
				onDismiss = {
					searchQuery = ""
					showSearchBar = false
				},
				modifier = Modifier.offset(y = (-12).dp)
			)
			
			LazyColumn(
				modifier = Modifier
					.fillMaxWidth(1f)
					//.padding(paddingValues)
					.padding(horizontal = 16.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				items(displayedLogs) { log ->
					AppointmentCard(
						log = log, isCaregiver,
						onClickCard = { selectedAppointmentForDetails = log },
						{
						showEditAppointment = true
						editAppointment = Appointment(
							log.id,
							log.name,
							log.location,
							log.doctorName,
							log.description
						)
					}, { swipedAppointmentId ->
						 viewModel.deleteAppoinment(profileId, swipedAppointmentId)
					})
				}
				
			}
		}
		if (!isCaregiver) {
			FloatingActionButton(
				onClick = { showAddAppointment = true },
				containerColor = Color.White,
				contentColor = Color(0xFF1c5f55), // Your brand green
				shape = CircleShape,
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.padding(
						end = 24.dp,
						bottom = paddingValues.calculateBottomPadding() + 24.dp
					)
			) {
				Icon(
					Icons.Filled.Add,
					"Add Appointment",
					modifier = Modifier.size(32.dp))
			}
		}
	}
	
	if (showAddAppointment) {
		AddAppointment(
			onSubmit = { newAppointment ->
				viewModel.postAppointment(profileId, newAppointment)
				showAddAppointment = false
			},
			onDismissRequest = { showAddAppointment = false }
		)
	}
	
	if (showEditAppointment) {
		EditAppointment(
			editAppointment,
			{ updatedAppointment ->
				viewModel.updateAppointment(profileId, updatedAppointment)
				showEditAppointment = false
			}
		) {
			showEditAppointment = false
		}
	}
	
	selectedAppointmentForDetails?.let { log ->
		AppointmentDetailBottomSheet(
			appointment = log,
			onDismiss = { selectedAppointmentForDetails = null },
			onNavigateToSchedule = {
				// Map AppointmentLog sang Appointment nếu cần thiết, tuỳ architecture của bạn
				val appointment = Appointment(log.id, log.name, log.location, log.doctorName, log.description)
				onNavigateToScheduleBuilder(appointment)
			}
		)
	}

}