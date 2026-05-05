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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.example.pillmate.domain.model.HealthMetric
import com.example.pillmate.domain.model.MetricType
import com.example.pillmate.presentation.viewmodel.VitalsViewModel
import com.example.pillmate.presentation.ui.components.LogVitalsBottomSheet
import com.example.pillmate.presentation.viewmodel.WeeklyStats
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VitalsScreen(
    viewModel: VitalsViewModel,
    paddingValues: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            VitalsHeader(
                onAddClick = { viewModel.toggleLogPanel(true) },
                onReportClick = { viewModel.toggleWeeklyReport(true) }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HydrationCard(
                        current = uiState.hydrationMl,
                        target = uiState.hydrationTarget
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "BLOOD PRESSURE",
                            value = uiState.latestBloodPressure,
                            unit = "mmHg",
                            status = uiState.bloodPressureStatus,
                            icon = R.drawable.ic_vitals
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "BODY WEIGHT",
                            value = uiState.latestWeight,
                            unit = "kg",
                            status = uiState.weightStatus,
                            icon = R.drawable.ic_person_clock // Proxy icon
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RECENT ACTIVITY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("View All", color = colorResource(id = R.color.primary_green), fontSize = 14.sp)
                    }
                }

                items(uiState.recentActivity.take(5)) { metric ->
                    RecentActivityItem(metric)
                }
            }
        }
    }

    if (uiState.showLogPanel) {
        LogVitalsBottomSheet(
            onDismiss = { viewModel.toggleLogPanel(false) },
            onSave = { type, v1, v2, unit -> viewModel.logMetric(type, v1, v2, unit) }
        )
    }

    if (uiState.showWeeklyReport) {
        WeeklyReportBottomSheet(
            stats = uiState.weeklyStats,
            onDismiss = { viewModel.toggleWeeklyReport(false) }
        )
    }
}

@Composable
fun VitalsHeader(onAddClick: () -> Unit, onReportClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daily Vitals", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0FAF6))
                        .clickable { onReportClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_history), contentDescription = null, tint = Color(0xFF1ABC9C), modifier = Modifier.size(20.dp))
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF1E6C54))
                }
            }
        }
    }
}

@Composable
fun HydrationCard(current: Int, target: Int) {
    val progress = (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_reminder), // Proxy for Water icon
                    contentDescription = null,
                    tint = Color(0xFF5D5DFF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hydration Goal", color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold)
            }
            
            Text(
                text = "${String.format("%,d", current)} / ${String.format("%,d", target)} ml",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                color = Color(0xFF2ECC71),
                trackColor = Color(0xFFF0F0F0)
            )
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(progress * 100).toInt()}% Completed", fontSize = 12.sp, color = Color.Gray)
                Text("${target - current}ml remaining", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    status: String,
    icon: Int
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(painterResource(icon), contentDescription = null, tint = Color(0xFFFF708D), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(unit, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(status, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun RecentActivityItem(metric: HealthMetric) {
    Card(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = if (metric.type == MetricType.WATER) R.drawable.ic_reminder else R.drawable.ic_vitals),
                    contentDescription = null,
                    tint = if (metric.type == MetricType.WATER) Color(0xFF5D5DFF) else Color(0xFFFF708D)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(metric.recordedAt), fontSize = 12.sp, color = Color.Gray)
                Text(metric.type.name.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
            }
            Text(
                text = if (metric.type == MetricType.BLOOD_PRESSURE) "${metric.valuePrimary.toInt()}/${metric.valueSecondary?.toInt() ?: "--"}" 
                       else "${metric.valuePrimary.toInt()} ${metric.unit}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReportBottomSheet(
    stats: WeeklyStats,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Weekly Insights", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Summary for the last 7 days", color = Color.Gray, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                WeeklyStatItem(Modifier.weight(1f), "Avg BP", stats.avgBP, Color(0xFFFF708D))
                WeeklyStatItem(Modifier.weight(1f), "Total Water", stats.totalWater, Color(0xFF5D5DFF))
                WeeklyStatItem(Modifier.weight(1f), "Avg Weight", stats.avgWeight, Color(0xFF2ECC71))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text("Activity Overview", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mock Graph / Bars
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(MetricType.BLOOD_PRESSURE, MetricType.WATER, MetricType.WEIGHT).forEach { type ->
                    val count = stats.activityCounts[type] ?: 0
                    ActivityBar(
                        label = type.name.replace("_", " "),
                        count = count,
                        max = 14, // Assuming 2 logs per day max
                        color = when(type) {
                            MetricType.BLOOD_PRESSURE -> Color(0xFFFF708D)
                            MetricType.WATER -> Color(0xFF5D5DFF)
                            else -> Color(0xFF2ECC71)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ABC9C))
            ) {
                Text("Close Report", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WeeklyStatItem(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ActivityBar(label: String, count: Int, max: Int, color: Color) {
    val progress = (count.toFloat() / max.toFloat()).coerceIn(0.1f, 1f)
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, color = Color.DarkGray)
            Text("$count logs", fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = color,
            trackColor = Color(0xFFF5F5F5)
        )
    }
}
