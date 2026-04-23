package com.example.pillmate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import com.example.pillmate.presentation.ui.PillMateApp

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            org.koin.compose.KoinContext {
                PillMateApp(
                    onSignOutComplete = {
                        finish()
                    }
                )
            }
        }
    }
}