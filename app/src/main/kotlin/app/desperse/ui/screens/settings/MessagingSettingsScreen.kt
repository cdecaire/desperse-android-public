package app.desperse.ui.screens.settings

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.core.preferences.MessagingPreferences
import app.desperse.data.repository.MessageRepository
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MessagingSettingsVM"

@HiltViewModel
class MessagingSettingsViewModel @Inject constructor(
    private val messagingPreferences: MessagingPreferences,
    private val messageRepository: MessageRepository
) : ViewModel() {

    data class MessagingPrefsState(
        val dmEnabled: Boolean = true,
        val allowBuyers: Boolean = true,
        val allowCollectors: Boolean = true,
        val collectorMinCount: Int = 3,
        val allowTippers: Boolean = true,
        val tipMinAmount: Int = 50,
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(MessagingPrefsState())
    val uiState: StateFlow<MessagingPrefsState> = _uiState.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            messageRepository.getDmPreferences()
                .onSuccess { prefs ->
                    _uiState.update {
                        it.copy(
                            dmEnabled = prefs.dmEnabled,
                            allowBuyers = prefs.allowBuyers,
                            allowCollectors = prefs.allowCollectors,
                            collectorMinCount = prefs.collectorMinCount,
                            allowTippers = prefs.allowTippers,
                            tipMinAmount = prefs.tipMinAmount,
                            isLoading = false
                        )
                    }
                    // Sync to local preferences
                    messagingPreferences.updateAll(
                        dmEnabled = prefs.dmEnabled,
                        allowBuyers = prefs.allowBuyers,
                        allowCollectors = prefs.allowCollectors,
                        collectorMinCount = prefs.collectorMinCount,
                        allowTippers = prefs.allowTippers,
                        tipMinAmount = prefs.tipMinAmount
                    )
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load DM preferences: ${error.message}")
                    // Fall back to local preferences
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
        }
    }

    fun setDmEnabled(enabled: Boolean) {
        val currentState = _uiState.value
        if (enabled && !currentState.allowBuyers && !currentState.allowCollectors && !currentState.allowTippers) {
            // Re-enabling DMs with all sub-options off, enable all
            _uiState.update {
                it.copy(dmEnabled = true, allowBuyers = true, allowCollectors = true, allowTippers = true)
            }
        } else {
            _uiState.update { it.copy(dmEnabled = enabled) }
        }
        savePreferences()
    }

    fun setAllowBuyers(enabled: Boolean) {
        val currentState = _uiState.value
        if (!enabled && !currentState.allowCollectors && !currentState.allowTippers) {
            // All will be off, auto-disable DMs
            _uiState.update {
                it.copy(dmEnabled = false, allowBuyers = false)
            }
        } else {
            _uiState.update { it.copy(allowBuyers = enabled) }
        }
        savePreferences()
    }

    fun setAllowCollectors(enabled: Boolean) {
        val currentState = _uiState.value
        if (!enabled && !currentState.allowBuyers && !currentState.allowTippers) {
            // All will be off, auto-disable DMs
            _uiState.update {
                it.copy(dmEnabled = false, allowCollectors = false)
            }
        } else {
            _uiState.update { it.copy(allowCollectors = enabled) }
        }
        savePreferences()
    }

    fun setAllowTippers(enabled: Boolean) {
        val currentState = _uiState.value
        if (!enabled && !currentState.allowBuyers && !currentState.allowCollectors) {
            // All will be off, auto-disable DMs
            _uiState.update {
                it.copy(dmEnabled = false, allowTippers = false)
            }
        } else {
            _uiState.update { it.copy(allowTippers = enabled) }
        }
        savePreferences()
    }

    fun setCollectorMinCount(count: Int) {
        val clamped = count.coerceIn(1, 100)
        _uiState.update { it.copy(collectorMinCount = clamped) }
        debounceSave()
    }

    fun setTipMinAmount(amount: Int) {
        val clamped = amount.coerceIn(1, 10000)
        _uiState.update { it.copy(tipMinAmount = clamped) }
        debounceSave()
    }

    private var saveJob: Job? = null

    private fun debounceSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(800L)
            savePreferences()
        }
    }

    private fun savePreferences() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isSaving = true) }

            // Save to local preferences first for immediate effect
            messagingPreferences.updateAll(
                dmEnabled = state.dmEnabled,
                allowBuyers = state.allowBuyers,
                allowCollectors = state.allowCollectors,
                collectorMinCount = state.collectorMinCount,
                allowTippers = state.allowTippers,
                tipMinAmount = state.tipMinAmount
            )

            // Sync to server
            messageRepository.updateDmPreferences(
                dmEnabled = state.dmEnabled,
                allowBuyers = state.allowBuyers,
                allowCollectors = state.allowCollectors,
                collectorMinCount = state.collectorMinCount,
                allowTippers = state.allowTippers,
                tipMinAmount = state.tipMinAmount
            )
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            dmEnabled = response.dmEnabled,
                            allowBuyers = response.allowBuyers,
                            allowCollectors = response.allowCollectors,
                            collectorMinCount = response.collectorMinCount,
                            allowTippers = response.allowTippers,
                            tipMinAmount = response.tipMinAmount,
                            isSaving = false
                        )
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to save DM preferences: ${error.message}")
                    _uiState.update { it.copy(isSaving = false) }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingSettingsScreen(
    onBack: () -> Unit,
    viewModel: MessagingSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val dmEnabled = state.dmEnabled
    val allowBuyers = state.allowBuyers
    val allowCollectors = state.allowCollectors
    val collectorMinCount = state.collectorMinCount
    val allowTippers = state.allowTippers
    val tipMinAmount = state.tipMinAmount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messaging") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        FaIcon(FaIcons.ArrowLeft, size = 20.dp)
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Control who can send you direct messages.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Master toggle
            MessagingToggle(
                label = "Direct Messages",
                description = "Allow eligible users to start new chats",
                icon = FaIcons.Message,
                checked = dmEnabled,
                onCheckedChange = { viewModel.setDmEnabled(it) },
                enabled = true
            )

            if (dmEnabled) {
                SettingsDivider()

                // Section header
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Eligibility Requirements",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Edition Buyers toggle
                MessagingToggle(
                    label = "Edition Buyers",
                    description = "Own any of your editions",
                    icon = FaIcons.BagShopping,
                    checked = allowBuyers,
                    onCheckedChange = { viewModel.setAllowBuyers(it) },
                    enabled = true
                )

                SettingsDivider()

                // Collectors toggle
                MessagingToggleWithInput(
                    label = "Collectors",
                    descriptionPrefix = "At least",
                    descriptionSuffix = "collectibles",
                    icon = FaIcons.Gem,
                    checked = allowCollectors,
                    onCheckedChange = { viewModel.setAllowCollectors(it) },
                    value = collectorMinCount,
                    onValueChange = { viewModel.setCollectorMinCount(it) },
                    min = 1,
                    max = 100,
                    enabled = true
                )

                SettingsDivider()

                // Tippers toggle
                MessagingToggleWithInput(
                    label = "Tippers",
                    descriptionPrefix = "At least",
                    descriptionSuffix = "SKR tipped",
                    icon = FaIcons.Coins,
                    checked = allowTippers,
                    onCheckedChange = { viewModel.setAllowTippers(it) },
                    value = tipMinAmount,
                    onValueChange = { viewModel.setTipMinAmount(it) },
                    min = 1,
                    max = 10000,
                    enabled = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info text
            Row {
                FaIcon(
                    icon = FaIcons.CircleInfo,
                    size = 14.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    style = FaIconStyle.Regular
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "To prevent spam, only your supporters can message you. Once someone starts a conversation, they can continue messaging even if they no longer meet the criteria.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MessagingToggle(
    label: String,
    description: String,
    icon: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
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
            enabled = enabled,
            modifier = Modifier.scale(0.85f)
        )
    }
}

@Composable
private fun MessagingToggleWithInput(
    label: String,
    descriptionPrefix: String,
    descriptionSuffix: String,
    icon: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int,
    enabled: Boolean = true
) {
    val focusManager = LocalFocusManager.current
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    fun commitValue() {
        val parsed = textValue.toIntOrNull()
        if (parsed != null) {
            onValueChange(parsed.coerceIn(min, max))
        } else {
            textValue = value.toString()
        }
    }

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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$descriptionPrefix ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                BasicTextField(
                    value = textValue,
                    onValueChange = { newText ->
                        if (newText.isEmpty() || newText.all { it.isDigit() }) {
                            textValue = newText
                        }
                    },
                    enabled = checked,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = if (checked)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            commitValue()
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .width(48.dp)
                        .background(
                            color = if (checked)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (checked)
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                commitValue()
                            }
                        },
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            innerTextField()
                        }
                    }
                )

                Text(
                    text = " $descriptionSuffix",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
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
