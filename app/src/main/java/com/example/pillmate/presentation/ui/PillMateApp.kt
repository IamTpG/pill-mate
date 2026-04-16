package com.example.pillmate.presentation.ui

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pillmate.presentation.ui.navigation.Screen
import com.example.pillmate.presentation.ui.navigation.bottomNavItems
import com.example.pillmate.presentation.ui.screens.*
import com.example.pillmate.presentation.viewmodel.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun PillMateApp(
    onSignOutComplete: () -> Unit
) {
    val navController = rememberNavController()
    val auth: com.google.firebase.auth.FirebaseAuth = org.koin.compose.koinInject()
    val startDestination = if (auth.currentUser != null) "main_graph" else "auth_graph"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth Graph
        androidx.navigation.compose.navigation(
            route = "auth_graph",
            startDestination = Screen.AuthOptions.route
        ) {
            composable(Screen.AuthOptions.route) {
                val viewModel: AuthViewModel = koinViewModel()
                SignUpOptionsScreen(
                    viewModel = viewModel,
                    onNavigateToSignIn = { navController.navigate(Screen.SignIn.route) },
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    onAuthSuccess = {
                        navController.navigate("main_graph") {
                            popUpTo("auth_graph") { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.SignIn.route) {
                val viewModel: AuthViewModel = koinViewModel()
                SignInScreen(
                    viewModel = viewModel,
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
        androidx.navigation.compose.navigation(
            route = "main_graph",
            startDestination = Screen.Home.route
        ) {
            composable(Screen.Home.route) {
                MainScaffold(navController, onSignOutComplete) {
                    val viewModel: HomeViewModel = koinViewModel()
                    HomeScreen(viewModel = viewModel)
                }
            }
            composable(Screen.Cabinet.route) {
                MainScaffold(navController, onSignOutComplete) {
                    val cabinetViewModel: CabinetViewModel = koinViewModel()
                    val drugLibraryViewModel: DrugLibraryViewModel = koinViewModel()
                    CabinetScreen(viewModel = cabinetViewModel, libraryViewModel = drugLibraryViewModel)
                }
            }
            composable(Screen.Reminders.route) {
                MainScaffold(navController, onSignOutComplete) {
                    val viewModel: ReminderViewModel = koinViewModel()
                    ReminderScreen(viewModel = viewModel)
                }
            }
            composable(Screen.Settings.route) {
                MainScaffold(navController, onSignOutComplete) {
                    SettingsScreen(onSignOutComplete = {
                        onSignOutComplete()
                        navController.navigate("auth_graph") {
                            popUpTo("main_graph") { inclusive = true }
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun MainScaffold(
    navController: androidx.navigation.NavHostController,
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
