package com.example.pillmate.presentation.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.pillmate.presentation.ui.navigation.Screen
import com.example.pillmate.presentation.ui.navigation.bottomNavItems
import com.example.pillmate.presentation.ui.screens.*
import com.example.pillmate.presentation.viewmodel.*
import org.koin.androidx.compose.koinViewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import org.koin.compose.koinInject
import androidx.navigation.navArgument

@Composable
fun PillMateApp(
    onSignOutComplete: () -> Unit
) {
    val navController = rememberNavController()
    val auth: FirebaseAuth = koinInject()
    val startDestination = if (auth.currentUser != null) "main_graph" else "auth_graph"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth Graph
        navigation(
            route = "auth_graph",
            startDestination = Screen.AuthOptions.route
        ) {
            composable(Screen.AuthOptions.route) {
                val viewModel: AuthViewModel = koinViewModel()
                SignUpOptionsScreen(
                    viewModel = viewModel,
                    onNavigateToSignIn = { email, password ->
                        navController.navigate("signin?email=$email&password=$password")
                    },
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    onAuthSuccess = {
                        navController.navigate("main_graph") {
                            popUpTo("auth_graph") { inclusive = true }
                        }
                    }
                )
            }
            composable(
                // Định nghĩa route chấp nhận tham số tùy chọn
                route = "signin?email={email}&password={password}",
                arguments = listOf(
                    navArgument("email") { defaultValue = "" },
                    navArgument("password") { defaultValue = "" }
                )
            ) { backStackEntry ->
                // Lấy dữ liệu từ URL truyền sang
                val email = backStackEntry.arguments?.getString("email") ?: ""
                val password = backStackEntry.arguments?.getString("password") ?: ""

                val viewModel: AuthViewModel = koinViewModel()

                SignInScreen(
                    viewModel = viewModel,
                    initialEmail = email,
                    initialPassword = password,
                    onAuthSuccess = {
                        navController.navigate("main_graph") {
                            popUpTo("auth_graph") { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SignUp.route) {
                val viewModel: AuthViewModel = koinViewModel()
                SignUpScreen(
                    viewModel = viewModel,
                    onAuthSuccess = {
                        navController.navigate("main_graph") {
                            popUpTo("auth_graph") { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Main App Graph
        navigation(
            route = "main_graph",
            startDestination = Screen.Home.route
        ) {
            composable(Screen.Home.route) {
                MainScaffold(navController, onSignOutComplete) { innerPadding ->
                    val viewModel: HomeViewModel = koinViewModel()
                    HomeScreen(
                        viewModel = viewModel,
                        paddingValues = innerPadding,
                        onTaskClick = { task ->
                            navController.navigate(
                                Screen.TaskAlarm.createRoute(
                                    sourceId = task.sourceId,
                                    scheduleId = task.scheduleId,
                                    title = task.title,
                                    details = task.doseDescription,
                                    type = task.taskType.name,
                                    instructions = "",
                                    time = task.time,
                                    rrule = "",
                                    dose = task.dose
                                )
                            )
                        },
                        onAddClick = { /* TODO */ },
                        onDebugClick = { navController.navigate(Screen.DebugMenu.route) }
                    )
                }
            }
            composable(Screen.Cabinet.route) {
                MainScaffold(navController, onSignOutComplete) { innerPadding ->
                    val cabinetViewModel: CabinetViewModel = koinViewModel()
                    val drugLibraryViewModel: DrugLibraryViewModel = koinViewModel()
                    CabinetScreen(
                        viewModel = cabinetViewModel,
                        libraryViewModel = drugLibraryViewModel,
                        paddingValues = innerPadding
                    )
                }
            }
            composable(Screen.Reminders.route) {
                MainScaffold(navController, onSignOutComplete) { innerPadding ->
                    val viewModel: ReminderViewModel = koinViewModel()
                    ReminderScreen(
                        viewModel = viewModel, 
                        paddingValues = innerPadding
                    )
                }
            }
            composable(Screen.Settings.route) {
                MainScaffold(navController, onSignOutComplete) { innerPadding ->
                    SettingsScreen(paddingValues = innerPadding, onSignOutComplete = {
                        onSignOutComplete()
                        navController.navigate("auth_graph") {
                            popUpTo("main_graph") { inclusive = true }
                        }
                    }, onNavigateToAuth = {
                        navController.navigate("auth_graph")
                    })
                }
            }
            composable(Screen.AIChat.route) {
                MainScaffold(navController, onSignOutComplete) { innerPadding ->
                    AIChatScreen(paddingValues = innerPadding)
                }
            }
            composable(Screen.DebugMenu.route) {
                val viewModel: DebugViewModel = koinViewModel()
                DebugMenuScreen(
                    navController = navController,
                    viewModel = viewModel
                )
            }
            
           composable(route = Screen.Appointment.route) {
                // Dynamically get the current user ID for the profileId
                val currentUserId = auth.currentUser?.uid ?: ""
                
                MainScaffold(navController, onSignOutComplete) { innerPadding ->
                    val appointmentViewModel: AppointmentViewModel = koinViewModel()
                    
                    AppointmentScreen(
                        viewModel = appointmentViewModel,
                        profileId = currentUserId,
                        paddingValues = innerPadding // Pass the scaffold padding here
                    )
                }
            }
            composable(Screen.ScheduleBuilder.route) {
                MainScaffold(navController, onSignOutComplete) { innerPadding ->
                    ScheduleBuilderFlowScreen(
                        paddingValues = innerPadding,
                        onCompleteMapping = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(
                route = Screen.TaskAlarm.route,
                arguments = listOf(
                    androidx.navigation.navArgument("sourceId") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = "" },
                    androidx.navigation.navArgument("scheduleId") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = "" },
                    androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = "" },
                    androidx.navigation.navArgument("details") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = "" },
                    androidx.navigation.navArgument("type") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = "" },
                    androidx.navigation.navArgument("instructions") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = "" },
                    androidx.navigation.navArgument("time") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = "" },
                    androidx.navigation.navArgument("rrule") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = "" },
                    androidx.navigation.navArgument("dose") { type = androidx.navigation.NavType.FloatType; defaultValue = 1.0f }
                ),
                deepLinks = listOf(
                    androidx.navigation.navDeepLink { uriPattern = "pillmate://alarm?sourceId={sourceId}&scheduleId={scheduleId}&title={title}&details={details}&type={type}&instructions={instructions}&time={time}&rrule={rrule}&dose={dose}" }
                )
            ) { backStackEntry ->
                val viewModel: TaskLogViewModel = koinViewModel()
                TaskAlarmScreen(
                    viewModel = viewModel,
                    sourceId = backStackEntry.arguments?.getString("sourceId") ?: "",
                    scheduleId = backStackEntry.arguments?.getString("scheduleId") ?: "",
                    title = backStackEntry.arguments?.getString("title") ?: "",
                    details = backStackEntry.arguments?.getString("details") ?: "",
                    taskTypeString = backStackEntry.arguments?.getString("type") ?: "OTHER",
                    instructions = backStackEntry.arguments?.getString("instructions") ?: "",
                    startTimeStr = backStackEntry.arguments?.getString("time") ?: "",
                    rrule = backStackEntry.arguments?.getString("rrule") ?: "",
                    dose = backStackEntry.arguments?.getFloat("dose") ?: 1.0f,
                    onDismiss = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun MainScaffold(
    navController: NavHostController,
    onSignOutComplete: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = screen.icon),
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1E6C54),
                            selectedTextColor = Color(0xFF1E6C54),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFFE8F5E9)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}
