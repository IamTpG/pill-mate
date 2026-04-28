package com.example.pillmate.presentation.ui.navigation

import androidx.annotation.DrawableRes
import com.example.pillmate.R

sealed class Screen(val route: String, val title: String, @DrawableRes val icon: Int) {
    object Home : Screen("home", "Home", R.drawable.ic_home)
    object Cabinet : Screen("cabinet", "Cabinet", R.drawable.pill)
    object Reminders : Screen("reminders", "Reminders", R.drawable.ic_reminder)
    object Settings : Screen("settings", "Settings", R.drawable.ic_settings)
    
    object Appointment : Screen("appointment_detail/{apptId}", "Appointment", R.drawable.ic_calendar) {
        fun createRoute(apptId: String) = "appointment_detail/$apptId"
    }
    // Auth screens
    object AuthOptions : Screen("auth_options", "Auth", 0)
    object SignIn : Screen("sign_in", "Sign In", 0)
    object SignUp : Screen("sign_up", "Sign Up", 0)
    
    // Debug screen
    object DebugMenu : Screen("debug_menu", "Debug Menu", 0)
    
    object Map: Screen("map", "Map", 0)
    // Schedule Builder screen
    object ScheduleBuilder : Screen("schedule_builder", "Schedule", android.R.drawable.ic_menu_today)

    // Task Alarm Screen
    object TaskAlarm : Screen(
        "task_alarm?sourceId={sourceId}&scheduleId={scheduleId}&title={title}&details={details}&type={type}&instructions={instructions}&time={time}&rrule={rrule}&dose={dose}",
        "Task Alarm",
        0
    ) {
        fun createRoute(
            sourceId: String, scheduleId: String, title: String, details: String,
            type: String, instructions: String, time: String, rrule: String, dose: Float
        ): String {
            val encTitle = android.net.Uri.encode(title.ifBlank { " " })
            val encDetails = android.net.Uri.encode(details.ifBlank { " " })
            val encType = android.net.Uri.encode(type.ifBlank { "OTHER" })
            val encInstr = android.net.Uri.encode(instructions.ifBlank { " " })
            val encTime = android.net.Uri.encode(time.ifBlank { " " })
            val encRrule = android.net.Uri.encode(rrule.ifBlank { " " })
            return "task_alarm?sourceId=$sourceId&scheduleId=$scheduleId&title=$encTitle&details=$encDetails&type=$encType&instructions=$encInstr&time=$encTime&rrule=$encRrule&dose=$dose"
        }
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Cabinet,
    Screen.ScheduleBuilder,
    Screen.Reminders,
    Screen.Appointment,
    Screen.Settings
)
