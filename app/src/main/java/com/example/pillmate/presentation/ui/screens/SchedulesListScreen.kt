package com.example.pillmate.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.example.pillmate.domain.model.Reminder
import com.example.pillmate.domain.model.ReminderType
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.presentation.ui.components.*
import com.example.pillmate.presentation.viewmodel.ScheduleBuilderUiState
import com.example.pillmate.presentation.viewmodel.ScheduleBuilderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesListScreen(
    viewModel: ScheduleBuilderViewModel,
    paddingValues: PaddingValues,
    uiState: ScheduleBuilderUiState,
    onAddClick: () -> Unit,
    onScheduleClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onBack: () -> Unit
) {
    var scheduleToDelete by remember { mutableStateOf<String?>(null) }
    
    var showReminderDialog by remember { mutableStateOf(false) }
    var selectedScheduleId by remember { mutableStateOf<String?>(null) }
    var selectedReminder by remember { mutableStateOf<Reminder?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Forest background
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Top bar matching other screens
            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        uiState.selectedMedication?.name ?: "Schedules",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(48.dp))
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

            if (uiState.existingSchedules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No schedules yet. Tap + to add one.", color = Color.LightGray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("SCHEDULES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(uiState.existingSchedules) { schedule ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ScheduleInfoCard(
                                schedule = schedule,
                                onClick = { onScheduleClick(schedule.id) },
                                onDelete = { scheduleToDelete = schedule.id }
                            )
                            RemindersCard(
                                schedule = schedule,
                                onAddReminder = {
                                    selectedScheduleId = schedule.id
                                    selectedReminder = null
                                    showReminderDialog = true
                                },
                                onEditReminder = { reminder ->
                                    selectedScheduleId = schedule.id
                                    selectedReminder = reminder
                                    showReminderDialog = true
                                },
                                onRemoveReminder = { reminder ->
                                    viewModel.removeReminderFromExistingSchedule(schedule.id, reminder)
                                }
                            )
                        }
                    }
                }
            }
        }
        
        if (showReminderDialog) {
            ReminderEditDialog(
                reminder = selectedReminder,
                onDismiss = { showReminderDialog = false },
                onSave = { minutes, type ->
                    val sId = selectedScheduleId ?: return@ReminderEditDialog
                    val old = selectedReminder
                    val new = Reminder(minutesBefore = minutes, type = type)
                    
                    if (old != null) {
                        viewModel.updateReminderInExistingSchedule(sId, old, new)
                    } else {
                        viewModel.addReminderToExistingSchedule(sId, new)
                    }
                    showReminderDialog = false
                }
            )
        }

        // FAB
        FloatingActionButton(
            onClick = onAddClick,
            containerColor = Color.White,
            contentColor = Color(0xFF1c5f55),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = paddingValues.calculateBottomPadding() + 24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Schedule", modifier = Modifier.size(32.dp))
        }
    }

    // Delete confirmation dialog
    if (scheduleToDelete != null) {
        AlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            containerColor = Color.White,
            title = { Text("Delete Schedule", color = Color.Black) },
            text = { Text("Are you sure you want to delete this schedule? This action cannot be undone.", color = Color.Gray) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClick(scheduleToDelete!!)
                        scheduleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ScheduleInfoCard(
    schedule: Schedule,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.name.ifBlank { schedule.frequency ?: "Daily" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                if (schedule.name.isNotBlank()) {
                    Text(text = schedule.frequency ?: "Daily", fontSize = 14.sp, color = Color.DarkGray)
                }
                Spacer(modifier = Modifier.height(4.dp))
                val timeString = schedule.doseTimes.joinToString(", ") { it.time }
                Text(text = "Times: $timeString", fontSize = 14.sp, color = Color.DarkGray)
                if (schedule.endDate != null) {
                    val format = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    Text(text = "Ends: ${format.format(schedule.endDate)}", fontSize = 12.sp, color = Color.Gray)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Schedule", tint = Color(0xFFD32F2F))
            }
        }
    }
}

@Composable
fun RemindersCard(
    schedule: Schedule,
    onAddReminder: () -> Unit,
    onEditReminder: (Reminder) -> Unit,
    onRemoveReminder: (Reminder) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)), // Light green tint
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp) // Indented
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Reminders",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1c5f55)
                )
                IconButton(onClick = onAddReminder, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add Reminder", tint = Color(0xFF1c5f55), modifier = Modifier.size(16.dp))
                }
            }
            
            if (schedule.reminders.isEmpty()) {
                Text("No reminders set", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
            } else {
                schedule.reminders.forEach { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        onEditClick = { onEditReminder(reminder) },
                        onRemoveClick = { onRemoveReminder(reminder) }
                    )
                }
            }
        }
    }
}
