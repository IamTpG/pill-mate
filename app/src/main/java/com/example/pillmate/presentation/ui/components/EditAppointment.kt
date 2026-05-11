package com.example.pillmate.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.pillmate.domain.model.Appointment

@Composable
fun EditAppointment(
	appointment: Appointment,
	onSave: (newAppointment: Appointment) -> Unit,
	onDismissRequest: () -> Unit
) {
	AppointmentForm(
		"Edit Appointment",
		"Save",
		appointment,
		onSave,
		onDismissRequest
	)
}

@Preview
@Composable
fun PreviewEditAppointment() {
	EditAppointment(Appointment(
		id = "123",
		name = "ABCD",
		location = "HJCM",
		doctorName = "CD",
		"dfas"
	), {}, {})
}