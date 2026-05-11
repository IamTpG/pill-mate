package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.presentation.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthRemindersBottomSheet(
    viewModel: ProfileViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val profile by viewModel.currentLocalProfile.collectAsState()
    val primaryGreen = Color(0xFF1ABC9C) // Match PrimaryGreen from SettingsScreen
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1B1B1B),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 24.dp, end = 24.dp)
        ) {
            Text(
                text = "Health Notifications",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Settings are synced with your profile.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Dynamic options per type
            val hydrationOptions = listOf(60 to "1h", 240 to "4h", 480 to "8h", 1440 to "Daily")
            val bpOptions = listOf(1440 to "Daily", 2880 to "2 Days", 10080 to "Weekly")
            val weightOptions = listOf(10080 to "Weekly", 20160 to "2 Weeks", 43200 to "Monthly")

            profile?.let { p ->
                HealthReminderItem(
                    label = "Hydration",
                    initEnabled = p.hydrationReminderEnabled,
                    initInterval = p.hydrationInterval,
                    options = hydrationOptions,
                    primaryGreen = primaryGreen,
                    onUpdate = { e, i -> viewModel.updateHealthReminder("HYDRATION", e, i) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))
                HealthReminderItem(
                    label = "Blood Pressure",
                    initEnabled = p.bpReminderEnabled,
                    initInterval = p.bpInterval,
                    options = bpOptions,
                    primaryGreen = primaryGreen,
                    onUpdate = { e, i -> viewModel.updateHealthReminder("BLOOD_PRESSURE", e, i) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))
                HealthReminderItem(
                    label = "Body Weight",
                    initEnabled = p.weightReminderEnabled,
                    initInterval = p.weightInterval,
                    options = weightOptions,
                    primaryGreen = primaryGreen,
                    onUpdate = { e, i -> viewModel.updateHealthReminder("WEIGHT", e, i) }
                )
            }
        }
    }
}

@Composable
fun HealthReminderItem(
    label: String,
    initEnabled: Boolean,
    initInterval: Int,
    options: List<Pair<Int, String>>,
    primaryGreen: Color,
    onUpdate: (Boolean, Int) -> Unit
) {
    var enabled by remember(initEnabled) { mutableStateOf(initEnabled) }
    var interval by remember(initInterval) { mutableIntStateOf(initInterval) }

    // Ensure interval is one of the valid options if enabled and not already valid
    LaunchedEffect(enabled, interval) {
        if (enabled && options.none { it.first == interval }) {
            val defaultInterval = options.first().first
            onUpdate(true, defaultInterval)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Switch(
                checked = enabled,
                onCheckedChange = { 
                    enabled = it
                    onUpdate(it, interval)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = primaryGreen,
                    checkedTrackColor = primaryGreen.copy(alpha = 0.5f)
                )
            )
        }
        
        if (enabled) {
            Text(
                text = "Reminder frequency:",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { (mins, text) ->
                    val isSelected = interval == mins
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) primaryGreen else Color.White.copy(alpha = 0.1f))
                            .clickable {
                                onUpdate(true, mins)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            color = if (isSelected) Color.White else Color.LightGray,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
