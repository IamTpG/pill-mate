package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pillmate.domain.model.Appointment
import com.example.pillmate.R

@Composable
fun AppointmentForm(
	title: String,
	buttonTitle: String,
	appointment: Appointment,
	onSubmit: (newAppointment: Appointment) -> Unit,
	onDismissRequest: () -> Unit
) {
	var formData by remember { mutableStateOf(appointment) }
	
	AlertDialog(
		onDismissRequest = {
			onDismissRequest
		},
		containerColor = Color.White,
		title = { Text(title, color = Color.Black) },
		text = {
			Column(
				modifier = Modifier.padding(horizontal = 10.dp).padding(top=20.dp),
				verticalArrangement = Arrangement.spacedBy(14.dp)
			) {
				OutlinedTextField(
					value = formData.name,
					onValueChange = { value ->
						formData = formData.copy(name = value)
					},
					label = { Text(stringResource(R.string.appointment_name))},
					modifier = Modifier.fillMaxWidth()
				)
				
				
				OutlinedTextField(
					value = formData.location,
					onValueChange = { value ->
						formData = formData.copy(location = value)
					},
					label = { Text(stringResource(R.string.appointment_location))},
					modifier = Modifier.fillMaxWidth()
				)
				
				OutlinedTextField(
					value = formData.doctorName,
					onValueChange = { value ->
						formData = formData.copy(doctorName = value)
					},
					label = { Text(stringResource(R.string.appointment_doctor_name))},
					modifier = Modifier.fillMaxWidth()
				)
				
				OutlinedTextField(
					value = formData. description,
					onValueChange = { value ->
						formData = formData.copy(description = value)
					},
					label = { Text(stringResource(R.string.appointment_description))},
					modifier = Modifier.fillMaxWidth()
				)
			}
		},
		confirmButton = {
			Button(
				onClick = { onSubmit(formData.copy())},
				colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1c5f55))
			) {
				Text(buttonTitle, color = Color.White)
			}
		},
		dismissButton = {
			TextButton(onClick = onDismissRequest) {
				Text("Cancel", color = Color.Gray)
			}
		}
	)
}