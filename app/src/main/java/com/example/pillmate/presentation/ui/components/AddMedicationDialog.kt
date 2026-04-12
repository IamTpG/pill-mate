package com.example.pillmate.presentation.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.pillmate.R
import java.util.Locale
import java.text.SimpleDateFormat
import com.example.pillmate.domain.model.Medication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationDialog(
    medicationToEdit: Medication? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, unit: String, count: Int, description: String, expirationDate: Long, imageUri: Uri?) -> Unit
) {
    var name by remember(medicationToEdit) { mutableStateOf(medicationToEdit?.name ?: "") }
    var unit by remember(medicationToEdit) { mutableStateOf(medicationToEdit?.unit ?: "Capsules") }
    var countText by remember(medicationToEdit) { mutableStateOf(medicationToEdit?.supply?.quantity?.toInt()?.let { if (it > 0) it.toString() else "" } ?: "") }
    var description by remember(medicationToEdit) { mutableStateOf(medicationToEdit?.description ?: "") }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = medicationToEdit?.supply?.expirationDate?.time,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= System.currentTimeMillis() - 86400000
            }
            override fun isSelectableYear(year: Int): Boolean {
                return true
            }
        }
    )
    val formattedDate = datePickerState.selectedDateMillis?.let {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
    } ?: ""
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> imageUri = uri }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(if (medicationToEdit != null) "Edit Medication" else "Add Medication", color = Color.Black) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Avatar Upload
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val modelToLoad = imageUri ?: medicationToEdit?.photoUrl
                    if (modelToLoad != null) {
                        AsyncImage(
                            model = modelToLoad,
                            contentDescription = "Medication Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.pill), // default pill
                            contentDescription = "Pill",
                            tint = Color(0xFF7CB899),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    // Edit Pencil Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .background(Color.White, CircleShape)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Image",
                            tint = Color(0xFF1c5f55),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medication Name (e.g., Ibuprofen)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (e.g., Daily, As needed)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit (e.g., Tablets, mg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it },
                    label = { Text("Current Inventory Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (formattedDate.isNotBlank()) "Expires: $formattedDate" else "Select Expiration Date")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = countText.toIntOrNull() ?: 0
                    
                    // Parse the date (default to 1 year from now if invalid/empty)
                    val selectedMillis = datePickerState.selectedDateMillis
                    val expDateLong = if (selectedMillis != null) {
                        selectedMillis
                    } else {
                        System.currentTimeMillis() + 31536000000L
                    }

                    if (name.isNotBlank()) {
                        onConfirm(name, unit, count, description, expDateLong, imageUri)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1c5f55)) // Brand Green
            ) {
                Text(if (medicationToEdit != null) "Save" else "Add", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK", color = Color(0xFF1c5f55))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
