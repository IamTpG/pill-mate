package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
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
	Column(modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth()) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Icon(
				painter = painterResource(id = R.drawable.ic_user_profile), // Replace with your profile icon
				contentDescription = null,
				tint = Color.White,
				modifier = Modifier.size(40.dp)
			)
			Spacer(modifier = Modifier.width(8.dp))
			Text(
				text = "Hi! How are you today ?",
				color = Color.White,
				fontSize = 24.sp,
				fontWeight = FontWeight.Bold
			)
		}
		
		Spacer(modifier = Modifier.height(40.dp))
		
		Box(
			modifier = Modifier
				.background(color = Color.White, shape = RoundedCornerShape(10.dp))
				.padding(4.dp)
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically
			) {
				// Progress Badge (e.g., 3/10)
				Surface(
					color = Color.White,
					shape = RoundedCornerShape(8.dp)
				) {
					Text(
						text = "$completedCount/$totalCount",
						modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
						fontSize = 16.sp,
						fontWeight = FontWeight.Bold,
						color = Color(0xFF1E5E52)
					)
				}
				
				//Spacer(modifier = Modifier.width(2.dp))
				
				
				// Search Bar Placeholder
				Row(
					modifier = Modifier
						.background(Color(0xFF1E5E52), RoundedCornerShape(6.dp))
						,
					verticalAlignment = Alignment.CenterVertically
				) {
					Button(
						onClick = {},
						colors = ButtonDefaults.buttonColors(
							containerColor = Color.Transparent,
							contentColor = Color.Black
						)
					) {
						Icon(
							Icons.Default.Search,
							contentDescription = null,
							tint = Color.White,
							modifier = Modifier.size(20.dp)
						)
						Spacer(modifier = Modifier.width(2.dp))
						Text("Search", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
					}
				}
			}
		}
		
		
	}
}

@Preview
@Composable
fun PreviewAppointmentHeader() {
	AppointmentHeader(3, 10)
}