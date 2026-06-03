package com.barisdincer.circlekeep.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.ui.NetworkViewModel
import com.barisdincer.circlekeep.ui.PersonWithWave
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: NetworkViewModel,
    onOpenEventLog: () -> Unit
) {
    val dashboard by viewModel.dashboardReminders.collectAsState()
    val people by viewModel.people.collectAsState()
    val waves by viewModel.waves.collectAsState()
    val interactions by viewModel.interactions.collectAsState()
    val activeContactTypes by viewModel.activeContactTypes.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var logSheet by remember { mutableStateOf<ContactLogSheetState?>(null) }
    var snoozeTarget by remember { mutableStateOf<PersonWithWave?>(null) }
    var showUpcoming by rememberSaveable { mutableStateOf(false) }
    val focusContacts = remember(dashboard.overdue, dashboard.today) {
        (dashboard.overdue + dashboard.today).sortedWith(
            compareByDescending<PersonWithWave> { it.daysOverdue }
                .thenBy { it.lastInteractionDate }
        )
    }

    LaunchedEffect(uiMessage) {
        val message = uiMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearUiMessage()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Bugün", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                            Text(
                                SimpleDateFormat("d MMMM, EEEE", Locale("tr", "TR")).format(Date()),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DashboardActionBar(
                    recentInteractionCount = interactions.size,
                    syncMessage = syncState.message,
                    isSyncing = syncState.isSyncing,
                    onSync = { viewModel.syncCallLog(context) },
                    onOpenBatchLog = onOpenEventLog
                )
            }

            item {
                ReminderSectionTitle(
                    title = "Öncelik listesi",
                    count = focusContacts.size,
                    subtitle = "Gecikenler ve bugün zamanı gelenler burada. Grup ritimleri kişi bazında takip edilir."
                )
            }

            if (focusContacts.isEmpty()) {
                item {
                    EmptyFocusCard()
                }
            }

            items(focusContacts, key = { "${it.person.id}-${it.contactType.key}" }) { contact ->
                ReminderCard(
                    contact = contact,
                    onLog = { logSheet = ContactLogSheetState.Single(contact) },
                    onSnooze = { snoozeTarget = contact }
                )
            }

            if (dashboard.snoozed.isNotEmpty()) {
                item {
                    ReminderSectionTitle(
                        title = "Ertelenenler",
                        count = dashboard.snoozed.size,
                        subtitle = "Seçtiğin tarihe kadar ana listeden saklanır; günü gelince tekrar görünür."
                    )
                }
                items(dashboard.snoozed, key = { "${it.person.id}-${it.contactType.key}" }) { contact ->
                    ReminderCard(
                        contact = contact,
                        onLog = { logSheet = ContactLogSheetState.Single(contact) },
                        onSnooze = { snoozeTarget = contact }
                    )
                }
            }

            item {
                UpcomingSectionPanel(
                    count = dashboard.upcoming.size,
                    expanded = showUpcoming,
                    nextContact = dashboard.upcoming.firstOrNull(),
                    contacts = dashboard.upcoming,
                    onToggle = { showUpcoming = !showUpcoming },
                    onLog = { contact -> logSheet = ContactLogSheetState.Single(contact) },
                    onSnooze = { contact -> snoozeTarget = contact }
                )
            }
        }

        logSheet?.let { sheet ->
            ContactLogSheet(
                sheet = sheet,
                people = people,
                waves = waves,
                contactTypes = activeContactTypes,
                onDismiss = { logSheet = null },
                onSave = { personIds, type, note, timestamp ->
                    viewModel.logInteractions(personIds, type, note, timestamp)
                    logSheet = null
                }
            )
        }

        snoozeTarget?.let { contact ->
            SnoozeUntilSheet(
                contact = contact,
                onDismiss = { snoozeTarget = null },
                onSave = { untilDate ->
                    viewModel.snoozeContactTypeUntil(contact.person.id, contact.contactType.key, untilDate)
                    snoozeTarget = null
                }
            )
        }
    }
}
