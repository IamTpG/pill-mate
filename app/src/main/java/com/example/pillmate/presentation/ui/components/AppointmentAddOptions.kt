package com.example.pillmate.presentation.ui.components

import com.example.pillmate.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.colorResource

@Composable
fun AppointmentAddOptions(
	onClickCloseButton: () -> Unit,
	onClickAddAppointment: () -> Unit
) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.height(400.dp),
		colors = CardDefaults.cardColors(containerColor = Color.White),
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
		
	) {
		Column {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.wrapContentHeight()
					.background(Color(0XFFD4D4D4))
					.padding(horizontal = 16.dp, vertical = 10.dp),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text="What would you like to add?",
					color = Color(0XFF5a9677),
					fontSize = 18.sp,
					fontWeight = FontWeight.Medium,
				)
				
				IconButton(onClick = {
					onClickCloseButton()
				}) {
					Icon(
						painter = painterResource(R.drawable.ic_closing),
						contentDescription = "Close Card Icon",
						modifier = Modifier.size(30.dp)
					)
				}
			}
			
			Column {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.clickable {
							onClickCloseButton()
							onClickAddAppointment()
						}
						.padding(top=16.dp, bottom = 16.dp, start = 10.dp),
					horizontalArrangement = Arrangement.Start,
					verticalAlignment = Alignment.CenterVertically
				) {
					Icon(
						painter = painterResource(R.drawable.ic_calendar),
						contentDescription = "Add Appointment Icon",
						modifier = Modifier.size(60.dp)
					)
					Spacer(modifier = Modifier.width(16.dp))
					Text(
						text="Add Appointment",
						color= colorResource(R.color.primary_green),
						fontSize = 20.sp,
					)
				}
				
			}
		}
	}
}

//@Preview
//@Composable
//fun PreviewComponents() {
//	AppointmentAddingOptions()
//}