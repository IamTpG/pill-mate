package com.example.pillmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.pillmate.presentation.ui.PillMateApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            PillMateApp(
                onSignOutComplete = {
                    finish()
                }
            )
        }
    }
}