package com.example.pillmate.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.example.pillmate.presentation.viewmodel.ScheduleBuilderViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import java.util.Date
import android.app.TimePickerDialog
import android.app.DatePickerDialog
import coil.compose.AsyncImage
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.foundation.layout.PaddingValues
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBuilderScreen(
    paddingValues: PaddingValues,
    viewModel: ScheduleBuilderViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val context = LocalContext.current
    var showReminderDialog by remember { mutableStateOf(false) }

    fun openDatePicker(isStartDate: Boolean) {
        val cal = Calendar.getInstance()
        val currentSelected = if (isStartDate) uiState.startDate ?: Date() else uiState.endDate ?: Date()
        cal.time = currentSelected
        val dpd = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, dayOfMonth)
                if (isStartDate) viewModel.setStartDate(selectedCal.time)
                else viewModel.setEndDate(selectedCal.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        // Date Logic Constraints
        if (isStartDate) {
            dpd.datePicker.minDate = System.currentTimeMillis() - 1000
        } else {
            uiState.startDate?.let { stDate ->
                val minEndCal = Calendar.getInstance()
                minEndCal.time = stDate
                minEndCal.add(Calendar.DAY_OF_YEAR, 1)
                dpd.datePicker.minDate = minEndCal.timeInMillis
            }
        }
        dpd.show()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.background), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Schedule Builder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(48.dp))
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

            LazyColumn(modifier = Modifier.fillMaxSize().weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                
                // Section 1: Selected Medication
                item {
                    Text("SELECTED MEDICATION", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(56.dp).background(Color(0xFFE8DECF), CircleShape).clip(CircleShape), contentAlignment = Alignment.Center) {
                                    if (uiState.selectedMedication?.photoUrl != null) {
                                        AsyncImage(
                                            model = android.net.Uri.parse(uiState.selectedMedication!!.photoUrl),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(uiState.selectedMedication?.name ?: "Unknown", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 16.sp)
                                    Text("${uiState.selectedMedication?.supply?.quantity?.toInt() ?: 0} ${uiState.selectedMedication?.unit ?: ""}", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                            Text("Change", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { onBack() })
                        }
                    }
                }

                // Section 2: Repeat Frequency
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("REPEAT FREQUENCY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Box(modifier = Modifier.background(Color.White, RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("Current: ${uiState.repeatFrequency}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        uiState.availableFrequencies.forEach { freq ->
                            val isSelected = uiState.repeatFrequency == freq
                            Box(modifier = Modifier
                                .weight(1f)
                                .padding(end = if (freq != uiState.availableFrequencies.last()) 8.dp else 0.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFF2E7D32) else Color.White)
                                .clickable { viewModel.setFrequency(freq) }
                                .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(freq, color = if (isSelected) Color.White else Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Section 3: Reminder Times
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("REMINDER TIMES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Box(modifier = Modifier.background(Color.White, RoundedCornerShape(16.dp)).clickable { showReminderDialog = true }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Time", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.reminderTimes.forEach { reminder ->
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(40.dp).background(Color(0xFFE8F5E9), CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(painter = painterResource(id = R.drawable.ic_reminder), contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(reminder.timeTitle, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 16.sp)
                                            Text(reminder.doseContext, color = Color.Gray, fontSize = 14.sp)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.removeReminderTime(reminder.id) }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = Color(0xFFE57373))
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Course Duration
                item {
                    Text("COURSE DURATION", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Start Date", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            Card(modifier = Modifier.fillMaxWidth().clickable { openDatePicker(true) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(uiState.startDate?.let { dateFormatter.format(it) } ?: "Select Date", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("End Date (Optional)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            Card(modifier = Modifier.fillMaxWidth().clickable { if (uiState.startDate != null) openDatePicker(false) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (uiState.startDate != null) Color.White else Color.LightGray.copy(alpha = 0.5f))) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(uiState.endDate?.let { dateFormatter.format(it) } ?: "Ongoing", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    if (uiState.endDate != null) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear End Date",
                                            tint = Color.Gray,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable { viewModel.setEndDate(null) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Bottom Button
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (uiState.error != null) {
                        Text(uiState.error!!, color = Color(0xFFE57373), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally))
                    }
                    Button(
                        onClick = { viewModel.saveSchedule() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(if (uiState.saveSuccess) "Saved!" else "Confirm Schedule", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
        // Custom Logic Dialogs
        if (showReminderDialog) {
            var selectedTimeText by remember { mutableStateOf("08:00 AM") }
            var doseText by remember { mutableStateOf("") }
            val unit = uiState.selectedMedication?.unit ?: "units"
            
            fun pickTime() {
                val cal = Calendar.getInstance()
                TimePickerDialog(context, { _, hour, minute ->
                    val amPm = if (hour >= 12) "PM" else "AM"
                    val hr = if (hour % 12 == 0) 12 else hour % 12
                    val formattedMin = minute.toString().padStart(2, '0')
                    selectedTimeText = "$hr:$formattedMin $amPm"
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
            }

            AlertDialog(
                onDismissRequest = { showReminderDialog = false },
                containerColor = Color.White,
                title = { Text("Add Reminder", color = Color.Black) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = { pickTime() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Select Time: $selectedTimeText", color = Color.Black)
                        }
                        OutlinedTextField(
                            value = doseText,
                            onValueChange = { newValue -> 
                                if (newValue.isEmpty()) {
                                    doseText = ""
                                } else {
                                    // only allow digits, and if starts with 0 and is more than 1 char, parse it out implicitly by converting to Int
                                    val formatted = newValue.filter { it.isDigit() }
                                    doseText = formatted
                                }
                            },
                            label = { Text("Amount ($unit)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val parsedDose = doseText.toIntOrNull()
                        val finalDose = if (parsedDose == null || parsedDose <= 0) "1" else parsedDose.toString()
                        val amount = finalDose.toInt()
                        val finalUnit = if (amount == 1 && (unit.equals("capsules", ignoreCase = true) || unit.equals("tablets", ignoreCase = true) || unit.equals("pills", ignoreCase = true))) {
                            unit.dropLast(1)
                        } else {
                            unit
                        }
                        viewModel.addReminderTime(selectedTimeText, "$finalDose $finalUnit")
                        showReminderDialog = false
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReminderDialog = false }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }
    }
}
