package com.example.pillmate.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.example.pillmate.presentation.viewmodel.AIChatViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    paddingValues: PaddingValues,
    viewModel: AIChatViewModel = koinViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var pendingDeleteSessionId by remember { mutableStateOf<String?>(null) }
    val uiState by viewModel.uiState.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val hasRealMessages = messages.isNotEmpty()
    val defaultGreeting = "Hello! I'm your PillMate Assistant. How can I help you manage your medications today?"

    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFE6F4EE),
                modifier = Modifier.width(280.dp)
            ) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Chat History",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E6C54)
                )
                Spacer(Modifier.height(4.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("New Chat") },
                    selected = false,
                    onClick = {
                        viewModel.createNewChat()
                        scope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color(0xFFD1EBDD),
                        selectedContainerColor = Color(0xFFB8DEC9)
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                sessions.forEach { chatSession ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.List, contentDescription = null) },
                            label = { Text(chatSession.title) },
                            selected = chatSession.id == uiState.currentSessionId,
                            onClick = {
                                viewModel.selectSession(chatSession.id)
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color(0xFFC5E4D2),
                                selectedContainerColor = Color(0xFF96CEB2)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { pendingDeleteSessionId = chatSession.id }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete chat",
                                tint = Color(0xFF1E6C54)
                            )
                        }
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background Image
            Image(
                painter = painterResource(id = R.drawable.background),
                contentDescription = "Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "PillMate AI",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!hasRealMessages) {
                        item {
                            ChatBubble(
                                text = defaultGreeting,
                                isBot = true,
                                timestamp = "Now"
                            )
                        }
                    }
                    items(messages) { msg ->
                        ChatBubble(
                            text = msg.text,
                            isBot = msg.isBot,
                            timestamp = formatTimestamp(msg.createdAt)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "PillMate AI provides info, not medical advice. Consult a professional for emergencies.",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        OutlinedTextField(
                            value = uiState.inputText,
                            onValueChange = viewModel::onInputChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && !event.isShiftPressed) {
                                        viewModel.sendMessage()
                                        true
                                    } else {
                                        false
                                    }
                                },
                            placeholder = { Text("Ask about medications...") },
                            shape = RoundedCornerShape(24.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = { viewModel.sendMessage() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = Color(0xFF1E6C54),
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White
                            ),
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.sendMessage()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send",
                                        tint = if (uiState.isThinking) Color.LightGray else Color.Gray
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (pendingDeleteSessionId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteSessionId = null },
            title = { Text("Delete chat?") },
            text = { Text("Are you sure you want to delete this chat? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val target = pendingDeleteSessionId
                    pendingDeleteSessionId = null
                    if (target != null) {
                        viewModel.deleteChat(target)
                    }
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSessionId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ChatBubble(
    text: String,
    isBot: Boolean,
    timestamp: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isBot) Arrangement.Start else Arrangement.End
    ) {
        if (isBot) {
            // Bot Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.pillmate_logo),
                    contentDescription = "Bot Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.6f),
                    contentScale = ContentScale.Crop
                )
                // Green dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Green)
                        .align(Alignment.BottomEnd)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isBot) Alignment.Start else Alignment.End,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isBot) 4.dp else 16.dp,
                    bottomEnd = if (isBot) 16.dp else 4.dp
                ),
                color = if (isBot) Color.White else Color(0xFF2A7A57),
                shadowElevation = 2.dp
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(16.dp),
                    color = if (isBot) Color(0xFF1E6C54) else Color.White,
                    fontSize = 16.sp
                )
            }
            Text(
                text = timestamp,
                fontSize = 10.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMillis))
}
