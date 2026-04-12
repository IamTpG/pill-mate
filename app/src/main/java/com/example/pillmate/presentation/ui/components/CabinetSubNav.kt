package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CabinetSubNav(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Replace Icons.Default.Star with your specific hand/pill drawable later
        IconButton(onClick = { /* Handle Click */ }) {
            Icon(Icons.Default.Star, contentDescription = "Inventory", tint = Color(0xFF7CB899))
        }
        IconButton(onClick = { /* Handle Click */ }) {
            Icon(Icons.Default.DateRange, contentDescription = "Calendar", tint = Color.White)
        }
        IconButton(onClick = { /* Handle Click */ }) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
        }
    }
}