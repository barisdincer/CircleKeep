package com.barisdincer.circlekeep.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.data.ThemeMode
import com.barisdincer.circlekeep.data.UserPreferences

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onSaveProfile: (String, String, String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    var displayName by remember { mutableStateOf(preferences.displayName) }
    var initials by remember { mutableStateOf(preferences.displayInitials) }
    var avatarColorKey by remember { mutableStateOf(preferences.avatarColorKey) }

    LaunchedEffect(preferences) {
        displayName = preferences.displayName
        initials = preferences.displayInitials
        avatarColorKey = preferences.avatarColorKey
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Profil", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ProfileAvatar(
                            initials = initials.ifBlank { "CK" },
                            color = avatarColor(avatarColorKey)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Yerel profil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Hesap yok, sunucu yok. Bu bilgiler sadece bu cihazda durur.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Adın") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = initials,
                        onValueChange = { initials = it.uppercase().take(3) },
                        label = { Text("Avatar harfleri") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
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
                    Button(
                        onClick = { onSaveProfile(displayName, initials, avatarColorKey) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Kaydet")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        ThemeModeChip("Sistem", Icons.Default.SettingsSuggest, ThemeMode.SYSTEM, preferences.themeMode, onThemeModeChange)
                        ThemeModeChip("Açık", Icons.Default.LightMode, ThemeMode.LIGHT, preferences.themeMode, onThemeModeChange)
                        ThemeModeChip("Koyu", Icons.Default.DarkMode, ThemeMode.DARK, preferences.themeMode, onThemeModeChange)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
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

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ProfileAvatar(initials: String, color: Color) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.take(3).ifBlank { "CK" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}

@Composable
private fun ColorChoice(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(label, style = MaterialTheme.typography.labelMedium)
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ThemeModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    themeMode: ThemeMode,
    selectedThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    FilterChip(
        selected = selectedThemeMode == themeMode,
        onClick = { onThemeModeChange(themeMode) },
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
    )
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
