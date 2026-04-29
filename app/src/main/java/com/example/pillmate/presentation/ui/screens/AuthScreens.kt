package com.example.pillmate.presentation.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.example.pillmate.data.local.entity.SavedAccountEntity
import com.example.pillmate.presentation.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

// Màu chủ đạo
val PrimaryGreen = Color(0xFF1c5f55)

@Composable
fun SignUpOptionsScreen(
    viewModel: AuthViewModel,
    onNavigateToSignIn: (String, String) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Quan sát danh sách tài khoản đã lưu
    val savedAccounts by viewModel.savedAccounts.collectAsState(initial = emptyList())

    // Lấy Client ID từ resources
    val webClientId = stringResource(id = R.string.default_web_client_id)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            viewModel.signInWithGoogle(credential)
        } catch (e: ApiException) {
            Toast.makeText(context, "Google: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Hàm gọi Google Sign In với tài khoản được chỉ định (emailHint)
    fun launchGoogleSignIn(emailHint: String? = null) {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()

        if (emailHint != null) {
            gsoBuilder.setAccountName(emailHint)
        }

        val googleSignInClient = GoogleSignIn.getClient(context, gsoBuilder.build())
        launcher.launch(googleSignInClient.signInIntent)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onAuthSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.1f))

            Image(
                painter = painterResource(id = R.drawable.pill),
                contentDescription = "pill icon",
                modifier = Modifier.size(100.dp)
            )

            Text(
                text = "Pillmate",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
            )

            if (savedAccounts.isNotEmpty()) {
                // GIAO DIỆN KHI ĐÃ CÓ TÀI KHOẢN LƯU
                Text(
                    text = stringResource(id = R.string.recent_login),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, end = 32.dp, bottom = 16.dp),
                    textAlign = TextAlign.Start
                )

                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(savedAccounts) { account ->
                        SavedAccountItem(account) {
                            if (account.loginMethod == "GOOGLE") {
                                launchGoogleSignIn(account.email)
                            } else {
                                onNavigateToSignIn(account.email, account.password ?: "")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = { launchGoogleSignIn() },
                    modifier = Modifier.width(350.dp).height(60.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.5.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = stringResource(id = R.string.continue_with_google), fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = { onNavigateToSignIn("", "") }) {
                    Text(stringResource(id = R.string.use_another_account), color = Color.White, fontSize = 16.sp)
                }
            } else {
                // GIAO DIỆN KHI CHƯA CÓ TÀI KHOẢN LƯU
                Button(
                    onClick = { onNavigateToSignIn("", "") },
                    modifier = Modifier.width(350.dp).height(60.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text(text = stringResource(id = R.string.sign_in_with_email), fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { launchGoogleSignIn() },
                    modifier = Modifier.width(350.dp).height(60.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.5.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = stringResource(id = R.string.continue_with_google), fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Text "Chưa có tài khoản? Đăng ký"
            AuthFooterText(
                mainText = stringResource(id = R.string.dont_have_account),
                actionText = stringResource(id = R.string.sign_up),
                onActionClick = onNavigateToSignUp
            )
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryGreen)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    viewModel: AuthViewModel,
    initialEmail: String = "",
    initialPassword: String = "",
    onAuthSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf(initialPassword) }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onAuthSuccess()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.pill),
                contentDescription = null,
                modifier = Modifier.padding(top = 60.dp).size(100.dp)
            )

            Text(
                text = stringResource(id = R.string.sign_in_with_email),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Form nhập liệu
            Column(modifier = Modifier.fillMaxWidth()) {
                AuthTextField(
                    label = stringResource(id = R.string.email),
                    value = email,
                    onValueChange = { email = it },
                    placeholder = stringResource(id = R.string.email_placeholder),
                    keyboardType = KeyboardType.Email
                )

                Spacer(modifier = Modifier.height(16.dp))

                AuthTextField(
                    label = stringResource(id = R.string.password),
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "************",
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { viewModel.signInWithEmail(email, password) },
                modifier = Modifier.width(200.dp).height(60.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text(stringResource(id = R.string.sign_in), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(id = R.string.cancel), color = Color.White)
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryGreen)
        }
    }
}

// --- Các Thành Phần Hỗ Trợ (Helper Components) ---

@Composable
fun SavedAccountItem(account: SavedAccountEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = account.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = account.email, color = Color.LightGray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false
) {
    Column {
        Text(text = label, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = PrimaryGreen
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun AuthFooterText(mainText: String, actionText: String, onActionClick: () -> Unit) {
    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(color = Color.White, fontSize = 16.sp)) {
            append(mainText)
        }
        append(" ")
        pushStringAnnotation(tag = "action", annotation = "action")
        withStyle(style = SpanStyle(color = Color.White, fontSize = 16.sp, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold)) {
            append(actionText)
        }
        pop()
    }

    androidx.compose.foundation.text.ClickableText(
        text = annotatedString,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "action", start = offset, end = offset)
                .firstOrNull()?.let { onActionClick() }
        },
        modifier = Modifier.padding(bottom = 32.dp),
        style = TextStyle(textAlign = TextAlign.Center)
    )
}

// Giữ lại hàm SignUpScreen cũ của bạn nhưng đã tối ưu padding
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var fullname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onAuthSuccess()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(painter = painterResource(id = R.drawable.pill), contentDescription = null, modifier = Modifier.padding(top = 40.dp).size(80.dp))
            Text(text = stringResource(id = R.string.sign_up_with_email), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))

            Spacer(modifier = Modifier.height(30.dp))

            AuthTextField(stringResource(id = R.string.full_name), fullname, { fullname = it }, stringResource(id = R.string.fullname_placeholder), KeyboardType.Text)
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(stringResource(id = R.string.email), email, { email = it }, stringResource(id = R.string.email_placeholder), KeyboardType.Email)
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(stringResource(id = R.string.password), password, { password = it }, "************", KeyboardType.Password, isPassword = true)

            Spacer(modifier = Modifier.height(30.dp))

            Button(onClick = { viewModel.signUpWithEmail(fullname, email, password) }, modifier = Modifier.width(200.dp).height(60.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)) {
                Text(stringResource(id = R.string.sign_up), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(id = R.string.cancel), color = Color.White)
            }
        }
    }
}