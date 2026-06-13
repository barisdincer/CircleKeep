package com.barisdincer.circlekeep.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.UserPreferences
import com.barisdincer.circlekeep.data.Wave
import com.barisdincer.circlekeep.preferences.UserPreferencesStore
import com.barisdincer.circlekeep.ui.dashboard.DashboardScreen
import com.barisdincer.circlekeep.ui.dashboard.EventLogScreen
import com.barisdincer.circlekeep.ui.design.CircleAvatar
import com.barisdincer.circlekeep.ui.design.CircleEmptyState
import com.barisdincer.circlekeep.ui.design.CircleRadius
import com.barisdincer.circlekeep.ui.design.CircleSearchField
import com.barisdincer.circlekeep.ui.design.CircleSpacing
import com.barisdincer.circlekeep.ui.design.circlePressable
import com.barisdincer.circlekeep.ui.logs.LogsScreen
import com.barisdincer.circlekeep.ui.people.AddPersonScreen
import com.barisdincer.circlekeep.ui.people.PeopleScreen
import com.barisdincer.circlekeep.ui.people.PersonDetailScreen
import com.barisdincer.circlekeep.ui.profile.ProfileScreen
import com.barisdincer.circlekeep.ui.reports.ReportsScreen
import com.barisdincer.circlekeep.ui.waves.GroupDetailScreen
import com.barisdincer.circlekeep.ui.waves.WavesScreen

private data class NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val matches: (String) -> Boolean,
)

private val navDestinations = listOf(
    NavDestination("dashboard", "Bugün", Icons.Default.Today) { it == "dashboard" || it == "event_log" },
    NavDestination("people", "Kişiler", Icons.Default.People) { it == "people" || it.startsWith("person_detail") || it.startsWith("add_person") },
    NavDestination("waves", "Gruplar", Icons.Default.Groups) { it == "waves" || it.startsWith("group_detail") },
    NavDestination("reports", "Özet", Icons.Default.Insights) { it == "reports" },
    NavDestination("profile", "Profil", Icons.Default.Person) { it == "profile" || it == "logs" },
)

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
    val peopleForSearch by viewModel.people.collectAsState()
    val wavesForSearch by viewModel.waves.collectAsState()
    var showQuickSearch by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val expandedNavigation = maxWidth >= 720.dp
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            bottomBar = {
                if (!expandedNavigation) {
                    AppBottomBar(
                        navController = navController,
                        searchEnabled = userPreferences.searchButtonEnabled,
                        onQuickSearch = { showQuickSearch = true }
                    )
                }
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (expandedNavigation) {
                    AppNavigationRail(
                        navController = navController,
                        searchEnabled = userPreferences.searchButtonEnabled,
                        onQuickSearch = { showQuickSearch = true }
                    )
                }
                AppNavHost(
                    navController = navController,
                    viewModel = viewModel,
                    userPreferences = userPreferences,
                    preferencesStore = preferencesStore,
                    paddingValues = paddingValues,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (showQuickSearch) {
            QuickSearchDialog(
                people = peopleForSearch,
                waves = wavesForSearch,
                onDismiss = { showQuickSearch = false },
                onPersonClick = { personId ->
                    showQuickSearch = false
                    navController.navigate("person_detail/$personId")
                },
                onWaveClick = { waveId ->
                    showQuickSearch = false
                    navController.navigate("group_detail/$waveId")
                }
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    viewModel: NetworkViewModel,
    userPreferences: UserPreferences,
    preferencesStore: UserPreferencesStore,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "dashboard",
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues),
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 24 } },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { it / 24 } },
    ) {
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onOpenEventLog = { navController.navigate("event_log") }
            )
        }
        composable("event_log") {
            EventLogScreen(
                people = viewModel.people.collectAsState().value,
                waves = viewModel.waves.collectAsState().value,
                contactTypes = viewModel.activeContactTypes.collectAsState().value,
                onBack = { navController.popBackStack() },
                onSave = { personIds, type, note, timestamp ->
                    viewModel.logInteractions(personIds, type, note, timestamp)
                    navController.popBackStack()
                }
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
                onLogsClick = { navController.navigate("logs") },
                onSaveProfile = preferencesStore::updateProfile,
                onThemeModeChange = preferencesStore::updateThemeMode,
                onToggleSearchButton = preferencesStore::updateSearchButtonEnabled,
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

@Composable
private fun AppBottomBar(navController: NavHostController, searchEnabled: Boolean, onQuickSearch: () -> Unit) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route.orEmpty()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = CircleSpacing.sm, vertical = CircleSpacing.xs)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CircleRadius.pill),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AnimatedVisibility(
                    visible = searchEnabled,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    NavSearchButton(onClick = onQuickSearch)
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    navDestinations.forEach { destination ->
                        BottomNavItem(
                            selected = destination.matches(currentDestination),
                            label = destination.label,
                            icon = destination.icon,
                            onClick = { navController.navigateTopLevel(destination.route) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavSearchButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .circlePressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = "Ara",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun BottomNavItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
        label = "navContainer",
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navContent",
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(CircleRadius.pill))
            .background(container)
            .circlePressable(onClick = onClick)
            .padding(horizontal = if (selected) 14.dp else 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = label, tint = content, modifier = Modifier.size(22.dp))
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = content, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun AppNavigationRail(navController: NavHostController, searchEnabled: Boolean, onQuickSearch: () -> Unit) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route.orEmpty()

    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
        if (searchEnabled) {
            NavigationRailItem(
                selected = false,
                onClick = onQuickSearch,
                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Ara") }
            )
        }
        navDestinations.forEach { destination ->
            NavigationRailItem(
                selected = destination.matches(currentDestination),
                onClick = { navController.navigateTopLevel(destination.route) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun QuickSearchDialog(
    people: List<Person>,
    waves: List<Wave>,
    onDismiss: () -> Unit,
    onPersonClick: (Int) -> Unit,
    onWaveClick: (Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val trimmedQuery = query.trim()
    val matchedPeople = people
        .filter { trimmedQuery.isNotBlank() && it.name.contains(trimmedQuery, ignoreCase = true) }
        .take(6)
    val matchedWaves = waves
        .filter { trimmedQuery.isNotBlank() && it.name.contains(trimmedQuery, ignoreCase = true) }
        .take(4)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(CircleRadius.card),
        title = { Text("Hızlı ara", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircleSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Kişi veya grup ara"
                )
                if (trimmedQuery.isBlank()) {
                    Text(
                        "Bir isim yazarak kişi detayına ya da grup görünümüne doğrudan geç.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (matchedPeople.isEmpty() && matchedWaves.isEmpty()) {
                    Text("Sonuç bulunamadı.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(matchedPeople, key = { "p-${it.id}" }) { person ->
                            QuickSearchRow(
                                title = person.name,
                                subtitle = person.phoneNumber.ifBlank { "Telefon yok" },
                                onClick = { onPersonClick(person.id) }
                            )
                        }
                        items(matchedWaves, key = { "w-${it.id}" }) { wave ->
                            QuickSearchRow(
                                title = wave.name,
                                subtitle = "${wave.frequencyDays} günde bir",
                                onClick = { onWaveClick(wave.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

@Composable
private fun QuickSearchRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CircleRadius.control))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .circlePressable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircleAvatar(name = title, size = 38.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = false }
        launchSingleTop = true
        restoreState = false
    }
}
