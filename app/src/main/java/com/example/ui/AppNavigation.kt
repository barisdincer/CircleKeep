package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.people.PeopleScreen
import com.example.ui.waves.WavesScreen
import com.example.NetworkApplication
import com.example.ui.reports.ReportsScreen
import com.example.ui.people.PersonDetailScreen
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as NetworkApplication
    val viewModel: NetworkViewModel = viewModel(
        factory = NetworkViewModelFactory(application.repository)
    )

    Scaffold(
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route
                
                NavigationBarItem(
                    selected = currentDestination == "dashboard",
                    label = { Text("Dashboard") },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    onClick = { 
                        navController.navigate("dashboard") { 
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                
                NavigationBarItem(
                    selected = currentDestination == "people",
                    label = { Text("Network") },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Network") },
                    onClick = { 
                        navController.navigate("people") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                
                NavigationBarItem(
                    selected = currentDestination == "waves",
                    label = { Text("Waves") },
                    icon = { Icon(Icons.Default.Waves, contentDescription = "Waves") },
                    onClick = { 
                        navController.navigate("waves") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                NavigationBarItem(
                    selected = currentDestination == "reports",
                    label = { Text("Reports") },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Reports") },
                    onClick = { 
                        navController.navigate("reports") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("dashboard") {
                DashboardScreen(viewModel)
            }
            composable("people") {
                PeopleScreen(viewModel, onPersonClick = { personId ->
                    navController.navigate("person_detail/$personId")
                })
            }
            composable("waves") {
                WavesScreen(viewModel)
            }
            composable("reports") {
                ReportsScreen(viewModel)
            }
            composable(
                route = "person_detail/{personId}",
                arguments = listOf(navArgument("personId") { type = androidx.navigation.NavType.IntType })
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getInt("personId") ?: return@composable
                PersonDetailScreen(
                    personId = personId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
