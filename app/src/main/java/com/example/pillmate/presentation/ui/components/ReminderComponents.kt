package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.example.pillmate.domain.model.Reminder
import com.example.pillmate.domain.model.ReminderType
import com.example.pillmate.domain.model.Schedule
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReminderHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0x40000000))
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Reminders",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ScheduleReminderItem(
    schedule: Schedule,
    onAddClick: () -> Unit,
    onEditClick: (Reminder) -> Unit,
    onRemoveClick: (Reminder) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = schedule.eventSnapshot.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatScheduleTime(schedule.startTime),
                            fontSize = 14.sp,
                            color = colorResource(id = R.color.status_upcoming)
                        )
                        val readableRRULE = formatRRULE(schedule.recurrenceRule)
                        if (readableRRULE.isNotBlank()) {
                            Text(
                                text = " | $readableRRULE",
                                fontSize = 14.sp,
                                color = Color(0x88000000),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Reminder",
                        tint = colorResource(id = R.color.primary_green)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            schedule.reminders.forEach { reminder ->
                ReminderRow(
                    reminder = reminder,
                    onEditClick = { onEditClick(reminder) },
                    onRemoveClick = { onRemoveClick(reminder) }
                )
            }
        }
    }
}

@Composable
fun ReminderRow(
    reminder: Reminder,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${reminder.minutesBefore} mins before (${reminder.type})",
            modifier = Modifier.weight(1f),
            color = Color(0xFF333333),
            fontSize = 14.sp
        )
        IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                tint = colorResource(id = R.color.status_upcoming),
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onRemoveClick, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = colorResource(id = R.color.status_missed),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ReminderEditDialog(
    reminder: Reminder?,
    onDismiss: () -> Unit,
    onSave: (minutes: Int, type: ReminderType) -> Unit
) {
    var minutes by remember { mutableStateOf(reminder?.minutesBefore?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(reminder?.type ?: ReminderType.NOTIFICATION) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reminder Details",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Minutes Before",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 15") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Reminder Type",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedType == ReminderType.NOTIFICATION,
                        onClick = { selectedType = ReminderType.NOTIFICATION }
                    )
                    Text("Notification", modifier = Modifier.padding(end = 16.dp))
                    RadioButton(
                        selected = selectedType == ReminderType.ALARM,
                        onClick = { selectedType = ReminderType.ALARM }
                    )
                    Text("Alarm")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = minutes.toIntOrNull() ?: 0
                    onSave(m, selectedType)
                },
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.primary_green))
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colorResource(id = R.color.status_upcoming))
            }
        }
    )
}

private fun formatScheduleTime(startTimeStr: String): String {
    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    val hhmmFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val hFormat = SimpleDateFormat("H:m", Locale.getDefault())
    val displayFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    val parsedTime: Date? = try {
        when {
            startTimeStr.contains("T") -> isoFormat.parse(startTimeStr)
            else -> try { hhmmFormat.parse(startTimeStr) } catch(e: Exception) { hFormat.parse(startTimeStr) }
        }
    } catch (e: Exception) { null }

    return if (parsedTime != null) displayFormat.format(parsedTime) else startTimeStr
}

private fun formatRRULE(rrule: String?): String {
    return when {
        rrule?.contains("FREQ=DAILY") == true -> "Daily"
        rrule?.contains("FREQ=WEEKLY") == true -> "Weekly"
        !rrule.isNullOrBlank() -> rrule
        else -> ""
    }
}
