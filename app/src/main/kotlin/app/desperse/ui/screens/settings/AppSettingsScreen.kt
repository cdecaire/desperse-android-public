package app.desperse.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.core.preferences.AppPreferences
import app.desperse.core.preferences.ExplorerOption
import app.desperse.core.preferences.ThemeMode
import app.desperse.data.dto.request.UpdatePreferencesRequest
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val api: DesperseApi
) : ViewModel() {

    val themeMode = appPreferences.themeMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ThemeMode.SYSTEM
    )

    val explorer = appPreferences.explorer.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ExplorerOption.ORB
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            appPreferences.setThemeMode(mode)
            // Sync to server
            val serverValue = when (mode) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "system"
            }
            safeApiCall { api.updatePreferences(UpdatePreferencesRequest(theme = serverValue)) }
        }
    }

    fun setExplorer(option: ExplorerOption) {
        viewModelScope.launch {
            appPreferences.setExplorer(option)
            // Sync to server
            val serverValue = when (option) {
                ExplorerOption.ORB -> "orb"
                ExplorerOption.SOLSCAN -> "solscan"
                ExplorerOption.SOLANA_EXPLORER -> "solana-explorer"
                ExplorerOption.SOLANAFM -> "solanafm"
            }
            safeApiCall { api.updatePreferences(UpdatePreferencesRequest(explorer = serverValue)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onBack: () -> Unit,
    viewModel: AppSettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val explorer by viewModel.explorer.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings") },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Configure your theme and blockchain explorer preferences.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Theme Setting
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FaIcon(
                    icon = when (themeMode) {
                        ThemeMode.LIGHT -> FaIcons.Sun
                        ThemeMode.DARK -> FaIcons.Moon
                        ThemeMode.SYSTEM -> FaIcons.CircleHalf
                    },
                    size = 18.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Column {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = when (themeMode) {
                            ThemeMode.SYSTEM -> "System default"
                            ThemeMode.LIGHT -> "Light mode"
                            ThemeMode.DARK -> "Dark mode"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ThemeSelector(
                currentMode = themeMode,
                onModeSelected = { viewModel.setThemeMode(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Explorer Preference
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FaIcon(
                    icon = FaIcons.ExternalLink,
                    size = 18.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    style = FaIconStyle.Regular
                )
                Column {
                    Text(
                        text = "Blockchain Explorer",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Choose which explorer to use for transaction links",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ExplorerSelector(
                currentExplorer = explorer,
                onExplorerSelected = { viewModel.setExplorer(it) }
            )
        }
    }
}

@Composable
private fun ThemeSelector(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeModeChip(
            label = "System",
            icon = FaIcons.CircleHalf,
            selected = currentMode == ThemeMode.SYSTEM,
            onClick = { onModeSelected(ThemeMode.SYSTEM) },
            modifier = Modifier.weight(1f)
        )
        ThemeModeChip(
            label = "Light",
            icon = FaIcons.Sun,
            selected = currentMode == ThemeMode.LIGHT,
            onClick = { onModeSelected(ThemeMode.LIGHT) },
            modifier = Modifier.weight(1f)
        )
        ThemeModeChip(
            label = "Dark",
            icon = FaIcons.Moon,
            selected = currentMode == ThemeMode.DARK,
            onClick = { onModeSelected(ThemeMode.DARK) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ThemeModeChip(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { FaIcon(icon, size = 16.dp) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun ExplorerSelector(
    currentExplorer: ExplorerOption,
    onExplorerSelected: (ExplorerOption) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExplorerOption.entries.take(2).forEach { option ->
                ExplorerOptionCard(
                    option = option,
                    isSelected = currentExplorer == option,
                    onClick = { onExplorerSelected(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExplorerOption.entries.drop(2).forEach { option ->
                ExplorerOptionCard(
                    option = option,
                    isSelected = currentExplorer == option,
                    onClick = { onExplorerSelected(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ExplorerOptionCard(
    option: ExplorerOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = option.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = option.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
