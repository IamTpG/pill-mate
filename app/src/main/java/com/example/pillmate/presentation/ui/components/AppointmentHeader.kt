package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R

@Composable
fun AppointmentHeader(completedCount: Int, totalCount: Int) {
	Column(modifier = Modifier.padding(vertical = 24.dp)) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Icon(
				painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with your profile icon
				contentDescription = null,
				tint = Color.White,
				modifier = Modifier.size(40.dp)
			)
			Spacer(modifier = Modifier.width(8.dp))
			Text(
				text = "Hi Tom ! How are you today ?",
				color = Color.White,
				fontSize = 20.sp,
				fontWeight = FontWeight.Bold
			)
		}
		
		Spacer(modifier = Modifier.height(20.dp))
		
		Row(verticalAlignment = Alignment.CenterVertically) {
			// Progress Badge (e.g., 3/10)
			Surface(
				color = Color.White,
				shape = RoundedCornerShape(8.dp)
			) {
				Text(
					text = "$completedCount/$totalCount",
					modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
					fontWeight = FontWeight.Bold,
					color = Color(0xFF1E5E52)
				)
			}
			
			Spacer(modifier = Modifier.width(12.dp))
			
			// Search Bar Placeholder
			Row(
				modifier = Modifier
					.background(Color(0xFF1E5E52), RoundedCornerShape(8.dp))
					.padding(horizontal = 12.dp, vertical = 6.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Button(onClick = {}) {
					Icon(
						Icons.Default.Search,
						contentDescription = null,
						tint = Color.White,
						modifier = Modifier.size(16.dp)
					)
					Spacer(modifier = Modifier.width(4.dp))
					Text("Search", color = Color.White, fontWeight = FontWeight.Bold)
				}
			}
		}
	}
}