package com.example.pillmate.presentation.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pillmate.R
import com.example.pillmate.presentation.viewmodel.AppointmentScheduleViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentScheduleForm(
	paddingValues: PaddingValues,
	viewModel: AppointmentScheduleViewModel,
	onBack: () -> Unit // Truyền navController.popBackStack() vào đây
) {
	val uiState by viewModel.uiState.collectAsState()
	val context = LocalContext.current
	val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
	var showReminderDialog by remember { mutableStateOf(false) }
	var reminderToDelete by remember { mutableStateOf<Int?>(null) } //
	
	fun openDatePicker(isStartDate: Boolean) {
		val cal = Calendar.getInstance()
		val currentSelected = if (isStartDate) uiState.startDate ?: Date() else uiState.endDate ?: Date()
		cal.time = currentSelected
		val dpd = DatePickerDialog(
			context,
			{ _, year, month, dayOfMonth ->
				val selectedCal = Calendar.getInstance()
				selectedCal.set(year, month, dayOfMonth)
				if (isStartDate) viewModel.setStartDate(selectedCal.time) else viewModel.setEndDate(selectedCal.time)
			},
			cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
		)
		dpd.show()
	}
	
	Box(modifier = Modifier.fillMaxSize()) {
		Image(painter = painterResource(id = R.drawable.background), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
		Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
		
		Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
			// Header
			Row(modifier = Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
				IconButton(onClick = onBack) {
					Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
				}
				Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
					Text(if (uiState.existingScheduleId != null) "Edit Appointment Schedule" else "New Appointment Schedule", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
				}
				if (uiState.readOnly) {
					IconButton(onClick = { viewModel.setEditMode() }) {
						Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
					}
				} else {
					Spacer(modifier = Modifier.width(48.dp))
				}
			}
			HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
			
			LazyColumn(modifier = Modifier.fillMaxSize().weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
				
				// Section 0: Schedule Name
				item {
					Text(
						"SCHEDULE NAME",
						color = Color.White,
						fontWeight = FontWeight.Bold,
						fontSize = 12.sp,
						modifier = Modifier.padding(bottom = 8.dp)
					)
					Card(
						modifier = Modifier.fillMaxWidth(),
						shape = RoundedCornerShape(16.dp),
						colors = CardDefaults.cardColors(containerColor = Color.White)
					) {
						OutlinedTextField(
							value = uiState.scheduleName,
							onValueChange = { viewModel.setScheduleName(it) },
							placeholder = { Text("e.g. Morning routine, Before bed", color = Color.LightGray) },
							singleLine = true,
							enabled = !uiState.readOnly,
							colors = OutlinedTextFieldDefaults.colors(
								focusedBorderColor = Color.Transparent,
								unfocusedBorderColor = Color.Transparent,
								disabledBorderColor = Color.Transparent,
								focusedTextColor = Color.Black,
								unfocusedTextColor = Color.Black,
								disabledTextColor = Color.DarkGray
							),
							modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
						)
					}
				}
				
				// Appointment Information
				item {
					Text("APPOINTMENT DETAILS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
					Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
						Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
							Box(modifier = Modifier.size(56.dp).background(Color(0xFFE8DECF), CircleShape).clip(CircleShape), contentAlignment = Alignment.Center) {
								Icon(painter = painterResource(R.drawable.ic_person_clock), contentDescription = null, tint = Color(0xFF1c5f55))
							}
							Spacer(modifier = Modifier.width(16.dp))
							Column {
								Text(uiState.selectedAppointment?.name ?: "Unknown Appointment", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 16.sp)
								Text(uiState.selectedAppointment?.doctorName ?: "", color = Color.Gray, fontSize = 14.sp)
							}
						}
					}
				}
				
				// Section 2: Repeat Frequency
				item {
					Row(
						modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(
							"REPEAT FREQUENCY",
							color = Color.White,
							fontWeight = FontWeight.Bold,
							fontSize = 12.sp
						)
						Box(
							modifier = Modifier.background(Color.White, RoundedCornerShape(12.dp))
								.padding(horizontal = 8.dp, vertical = 4.dp)
						) {
							Text(
								"Current: ${uiState.repeatFrequency}",
								color = Color(0xFF2E7D32),
								fontWeight = FontWeight.Bold,
								fontSize = 10.sp
							)
						}
					}
					
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween
					) {
						uiState.availableFrequencies.forEach { freq ->
							val isSelected = uiState.repeatFrequency == freq
							Box(
								modifier = Modifier
								.weight(1f)
								.padding(end = if (freq != uiState.availableFrequencies.last()) 8.dp else 0.dp)
								.clip(RoundedCornerShape(20.dp))
								.background(if (isSelected) Color(0xFF2E7D32) else Color.White)
								.clickable { if (!uiState.readOnly) viewModel.setFrequency(freq) }
								.padding(vertical = 12.dp),
								contentAlignment = Alignment.Center
							) {
								Text(
									freq,
									color = if (isSelected) Color.White else Color.DarkGray,
									fontWeight = FontWeight.Bold,
									fontSize = 12.sp
								)
							}
						}
					}
					
					if (uiState.repeatFrequency == "Weekly") {
						Spacer(modifier = Modifier.height(16.dp))
						Text(
							"SELECT DAYS",
							color = Color.White,
							fontWeight = FontWeight.Bold,
							fontSize = 12.sp,
							modifier = Modifier.padding(bottom = 8.dp)
						)
						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.SpaceBetween
						) {
							val days =
								listOf("Mo" to 1, "Tu" to 2, "We" to 3, "Th" to 4, "Fr" to 5, "Sa" to 6, "Su" to 7)
							days.forEach { (label, dayValue) ->
								val isSelected = uiState.selectedDaysOfWeek.contains(dayValue)
								Box(
									modifier = Modifier
										.size(40.dp)
										.clip(CircleShape)
										.background(if (isSelected) Color(0xFF2E7D32) else Color.White)
										.clickable { if (!uiState.readOnly) viewModel.toggleDayOfWeek(dayValue) },
									contentAlignment = Alignment.Center
								) {
									Text(
										label,
										color = if (isSelected) Color.White else Color.DarkGray,
										fontWeight = FontWeight.Bold,
										fontSize = 14.sp
									)
								}
							}
						}
					} else if (uiState.repeatFrequency == "Interval") {
						Spacer(modifier = Modifier.height(16.dp))
						Text(
							"EVERY",
							color = Color.White,
							fontWeight = FontWeight.Bold,
							fontSize = 12.sp,
							modifier = Modifier.padding(bottom = 8.dp)
						)
						Row(
							modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
							horizontalArrangement = Arrangement.spacedBy(8.dp),
							verticalAlignment = Alignment.CenterVertically
						) {
							OutlinedTextField(
								value = uiState.intervalValue,
								onValueChange = {
									if (!uiState.readOnly) {
										val formatted = it.filter { char -> char.isDigit() }
										viewModel.setIntervalValue(formatted)
									}
								},
								keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
								modifier = Modifier.weight(1f),
								colors = OutlinedTextFieldDefaults.colors(
									focusedBorderColor = Color.Transparent,
									unfocusedBorderColor = Color.Transparent,
									disabledBorderColor = Color.Transparent,
									focusedContainerColor = Color.White,
									unfocusedContainerColor = Color.White,
									focusedTextColor = Color.Black,
									unfocusedTextColor = Color.Black
								),
								shape = RoundedCornerShape(12.dp)
							)
							Box(modifier = Modifier.weight(1f).height(IntrinsicSize.Min)) {
								var expanded by remember { mutableStateOf(false) }
								OutlinedButton(
									onClick = { if (!uiState.readOnly) expanded = true },
									modifier = Modifier.fillMaxWidth().fillMaxHeight(),
									shape = RoundedCornerShape(12.dp),
									colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
								) {
									Text(uiState.intervalUnit, color = Color.Black)
								}
								DropdownMenu(
									expanded = expanded,
									onDismissRequest = { expanded = false },
									modifier = Modifier.background(Color.White)
								) {
									uiState.intervalUnits.forEach { unit ->
										DropdownMenuItem(
											text = { Text(unit, color = Color.Black) },
											onClick = {
												viewModel.setIntervalUnit(unit)
												expanded = false
											}
										)
									}
								}
							}
						}
					}
				}
				
				// Start & End Date
				item {
					Text("COURSE DURATION", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
					Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
						Column(modifier = Modifier.weight(1f)) {
							Text("Start Date", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
							Card(modifier = Modifier.fillMaxWidth().clickable { if (!uiState.readOnly) openDatePicker(true) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
								Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
									Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
									Spacer(modifier = Modifier.width(8.dp))
									Text(uiState.startDate?.let { dateFormatter.format(it) } ?: "Select Date", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
								}
							}
						}
						Column(modifier = Modifier.weight(1f)) {
							Text("End Date (Optional)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
							Card(modifier = Modifier.fillMaxWidth().clickable { if (!uiState.readOnly && uiState.startDate != null) openDatePicker(false) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (uiState.startDate != null) Color.White else Color.LightGray.copy(alpha = 0.5f))) {
								Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
									Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
									Spacer(modifier = Modifier.width(8.dp))
									Text(uiState.endDate?.let { dateFormatter.format(it) } ?: "Ongoing", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
									if (uiState.endDate != null) {
										Icon(
											imageVector = Icons.Default.Clear,
											contentDescription = "Clear End Date",
											tint = Color.Gray,
											modifier = Modifier.size(20.dp).clickable { if (!uiState.readOnly) viewModel.setEndDate(null) }
										)
									}
								}
							}
						}
					}
				}
			}
			
			// Save Button
			if (!uiState.readOnly) {
				Button(
					onClick = { viewModel.saveSchedule() },
					modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
					shape = RoundedCornerShape(50),
					colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
				) {
					if (uiState.isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
					else Text("Save Schedule", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
				}
			}
		}
		
		// Time Picker Dialog
//		if (showReminderDialog) {
//			var selectedTimeText by remember { mutableStateOf<String?>(null) }
//			var timeError by remember { mutableStateOf(false) }
//
//			AlertDialog(
//				onDismissRequest = { showReminderDialog = false },
//				containerColor = Color.White,
//				title = { Text("Add Reminder Time", color = Color.Black) },
//				text = {
//					Column {
//						OutlinedButton(
//							onClick = {
//								timeError = false
//								val cal = Calendar.getInstance()
//								TimePickerDialog(context, { _, hour, minute ->
//									val amPm = if (hour >= 12) "PM" else "AM"
//									val hr = if (hour % 12 == 0) 12 else hour % 12
//									val formattedMin = minute.toString().padStart(2, '0')
//									selectedTimeText = "$hr:$formattedMin $amPm"
//								}, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
//							},
//							modifier = Modifier.fillMaxWidth()
//						) { Text(selectedTimeText?.let { "Time: $it" } ?: "Select Time", color = Color.Black) }
//
//						if (timeError) {
//							Text("Please select a time.", color = Color(0xFFB3261E), fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
//						}
//					}
//				},
//				confirmButton = {
//					Button(onClick = {
//						if (selectedTimeText == null) {
//							timeError = true
//							return@Button
//						}
//						// Chỉ truyền timeTitle
//						viewModel.addReminderTime(selectedTimeText!!)
//						showReminderDialog = false
//					}, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Add") }
//				},
//				dismissButton = { TextButton(onClick = { showReminderDialog = false }) { Text("Cancel", color = Color.Gray) } }
//			)
//		}
	}
}