package app.desperse.ui.screens.settings

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
import app.desperse.ui.components.DesperseBackButton
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.data.dto.request.UpdateCreatorSettingsRequest
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIcons
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

val LICENSE_PRESETS = listOf(
    "All Rights Reserved",
    "CC0",
    "CC-BY-4.0",
    "CC-BY-SA-4.0",
    "CC-BY-NC-4.0",
    "CC-BY-NC-ND-4.0",
    "CUSTOM"
)

val SUGGESTED_STATEMENTS = mapOf(
    "All Rights Reserved" to "All rights reserved. No reproduction, distribution, or derivative works permitted without written permission from the rights holder.",
    "CC0" to "This work is dedicated to the public domain. You may copy, modify, distribute, and perform the work, even for commercial purposes, without asking permission.",
    "CC-BY-4.0" to "You are free to share and adapt this work for any purpose, including commercially, as long as you give appropriate credit to the original creator.",
    "CC-BY-SA-4.0" to "You are free to share and adapt this work for any purpose, including commercially, as long as you give appropriate credit and distribute any derivative works under the same license.",
    "CC-BY-NC-4.0" to "You are free to share and adapt this work as long as you give appropriate credit. Commercial use is not permitted without written permission from the rights holder.",
    "CC-BY-NC-ND-4.0" to "You may share this work as long as you give appropriate credit. No commercial use or derivative works are permitted without written permission from the rights holder."
)

data class CopyrightSettingsState(
    val licensePreset: String? = null,
    val licenseCustom: String = "",
    val copyrightHolder: String = "",
    val copyrightRights: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    // Initial values for dirty tracking
    val initialLicensePreset: String? = null,
    val initialLicenseCustom: String = "",
    val initialCopyrightHolder: String = "",
    val initialCopyrightRights: String = ""
) {
    val isDirty: Boolean
        get() = licensePreset != initialLicensePreset ||
                licenseCustom != initialLicenseCustom ||
                copyrightHolder != initialCopyrightHolder ||
                copyrightRights != initialCopyrightRights
}

@HiltViewModel
class CopyrightSettingsViewModel @Inject constructor(
    private val api: DesperseApi
) : ViewModel() {

    private val _state = MutableStateFlow(CopyrightSettingsState())
    val state: StateFlow<CopyrightSettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = safeApiCall { api.getCreatorSettings() }) {
                is ApiResult.Success -> {
                    val data = result.data
                    _state.update {
                        it.copy(
                            licensePreset = data.copyrightLicensePreset,
                            licenseCustom = data.copyrightLicenseCustom ?: "",
                            copyrightHolder = data.copyrightHolder ?: "",
                            copyrightRights = data.copyrightRights ?: "",
                            initialLicensePreset = data.copyrightLicensePreset,
                            initialLicenseCustom = data.copyrightLicenseCustom ?: "",
                            initialCopyrightHolder = data.copyrightHolder ?: "",
                            initialCopyrightRights = data.copyrightRights ?: "",
                            isLoading = false
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun updateLicensePreset(preset: String?) {
        _state.update { it.copy(licensePreset = preset, saveSuccess = false) }
    }

    fun updateLicenseCustom(text: String) {
        if (text.length <= 100) {
            _state.update { it.copy(licenseCustom = text, saveSuccess = false) }
        }
    }

    fun updateCopyrightHolder(text: String) {
        if (text.length <= 200) {
            _state.update { it.copy(copyrightHolder = text, saveSuccess = false) }
        }
    }

    fun updateCopyrightRights(text: String) {
        if (text.length <= 1000) {
            _state.update { it.copy(copyrightRights = text, saveSuccess = false) }
        }
    }

    fun save() {
        val current = _state.value
        if (!current.isDirty || current.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, saveSuccess = false) }

            val request = UpdateCreatorSettingsRequest(
                copyrightLicensePreset = current.licensePreset,
                copyrightLicenseCustom = if (current.licensePreset == "CUSTOM") current.licenseCustom.ifBlank { null } else null,
                copyrightHolder = current.copyrightHolder.ifBlank { null },
                copyrightRights = current.copyrightRights.ifBlank { null }
            )

            when (val result = safeApiCall { api.updateCreatorSettings(request) }) {
                is ApiResult.Success -> {
                    val data = result.data
                    _state.update {
                        it.copy(
                            licensePreset = data.copyrightLicensePreset,
                            licenseCustom = data.copyrightLicenseCustom ?: "",
                            copyrightHolder = data.copyrightHolder ?: "",
                            copyrightRights = data.copyrightRights ?: "",
                            initialLicensePreset = data.copyrightLicensePreset,
                            initialLicenseCustom = data.copyrightLicenseCustom ?: "",
                            initialCopyrightHolder = data.copyrightHolder ?: "",
                            initialCopyrightRights = data.copyrightRights ?: "",
                            isSaving = false,
                            saveSuccess = true
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isSaving = false, error = result.message) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyrightSettingsScreen(
    onBack: () -> Unit,
    viewModel: CopyrightSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Copyright & Licensing") },
                navigationIcon = {
                    DesperseBackButton(onClick = onBack)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                            text = state.error ?: "Failed to load settings",
                            color = MaterialTheme.colorScheme.error
                        )
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
                        text = "Set default copyright and licensing for all new collectibles and editions. You can override these per-post.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // License Type dropdown
                    LicensePresetDropdown(
                        selected = state.licensePreset,
                        onSelected = { viewModel.updateLicensePreset(it) }
                    )

                    // Custom License field (only when CUSTOM)
                    if (state.licensePreset == "CUSTOM") {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = state.licenseCustom,
                            onValueChange = { viewModel.updateLicenseCustom(it) },
                            label = { Text("Custom License") },
                            placeholder = { Text("e.g., MIT, Apache-2.0") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text("${state.licenseCustom.length} / 100")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Copyright Holder
                    OutlinedTextField(
                        value = state.copyrightHolder,
                        onValueChange = { viewModel.updateCopyrightHolder(it) },
                        label = { Text("Rights Holder") },
                        placeholder = { Text("Legal name or entity") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text("${state.copyrightHolder.length} / 200")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Rights Statement
                    OutlinedTextField(
                        value = state.copyrightRights,
                        onValueChange = { viewModel.updateCopyrightRights(it) },
                        label = { Text("Rights Statement") },
                        placeholder = { Text("Describe usage rights and restrictions") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text("${state.copyrightRights.length} / 1000")
                            }
                        }
                    )

                    // Use suggested statement button
                    val suggestedStatement = state.licensePreset?.let { SUGGESTED_STATEMENTS[it] }
                    if (suggestedStatement != null && state.copyrightRights != suggestedStatement) {
                        TextButton(
                            onClick = { viewModel.updateCopyrightRights(suggestedStatement) }
                        ) {
                            Text("Use suggested statement")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Error message
                    if (state.error != null && !state.isLoading) {
                        Text(
                            text = state.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Success message
                    if (state.saveSuccess) {
                        Text(
                            text = "Settings saved",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Save button
                    Button(
                        onClick = { viewModel.save() },
                        enabled = state.isDirty && !state.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicensePresetDropdown(
    selected: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val displayText = selected ?: "None"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("License Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            LICENSE_PRESETS.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset) },
                    onClick = {
                        onSelected(preset)
                        expanded = false
                    }
                )
            }
        }
    }
}
