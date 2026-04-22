package com.example.pillmate.presentation.ui.screens

import com.example.pillmate.data.local.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

// Define internal navigation states
enum class SettingsRoute {
    OPTIONS, EDIT_PROFILE, SWITCH_PROFILE
}

@Composable
fun SettingsScreen(
    paddingValues: PaddingValues,
    onSignOutComplete: () -> Unit,
    onNavigateToAuth: () -> Unit
) {
    val auth: FirebaseAuth = koinInject()
    val database: AppDatabase = koinInject()
    val context = LocalContext.current
    val currentUser = auth.currentUser

    val profileViewModel: com.example.pillmate.presentation.viewmodel.ProfileViewModel = koinViewModel()

    // Load data from Firestore when the screen opens
    LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
    }

    // Observe the CURRENT LOCAL PROFILE from Room
    val currentLocalProfile by profileViewModel.currentLocalProfile.collectAsState()

    // Use the local profile's name for the UI! (Falls back to Firebase/Auth if null)
    val displayName = currentLocalProfile?.name ?: currentUser?.displayName ?: "User"

    var currentRoute by remember { mutableStateOf(SettingsRoute.OPTIONS) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Shared Background
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        // Internal Navigation
        when (currentRoute) {
            SettingsRoute.OPTIONS -> {
                ProfileOptionsScreen(
                    paddingValues = paddingValues,
                    userName = displayName,
                    onEditClick = { currentRoute = SettingsRoute.EDIT_PROFILE },
                    onSwitchClick = { currentRoute = SettingsRoute.SWITCH_PROFILE },
                    onLogoutClick = { performSignOut(context, auth, database, currentLocalProfile?.id, onSignOutComplete) }
                )
            }
            SettingsRoute.EDIT_PROFILE -> {
                EditProfileScreen(
                    viewModel = profileViewModel,
                    paddingValues = paddingValues,
                    onBack = {
                        profileViewModel.loadProfile()
                        currentRoute = SettingsRoute.OPTIONS
                    }
                )
            }
            SettingsRoute.SWITCH_PROFILE -> {
                SwitchProfileScreen(
                    viewModel = profileViewModel,
                    paddingValues = paddingValues,
                    onBack = { currentRoute = SettingsRoute.OPTIONS },
                    onAddProfileClick = onNavigateToAuth
                )
            }
        }
    }
}

@Composable
fun ProfileOptionsScreen(
    paddingValues: PaddingValues,
    userName: String,
    onEditClick: () -> Unit,
    onSwitchClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "Profile",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Avatar",
                tint = Color.White,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = userName,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Buttons
        SettingsButton(
            text = "Edit Profile",
            icon = Icons.Default.Edit,
            onClick = onEditClick
        )
        SettingsButton(
            text = "Switch Profile",
            icon = Icons.Default.AccountCircle, // Placeholder icon
            onClick = onSwitchClick
        )
        SettingsButton(
            text = "Manage Caregiver Access",
            icon = Icons.Default.Lock, // Placeholder icon
            onClick = { /* TODO */ }
        )
        SettingsButton(
            text = "Language",
            icon = Icons.Default.Settings, // Placeholder icon
            onClick = { /* TODO */ }
        )
        SettingsButton(
            text = "Log out",
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            onClick = onLogoutClick
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { /* Emergency Action */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .height(60.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)) // Red color
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EMERGENCY",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: com.example.pillmate.presentation.viewmodel.ProfileViewModel,
    paddingValues: PaddingValues,
    onBack: () -> Unit
) {
    val profileData by viewModel.profileData.collectAsState()

    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var healthInfo by remember { mutableStateOf("") }

    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    LaunchedEffect(profileData) {
        if (profileData.isNotEmpty()) {
            name = profileData["fullName"] ?: ""
            dob = profileData["dateOfBirth"] ?: ""
            healthInfo = profileData["healthInformation"] ?: ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            actions = {
                Button(
                    onClick = {
                        viewModel.saveProfile(name, dob, healthInfo) {
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Avatar
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, contentDescription = "Avatar", tint = Color.White, modifier = Modifier.fillMaxSize())
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-12).dp, y = (-12).dp).size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Edit Avatar", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Form Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                UnderlinedProfileField(label = "Name", value = name, onValueChange = { name = it })

                // Clickable Date of Birth Field
                ClickableUnderlinedProfileField(
                    label = "Date of Birth",
                    value = dob.ifBlank { "Select Date" },
                    onClick = { showDatePicker = true }
                )

                UnderlinedProfileField(
                    label = "Health Information",
                    value = healthInfo,
                    onValueChange = { healthInfo = it },
                    placeholder = "Tap to edit"
                )
            }
        }
    }

    // The DatePickerDialog UI
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Format the milliseconds into a readable string like "August 20, 1987"
                        val formatter = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
                        dob = formatter.format(java.util.Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = PrimaryGreen)
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

@Composable
fun SwitchProfileScreen(
    viewModel: com.example.pillmate.presentation.viewmodel.ProfileViewModel,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    onAddProfileClick: () -> Unit
) {
    // Observe profiles directly from the local Room database
    val profiles by viewModel.localProfiles.collectAsState()
    val currentProfile by viewModel.currentLocalProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Switch Profile",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (profiles.isEmpty()) {
            CircularProgressIndicator(color = PrimaryGreen)
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(profiles) { profile ->
                    ProfileSelectCard(
                        name = profile.name,
                        role = profile.role,
                        isSelected = profile.id == currentProfile?.id, // Compare with the current active DB profile
                        onClick = {
                            viewModel.switchActiveProfile(profile.id)
                        }
                    )
                }
            }
        }

        Button(
            onClick = onAddProfileClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .height(60.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Text("ADD PROFILE", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// --- Helper Components ---

@Composable
fun SettingsButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .height(60.dp),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun UnderlinedProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.White,
                unfocusedIndicatorColor = Color.White,
                cursorColor = Color.White
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
        )
    }
}

@Composable
fun ProfileSelectCard(
    name: String,
    role: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryGreen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(BorderStroke(2.dp, Color.White), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = role, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }

            // Checkmark circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = PrimaryGreen)
                }
            }
        }
    }
}

private fun performSignOut(
    context: Context,
    auth: FirebaseAuth,
    database: com.example.pillmate.data.local.database.AppDatabase,
    activeProfileId: String?,
    onComplete: () -> Unit
) {
    auth.signOut()

    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        // 1. Target and delete ONLY the profile you were actively viewing in the UI
        if (activeProfileId != null) {
            database.profileDao().deleteProfileById(activeProfileId)
        }

        // 2. Reset the active flag so the NEXT person who logs in becomes the primary automatically
        database.profileDao().clearCurrentProfile()

        // Switch back to the Main thread to finish the UI navigation
        launch(kotlinx.coroutines.Dispatchers.Main) {
            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
            googleClient.signOut().addOnCompleteListener {
                onComplete()
            }
        }
    }
}

@Composable
fun ClickableUnderlinedProfileField(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        // Use a disabled TextField to keep the exact same styling as other fields
        TextField(
            value = value,
            onValueChange = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                disabledContainerColor = Color.Transparent,
                disabledTextColor = Color.White,
                disabledIndicatorColor = Color.White
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
        )
    }
}