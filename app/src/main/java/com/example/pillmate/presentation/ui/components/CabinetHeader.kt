package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.shape.CircleShape

@Composable
fun CabinetHeader(
    healthScore: Int,
    activeCount: Int,
    lowStockCount: Int,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4FAF7))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Cabinet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3D34)
                    )
                    Text(
                        text = "Your cabinet is $healthScore% healthy today.",
                        fontSize = 14.sp,
                        color = Color(0xFF4A6B5D)
                    )
                }
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search Medication Info",
                        tint = Color(0xFF4A6B5D),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBox(
                    modifier = Modifier.weight(1f),
                    number = activeCount.toString(),
                    label = "ACTIVE MEDS",
                    numberColor = Color(0xFF2E7D32)
                )
                StatBox(
                    modifier = Modifier.weight(1f),
                    number = lowStockCount.toString(),
                    label = "LOW STOCK",
                    numberColor = Color(0xFFD32F2F)
                )
            }
        }
    }
}

@Composable
private fun StatBox(modifier: Modifier, number: String, label: String, numberColor: Color) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = number, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = numberColor)
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
    }
}