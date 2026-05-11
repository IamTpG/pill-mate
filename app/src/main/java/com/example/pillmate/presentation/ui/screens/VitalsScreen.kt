package com.example.pillmate.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.example.pillmate.domain.model.HealthMetric
import com.example.pillmate.domain.model.MetricType
import com.example.pillmate.presentation.viewmodel.VitalsViewModel
import com.example.pillmate.presentation.ui.components.LogVitalsBottomSheet
import com.example.pillmate.presentation.ui.components.HydrationGoalDialog
import com.example.pillmate.presentation.ui.components.HealthRemindersBottomSheet
import com.example.pillmate.presentation.viewmodel.ProfileViewModel
import com.example.pillmate.presentation.viewmodel.DailyHydration
import com.example.pillmate.presentation.viewmodel.WeeklyStats
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VitalsScreen(
    viewModel: VitalsViewModel,
    paddingValues: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val profileViewModel: ProfileViewModel = koinViewModel()
    val currentLocalProfile by profileViewModel.currentLocalProfile.collectAsState()
    val isCaregiver = currentLocalProfile?.role == "Caregiver_View"
    var showHydrationDialog by remember { mutableStateOf(false) }
    var showHealthRemindersSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            VitalsHeader(
                showAdd = !isCaregiver,
                showSettings = !isCaregiver,
                onAddClick = { viewModel.toggleLogPanel(true) },
                onReportClick = { viewModel.toggleWeeklyReport(true) },
                onSettingsClick = { showHealthRemindersSheet = true }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HydrationCard(
                        current = uiState.hydrationMl,
                        target = uiState.hydrationTarget,
                        onClick = { if (!isCaregiver) showHydrationDialog = true }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.blood_pressure_label),
                            value = uiState.latestBloodPressure,
                            unit = "mmHg",
                            status = uiState.bloodPressureStatus,
                            icon = R.drawable.ic_vitals_outlined
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.body_weight_label),
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
                        Text(stringResource(R.string.recent_activity), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(stringResource(R.string.view_all), color = colorResource(id = R.color.primary_green), fontSize = 14.sp)
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

    if (showHydrationDialog) {
        HydrationGoalDialog(
            currentGoal = uiState.hydrationTarget,
            onDismiss = { showHydrationDialog = false },
            onSave = { goal ->
                viewModel.updateHydrationTarget(goal)
                showHydrationDialog = false
            }
        )
    }

    if (uiState.showWeeklyReport) {
        WeeklyReportBottomSheet(
            stats = uiState.weeklyStats,
            onDismiss = { viewModel.toggleWeeklyReport(false) }
        )
    }

    if (showHealthRemindersSheet) {
        HealthRemindersBottomSheet(
            viewModel = profileViewModel,
            onDismiss = { showHealthRemindersSheet = false }
        )
    }
}

@Composable
fun VitalsHeader(
    showAdd: Boolean,
    showSettings: Boolean,
    onAddClick: () -> Unit,
    onReportClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
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
            Text(stringResource(R.string.vitals_title), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
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
                if (showSettings) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onSettingsClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                if (showAdd) {
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
}

@Composable
fun HydrationCard(current: Int, target: Int, onClick: () -> Unit) {
    val progress = (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_water_drop), // Water icon
                    contentDescription = null,
                    tint = Color(0xFF5D5DFF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.hydration_goal), color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold)
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
                Text(stringResource(R.string.completed_percent, (progress * 100).toInt()), fontSize = 12.sp, color = Color.Gray)
                Text(stringResource(R.string.remaining_ml, target - current), fontSize = 12.sp, color = Color.Gray)
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
                    painter = painterResource(id = if (metric.type == MetricType.WATER) R.drawable.ic_water_drop else R.drawable.ic_vitals_outlined),
                    contentDescription = null,
                    tint = if (metric.type == MetricType.WATER) Color(0xFF5D5DFF) else Color(0xFFFF708D)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(metric.recordedAt), fontSize = 12.sp, color = Color.Gray)
                val typeLabel = when(metric.type) {
                    MetricType.WATER -> stringResource(R.string.metric_water)
                    MetricType.BLOOD_PRESSURE -> stringResource(R.string.metric_blood_pressure)
                    MetricType.WEIGHT -> stringResource(R.string.metric_weight)
                    MetricType.HEART_RATE -> stringResource(R.string.metric_heart_rate)
                }
                Text(typeLabel, fontWeight = FontWeight.Bold)            }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(stringResource(R.string.weekly_insights), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(stats.dateRange.ifBlank { stringResource(R.string.weekly_summary }, color = Color.Gray, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeeklyStatItem(Modifier.weight(1f), "Logs", stats.totalLogs.toString(), Color(0xFF1ABC9C))
                WeeklyStatItem(Modifier.weight(1f), "Water Avg", stats.avgWaterPerDay, Color(0xFF5D5DFF))
                WeeklyStatItem(Modifier.weight(1f), "Goal Days", stats.hydrationGoalSummary, Color(0xFF2ECC71))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            ReportSectionTitle("Hydration")
            Text("Total intake: ${stats.totalWater}", color = Color.Gray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(16.dp))

            WeeklyHydrationTrend(stats.dailyHydration)

            Spacer(modifier = Modifier.height(24.dp))
            ReportSectionTitle("Blood Pressure")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeeklyStatItem(Modifier.weight(1f), "Average", stats.avgBP, Color(0xFFFF708D))
                WeeklyStatItem(Modifier.weight(1f), "Latest", stats.latestBpStatus, Color(0xFFFF708D))
                WeeklyStatItem(Modifier.weight(1f), "Above Normal", "${stats.highBpReadings}/${stats.bpReadings}", Color(0xFFFF708D))
            }

            Spacer(modifier = Modifier.height(24.dp))
            ReportSectionTitle("Weight")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeeklyStatItem(Modifier.weight(1f), "Average", if (stats.weightReadings > 0) stats.avgWeight else "--", Color(0xFF2ECC71))
                WeeklyStatItem(Modifier.weight(1f), "Change", stats.weightChange, Color(0xFF2ECC71))
            }

            Spacer(modifier = Modifier.height(24.dp))
            ReportSectionTitle("Activity")
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val maxLogs = (stats.activityCounts.values.maxOrNull() ?: 1).coerceAtLeast(1)
                listOf(MetricType.BLOOD_PRESSURE, MetricType.WATER, MetricType.WEIGHT).forEach { type ->
                    val count = stats.activityCounts[type] ?: 0
                    ActivityBar(
                        label = type.readableName(),
                        count = count,
                        max = maxLogs,
                        color = when(type) {
                            MetricType.BLOOD_PRESSURE -> Color(0xFFFF708D)
                            MetricType.WATER -> Color(0xFF5D5DFF)
                            else -> Color(0xFF2ECC71)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            ReportSectionTitle("Takeaways")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stats.insights.ifEmpty { listOf("Start logging vitals to build a weekly trend.") }.forEach { insight ->
                    Text(
                        text = insight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFAFAFA))
                            .padding(12.dp),
                        color = Color(0xFF333333),
                        fontSize = 13.sp
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
                Text(stringResource(R.string.close_report), color = Color.White, fontWeight = FontWeight.Bold)
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
fun ReportSectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF222222))
}

@Composable
fun WeeklyHydrationTrend(days: List<DailyHydration>) {
    if (days.isEmpty()) {
        Text("No hydration logs this week.", color = Color.Gray, fontSize = 13.sp)
        return
    }

    val maxAmount = days.maxOf { it.amountMl }.coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val barHeight = ((day.amountMl.toFloat() / maxAmount.toFloat()).coerceIn(0.05f, 1f) * 72).dp
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text("${day.amountMl}", fontSize = 10.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(if (day.goalMet) Color(0xFF2ECC71) else Color(0xFF5D5DFF))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(day.label, fontSize = 11.sp, color = Color.DarkGray)
            }
        }
    }
}

@Composable
fun ActivityBar(label: String, count: Int, max: Int, color: Color) {
    val progress = if (count == 0) 0f else (count.toFloat() / max.toFloat()).coerceIn(0.08f, 1f)
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, color = Color.DarkGray)
            Text(stringResource(R.string.logs_count, count), fontSize = 12.sp, color = Color.Gray)
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

private fun MetricType.readableName(): String {
    return when (this) {
        MetricType.BLOOD_PRESSURE -> "Blood pressure"
        MetricType.WATER -> "Water"
        MetricType.WEIGHT -> "Weight"
        MetricType.HEART_RATE -> "Heart rate"
    }
}
