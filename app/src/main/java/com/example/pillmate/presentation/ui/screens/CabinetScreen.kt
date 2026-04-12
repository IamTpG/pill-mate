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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.example.pillmate.presentation.ui.components.CabinetHeader
import com.example.pillmate.presentation.ui.components.CabinetSubNav
import com.example.pillmate.presentation.ui.components.MedicationCard
import com.example.pillmate.presentation.ui.components.SearchBar
import com.example.pillmate.presentation.ui.components.AddMedicationDialog
import com.example.pillmate.presentation.viewmodel.CabinetViewModel

import com.example.pillmate.presentation.ui.screens.MedicationDetailScreen
import com.example.pillmate.domain.model.Medication
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.pillmate.data.local.entity.SupplyLogEntity

@Composable
fun CabinetScreen(
    viewModel: CabinetViewModel,
    modifier: Modifier = Modifier
) {
    // 1. Observe ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // State to track if the popup is visible
    var showAddDialog by remember { mutableStateOf(false) }
    var showActiveTab by remember { mutableStateOf(true) }
    var selectedMedication by remember { mutableStateOf<Medication?>(null) }
    var medicationToEdit by remember { mutableStateOf<Medication?>(null) }
    var showLogDoseDialog by remember { mutableStateOf(false) }

    // Update selectedMedication if it was modified in the DB
    if (selectedMedication != null) {
        val updated = uiState.activeMedications.find { it.id == selectedMedication?.id } 
            ?: uiState.expiredMedications.find { it.id == selectedMedication?.id }
        if (updated != null && updated != selectedMedication) {
            selectedMedication = updated
        }
    }

    // 2. Main Container
    Box(modifier = modifier.fillMaxSize()) {
        
        if (selectedMedication != null) {
            val logsFlow = remember(selectedMedication!!.id) {
                viewModel.getLogsForMedication(selectedMedication!!.id)
            }
            val logs by logsFlow.collectAsState(initial = emptyList<SupplyLogEntity>())
            
            MedicationDetailScreen(
                medication = selectedMedication!!,
                logs = logs,
                onBack = { selectedMedication = null },
                onEditClick = { medicationToEdit = selectedMedication },
                onLogDoseClick = { showLogDoseDialog = true }
            )
        } else {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = "Forest Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        // 3. Scrollable Content
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, 
                end = 16.dp, 
                top = 48.dp, 
                bottom = 100.dp // Extra padding at the bottom so the SubNav doesn't cover the last pill!
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            
            // --- HEADER ---
            item {
                CabinetHeader(
                    healthScore = uiState.healthScore,
                    activeCount = uiState.activeMedsCount,
                    lowStockCount = uiState.lowStockCount
                )
            }

            // --- SEARCH BAR ---
            item {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) }
                )
            }

            // --- YOUR MEDICATIONS SECTION ---
            item {
                SectionTitle(
                    title = "Your Medications",
                    showActiveTab = showActiveTab,
                    onToggle = { showActiveTab = it }
                )
            }

            val currentMeds = if (showActiveTab) uiState.activeMedications else uiState.expiredMedications
            if (currentMeds.isEmpty()) {
                item {
                    Text(
                        text = if (showActiveTab) "No active medications" else "No expired medications",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(currentMeds) { medication ->
                    MedicationCard(
                        medication = medication,
                        onClick = { selectedMedication = medication }
                    )
                }
            }
        }

        // 4. Sub Navigation
        CabinetSubNav(
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = Color.White,
            contentColor = Color(0xFF1c5f55), // Your brand green
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 100.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Log Medicine",
                modifier = Modifier.size(32.dp)
            )
        }
        } // End of else block for main screen content

        if (showAddDialog || medicationToEdit != null) {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            
            AddMedicationDialog(
                medicationToEdit = medicationToEdit,
                onDismiss = { 
                    showAddDialog = false
                    medicationToEdit = null
                },
                onConfirm = { name, unit, count, description, expirationDate, imageUri ->
                    coroutineScope.launch(Dispatchers.IO) {
                        var photoUrl: String? = null
                        if (imageUri != null) {
                            try {
                                val fileName = "med_${System.currentTimeMillis()}.jpg"
                                val file = java.io.File(context.filesDir, fileName)
                                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                                    java.io.FileOutputStream(file).use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                                photoUrl = file.absolutePath
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        if (medicationToEdit != null) {
                            viewModel.updateMedication(medicationToEdit!!, name, unit, count, description, expirationDate, photoUrl)
                        } else {
                            viewModel.addMedication(name, unit, count, description, expirationDate, photoUrl)
                        }
                    }
                    showAddDialog = false
                    medicationToEdit = null
                }
            )
        }

        if (showLogDoseDialog && selectedMedication != null) {
            var amountText by remember { mutableStateOf("1") }
            var reasonText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showLogDoseDialog = false },
                containerColor = Color.White,
                title = { Text("Log Dose", color = Color.Black) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Amount Taken") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = reasonText,
                            onValueChange = { reasonText = it },
                            label = { Text("Reason / Note") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = amountText.toIntOrNull() ?: 1
                            viewModel.logDose(selectedMedication!!.id, amount, reasonText)
                            showLogDoseDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E6C54))
                    ) {
                        Text("Save", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogDoseDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}

// Helper component for the section titles
@Composable
private fun SectionTitle(title: String, showActiveTab: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        // Active/Expired toggle
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (showActiveTab) Color.White.copy(alpha = 0.4f) else Color.Transparent)
                        .clickable { onToggle(true) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Active", color = if (showActiveTab) Color.White else Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (!showActiveTab) Color.White.copy(alpha = 0.4f) else Color.Transparent)
                        .clickable { onToggle(false) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Expired", color = if (!showActiveTab) Color.White else Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}