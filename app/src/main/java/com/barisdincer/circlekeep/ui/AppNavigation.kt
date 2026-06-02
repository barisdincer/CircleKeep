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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.barisdincer.circlekeep.ui.logs.LogsScreen
import com.barisdincer.circlekeep.ui.people.AddPersonScreen
import com.barisdincer.circlekeep.ui.people.PeopleScreen
import com.barisdincer.circlekeep.ui.people.PersonDetailScreen
import com.barisdincer.circlekeep.ui.profile.ProfileScreen
import com.barisdincer.circlekeep.ui.reports.ReportsScreen
import com.barisdincer.circlekeep.ui.waves.GroupDetailScreen
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
                    viewModel = viewModel
                )
            }
            composable("people") {
                PeopleScreen(
                    viewModel = viewModel,
                    onPersonClick = { personId ->
                        navController.navigate("person_detail/$personId")
                    },
                    onAddPersonClick = {
                        navController.navigate("add_person")
                    }
                )
            }
            composable(
                route = "add_person?waveId={waveId}",
                arguments = listOf(navArgument("waveId") {
                    type = NavType.IntType
                    defaultValue = -1
                })
            ) { backStackEntry ->
                val waveId = backStackEntry.arguments?.getInt("waveId")?.takeIf { it > 0 }
                AddPersonScreen(
                    waves = viewModel.waves.collectAsState().value,
                    contactTypes = viewModel.activeContactTypes.collectAsState().value,
                    initialWaveId = waveId,
                    onBack = { navController.popBackStack() },
                    onAdd = { name, phone, selectedWaveId, contactLookupKey, initialType, initialTimestamp, initialNote, customFrequencyDays ->
                        viewModel.addPerson(
                            name = name,
                            phoneNumber = phone,
                            waveId = selectedWaveId,
                            contactLookupKey = contactLookupKey,
                            initialInteractionType = initialType,
                            initialInteractionTimestamp = initialTimestamp,
                            initialInteractionNote = initialNote,
                            customFrequencyDays = customFrequencyDays
                        )
                        navController.popBackStack()
                    }
                )
            }
            composable("waves") {
                WavesScreen(
                    viewModel = viewModel,
                    onGroupClick = { waveId ->
                        navController.navigate("group_detail/$waveId")
                    }
                )
            }
            composable("logs") {
                LogsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("reports") {
                ReportsScreen(viewModel)
            }
            composable("profile") {
                ProfileScreen(
                    preferences = userPreferences,
                    backupState = viewModel.backupState.collectAsState().value,
                    onBack = { navController.popBackStack() },
                    onLogsClick = { navController.navigate("logs") },
                    onSaveProfile = preferencesStore::updateProfile,
                    onThemeModeChange = preferencesStore::updateThemeMode,
                    onCreateBackupJson = viewModel::createBackupJson,
                    onRestoreBackupJson = viewModel::restoreBackupJson
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
                    onBack = { navController.popBackStack() },
                    onDeleted = {
                        navController.navigateTopLevel("people")
                    }
                )
            }
            composable(
                route = "group_detail/{waveId}",
                arguments = listOf(navArgument("waveId") { type = NavType.IntType })
            ) { backStackEntry ->
                val waveId = backStackEntry.arguments?.getInt("waveId") ?: return@composable
                GroupDetailScreen(
                    waveId = waveId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onPersonClick = { personId ->
                        navController.navigate("person_detail/$personId")
                    },
                    onAddPersonToGroup = { selectedWaveId ->
                        navController.navigate("add_person?waveId=$selectedWaveId")
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(navController: NavHostController) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route.orEmpty()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
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
                selected = currentDestination == "people" || currentDestination.startsWith("person_detail") || currentDestination.startsWith("add_person"),
                label = "Kişiler",
                icon = Icons.Default.Group,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigateTopLevel("people") }
            )
            CompactNavItem(
                selected = currentDestination == "waves" || currentDestination.startsWith("group_detail"),
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
            CompactNavItem(
                selected = currentDestination == "profile" || currentDestination == "logs",
                label = "Profil",
                icon = Icons.Default.Person,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigateTopLevel("profile") }
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
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp), tint = contentColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = false }
        launchSingleTop = true
        restoreState = false
    }
}
