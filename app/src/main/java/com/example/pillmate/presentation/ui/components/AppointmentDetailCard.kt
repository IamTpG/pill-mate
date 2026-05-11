package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.example.pillmate.domain.model.AppointmentLog // Hoặc Appointment tùy logic map của bạn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailBottomSheet(
	appointment: AppointmentLog,
	onDismiss: () -> Unit,
	onNavigateToSchedule: () -> Unit
) {
	ModalBottomSheet(
		onDismissRequest = onDismiss,
		containerColor = Color.White,
		shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(24.dp),
			horizontalAlignment = Alignment.Start
		) {
			Text(appointment.name, color = colorResource(R.color.primary_green), fontSize = 22.sp, fontWeight = FontWeight.Bold)
			Spacer(modifier = Modifier.height(16.dp))
			
			Text("Doctor:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
			Text(appointment.doctorName, fontSize = 16.sp, color = Color.Black)
			Spacer(modifier = Modifier.height(8.dp))
			
			Text("Location:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
			Text(appointment.location, fontSize = 16.sp, color = Color.Black)
			Spacer(modifier = Modifier.height(8.dp))
			
			Text("Notes:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
			Text(appointment.description, fontSize = 16.sp, color = Color.Black)
			
			Spacer(modifier = Modifier.height(32.dp))
			
			Button(
				onClick = {
					onDismiss()
					onNavigateToSchedule()
				},
				modifier = Modifier.fillMaxWidth().height(56.dp),
				shape = RoundedCornerShape(50),
				colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.primary_green))
			) {
				Text("Set up Schedule", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
			}
			Spacer(modifier = Modifier.height(16.dp))
		}
	}
}