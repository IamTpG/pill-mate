package com.example.pillmate.presentation.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pillmate.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    onSignOutComplete: () -> Unit
) {
    val auth: FirebaseAuth = koinInject()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
    ) {
        Button(
            onClick = {
                performSignOut(context, auth, onSignOutComplete)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Out")
        }
    }
}

private fun performSignOut(context: Context, auth: FirebaseAuth, onComplete: () -> Unit) {
    auth.signOut()

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()

    val googleClient = GoogleSignIn.getClient(context, gso)

    googleClient.signOut().addOnCompleteListener {
        onComplete()
    }
}
