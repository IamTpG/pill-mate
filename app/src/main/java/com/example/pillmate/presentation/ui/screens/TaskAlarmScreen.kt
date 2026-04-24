package com.example.pillmate.presentation.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.notification.TaskNotificationManager
import com.example.pillmate.presentation.viewmodel.TaskLogViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAlarmScreen(
    viewModel: TaskLogViewModel,
    sourceId: String,
    scheduleId: String,
    title: String,
    details: String,
    taskTypeString: String,
    instructions: String,
    startTimeStr: String,
    rrule: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val taskType = try { TaskType.valueOf(taskTypeString) } catch (e: Exception) { TaskType.OTHER }

    // Observers
    val logResult by viewModel.logResult.collectAsState()
    val availableSupplies by viewModel.availableSupplies.collectAsState()
    var selectedSupplyId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sourceId) {
        if (taskType == TaskType.MEDICATION) {
            viewModel.fetchSupplies(sourceId)
        }
    }

    // Effect to select the smartest default (lowest stock)
    LaunchedEffect(availableSupplies) {
        if (selectedSupplyId == null && availableSupplies.isNotEmpty()) {
            selectedSupplyId = availableSupplies.filter { it.quantity > 0 }.minByOrNull { it.quantity }?.id 
                ?: availableSupplies.firstOrNull()?.id
        }
    }

    LaunchedEffect(logResult) {
        logResult?.let { result ->
            if (result.isSuccess) {
                Toast.makeText(context, "Task completed!", Toast.LENGTH_SHORT).show()
                TaskNotificationManager(context).dismissNotification(scheduleId)
                onDismiss()
            } else {
                Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Time calculations
    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    val displayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val fallbackFormat = SimpleDateFormat("H:m", Locale.getDefault())

    val parsedStart: Date? = try {
        when {
            startTimeStr.contains("T") -> isoFormat.parse(startTimeStr)
            startTimeStr.isNotBlank() -> {
                try { displayFormat.parse(startTimeStr) } 
                catch (e: Exception) { fallbackFormat.parse(startTimeStr) }
            }
            else -> null
        }
    } catch (e: Exception) { null }

    val formattedTime = if (parsedStart != null) "Scheduled: ${displayFormat.format(parsedStart)}" else "Scheduled: $startTimeStr"
    var nextTimeStr = ""
    if (rrule.isNotBlank() && parsedStart != null) {
        val cal = Calendar.getInstance()
        cal.time = parsedStart
        cal.add(Calendar.DAY_OF_YEAR, 1)
        nextTimeStr = "Next: ${displayFormat.format(cal.time)}"
    }

    val actionButtonText = when (taskType) {
        TaskType.MEDICATION -> "TAKE NOW"
        TaskType.APPOINTMENT -> "ATTEND"
        TaskType.EXERCISE -> "START"
        else -> "COMPLETE"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.weight(0.05f))

            Image(
                painter = painterResource(id = R.drawable.pill),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x40000000)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = instructions.ifBlank { "No description" }, color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = details, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = formattedTime, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        if (nextTimeStr.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = nextTimeStr, color = Color(0xFFB0B0B0))
                            }
                        }
                    }
                }
            }

            // SUPPLY SELECTION UI
            if (taskType == TaskType.MEDICATION && availableSupplies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Select Batch", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableSupplies.size) { index ->
                        val supply = availableSupplies[index]
                        val isSelected = selectedSupplyId == supply.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSupplyId = supply.id },
                            label = { Text("${supply.batchName} (${supply.quantity})") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0x20FFFFFF),
                                selectedContainerColor = Color(0xFF4CAF50),
                                labelColor = Color.White,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            Button(
                onClick = { viewModel.onTakeClicked(sourceId, scheduleId, taskType, Date(), 1.0f, selectedSupplyId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text(text = actionButtonText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { viewModel.onSkipClicked(sourceId, scheduleId, taskType, Date()) },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White) // Assuming border is implicit if handled, actually OutlinedButton default has border
                ) {
                    Text(text = "SKIP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                OutlinedButton(
                    onClick = { viewModel.onSnoozeClicked(sourceId, scheduleId, taskType, Date()) },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = "SNOOZE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
