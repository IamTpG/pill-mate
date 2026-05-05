package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.example.pillmate.domain.model.MetricType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogVitalsBottomSheet(
    onDismiss: () -> Unit,
    onSave: (MetricType, Double, Double?, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedType by remember { mutableStateOf(MetricType.BLOOD_PRESSURE) }
    
    // BP states
    var systolic by remember { mutableStateOf("120") }
    var diastolic by remember { mutableStateOf("80") }

    // Water state
    var waterMl by remember { mutableStateOf("250") }

    // Weight state
    var weightKg by remember { mutableStateOf("70.0") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Log Daily Vital", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.clip(CircleShape).background(Color(0xFFF5F5F5))) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Metric Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    MetricType.BLOOD_PRESSURE to R.drawable.ic_vitals,
                    MetricType.WATER to R.drawable.ic_reminder,
                    MetricType.WEIGHT to R.drawable.ic_person_clock
                ).forEach { (type, icon) ->
                    MetricSelectorItem(
                        modifier = Modifier.weight(1f),
                        isSelected = selectedType == type,
                        label = type.name.replace("_", " "),
                        icon = icon,
                        onClick = { selectedType = type }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            when (selectedType) {
                MetricType.BLOOD_PRESSURE -> {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        BPIntakeField(label = "SYSTOLIC", value = systolic, onValueChange = { systolic = it })
                        Text("/", fontSize = 32.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))
                        BPIntakeField(label = "DIASTOLIC", value = diastolic, onValueChange = { diastolic = it })
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF2ECC71)).padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_vitals), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Blood pressure readings are best taken when you are relaxed and seated. Avoid caffeine 30 mins prior.",
                                color = Color.White, fontSize = 12.sp
                            )
                        }
                    }
                }
                MetricType.WATER -> {
                    StandardIntakeField(label = "WATER INTAKE", value = waterMl, unit = "ml", onValueChange = { waterMl = it })
                }
                MetricType.WEIGHT -> {
                    StandardIntakeField(label = "BODY WEIGHT", value = weightKg, unit = "kg", onValueChange = { weightKg = it })
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    when (selectedType) {
                        MetricType.BLOOD_PRESSURE -> onSave(MetricType.BLOOD_PRESSURE, systolic.toDoubleOrNull() ?: 0.0, diastolic.toDoubleOrNull(), "mmHg")
                        MetricType.WATER -> onSave(MetricType.WATER, waterMl.toDoubleOrNull() ?: 0.0, null, "ml")
                        MetricType.WEIGHT -> onSave(MetricType.WEIGHT, weightKg.toDoubleOrNull() ?: 0.0, null, "kg")
                        else -> {}
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_history), contentDescription = null, tint = Color(0xFF2ECC71))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Vital", color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                Text("Cancel", color = Color.Gray)
            }
        }
    }
}

@Composable
fun MetricSelectorItem(
    modifier: Modifier,
    isSelected: Boolean,
    label: String,
    icon: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(80.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color.White else Color(0xFFF9F9F9)),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2ECC71)) else null
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (isSelected) Color(0xFF2ECC71) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Color(0xFF2ECC71) else Color.Gray)
        }
    }
}

@Composable
fun BPIntakeField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.size(100.dp, 60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.Bottom) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    )
                    Text("mmHg", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp))
                }
            }
        }
    }
}

@Composable
fun StandardIntakeField(label: String, value: String, unit: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.height(80.dp).fillMaxWidth(0.6f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.Bottom) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    )
                    Text(unit, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                }
            }
        }
    }
}

@Composable
fun BasicTextField(value: String, onValueChange: (String) -> Unit, textStyle: androidx.compose.ui.text.TextStyle) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
    )
}
