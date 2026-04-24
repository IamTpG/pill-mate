package com.example.pillmate.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.pillmate.R
import com.example.pillmate.presentation.ui.components.AddAppointment
import com.example.pillmate.presentation.ui.components.AppointmentAddOptions
import com.example.pillmate.presentation.ui.components.AppointmentCard
import com.example.pillmate.presentation.viewmodel.AppointmentViewModel
import org.koin.androidx.compose.koinViewModel
import com.example.pillmate.presentation.ui.components.AppointmentHeader

@Composable
fun AppointmentScreen(
	viewModel: AppointmentViewModel,
	profileId: String,
	paddingValues: PaddingValues // Added to respect MainScaffold
) {
	val logs by viewModel.uiState.collectAsState()
	
	var isShowAddOptions by remember { mutableStateOf(false) }
	
	var isShowAddAppointment by remember { mutableStateOf(false)}
	
	LaunchedEffect(profileId) {
		if (profileId.isNotEmpty()) {
			viewModel.fetchAppointments(profileId)
		}
	}
	
	Box(modifier = Modifier.fillMaxSize()) {
		// Background stays full screen
		Image(
			painter = painterResource(id = R.drawable.background),
			contentDescription = null,
			modifier = Modifier.fillMaxSize(),
			contentScale = ContentScale.Crop
		)
		
		// Apply innerPadding (from Scaffold) and horizontal padding to the list
		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues) // Use the padding from MainScaffold
				.padding(horizontal = 16.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			item {
				// Pass calculated counts for the "3/10" logic
//				AppointmentHeader(
//					completedCount = logs.count { it.status.name == "COMPLETED" },
//					totalCount = logs.size
//				)
			}
			
			items(logs) { log ->
				AppointmentCard(log = log)
			}
			
			item {
				Button(
					onClick = { isShowAddOptions = true },
					colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E5E52)),
					shape = RoundedCornerShape(24.dp),
					modifier = Modifier
						.fillMaxWidth(0.6f)
						.padding(vertical = 24.dp)
				) {
					Text("Add more", color = Color.White)
				}
			}
		}
	}
	
	if (isShowAddOptions) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(Color.Black.copy(alpha = 0.5f)), // Dimmed background
			contentAlignment = Alignment.Center // Centers the Card
		) {
			AppointmentAddOptions({ isShowAddOptions = false}) {
				isShowAddAppointment = true
			}
		}
	}
	
	if (isShowAddAppointment) {
		AddAppointment({ newAppointment ->
			viewModel.postAppointment(profileId, newAppointment)
			isShowAddAppointment = false
		}) {
			isShowAddAppointment = false
		}
	}
}