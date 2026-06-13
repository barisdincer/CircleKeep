package com.barisdincer.circlekeep.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.barisdincer.circlekeep.ui.NetworkViewModel
import com.barisdincer.circlekeep.ui.PersonWithWave
import com.barisdincer.circlekeep.ui.design.CircleCard
import com.barisdincer.circlekeep.ui.design.CircleHeroCard
import com.barisdincer.circlekeep.ui.design.CirclePrimaryButton
import com.barisdincer.circlekeep.ui.design.CircleScreenScaffold
import com.barisdincer.circlekeep.ui.design.CircleSectionHeader
import com.barisdincer.circlekeep.ui.design.CircleSpacing
import com.barisdincer.circlekeep.ui.design.CircleStatChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: NetworkViewModel,
    onOpenEventLog: () -> Unit
) {
    val dashboard by viewModel.dashboardReminders.collectAsState()
    val presentation by viewModel.dashboardPresentation.collectAsState()
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
    val callLogPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.syncCallLog(context)
        } else {
            viewModel.syncCallLog(context)
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
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

    CircleScreenScaffold(
        title = "Bugün",
        subtitle = SimpleDateFormat("d MMMM, EEEE", Locale("tr", "TR")).format(Date()),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = CircleSpacing.md),
            contentPadding = PaddingValues(top = CircleSpacing.xs, bottom = CircleSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)
        ) {
            item {
                DashboardFocusHero(
                    waitingCount = presentation.waitingCount,
                    overdueCount = presentation.overdueCount,
                    todayCount = presentation.todayCount,
                    upcomingCount = presentation.upcomingCount,
                    snoozedCount = presentation.snoozedCount,
                    nextName = presentation.nextContact?.personName
                )
            }

            if (needsNotificationPermission) {
                item {
                    DashboardNotificationPermissionCard(
                        onEnable = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    )
                }
            }

            item {
                DashboardActionBar(
                    recentInteractionCount = interactions.size,
                    syncMessage = syncState.message,
                    isSyncing = syncState.isSyncing,
                    onSync = {
                        if (
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.syncCallLog(context)
                        } else {
                            callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                        }
                    },
                    onOpenBatchLog = onOpenEventLog
                )
            }

            item {
                CircleSectionHeader(
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

@Composable
private fun DashboardNotificationPermissionCard(onEnable: () -> Unit) {
    CircleCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer, border = null) {
        Row(
            modifier = Modifier.padding(CircleSpacing.md).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.Icon(
                Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Günlük hatırlatmaları aç", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(
                    "CircleKeep yalnızca zamanı gelen yerel ritimler için bildirim gönderir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            CirclePrimaryButton(text = "Aç", onClick = onEnable)
        }
    }
}

@Composable
private fun DashboardFocusHero(
    waitingCount: Int,
    overdueCount: Int,
    todayCount: Int,
    upcomingCount: Int,
    snoozedCount: Int,
    nextName: String?
) {
    CircleHeroCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (waitingCount == 0) "Bugün sakin" else "$waitingCount ritim dikkat istiyor",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                nextName?.let { "Sıradaki öneri: $it. Küçük bir temas yeter." }
                    ?: "Geciken ya da yaklaşan ritim yok; ilişkiler düzenli akıyor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircleStatChip(label = "Geciken", value = overdueCount, icon = Icons.Default.WarningAmber, modifier = Modifier.weight(1f))
            CircleStatChip(label = "Bugün", value = todayCount, icon = Icons.Default.CheckCircle, modifier = Modifier.weight(1f))
            CircleStatChip(
                label = if (snoozedCount > 0) "Yakın · $snoozedCount ert." else "Yakın",
                value = upcomingCount,
                icon = Icons.Default.Schedule,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
