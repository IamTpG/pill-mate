package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R

@Composable
fun AppointmentHeader(totalCount: Int) {
	
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp)
	) {
		
		Text(
			"Appointments",
			fontWeight = FontWeight.Bold,
			fontSize = 24.sp,
			color = Color.White
		)
		
		Spacer(modifier = Modifier.height(16.dp))
		
		Card(
			modifier = Modifier.fillMaxWidth(),
			shape = RoundedCornerShape(24.dp),
			colors = CardDefaults.cardColors(containerColor = Color(0xFFF4FAF7))
		) {
			// Bọc tất cả vào một Column tổng để các thành phần xếp hàng dọc
			Column(modifier = Modifier.padding(24.dp)) {
				
				// Phần 1: Tiêu đề
				Column {
					Text(
						text = "Your Cabinet",
						fontSize = 20.sp,
						fontWeight = FontWeight.Bold,
						color = Color(0xFF1E3D34)
					)
					Text(
						text = "Keep track your health journey",
						fontSize = 14.sp,
						color = Color(0xFF4A6B5D)
					)
				}
				
				Spacer(modifier = Modifier.height(16.dp)) // Khoảng cách giữa tiêu đề và box bên dưới
				
				// Phần 2: Box chứa Calendar
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.clip(RoundedCornerShape(14.dp))
						.background(Color(0xFFD8F3DC))
						.padding(horizontal = 16.dp, vertical = 14.dp)
				) {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.Start,
						verticalAlignment = Alignment.CenterVertically
					) {
						Box(
							modifier = Modifier
								.padding(5.dp)
								.clip(CircleShape)
								.background(Color(0xFF2D6A4F))
								.padding(8.dp)
						) {
							Icon(
								painter = painterResource(id = R.drawable.ic_calendar),
								contentDescription = null,
								tint = Color.White,
								modifier = Modifier.size(24.dp)
							)
						}
						
						Spacer(modifier = Modifier.width(5.dp))
						
						Column(
							modifier = Modifier
								.fillMaxWidth()
						) {
							Text(
								"Overview",
								fontWeight = FontWeight.Normal,
								fontSize = 12.sp
							)
							
							Text(
								"Total Appointments: $totalCount",
								fontWeight = FontWeight.Bold,
								fontSize = 20.sp
							)
							
						}
					}
				}
			}
		}
	}
}

@Preview
@Composable
fun PreviewAppointmentHeader() {
	AppointmentHeader(10)
}