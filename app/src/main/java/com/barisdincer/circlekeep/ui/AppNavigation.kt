package com.barisdincer.circlekeep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.barisdincer.circlekeep.NetworkApplication
import com.barisdincer.circlekeep.data.UserPreferences
import com.barisdincer.circlekeep.preferences.UserPreferencesStore
import com.barisdincer.circlekeep.ui.dashboard.DashboardScreen
import com.barisdincer.circlekeep.ui.people.PeopleScreen
import com.barisdincer.circlekeep.ui.people.PersonDetailScreen
import com.barisdincer.circlekeep.ui.profile.ProfileScreen
import com.barisdincer.circlekeep.ui.reports.ReportsScreen
import com.barisdincer.circlekeep.ui.waves.WavesScreen

@Composable
fun AppNavigation(
    userPreferences: UserPreferences,
    preferencesStore: UserPreferencesStore
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as NetworkApplication
    val viewModel: NetworkViewModel = viewModel(
        factory = NetworkViewModelFactory(application.repository)
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    userPreferences = userPreferences,
                    onProfileClick = { navController.navigate("profile") }
                )
            }
            composable("people") {
                PeopleScreen(
                    viewModel = viewModel,
                    onPersonClick = { personId ->
                        navController.navigate("person_detail/$personId")
                    }
                )
            }
            composable("waves") {
                WavesScreen(viewModel)
            }
            composable("reports") {
                ReportsScreen(viewModel)
            }
            composable("profile") {
                ProfileScreen(
                    preferences = userPreferences,
                    onBack = { navController.popBackStack() },
                    onSaveProfile = preferencesStore::updateProfile,
                    onThemeModeChange = preferencesStore::updateThemeMode
                )
            }
            composable(
                route = "person_detail/{personId}",
                arguments = listOf(navArgument("personId") { type = NavType.IntType })
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

@Composable
private fun BottomNavigationBar(navController: NavHostController) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
            ) {
                NavigationBarItem(
                    selected = currentDestination == "dashboard",
                    label = { Text("Bugün") },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Bugün") },
                    onClick = { navController.navigateTopLevel("dashboard") }
                )
                NavigationBarItem(
                    selected = currentDestination == "people",
                    label = { Text("Kişiler") },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Kişiler") },
                    onClick = { navController.navigateTopLevel("people") }
                )
                NavigationBarItem(
                    selected = currentDestination == "waves",
                    label = { Text("Gruplar") },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Gruplar") },
                    onClick = { navController.navigateTopLevel("waves") }
                )
                NavigationBarItem(
                    selected = currentDestination == "reports",
                    label = { Text("Özet") },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Özet") },
                    onClick = { navController.navigateTopLevel("reports") }
                )
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
