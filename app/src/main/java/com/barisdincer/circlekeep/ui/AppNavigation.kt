package com.barisdincer.circlekeep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import com.barisdincer.circlekeep.ui.design.CircleSearchField
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
                    BottomNavigationBar(
                        navController = navController,
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
            .padding(paddingValues)
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
private fun BottomNavigationBar(navController: NavHostController, onQuickSearch: () -> Unit) {
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
                selected = false,
                label = "Ara",
                icon = Icons.Default.Search,
                modifier = Modifier.width(52.dp),
                onClick = onQuickSearch
            )
            CompactNavItem(
                selected = currentDestination == "dashboard" || currentDestination == "event_log",
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
private fun AppNavigationRail(navController: NavHostController, onQuickSearch: () -> Unit) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route.orEmpty()

    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationRailItem(
            selected = false,
            onClick = onQuickSearch,
            icon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text("Ara") }
        )
        NavigationRailItem(
            selected = currentDestination == "dashboard" || currentDestination == "event_log",
            onClick = { navController.navigateTopLevel("dashboard") },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
            label = { Text("Bugün") }
        )
        NavigationRailItem(
            selected = currentDestination == "people" || currentDestination.startsWith("person_detail") || currentDestination.startsWith("add_person"),
            onClick = { navController.navigateTopLevel("people") },
            icon = { Icon(Icons.Default.Group, contentDescription = null) },
            label = { Text("Kişiler") }
        )
        NavigationRailItem(
            selected = currentDestination == "waves" || currentDestination.startsWith("group_detail"),
            onClick = { navController.navigateTopLevel("waves") },
            icon = { Icon(Icons.Default.Group, contentDescription = null) },
            label = { Text("Gruplar") }
        )
        NavigationRailItem(
            selected = currentDestination == "reports",
            onClick = { navController.navigateTopLevel("reports") },
            icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            label = { Text("Özet") }
        )
        NavigationRailItem(
            selected = currentDestination == "profile" || currentDestination == "logs",
            onClick = { navController.navigateTopLevel("profile") },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Profil") }
        )
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
        title = { Text("Hızlı ara") },
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (matchedPeople.isEmpty() && matchedWaves.isEmpty()) {
                    Text("Sonuç bulunamadı.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
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
