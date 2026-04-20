package com.example.pillmate.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import com.example.pillmate.R
import com.example.pillmate.domain.model.Reminder
import com.example.pillmate.domain.model.Schedule
import com.example.pillmate.presentation.ui.components.*
import com.example.pillmate.presentation.viewmodel.ReminderViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment

@Composable
fun ReminderScreen(
    viewModel: ReminderViewModel,
    paddingValues: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showDialog by remember { mutableStateOf(false) }
    var selectedScheduleId by remember { mutableStateOf<String?>(null) }
    var selectedReminder by remember { mutableStateOf<Reminder?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            ReminderHeader()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(uiState.schedules) { schedule ->
                    ScheduleReminderItem(
                        schedule = schedule,
                        onAddClick = {
                            selectedScheduleId = schedule.id
                            selectedReminder = null
                            showDialog = true
                        },
                        onEditClick = { reminder ->
                            selectedScheduleId = schedule.id
                            selectedReminder = reminder
                            showDialog = true
                        },
                        onRemoveClick = { reminder ->
                            viewModel.removeReminder(schedule.id, reminder)
                        }
                    )
                }
            }
        }

        if (showDialog) {
            ReminderEditDialog(
                reminder = selectedReminder,
                onDismiss = { showDialog = false },
                onSave = { minutes, type ->
                    val scheduleId = selectedScheduleId ?: return@ReminderEditDialog
                    val oldReminder = selectedReminder
                    val newReminder = Reminder(minutesBefore = minutes, type = type)
                    
                    if (oldReminder != null) {
                        viewModel.updateReminder(scheduleId, oldReminder, newReminder)
                    } else {
                        viewModel.addReminder(scheduleId, newReminder)
                    }
                    showDialog = false
                }
            )
        }
    }
}
