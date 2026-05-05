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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.pillmate.R
import com.google.firebase.auth.FirebaseAuth
import com.example.pillmate.notification.HealthReminderManager
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import com.example.pillmate.presentation.viewmodel.ProfileViewModel
import com.example.pillmate.utils.generateQRCodeBitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

// Define internal navigation states
enum class SettingsRoute {
    OPTIONS, EDIT_PROFILE, CAREGIVER_HUB
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
        profileViewModel.syncCurrentProfile()
    }

    // Observe the CURRENT LOCAL PROFILE from Room
    val currentLocalProfile by profileViewModel.currentLocalProfile.collectAsState()

    // Use the localized default user string
    val defaultUserText = stringResource(id = R.string.user_default)
    val displayName = currentLocalProfile?.name ?: currentUser?.displayName ?: defaultUserText
    val isCaregiver = currentLocalProfile?.role == "Caregiver_View"

    var currentRoute by remember { mutableStateOf(SettingsRoute.OPTIONS) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Shared Background
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = stringResource(id = R.string.background_desc),
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
                    isCaregiver = isCaregiver,
                    onEditClick = { currentRoute = SettingsRoute.EDIT_PROFILE },
                    onLogoutClick = { performSignOut(context, auth, database, onSignOutComplete) },
                    onCaregiverHubClick = { currentRoute = SettingsRoute.CAREGIVER_HUB }
                )
            }
            SettingsRoute.EDIT_PROFILE -> {
                EditProfileScreen(
                    viewModel = profileViewModel,
                    paddingValues = paddingValues,
                    onBack = {
                        profileViewModel.syncCurrentProfile()
                        currentRoute = SettingsRoute.OPTIONS
                    }
                )
            }
            SettingsRoute.CAREGIVER_HUB -> {
                CaregiverHubScreen (
                    viewModel = profileViewModel,
                    paddingValues = paddingValues,
                    onBack = { currentRoute = SettingsRoute.OPTIONS }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileOptionsScreen(
    paddingValues: PaddingValues,
    userName: String,
    isCaregiver: Boolean,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onCaregiverHubClick: () -> Unit
) {
    var languageMenuExpanded by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val currentLanguageCode = configuration.locales[0].language

    var showLanguageSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val currentLanguageDisplay = if (currentLanguageCode == "vi") {
        stringResource(id = R.string.vietnamese)
    } else {
        stringResource(id = R.string.english)
    }

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
                text = stringResource(id = R.string.profile),
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
                contentDescription = stringResource(id = R.string.avatar_desc),
                tint = Color.White,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Username remains dynamic variable
        Text(
            text = userName,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Buttons
        if (!isCaregiver) {
            SettingsButton(
                text = stringResource(id = R.string.edit_profile),
                icon = Icons.Default.Edit,
                onClick = onEditClick
            )
        }
        SettingsButton(
            text = stringResource(id = R.string.caregiver_hub),
            icon = Icons.Default.Face,
            onClick = onCaregiverHubClick
        )
        
        var showHealthRemindersSheet by remember { mutableStateOf(false) }
        val healthReminderManager: HealthReminderManager = koinInject()

        SettingsButton(
            text = "Health Notifications",
            icon = Icons.Default.Notifications,
            onClick = { showHealthRemindersSheet = true }
        )

        if (showHealthRemindersSheet) {
            HealthRemindersBottomSheet(
                manager = healthReminderManager,
                onDismiss = { showHealthRemindersSheet = false }
            )
        }

        // Language Button with Dropdown Menu
        Box(modifier = Modifier.fillMaxWidth()) {
            SettingsButton(
                text = "${stringResource(id = R.string.language)}: $currentLanguageDisplay",
                icon = Icons.Default.Settings,
                onClick = { showLanguageSheet = true }
            )

            if (showLanguageSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showLanguageSheet = false },
                    sheetState = sheetState,
                    containerColor = Color(0xFF1B1B1B),
                    dragHandle = {
                        BottomSheetDefaults.DragHandle(
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 40.dp, start = 24.dp, end = 24.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.language),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        LanguageItem(
                            label = stringResource(id = R.string.english),
                            isSelected = currentLanguageCode == "en",
                            onClick = {
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                                showLanguageSheet = false
                            }
                        )

                        LanguageItem(
                            label = stringResource(id = R.string.vietnamese),
                            isSelected = currentLanguageCode == "vi",
                            onClick = {
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("vi"))
                                showLanguageSheet = false
                            }
                        )
                    }
                }
            }
        }

        if (!isCaregiver) {
            SettingsButton(
                text = stringResource(id = R.string.log_out),
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = onLogoutClick
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (!isCaregiver) {
            Button(
                onClick = { /* Emergency Action */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .height(60.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.emergency),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
    // Theo dõi trực tiếp từ Room DB để lấy dữ liệu realtime
    val currentProfile by viewModel.currentLocalProfile.collectAsState()

    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var healthInfo by remember { mutableStateOf("") }

    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val configuration = LocalConfiguration.current

    LaunchedEffect(currentProfile) {
        currentProfile?.let {
            name = it.name
            healthInfo = it.healthInformation
            selectedDateMillis = it.dateOfBirth

            if (it.dateOfBirth != null) {
                val currentLang = configuration.locales[0].language
                val pattern = if (currentLang == "vi") "dd/MM/yyyy" else "MMM dd, yyyy"
                val formatter = java.text.SimpleDateFormat(pattern, java.util.Locale(currentLang))
                dob = formatter.format(java.util.Date(it.dateOfBirth))
            } else {
                dob = ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(id = R.string.edit_profile), color = Color.White, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_desc), tint = Color.White)
                }
            },
            actions = {
                Button(
                    onClick = {
                        viewModel.saveProfile(name, selectedDateMillis, healthInfo) {
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(stringResource(id = R.string.save), color = Color.White, fontWeight = FontWeight.Bold)
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
                Icon(Icons.Outlined.Person, contentDescription = stringResource(id = R.string.avatar_desc), tint = Color.White, modifier = Modifier.fillMaxSize())
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-12).dp, y = (-12).dp).size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(id = R.string.edit_avatar), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
                UnderlinedProfileField(
                    label = stringResource(id = R.string.name_label),
                    value = name,
                    onValueChange = { name = it }
                )

                // Clickable Date of Birth Field
                ClickableUnderlinedProfileField(
                    label = stringResource(id = R.string.dob_label),
                    value = dob.ifBlank { stringResource(id = R.string.select_date) },
                    onClick = { showDatePicker = true }
                )

                UnderlinedProfileField(
                    label = stringResource(id = R.string.health_info_label),
                    value = healthInfo,
                    onValueChange = { healthInfo = it },
                    placeholder = stringResource(id = R.string.tap_to_edit)
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
                        selectedDateMillis = millis

                        val currentLang = configuration.locales[0].language
                        val pattern = if (currentLang == "vi") "dd/MM/yyyy" else "MMM dd, yyyy"
                        val formatter = java.text.SimpleDateFormat(pattern, java.util.Locale(currentLang))
                        dob = formatter.format(java.util.Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(id = R.string.ok), color = PrimaryGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(id = R.string.cancel), color = Color.Gray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
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
                    Icon(Icons.Default.Check, contentDescription = stringResource(id = R.string.selected_desc), tint = PrimaryGreen)
                }
            }
        }
    }
}

private fun performSignOut(
    context: Context,
    auth: FirebaseAuth,
    database: com.example.pillmate.data.local.database.AppDatabase,
    onComplete: () -> Unit
) {
    // 1. Đăng xuất khỏi Firebase (Phiên làm việc hiện tại)
    auth.signOut()

    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        // 🟢 1. Xóa toàn bộ bảng profiles (Bao gồm cả Primary hiện tại và Caregiver_View)
        // Thông tin đăng nhập đã được lưu ở bảng 'saved_accounts' từ trước nên không bị mất
        database.profileDao().clearAllProfiles()

        launch(Dispatchers.Main) {
            // 2. Đăng xuất Google Client
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
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

@Composable
fun LanguageItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PrimaryGreen.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White, fontSize = 16.sp)
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryGreen)
        }
    }
}

@Composable
fun CaregiverHubScreen(
    viewModel: ProfileViewModel,
    paddingValues: PaddingValues,
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(stringResource(R.string.following), stringResource(R.string.grant_access))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(stringResource(id = R.string.care_management), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        // --- TAB BAR ---
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = PrimaryGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Color.White
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTabIndex == index) Color.White else Color.Gray,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // --- CONTENT ---
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> FollowedTabContent(viewModel) // Nội dung danh sách người theo dõi
                1 -> GrantAccessTabContent(viewModel) // Nội dung tạo mã QR
            }
        }
    }
}

