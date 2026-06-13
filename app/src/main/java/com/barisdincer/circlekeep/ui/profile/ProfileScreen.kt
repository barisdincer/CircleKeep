package com.barisdincer.circlekeep.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.data.ThemeMode
import com.barisdincer.circlekeep.data.UserPreferences
import com.barisdincer.circlekeep.ui.BackupUiState
import com.barisdincer.circlekeep.ui.design.CircleCard
import com.barisdincer.circlekeep.ui.design.CircleChip
import com.barisdincer.circlekeep.ui.design.CirclePrimaryButton
import com.barisdincer.circlekeep.ui.design.CircleRadius
import com.barisdincer.circlekeep.ui.design.CircleScreenScaffold
import com.barisdincer.circlekeep.ui.design.CircleSpacing
import com.barisdincer.circlekeep.ui.design.CircleTonalButton
import com.barisdincer.circlekeep.ui.design.circlePressable

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    preferences: UserPreferences,
    backupState: BackupUiState,
    onLogsClick: () -> Unit,
    onSaveProfile: (String, String, String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onToggleSearchButton: (Boolean) -> Unit,
    onCreateBackupJson: ((String) -> Unit) -> Unit,
    onRestoreBackupJson: (String) -> Unit
) {
    var displayName by remember { mutableStateOf(preferences.displayName) }
    var initials by remember { mutableStateOf(preferences.displayInitials) }
    var avatarColorKey by remember { mutableStateOf(preferences.avatarColorKey) }
    var editingProfile by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var pendingBackupJson by remember { mutableStateOf<String?>(null) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingBackupJson ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            }
        }
        pendingBackupJson = null
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val json = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
            if (json != null) {
                onRestoreBackupJson(json)
            }
        }
    }

    LaunchedEffect(preferences) {
        displayName = preferences.displayName
        initials = preferences.displayInitials
        avatarColorKey = preferences.avatarColorKey
    }

    CircleScreenScaffold(title = "Profil") { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CircleSpacing.md),
            verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)
        ) {
            CircleCard {
                Column(
                    modifier = Modifier.padding(CircleSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileAvatar(
                            initials = initials.ifBlank { "CK" },
                            color = avatarColor(avatarColorKey)
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                displayName.ifBlank { "CircleKeep profili" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Yerel profil · sunucusuz",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { editingProfile = !editingProfile }) {
                            Icon(Icons.Default.Edit, contentDescription = "Profili düzenle")
                        }
                    }
                    AnimatedVisibility(visible = editingProfile, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        Column(verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)) {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Adın") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(CircleRadius.control)
                            )
                            OutlinedTextField(
                                value = initials,
                                onValueChange = { initials = it.uppercase().take(3) },
                                label = { Text("Avatar harfleri") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(CircleRadius.control)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                avatarOptions.forEach { option ->
                                    ColorChoice(
                                        label = option.label,
                                        color = option.color,
                                        selected = avatarColorKey == option.key,
                                        onClick = { avatarColorKey = option.key }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        displayName = preferences.displayName
                                        initials = preferences.displayInitials
                                        avatarColorKey = preferences.avatarColorKey
                                        editingProfile = false
                                    }
                                ) {
                                    Text("Vazgeç")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                CirclePrimaryButton(
                                    text = "Kaydet",
                                    onClick = {
                                        onSaveProfile(displayName, initials, avatarColorKey)
                                        editingProfile = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            CircleCard(onClick = onLogsClick) {
                Row(
                    modifier = Modifier.padding(CircleSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(CircleRadius.control))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Temas logları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Eklenmiş etkinlikleri ve tekil temas kayıtlarını düzenle veya sil.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            CircleCard {
                Column(
                    modifier = Modifier.padding(CircleSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)
                ) {
                    Text("JSON yedeği", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        backupState.message ?: "Telefon değiştirmeden veya uygulamayı yeniden kurmadan önce yerel yedeğini alabilirsin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CirclePrimaryButton(
                            text = "Dışa aktar",
                            onClick = {
                                onCreateBackupJson { json ->
                                    pendingBackupJson = json
                                    createBackupLauncher.launch("circlekeep-yedek.json")
                                }
                            }
                        )
                        CircleTonalButton(
                            text = "Geri yükle",
                            onClick = {
                                restoreBackupLauncher.launch(arrayOf("application/json", "text/*"))
                            }
                        )
                    }
                }
            }

            CircleCard {
                Column(
                    modifier = Modifier.padding(CircleSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.SettingsSuggest, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Görünüm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Varsayılan olarak telefon temasını takip eder. İstersen CircleKeep için ayrı seçebilirsin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircleChip(preferences.themeMode == ThemeMode.SYSTEM, "Sistem", leadingIcon = Icons.Default.SettingsSuggest) { onThemeModeChange(ThemeMode.SYSTEM) }
                        CircleChip(preferences.themeMode == ThemeMode.LIGHT, "Açık", leadingIcon = Icons.Default.LightMode) { onThemeModeChange(ThemeMode.LIGHT) }
                        CircleChip(preferences.themeMode == ThemeMode.DARK, "Koyu", leadingIcon = Icons.Default.DarkMode) { onThemeModeChange(ThemeMode.DARK) }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .circlePressable { onToggleSearchButton(!preferences.searchButtonEnabled) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Alt çubukta arama düğmesi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Kapatırsan alt menüdeki hızlı arama düğmesi gizlenir.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.searchButtonEnabled,
                            onCheckedChange = onToggleSearchButton
                        )
                    }
                }
            }

            CircleCard(containerColor = MaterialTheme.colorScheme.primaryContainer, border = null) {
                Column(
                    modifier = Modifier.padding(CircleSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "Gizlilik modeli",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        "Profil, kişiler, gruplar ve yedekler yerel kalır. Profil bilgileri bu fazda JSON yedeğine eklenmez.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.padding(bottom = CircleSpacing.lg))
        }
    }
}

@Composable
private fun ProfileAvatar(initials: String, color: Color) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.take(3).ifBlank { "CK" },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}

@Composable
private fun ColorChoice(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(CircleRadius.pill)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, borderColor), shape)
            .circlePressable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelLarge)
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

private data class AvatarOption(
    val key: String,
    val label: String,
    val color: Color
)

private val avatarOptions = listOf(
    AvatarOption("green", "Yeşil", Color(0xFF166C5B)),
    AvatarOption("purple", "Mor", Color(0xFF6B5A7A)),
    AvatarOption("amber", "Amber", Color(0xFF8B5D1E)),
    AvatarOption("blue", "Mavi", Color(0xFF2563EB))
)

private fun avatarColor(key: String): Color {
    return avatarOptions.find { it.key == key }?.color ?: avatarOptions.first().color
}
