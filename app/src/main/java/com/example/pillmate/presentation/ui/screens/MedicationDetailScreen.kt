package com.example.pillmate.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.example.pillmate.domain.model.Medication
import com.example.pillmate.domain.model.InventoryLog
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDetailScreen(
    medication: Medication,
    logs: List<InventoryLog>,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onLogDoseClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            CenterAlignedTopAppBar(
                title = { Text("Medication Details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )

            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Top White Card
                item {
                    Card(
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6FBF8)),
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Pill Image in a Circle
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (medication.photoUrl != null) {
                                    AsyncImage(
                                        model = medication.photoUrl,
                                        contentDescription = "Pill photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.pill), // pill icon
                                        contentDescription = "Pill",
                                        tint = Color(0xFF7CB899),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = medication.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3D34)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = medication.description?.takeIf { it.isNotBlank() } ?: "Take as prescribed",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            val isExpired = medication.supply?.expirationDate?.before(java.util.Date()) == true
                            val badgeColor = if (isExpired) Color(0xFFFFEBEE) else Color(0xFFFCF4F4)
                            val textColor = if (isExpired) Color(0xFFD32F2F) else Color(0xFF333333)
                            val badgeText = if (isExpired) "Expired" else "Active Prescription"
                            
                            Box(
                                modifier = Modifier.background(badgeColor, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(badgeText, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val qty = medication.supply?.quantity?.toInt() ?: 0
                        val unitLabel = if (qty == 1) medication.unit.removeSuffix("s") else medication.unit
                        InfoCard(
                            modifier = Modifier.weight(1f),
                            label = "AMOUNT",
                            value = "$qty $unitLabel",
                            icon = Icons.Outlined.Info
                        )
                        val expiryDate = medication.supply?.expirationDate?.let {
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
                        } ?: "Unknown"
                        InfoCard(
                            modifier = Modifier.weight(1f),
                            label = "EXPIRY",
                            value = expiryDate,
                            icon = Icons.Outlined.DateRange
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    if (logs.isEmpty()) {
                        Text("No logs yet", modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray)
                    } else {
                        logs.sortedByDescending { it.timestamp }.forEach { log ->
                            val status = if (log.changeAmount <= 0) {
                                val qty = kotlin.math.abs(log.changeAmount).toInt()
                                val unitLabel = if (qty == 1) medication.unit.removeSuffix("s") else medication.unit
                                "Taken $qty $unitLabel"
                            } else "Refilled/Adjusted"
                            val timestampStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(log.timestamp)
                            HistoryCard(status, timestampStr, isSkipped = false, reason = log.reason)
                        }
                    }
                }
            }
        }

        val isExpired = medication.supply?.expirationDate?.before(java.util.Date()) == true
        val isOutOfStock = (medication.supply?.quantity?.toInt() ?: 0) <= 0
        val isDisabled = isExpired || isOutOfStock
        val buttonLabel = when {
            isExpired -> "Medication Expired"
            isOutOfStock -> "Out of Stock"
            else -> "+ Log Current Dose"
        }

        // Floating Action Button - Bottom Sticky
        Button(
            onClick = onLogDoseClick,
            enabled = !isDisabled,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 140.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E6C54),
                disabledContainerColor = Color.LightGray,
                disabledContentColor = Color.White
            )
        ) {
            Text(buttonLabel, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = Color.White,
                title = { Text("Delete Medication", color = Color.Black) },
                text = { Text("Are you sure you want to delete this medication?", color = Color.Gray) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            onDeleteClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
fun InfoCard(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFFE8F5E9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(value, fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HistoryCard(status: String, timestamp: String, isSkipped: Boolean, reason: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isSkipped) Icons.Outlined.Info else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = if (isSkipped) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(status, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(timestamp, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Box(
                modifier = Modifier.background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 4.dp).widthIn(max = 100.dp)
            ) {
                Text(reason.takeIf { it.isNotBlank() } ?: "Auto-Log", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}
