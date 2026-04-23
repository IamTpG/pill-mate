package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.pillmate.R
import com.example.pillmate.domain.model.AppointmentLog
import com.example.pillmate.domain.model.LogStatus
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ScheduleCard(log: AppointmentLog) {
	val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
	Card(
		shape = RoundedCornerShape(20.dp),
		colors = CardDefaults.cardColors(containerColor = Color.White),
		modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
	) {
		Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
			Column(modifier = Modifier.weight(1f)) {
				Text(log.title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
				Text(
					text = "Time: ${timeFormat.format(log.scheduledTime)} | ${log.status.name}",
					color = if (log.status == LogStatus.COMPLETED) Color.Green else Color.Red,
					fontSize = 12.sp
				)
				log.location?.let { Text("At: $it", fontSize = 12.sp) }
			}
			Icon(painterResource(R.drawable.ic_launcher_background), null, Modifier.size(50.dp), tint = Color.Unspecified)
		}
	}
}