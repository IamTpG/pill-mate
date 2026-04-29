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
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import com.example.pillmate.presentation.viewmodel.ProfileViewModel
import com.example.pillmate.utils.generateQRCodeBitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

// Define internal navigation states
enum class SettingsRoute {
    OPTIONS, EDIT_PROFILE, SWITCH_PROFILE, CAREGIVER_SHARE
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
                    onEditClick = { currentRoute = SettingsRoute.EDIT_PROFILE },
                    onSwitchClick = { currentRoute = SettingsRoute.SWITCH_PROFILE },
                    onLogoutClick = { performSignOut(context, auth, database, currentLocalProfile?.id, onSignOutComplete) },
                            onCaregiverClick = { currentRoute = SettingsRoute.CAREGIVER_SHARE }
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
            SettingsRoute.SWITCH_PROFILE -> {
                SwitchProfileScreen(
                    viewModel = profileViewModel,
                    paddingValues = paddingValues,
                    onBack = { currentRoute = SettingsRoute.OPTIONS },
                    onAddProfileClick = onNavigateToAuth
                )
            }
            SettingsRoute.CAREGIVER_SHARE -> {
                CaregiverShareScreen(
                    viewModel = profileViewModel,
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
    onEditClick: () -> Unit,
    onSwitchClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onCaregiverClick: () -> Unit
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
        SettingsButton(
            text = stringResource(id = R.string.edit_profile),
            icon = Icons.Default.Edit,
            onClick = onEditClick
        )
        SettingsButton(
            text = stringResource(id = R.string.switch_profile),
            icon = Icons.Default.AccountCircle,
            onClick = onSwitchClick
        )
        SettingsButton(
            text = stringResource(id = R.string.manage_caregiver_access),
            icon = Icons.Default.Lock,
            onClick = onCaregiverClick
        )

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

        SettingsButton(
            text = stringResource(id = R.string.log_out),
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

    var showLinkDialog by remember { mutableStateOf(false) }
    var shareCodeInput by remember { mutableStateOf("") }
    var linkMessage by remember { mutableStateOf("") }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            // Khi quét thành công, nó sẽ trả về chuỗi "pillmate://share/A8X2B9"
            // Mình cắt lấy 6 ký tự cuối cùng
            val scannedCode = result.contents.replace("pillmate://share/", "")

            if (scannedCode.length == 6) {
                shareCodeInput = scannedCode // Điền tự động vào ô Text
                // Tự động gọi API luôn cho tiện
                viewModel.linkCaregiverProfile(scannedCode) { success, msg ->
                    linkMessage = msg
                    if (success) shareCodeInput = ""
                }
            } else {
                linkMessage = "Mã QR không hợp lệ!"
            }
        }
    }

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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_desc), tint = Color.White)
            }
            Text(
                text = stringResource(id = R.string.switch_profile),
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
                        isSelected = profile.id == currentProfile?.id,
                        onClick = {
                            viewModel.switchActiveProfile(profile.id)
                        }
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { showLinkDialog = true },
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp).height(60.dp),
            shape = RoundedCornerShape(15.dp),
            border = BorderStroke(2.dp, PrimaryGreen)
        ) {
            Text("THEO DÕI NGƯỜI BỆNH", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
            Text(stringResource(id = R.string.add_profile), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = {
                showLinkDialog = false
                linkMessage = ""
            },
            title = { Text("Theo dõi người bệnh") },
            text = {
                Column {
                    Text("Nhập mã truy cập 6 ký tự để xem tủ thuốc và lịch uống.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = shareCodeInput,
                        onValueChange = { shareCodeInput = it.uppercase().take(6) }, // Ép viết hoa và tối đa 6 ký tự
                        placeholder = { Text("VD: A8X2B9") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // NÚT MỞ CAMERA QUÉT QR
                    Button(
                        onClick = {
                            val options = ScanOptions()
                            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            options.setPrompt("Hướng camera về phía mã QR của người bệnh")
                            options.setCameraId(0) // Dùng Camera sau
                            options.setBeepEnabled(true) // Kêu "Bíp" khi quét xong
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
fun CaregiverShareScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val shareCode by viewModel.shareCode.collectAsState()

    // Tự động tạo mã khi vào màn hình
    LaunchedEffect(Unit) {
        viewModel.generateSecureShareCode()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_desc), tint = Color.White)
            }
            Text(
                text = "Caregiver Access",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Text("Quét mã QR hoặc nhập mã bên dưới bằng điện thoại của người chăm sóc.", color = Color.LightGray, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(32.dp))

        // Hiển thị QR Code
        if (shareCode != null) {
            val qrBitmap = remember(shareCode) { generateQRCodeBitmap("pillmate://share/$shareCode") }

            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = "QR Code",
                    modifier = Modifier.size(250.dp).clip(RoundedCornerShape(16.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hiển thị Code chữ để nhập tay
            Text("MÃ TRUY CẬP", color = Color.Gray, fontSize = 14.sp)
            Text(
                text = shareCode!!,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp,
                color = PrimaryGreen
            )
        } else {
            CircularProgressIndicator(color = PrimaryGreen)
        }
    }
}