package com.example.pillmate.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.pillmate.domain.model.Appointment
import com.example.pillmate.R

@Composable
fun EditAppointment(
	appointment: Appointment,
	onSave: (newAppointment: Appointment) -> Unit,
	onDismissRequest: () -> Unit
) {
	AppointmentForm(
		stringResource(R.string.edit_appointment_label),
		stringResource(R.string.save),
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