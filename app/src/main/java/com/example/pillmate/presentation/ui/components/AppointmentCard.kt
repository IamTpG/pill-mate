package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.example.pillmate.domain.model.AppointmentLog
import com.example.pillmate.R
import org.intellij.lang.annotations.JdkConstants

@Composable
fun AppointmentCard(log: AppointmentLog) {
	//val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
	Card(
		shape = RoundedCornerShape(20.dp),
		colors = CardDefaults.cardColors(containerColor = Color.White),
		modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
				Text(log.name, color = colorResource(R.color.primary_green),fontSize = 16.sp, fontWeight = FontWeight.Bold)
				Spacer(modifier = Modifier.height(10.dp))
				Text("Location: ${log.location}")
				Text("Doctor: ${log.doctorName}")
				Text("Description: ${log.description}")
			}
			
			Icon(
				painter = painterResource(R.drawable.ic_person_clock),
				contentDescription = "Appointment Card Icon"
			)
		}
	}
}

@Preview
@Composable
fun PreviewAppointmentCard() {
	AppointmentCard(AppointmentLog(
		"123",
		"Monthly Appointment",
		"Doctor House",
		"Mr. Thang",
		"Don't be late"
	))
}