@Composable
fun FollowedTabContent(viewModel: ProfileViewModel) {
    val followedProfiles by viewModel.followedProfiles.collectAsState()
    val currentProfile by viewModel.currentLocalProfile.collectAsState()

    val localProfiles by viewModel.localProfiles.collectAsState()
    val primaryProfile = localProfiles.firstOrNull()

    var showLinkDialog by remember { mutableStateOf(false) }
    var shareCodeInput by remember { mutableStateOf("") }
    var linkMessage by remember { mutableStateOf("") }
    val primaryGreen = Color(0xFF1c5f55)

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val scannedCode = result.contents.replace("pillmate://share/", "")
            if (scannedCode.length == 6) {
                shareCodeInput = scannedCode
                viewModel.linkCaregiverProfile(scannedCode) { success, msg ->
                    linkMessage = msg
                    if (success) shareCodeInput = ""
                }
            } else {
                linkMessage = "Mã QR không hợp lệ!"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        if (primaryProfile != null && currentProfile?.id != primaryProfile.id) {
            Button(
                onClick = { viewModel.switchActiveProfile(primaryProfile.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)) // Màu đỏ hoặc cam để nổi bật
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.back_to_your_profile), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (followedProfiles.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(id = R.string.no_one_followed), color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(followedProfiles) { profile ->
                    ProfileSelectCard(
                        name = profile.name,
                        role = stringResource(id = R.string.following),
                        isSelected = profile.id == currentProfile?.id,
                        onClick = { viewModel.switchActiveProfile(profile.id) }
                    )
                }
            }
        }

        Button(
            onClick = { showLinkDialog = true },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Text(stringResource(id = R.string.follow_new_person))
        }
    }

    // --- DIALOG NHẬP MÃ / QUÉT MÃ ---
    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = {
                showLinkDialog = false
                linkMessage = ""
            },
            title = { Text(stringResource(id = R.string.follow_patient_title)) },
            text = {
                Column {
                    Text(stringResource(id = R.string.follow_patient_desc))
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = shareCodeInput,
                        onValueChange = { shareCodeInput = it.uppercase().take(6) },
                        placeholder = { Text("XXXXXX") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Nút quét QR
                    Button(
                        onClick = {
                            val options = ScanOptions()
                            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            options.setPrompt("Hướng camera về phía mã QR của người bệnh")
                            options.setCameraId(0)
                            options.setBeepEnabled(true)
                            options.setOrientationLocked(false)
                            scanLauncher.launch(options)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Quét mã QR", color = Color.White)
                    }

                    if (linkMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(linkMessage, color = if (linkMessage.contains("thành công")) PrimaryGreen else Color.Red)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (shareCodeInput.length == 6) {
                        viewModel.linkCaregiverProfile(shareCodeInput) { success, msg ->
                            linkMessage = msg
                            if (success) { shareCodeInput = "" }
                        }
                    } else {
                        linkMessage = "Mã phải có đúng 6 ký tự."
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)) {
                    Text("Liên kết")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLinkDialog = false
                    linkMessage = ""
                }) { Text("Đóng", color = Color.Gray) }
            }
        )
    }
}

