package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.presentation.model.CalendarDay
import com.example.pillmate.presentation.model.HomeTask
import java.util.Date

@Composable
fun HomeHeader(
    onAddClick: () -> Unit,
    onDebugClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0x40000000))
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Home",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Row {
            IconButton(onClick = onDebugClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Debug Menu",
                    tint = Color.White
                )
            }
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add task",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ProgressCard(
    completed: Int,
    total: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            Column {
                Text(
                    text = "TODAY'S PROGRESS",
                    color = colorResource(id = R.color.primary_green),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$completed/$total",
                        color = Color.Black,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "tasks completed",
                        color = colorResource(id = R.color.status_upcoming),
                        fontSize = 16.sp
                    )
                }
                LinearProgressIndicator(
                    progress = if (total > 0) completed.toFloat() / total else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = colorResource(id = R.color.primary_green),
                    trackColor = Color(0xFFE0E0E0)
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.CenterEnd)
                    .graphicsLayer(alpha = 0.2f),
                tint = colorResource(id = R.color.primary_green)
            )
        }
    }
}

@Composable
fun CalendarRow(
    days: List<CalendarDay>,
    onDateSelected: (Date) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days) { day ->
            CalendarDayItem(day = day, onClick = { onDateSelected(day.date) })
        }
    }
}

@Composable
fun CalendarDayItem(
    day: CalendarDay,
    onClick: () -> Unit
) {
    val backgroundColor = if (day.isSelected) colorResource(id = R.color.primary_green) else Color.White
    val contentColor = if (day.isSelected) Color.White else Color.Black
    val dayOfWeekColor = if (day.isSelected) Color.White else colorResource(id = R.color.status_upcoming)

    Column(
        modifier = Modifier
            .width(60.dp)
            .height(85.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.dayOfWeek,
            color = dayOfWeekColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = day.dayOfMonth,
            color = contentColor,
            fontSize = if (day.isSelected) 20.sp else 18.sp,
            fontWeight = FontWeight.Bold
        )
        if (day.isSelected) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(4.dp)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun TaskItem(
    task: HomeTask,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colorResource(id = R.color.primary_green), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.pill),
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.time,
                        color = colorResource(id = R.color.status_upcoming),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(task.status)
                }
                Text(
                    text = task.title,
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = task.doseDescription,
                    color = colorResource(id = R.color.status_upcoming),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: LogStatus?) {
    val (statusText, textColorRes, bgColor) = when (status) {
        LogStatus.COMPLETED -> Triple("Done", R.color.status_done, Color(0xFFD5F5E3))
        LogStatus.MISSED -> Triple("Missed", R.color.status_missed, Color(0xFFFADBD8))
        LogStatus.SKIPPED -> Triple("Skipped", R.color.status_upcoming, Color(0xFFEBEDEF))
        LogStatus.SNOOZED -> Triple("Snoozed", R.color.status_snoozed, Color(0xFFFCF3CF))
        LogStatus.LATE -> Triple("Late", R.color.status_missed, Color(0xFFFADBD8))
        null -> Triple("Upcoming", R.color.status_upcoming, Color(0xFFEBF5FB))
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = statusText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = colorResource(id = textColorRes),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

