package com.example.pillmate.presentation.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pillmate.authentication.SignUpOptionsActivity
import com.example.pillmate.R
import com.example.pillmate.MainActivity
import com.google.firebase.auth.FirebaseAuth
import org.koin.android.ext.android.inject

class LauncherActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Just show background while routing
        setContentView(R.layout.activity_launcher)

        val destination = if (auth.currentUser != null) {
            MainActivity::class.java
        } else {
            SignUpOptionsActivity::class.java
        }

        val intent = Intent(this, destination)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}