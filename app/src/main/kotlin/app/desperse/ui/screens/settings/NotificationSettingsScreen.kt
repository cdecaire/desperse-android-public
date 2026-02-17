package app.desperse.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.data.dto.request.NotificationPreferencesUpdate
import app.desperse.data.dto.request.UpdatePreferencesRequest
import app.desperse.data.dto.response.NotificationPreferences
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettingsState(
    val prefs: NotificationPreferences = NotificationPreferences(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val api: DesperseApi
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationSettingsState())
    val state: StateFlow<NotificationSettingsState> = _state.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = safeApiCall { api.getPreferences() }) {
                is ApiResult.Success -> {
                    val notifPrefs = result.data.preferences.notifications ?: NotificationPreferences()
                    _state.update { it.copy(prefs = notifPrefs, isLoading = false) }
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun toggle(field: String, enabled: Boolean) {
        // Optimistic update
        _state.update { state ->
            val updated = when (field) {
                "messages" -> state.prefs.copy(messages = enabled)
                "follows" -> state.prefs.copy(follows = enabled)
                "likes" -> state.prefs.copy(likes = enabled)
                "comments" -> state.prefs.copy(comments = enabled)
                "collects" -> state.prefs.copy(collects = enabled)
                "purchases" -> state.prefs.copy(purchases = enabled)
                "mentions" -> state.prefs.copy(mentions = enabled)
                else -> state.prefs
            }
            state.copy(prefs = updated)
        }

        // Persist to server
        viewModelScope.launch {
            val notifUpdate = when (field) {
                "messages" -> NotificationPreferencesUpdate(messages = enabled)
                "follows" -> NotificationPreferencesUpdate(follows = enabled)
                "likes" -> NotificationPreferencesUpdate(likes = enabled)
                "comments" -> NotificationPreferencesUpdate(comments = enabled)
                "collects" -> NotificationPreferencesUpdate(collects = enabled)
                "purchases" -> NotificationPreferencesUpdate(purchases = enabled)
                "mentions" -> NotificationPreferencesUpdate(mentions = enabled)
                else -> return@launch
            }
            when (val result = safeApiCall {
                api.updatePreferences(UpdatePreferencesRequest(notifications = notifUpdate))
            }) {
                is ApiResult.Success -> {
                    val notifPrefs = result.data.preferences.notifications ?: NotificationPreferences()
                    _state.update { it.copy(prefs = notifPrefs) }
                }
                is ApiResult.Error -> {
                    // Revert on failure
                    _state.update { state ->
                        val reverted = when (field) {
                            "messages" -> state.prefs.copy(messages = !enabled)
                            "follows" -> state.prefs.copy(follows = !enabled)
                            "likes" -> state.prefs.copy(likes = !enabled)
                            "comments" -> state.prefs.copy(comments = !enabled)
                            "collects" -> state.prefs.copy(collects = !enabled)
                            "purchases" -> state.prefs.copy(purchases = !enabled)
                            "mentions" -> state.prefs.copy(mentions = !enabled)
                            else -> state.prefs
                        }
                        state.copy(prefs = reverted)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        FaIcon(FaIcons.ArrowLeft, size = 20.dp)
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null && state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.error ?: "Failed to load preferences",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.toggle("", false) }) {
                            Text("Retry")
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Choose which notifications you want to receive.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    NotificationToggle(
                        label = "Messages",
                        description = "When you receive a new message",
                        icon = FaIcons.Message,
                        checked = state.prefs.messages,
                        onCheckedChange = { viewModel.toggle("messages", it) }
                    )

                    SettingsDivider()

                    NotificationToggle(
                        label = "New followers",
                        description = "When someone follows you",
                        icon = FaIcons.UserPlus,
                        checked = state.prefs.follows,
                        onCheckedChange = { viewModel.toggle("follows", it) }
                    )

                    SettingsDivider()

                    NotificationToggle(
                        label = "Likes",
                        description = "When someone likes your post",
                        icon = FaIcons.Heart,
                        checked = state.prefs.likes,
                        onCheckedChange = { viewModel.toggle("likes", it) }
                    )

                    SettingsDivider()

                    NotificationToggle(
                        label = "Comments",
                        description = "When someone comments on your post",
                        icon = FaIcons.Comment,
                        checked = state.prefs.comments,
                        onCheckedChange = { viewModel.toggle("comments", it) }
                    )

                    SettingsDivider()

                    NotificationToggle(
                        label = "Collects",
                        description = "When someone collects your free collectible",
                        icon = FaIcons.Gem,
                        checked = state.prefs.collects,
                        onCheckedChange = { viewModel.toggle("collects", it) }
                    )

                    SettingsDivider()

                    NotificationToggle(
                        label = "Purchases",
                        description = "When someone buys your edition",
                        icon = FaIcons.Wallet,
                        checked = state.prefs.purchases,
                        onCheckedChange = { viewModel.toggle("purchases", it) }
                    )

                    SettingsDivider()

                    NotificationToggle(
                        label = "Mentions",
                        description = "When someone mentions you in a post or comment",
                        icon = FaIcons.At,
                        checked = state.prefs.mentions,
                        onCheckedChange = { viewModel.toggle("mentions", it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationToggle(
    label: String,
    description: String,
    icon: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FaIcon(
            icon = icon,
            size = 18.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            style = FaIconStyle.Regular
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.85f)
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}
