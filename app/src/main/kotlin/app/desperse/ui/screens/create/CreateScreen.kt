package app.desperse.ui.screens.create

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.desperse.data.model.MediaConstants
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.MentionTextField
import app.desperse.ui.screens.create.components.CategorySelector
import app.desperse.ui.screens.create.components.CoverPickerCard
import app.desperse.ui.screens.create.components.EditionOptionsCard
import app.desperse.ui.screens.create.components.FormCard
import app.desperse.ui.screens.create.components.MultiMediaPicker
import app.desperse.ui.screens.create.components.NftMetadataCard
import app.desperse.ui.screens.create.components.PermanentStorageSection
import app.desperse.ui.screens.create.components.PostTypeSelector
import app.desperse.ui.screens.create.components.TimedEditionSection
import app.desperse.ui.screens.settings.LICENSE_PRESETS
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.toneCollectible
import app.desperse.ui.theme.toneDestructive
import app.desperse.ui.theme.toneEdition
import app.desperse.ui.theme.toneStandard
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    editPostId: String? = null,
    onPostCreated: (String) -> Unit,
    onClose: () -> Unit,
    onManageStorageCredits: () -> Unit = {},
    viewModel: CreatePostViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load post for edit mode
    LaunchedEffect(editPostId) {
        if (editPostId != null) {
            viewModel.loadPostForEdit(editPostId)
        }
    }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is CreatePostEvent.PostCreated -> onPostCreated(event.postId)
                is CreatePostEvent.PostUpdated -> onClose()
                is CreatePostEvent.PostDeleted -> onClose()
                is CreatePostEvent.Error -> { /* Error shown in UI state */ }
            }
        }
    }

    val isNftType = uiState.postType == "collectible" || uiState.postType == "edition"
    val isEdition = uiState.postType == "edition"

    // Load creator copyright defaults when post type changes to collectible/edition
    LaunchedEffect(uiState.postType) {
        if (isNftType) {
            viewModel.loadCreatorDefaults()
        }
    }

    // Derive needsCover from whether any media item is non-previewable
    val hasNonPreviewable = remember(uiState.mediaItems) {
        uiState.mediaItems.any { !MediaConstants.isPreviewable(it.mediaType) }
    }
    val needsCover = hasNonPreviewable

    // Derive primary media type for edition protect-download check
    val primaryMediaType = remember(uiState.mediaItems) {
        uiState.mediaItems.firstOrNull { !MediaConstants.isPreviewable(it.mediaType) }?.mediaType ?: ""
    }

    // Theme-aware tone colors
    val standardColor = toneStandard()
    val collectibleColor = toneCollectible()
    val editionColor = toneEdition()
    val destructiveColor = toneDestructive()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) "Edit Post" else "Create Post",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onClose) {
                        FaIcon(FaIcons.Xmark, size = 20.dp)
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        // Delete button
                        TextButton(
                            onClick = { viewModel.showDeleteConfirmation() },
                            enabled = !uiState.isDeleting
                        ) {
                            Text(
                                "Delete",
                                color = destructiveColor.copy(
                                    alpha = if (uiState.isDeleting) 0.5f else 1f
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = standardColor)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DesperseSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xxl)
            ) {
                Spacer(modifier = Modifier.height(DesperseSpacing.xs))

                // 1. Media section (label + picker, no card wrapper — dashed border already)
                Column(verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)) {
                    Text(
                        text = "Media",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    MultiMediaPicker(
                        mediaItems = uiState.mediaItems,
                        isLocked = uiState.fieldLocking.isMediaLocked,
                        onAddMedia = { viewModel.addMediaItem(it) },
                        onRemoveMedia = { viewModel.removeMediaItem(it) }
                    )

                    // Cover image picker (for audio/document/3D)
                    if (needsCover || uiState.coverMedia != null) {
                        CoverPickerCard(
                            coverMedia = uiState.coverMedia,
                            onCoverSelected = { viewModel.onCoverSelected(it) },
                            onRemove = { viewModel.removeCover() }
                        )
                    }
                }

                // 2. Post Type Selector (hidden in edit mode)
                if (!uiState.isEditMode) {
                    PostTypeSelector(
                        selectedType = uiState.postType,
                        onTypeSelected = { viewModel.updatePostType(it) }
                    )
                }

                // 3. Caption + Categories + NFT fields card
                FormCard {
                    // Caption section
                    Column(verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)) {
                        Text(
                            text = "Caption",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        var captionFocused by remember { mutableStateOf(false) }
                        val captionTone = when (uiState.postType) {
                            "collectible" -> collectibleColor
                            "edition" -> editionColor
                            else -> standardColor
                        }
                        val captionBorderColor = if (captionFocused) captionTone
                            else MaterialTheme.colorScheme.outlineVariant
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (captionFocused) 2.dp else 1.dp,
                                    color = captionBorderColor,
                                    shape = RoundedCornerShape(DesperseRadius.sm)
                                )
                                .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.md)
                        ) {
                            MentionTextField(
                                value = uiState.caption,
                                onValueChange = { viewModel.updateCaption(it) },
                                onSearch = { query -> viewModel.searchMentionUsers(query) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 80.dp),
                                placeholder = "Write a caption...",
                                enabled = uiState.fieldLocking.isCaptionEditable,
                                maxLines = 8,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                onFocusChanged = { captionFocused = it }
                            )
                        }
                        // Character counter + hashtag hint
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DesperseSpacing.lg),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Use #hashtags for custom topics",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${uiState.caption.length} / 2000",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(DesperseSpacing.lg))

                    // Categories section
                    CategorySelector(
                        selectedCategories = uiState.selectedCategories,
                        onToggle = { viewModel.toggleCategory(it) },
                        enabled = uiState.fieldLocking.areCategoriesEditable
                    )

                    // NFT Name & Description (for collectible/edition) — inside same card
                    if (isNftType) {
                        Spacer(modifier = Modifier.height(DesperseSpacing.lg))

                        val nftTone = if (isEdition) editionColor else collectibleColor

                        Column(verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)) {
                            Text(
                                text = "NFT Name" + if (isEdition) " *" else "",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = uiState.nftName,
                                onValueChange = { viewModel.updateNftName(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Enter NFT name") },
                                singleLine = true,
                                enabled = uiState.fieldLocking.areNftFieldsEditable,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = nftTone,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    cursorColor = nftTone,
                                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(DesperseRadius.sm),
                                supportingText = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            "${uiState.nftName.length} / 32",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )

                            Text(
                                text = "NFT Description",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = uiState.nftDescription,
                                onValueChange = { viewModel.updateNftDescription(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Enter NFT description") },
                                minLines = 3,
                                maxLines = 5,
                                enabled = uiState.fieldLocking.areNftFieldsEditable,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = nftTone,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    cursorColor = nftTone,
                                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(DesperseRadius.sm),
                                supportingText = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            "${uiState.nftDescription.length} / 5000",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // 4. Edition Options card (edition only)
                if (isEdition) {
                    FormCard {
                        EditionOptionsCard(
                            priceDisplay = uiState.priceDisplay,
                            currency = uiState.currency,
                            maxSupplyEnabled = uiState.maxSupplyEnabled,
                            maxSupplyDisplay = uiState.maxSupplyDisplay,
                            protectDownload = uiState.protectDownload,
                            showProtectDownload = primaryMediaType in setOf("document", "audio", "3d"),
                            pricingEnabled = uiState.fieldLocking.arePricingEditable,
                            onPriceChange = { viewModel.updatePrice(it) },
                            onCurrencyChange = { viewModel.updateCurrency(it) },
                            onMaxSupplyToggle = { viewModel.toggleMaxSupply(it) },
                            onMaxSupplyChange = { viewModel.updateMaxSupply(it) },
                            onProtectDownloadChange = { viewModel.updateProtectDownload(it) }
                        )
                    }

                    // 4b. Timed Edition card (edition only)
                    FormCard {
                        TimedEditionSection(
                            enabled = uiState.mintWindowEnabled,
                            startMode = uiState.mintWindowStartMode,
                            startTime = uiState.mintWindowStartTime,
                            durationHours = uiState.mintWindowDurationHours,
                            isLocked = uiState.fieldLocking.areTimeWindowFieldsLocked,
                            onToggle = { viewModel.toggleMintWindow(it) },
                            onStartModeChange = { viewModel.updateMintWindowStartMode(it) },
                            onStartTimeChange = { viewModel.updateMintWindowStartTime(it) },
                            onDurationChange = { viewModel.updateMintWindowDurationHours(it) }
                        )
                    }

                    // 5. Permanent Storage card (edition only, not in edit mode)
                    if (!uiState.isEditMode || uiState.storageType == "arweave") {
                        FormCard {
                            PermanentStorageSection(
                                storageType = uiState.storageType,
                                fundingState = uiState.arweaveFundingState,
                                isLocked = uiState.fieldLocking.isStorageTypeLocked,
                                isEditMode = uiState.isEditMode,
                                onStorageTypeChange = { viewModel.updateStorageType(it) },
                                onManageCreditsClick = onManageStorageCredits,
                                onRetryCheck = { viewModel.checkArweaveFunding() }
                            )
                        }
                    }
                }

                // 6. NFT Metadata card (collectible/edition, collapsible)
                if (isNftType) {
                    FormCard {
                        NftMetadataCard(
                            nftSymbol = uiState.nftSymbol,
                            royalties = uiState.royalties,
                            isMutable = uiState.isMutable,
                            nftFieldsEditable = uiState.fieldLocking.areNftFieldsEditable,
                            mutabilityEditable = uiState.fieldLocking.isMutabilityEditable,
                            onSymbolChange = { viewModel.updateNftSymbol(it) },
                            onRoyaltiesChange = { viewModel.updateRoyalties(it) },
                            onMutableChange = { viewModel.updateIsMutable(it) }
                        )
                    }
                }

                // 7. Copyright & Licensing (collectible/edition only)
                if (isNftType) {
                    FormCard {
                        CopyrightSection(
                            licensePreset = uiState.copyrightLicensePreset,
                            licenseCustom = uiState.copyrightLicenseCustom,
                            copyrightHolder = uiState.copyrightHolder,
                            copyrightRights = uiState.copyrightRights,
                            enabled = uiState.fieldLocking.areNftFieldsEditable,
                            accentColor = if (isEdition) editionColor else collectibleColor,
                            onPresetChange = { viewModel.updateCopyrightLicensePreset(it) },
                            onCustomChange = { viewModel.updateCopyrightLicenseCustom(it) },
                            onHolderChange = { viewModel.updateCopyrightHolder(it) },
                            onRightsChange = { viewModel.updateCopyrightRights(it) }
                        )
                    }
                }

                // Error banner
                uiState.submitError?.let { error ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = destructiveColor.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            error,
                            modifier = Modifier.padding(DesperseSpacing.md),
                            style = MaterialTheme.typography.bodySmall,
                            color = destructiveColor
                        )
                    }
                }

                // Publish / Save button (full-width pill, matching web)
                Button(
                    onClick = { viewModel.submit() },
                    enabled = viewModel.isValid() && !uiState.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        disabledContentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    )
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (uiState.isEditMode) "Save" else "Publish",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Bottom spacing
                Spacer(modifier = Modifier.height(DesperseSpacing.xxxl))
            }
        }

        // Delete confirmation dialog
        if (uiState.showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteConfirmation() },
                title = { Text("Delete Post") },
                text = {
                    Text(
                        "Are you sure you want to delete this post? This action cannot be undone." +
                            if (uiState.editState?.isMinted == true)
                                "\n\nNote: NFTs already exist on-chain and will not be affected."
                            else ""
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.deletePost() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = destructiveColor
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDeleteConfirmation() }) {
                        Text("Cancel")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopyrightSection(
    licensePreset: String?,
    licenseCustom: String,
    copyrightHolder: String,
    copyrightRights: String,
    enabled: Boolean,
    accentColor: Color,
    onPresetChange: (String?) -> Unit,
    onCustomChange: (String) -> Unit,
    onHolderChange: (String) -> Unit,
    onRightsChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)) {
        Text(
            text = "Copyright & Licensing",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Set copyright and licensing for this post",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // License dropdown
        var expanded by remember { mutableStateOf(false) }
        val displayText = licensePreset ?: "None"

        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            OutlinedTextField(
                value = displayText,
                onValueChange = {},
                readOnly = true,
                label = { Text("License Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    cursorColor = accentColor,
                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(DesperseRadius.sm)
            )
            ExposedDropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = {
                        onPresetChange(null)
                        expanded = false
                    }
                )
                LICENSE_PRESETS.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset) },
                        onClick = {
                            onPresetChange(preset)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Custom license text (only when CUSTOM)
        if (licensePreset == "CUSTOM") {
            OutlinedTextField(
                value = licenseCustom,
                onValueChange = onCustomChange,
                label = { Text("Custom License") },
                placeholder = { Text("e.g., MIT, Apache-2.0") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = enabled,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    cursorColor = accentColor,
                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(DesperseRadius.sm),
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text("${licenseCustom.length} / 100")
                    }
                }
            )
        }

        // Rights Holder
        OutlinedTextField(
            value = copyrightHolder,
            onValueChange = onHolderChange,
            label = { Text("Rights Holder") },
            placeholder = { Text("Legal name or entity") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                cursorColor = accentColor,
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(DesperseRadius.sm),
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text("${copyrightHolder.length} / 200")
                }
            }
        )

        // Rights Statement
        OutlinedTextField(
            value = copyrightRights,
            onValueChange = onRightsChange,
            label = { Text("Rights Statement") },
            placeholder = { Text("Describe usage rights and restrictions") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                cursorColor = accentColor,
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(DesperseRadius.sm),
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text("${copyrightRights.length} / 1000")
                }
            }
        )
    }
}
