package app.desperse.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.core.auth.PrivyAuthManager
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val privyAuthManager: PrivyAuthManager
) : ViewModel() {

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    fun logout() {
        android.util.Log.d("SettingsViewModel", "Logout button tapped")
        viewModelScope.launch {
            android.util.Log.d("SettingsViewModel", "Calling privyAuthManager.logout()...")
            privyAuthManager.logout()
            android.util.Log.d("SettingsViewModel", "Logout complete")
            _events.emit(SettingsEvent.LoggedOut)
        }
    }
}

sealed class SettingsEvent {
    object LoggedOut : SettingsEvent()
}

/**
 * Settings item data class for structured settings list
 */
data class SettingsItemData(
    val route: String,
    val label: String,
    val icon: String,
    val description: String
)

/**
 * Settings categories matching web app structure
 */
private val accountSettings = listOf(
    SettingsItemData(
        route = "settings/profile-info",
        label = "Profile Info",
        icon = FaIcons.User,
        description = "Update your profile and username"
    ),
    SettingsItemData(
        route = "settings/wallets",
        label = "Wallets & Linked",
        icon = FaIcons.Wallet,
        description = "Manage connected wallets and accounts"
    ),
    SettingsItemData(
        route = "settings/notifications",
        label = "Notifications",
        icon = FaIcons.Bell,
        description = "Choose which notifications to receive"
    ),
    SettingsItemData(
        route = "settings/messaging",
        label = "Messaging",
        icon = FaIcons.Message,
        description = "Control who can message you"
    ),
    SettingsItemData(
        route = "settings/app",
        label = "App Settings",
        icon = FaIcons.Gear,
        description = "Preferences and app configuration"
    )
)

private val generalSettings = listOf(
    SettingsItemData(
        route = "settings/help",
        label = "Help & About",
        icon = FaIcons.CircleInfo,
        description = "Learn more and get support"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.LoggedOut -> {
                    // Navigation will be handled by auth state change in DesperseNavGraph
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        FaIcon(FaIcons.ArrowLeft, size = 20.dp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Account Section
            SettingsSectionHeader("Account")

            accountSettings.forEach { item ->
                SettingsItemRow(
                    item = item,
                    onClick = { onNavigate(item.route) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // General Section
            SettingsSectionHeader("General")

            generalSettings.forEach { item ->
                SettingsItemRow(
                    item = item,
                    onClick = { onNavigate(item.route) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logout
            Surface(
                onClick = { viewModel.logout() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FaIcon(FaIcons.ArrowRightFromBracket, size = 18.dp, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Log Out",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsItemRow(
    item: SettingsItemData,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FaIcon(
                icon = item.icon,
                size = 20.dp,
                style = FaIconStyle.Regular,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FaIcon(
                FaIcons.ChevronRight,
                size = 14.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

