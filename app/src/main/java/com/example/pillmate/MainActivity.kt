package com.example.pillmate

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.pillmate.presentation.ui.HomeActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.android.ext.android.inject
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.authentication.SignUpOptionsActivity
import com.example.pillmate.presentation.ui.screens.CabinetScreen
import com.example.pillmate.presentation.viewmodel.CabinetViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
class MainActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by inject()

    // Koin injects the ViewModel
    private val cabinetViewModel: CabinetViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                CabinetScreen(viewModel = cabinetViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if the current user exists
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // If null (logged out), immediately transition to the login screen
            val intent = Intent(this, SignUpOptionsActivity::class.java)
            // Clear the activity backstack so they can't hit 'Back' to bypass login
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        // If the user is logged in, we stay on MainActivity to show the CabinetScreen!
    }

    private fun performSignOut() {
        // 1. Sign out of Firebase Auth
        auth.signOut()

        // 2. Sign out of the Google Sign-In system (Clears cache)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            // 3. After Google sign-out is successful, return to the Login screen
            val intent = Intent(this, SignUpOptionsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}