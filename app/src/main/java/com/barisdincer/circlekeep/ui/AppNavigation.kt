package com.barisdincer.circlekeep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactNavItem(
                selected = currentDestination == "dashboard",
                label = "Bugün",
                icon = Icons.Default.Dashboard,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigateTopLevel("dashboard") }
            )
            CompactNavItem(
                selected = currentDestination == "people",
                label = "Kişiler",
                icon = Icons.Default.Group,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigateTopLevel("people") }
            )
            CompactNavItem(
                selected = currentDestination == "waves",
                label = "Gruplar",
                icon = Icons.Default.Group,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigateTopLevel("waves") }
            )
            CompactNavItem(
                selected = currentDestination == "reports",
                label = "Özet",
                icon = Icons.Default.DateRange,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigateTopLevel("reports") }
            )
        }
    }
}

@Composable
private fun CompactNavItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp), tint = contentColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
