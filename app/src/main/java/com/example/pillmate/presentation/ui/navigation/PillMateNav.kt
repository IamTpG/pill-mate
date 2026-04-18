package com.example.pillmate.presentation.ui.navigation

import androidx.annotation.DrawableRes
import com.example.pillmate.R

sealed class Screen(val route: String, val title: String, @DrawableRes val icon: Int) {
    object Home : Screen("home", "Home", R.drawable.ic_home)
    object Cabinet : Screen("cabinet", "Cabinet", R.drawable.pill)
    object Reminders : Screen("reminders", "Reminders", R.drawable.ic_reminder)
    object Settings : Screen("settings", "Settings", R.drawable.ic_settings)
    
    // Auth screens
    object AuthOptions : Screen("auth_options", "Auth", 0)
    object SignIn : Screen("sign_in", "Sign In", 0)
    object SignUp : Screen("sign_up", "Sign Up", 0)
    
    // Debug screen
    object DebugMenu : Screen("debug_menu", "Debug Menu", 0)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Cabinet,
    Screen.Reminders,
    Screen.Settings
)
