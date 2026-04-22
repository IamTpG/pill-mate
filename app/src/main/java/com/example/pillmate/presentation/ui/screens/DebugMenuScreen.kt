package com.example.pillmate.presentation.ui.screens

import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.presentation.viewmodel.DebugViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale.getDefault

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugMenuScreen(
    navController: NavController,
    viewModel: DebugViewModel
) {
    val context = LocalContext.current
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf<Schedule?>(null) }
    var schedulesList by remember { mutableStateOf<List<Schedule>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Menu") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    viewModel.generateSampleData(
                        onSuccess = { Toast.makeText(context, "Data Generated!", Toast.LENGTH_SHORT).show() },
                        onError = { Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show() }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Generate Sample Data") }

            Button(
                onClick = {
                    viewModel.clearUserData(
                        onSuccess = { Toast.makeText(context, "Data Cleared!", Toast.LENGTH_SHORT).show() },
                        onError = { Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show() }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Clear All User Data") }

            Button(
                onClick = {
                    viewModel.triggerRandomAlarm(
                        onSuccess = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                        onError = { Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show() }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Trigger Random Alarm") }

            Button(
                onClick = {
                    viewModel.createTestScheduleIn1Min(
                        onSuccess = {
                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            val next = alarmManager.nextAlarmClock?.triggerTime
                            val timeMsg = if (next != null) {
                                SimpleDateFormat("HH:mm:ss", getDefault()).format(
                                    Date(
                                        next
                                    )
                                )
                            } else "None"
                            Toast.makeText(context, "Next alarm: $timeMsg", Toast.LENGTH_LONG).show()
                        },
                        onError = { Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show() }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Test Schedule (in 1 min)") }

            Button(
                onClick = {
                    viewModel.getSchedulesList(
                        onSuccess = {
                            if (it.isEmpty()) Toast.makeText(context, "No schedules found", Toast.LENGTH_SHORT).show()
                            else {
                                schedulesList = it
                                showScheduleDialog = true
                            }
                        },
                        onError = { Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show() }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) { Text("Trigger Specific Reminder") }

            Button(
                onClick = {
                    viewModel.forceSync(
                        onSuccess = { Toast.makeText(context, "Sync Completed!", Toast.LENGTH_SHORT).show() },
                        onError = { Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show() }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) { Text("Force Alarm Sync") }

            Button(
                onClick = {
                    val ids = viewModel.getScheduledIds()
                    if (ids.isEmpty()) Toast.makeText(context, "No alarms tracked in local storage", Toast.LENGTH_SHORT).show()
                    else {
                        AlertDialog.Builder(context)
                            .setTitle("Active RequestCodes")
                            .setMessage(ids.joinToString("\n"))
                            .setPositiveButton("OK", null)
                            .show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) { Text("View Alarm Tracker Prefs") }

            Button(
                onClick = {
                    viewModel.clearAlarmTracker()
                    Toast.makeText(context, "Alarm Tracker Cleared!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Clear Alarm Tracker Prefs") }
        }

        if (showScheduleDialog) {
            AlertDialog(
                onDismissRequest = { showScheduleDialog = false },
                title = { Text("Select Schedule") },
                text = {
                    LazyColumn {
                        items(schedulesList) { schedule ->
                            TextButton(
                                onClick = {
                                    showScheduleDialog = false
                                    showReminderDialog = schedule
                                }
                            ) {
                                Text("${schedule.eventSnapshot.title} (${schedule.startTime})")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showScheduleDialog = false }) { Text("Cancel") }
                }
            )
        }

        showReminderDialog?.let { schedule ->
            AlertDialog(
                onDismissRequest = { showReminderDialog = null },
                title = { Text("Select Reminder") },
                text = {
                    if (schedule.reminders.isEmpty()) {
                        Text("No reminders in this schedule!")
                    } else {
                        LazyColumn {
                            items(schedule.reminders) { reminder ->
                                TextButton(
                                    onClick = {
                                        viewModel.triggerSpecificReminder(
                                            schedule,
                                            reminder,
                                            onSuccess = {
                                                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                                                showReminderDialog = null
                                            },
                                            onError = {
                                                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                ) {
                                    Text("${reminder.type} at ${reminder.minutesBefore}m before")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showReminderDialog = null }) { Text("Cancel") }
                }
            )
        }
    }
}
