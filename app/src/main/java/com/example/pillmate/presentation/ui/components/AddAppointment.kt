package com.example.pillmate.presentation.ui.components

import android.widget.Space
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.example.pillmate.domain.model.Appointment


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppointment(
	onSubmit: (newAppointment: Appointment) -> Unit,
	onDismissRequest: () -> Unit
) {
	val sheetState = rememberModalBottomSheetState(
		skipPartiallyExpanded = true
	)
	
	var formData by remember { mutableStateOf(Appointment()) }
	
	ModalBottomSheet(
		modifier = Modifier.fillMaxWidth().fillMaxHeight(),
		sheetState = sheetState,
		onDismissRequest = onDismissRequest
	) {
		
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(20.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			TextButton(onClick = onDismissRequest) {
				Text(
					text = "Cancel",
					style = MaterialTheme.typography.labelLarge, // Automatically 14sp with perfect letter spacing
					color = colorResource(R.color.primary_green)
				)
				
			}
			Text(
				text = "Appointment Details",
				style = MaterialTheme.typography.titleLarge, // Automatically 22sp and Semi-bold
				color = colorResource(R.color.primary_green),
				modifier = Modifier.align(Alignment.CenterVertically)
			)
		}
		
		Column(
			modifier = Modifier.padding(horizontal = 10.dp).padding(top=20.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp)
		) {
			OutlinedTextField(
				value = formData.name,
				onValueChange = { value ->
					formData = formData.copy(name = value)
				},
				label = { Text("Appointment Name")},
				modifier = Modifier.fillMaxWidth()
			)
			
			
			OutlinedTextField(
				value = formData.location,
				onValueChange = { value ->
					formData = formData.copy(location = value)
				},
				label = { Text("Location")},
				modifier = Modifier.fillMaxWidth()
			)
			
			OutlinedTextField(
				value = formData.doctorName,
				onValueChange = { value ->
					formData = formData.copy(doctorName = value)
				},
				label = { Text("Doctor Name")},
				modifier = Modifier.fillMaxWidth()
			)
			
			OutlinedTextField(
				value = formData. description,
				onValueChange = { value ->
					formData = formData.copy(description = value)
				},
				label = { Text("Description")},
				modifier = Modifier.fillMaxWidth()
			)
		}
		
		ElevatedButton(
			onClick = {
				onSubmit(formData.copy())
			},
			modifier = Modifier
				.align(Alignment.CenterHorizontally)
				.padding(top=20.dp)
				.fillMaxWidth()
				.padding(horizontal = 10.dp),
			colors = ButtonDefaults.buttonColors(
				containerColor = Color(0XFF2ECC71),
				contentColor = Color.White
			)) {
			
			Text("Submit")
		}
	}
}

@Preview
@Composable
fun PreviewAddAppointment() {
	AddAppointment({}, {})
}