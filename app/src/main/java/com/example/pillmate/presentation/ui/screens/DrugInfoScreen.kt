package com.example.pillmate.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.example.pillmate.domain.model.DrugInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrugInfoScreen(
    drug: DrugInfo,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.background), contentDescription = "Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text("Drug Information", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) { Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBF9)) // Slightly green-tinted white
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Box(modifier = Modifier.background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("Analgesic & Antipyretic", color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(drug.brandName.take(30), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                                    Text(drug.genericName.take(40), fontSize = 14.sp, color = Color.DarkGray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, maxLines = 1)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.size(48.dp).background(Color.White, CircleShape).border(1.dp, Color(0xFFE0E0E0), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Health", tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TagBadge("Over-the-Counter")
                                TagBadge("FDA Approved")
                            }
                        }
                    }
                }

                // Critical Safety Warning
                if (!drug.warnings.isNullOrBlank()) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.width(4.dp).height(intrinsicSize = IntrinsicSize.Min).background(Color(0xFFE57373), RoundedCornerShape(2.dp)))
                            Column(modifier = Modifier.padding(start = 12.dp, end = 16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("CRITICAL SAFETY WARNING", color = Color(0xFFE57373), fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(drug.warnings.take(400) + if(drug.warnings.length > 400) "..." else "", color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }

                // Accordions
                if (drug.infoSections.isEmpty()) {
                    item {
                        Text("No detailed sections provided by FDA.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    drug.infoSections.forEach { section ->
                        val (title, content) = section
                        val subtitle = when(title) {
                            "Indications & Usage" -> "What this drug treats"
                            "Dosage & Administration" -> "How to take correctly"
                            "Contraindications" -> "When not to use"
                            "Active Ingredient" -> "Active chemical makeup"
                            "Do Not Use", "Stop Use" -> "Critical warnings"
                            else -> "More usage specifics"
                        }
                                       
                        val icon = if (title == "Contraindications" || title == "Do Not Use" || title == "Stop Use") Icons.Outlined.Warning else Icons.Outlined.Info
                        
                        item {
                            AccordionSection(title, subtitle, icon, content)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TagBadge(text: String) {
    Box(modifier = Modifier.border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, fontSize = 10.sp, color = Color.DarkGray)
    }
}

@Composable
fun AccordionSection(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(Color(0xFFE8F5E9), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        Text(subtitle, fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Expand", tint = Color.Gray)
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(content, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 20.sp)
                }
            }
        }
    }
}