@Composable
fun GrantAccessTabContent(viewModel: ProfileViewModel) {
    val shareCode by viewModel.shareCode.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.generateSecureShareCode()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(id = R.string.grant_access_desc),
            color = Color.LightGray, textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (shareCode != null) {
            val qrBitmap = remember(shareCode) { generateQRCodeBitmap("pillmate://share/$shareCode") }
            qrBitmap?.let {
                Image(bitmap = it, contentDescription = "QR", modifier = Modifier.size(200.dp).clip(RoundedCornerShape(16.dp)))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(id = R.string.access_code_label), color = Color.Gray, fontSize = 14.sp)
            Text(shareCode!!, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGreen, letterSpacing = 8.sp)
        } else {
            CircularProgressIndicator(color = PrimaryGreen)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthRemindersBottomSheet(
    manager: HealthReminderManager,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1B1B1B),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 24.dp, end = 24.dp)
        ) {
            Text(
                text = "Health Notifications",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Settings are local to this device.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            HealthReminderItem(label = "Hydration", type = "HYDRATION", manager = manager)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))
            HealthReminderItem(label = "Blood Pressure", type = "BLOOD_PRESSURE", manager = manager)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))
            HealthReminderItem(label = "Body Weight", type = "WEIGHT", manager = manager)
        }
    }
}

@Composable
fun HealthReminderItem(
    label: String,
    type: String,
    manager: HealthReminderManager
) {
    var enabled by remember { mutableStateOf(manager.isEnabled(type)) }
    var interval by remember { mutableIntStateOf(manager.getInterval(type)) }
    var showIntervalDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Switch(
                checked = enabled,
                onCheckedChange = { 
                    enabled = it
                    manager.updateSetting(type, it, interval)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryGreen,
                    checkedTrackColor = PrimaryGreen.copy(alpha = 0.5f)
                )
            )
        }
        
        if (enabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showIntervalDialog = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val intervalText = when (interval) {
                    60 -> "Hourly"
                    240 -> "Every 4 hours"
                    480 -> "Every 8 hours"
                    720 -> "Twice daily"
                    1440 -> "Daily"
                    else -> "Every $interval minutes"
                }
                Text(text = "Frequency: ", color = Color.Gray, fontSize = 14.sp)
                Text(text = intervalText, color = PrimaryGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showIntervalDialog = false },
            containerColor = Color(0xFF2C2C2C),
            title = { Text("Select Frequency", color = Color.White) },
            text = {
                val options = listOf(
                    60 to "Hourly",
                    240 to "Every 4 hours",
                    480 to "Every 8 hours",
                    720 to "Twice daily",
                    1440 to "Daily"
                )
                Column {
                    options.forEach { (mins, text) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    interval = mins
                                    manager.updateSetting(type, enabled, mins)
                                    showIntervalDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = interval == mins,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = text, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIntervalDialog = false }) {
                    Text("OK", color = PrimaryGreen)
                }
            }
        )
    }
}