package app.desperse.ui.screens.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.desperse.ui.components.DesperseBackButton
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIcons
import app.desperse.ui.screens.create.components.AttachmentSheet
import app.desperse.ui.screens.create.components.CopyrightSheet
import app.desperse.ui.screens.create.components.EditionDetailsSheet
import app.desperse.ui.screens.create.components.NftDetailsSheet
import app.desperse.ui.screens.create.components.PermanentStorageSheet
import app.desperse.ui.screens.create.components.PostTypeSelector
import app.desperse.ui.screens.create.components.TimedEditionSheet
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
    onBack: (() -> Unit)? = null,
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

    // Theme-aware tone colors
    val standardColor = toneStandard()
    val collectibleColor = toneCollectible()
    val editionColor = toneEdition()
    val destructiveColor = toneDestructive()

    // Sheet states
    var showNftDetailsSheet by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showEditionDetailsSheet by remember { mutableStateOf(false) }
    var showTimedEditionSheet by remember { mutableStateOf(false) }
    var showStorageSheet by remember { mutableStateOf(false) }
    var showCopyrightSheet by remember { mutableStateOf(false) }

    // --- Sheets ---

    if (isNftType) {
        val nftAccent = if (isEdition) editionColor else collectibleColor

        NftDetailsSheet(
            isOpen = showNftDetailsSheet,
            onDismiss = { showNftDetailsSheet = false },
            nftName = uiState.nftName,
            nftDescription = uiState.nftDescription,
            isMutable = uiState.isMutable,
            nftSymbol = uiState.nftSymbol,
            royalties = uiState.royalties,
            enabled = uiState.fieldLocking.areNftFieldsEditable,
            mutabilityEditable = uiState.fieldLocking.isMutabilityEditable,
            accentColor = nftAccent,
            isEdition = isEdition,
            onNameChange = { viewModel.updateNftName(it) },
            onDescriptionChange = { viewModel.updateNftDescription(it) },
            onMutableChange = { viewModel.updateIsMutable(it) },
            onSymbolChange = { viewModel.updateNftSymbol(it) },
            onRoyaltiesChange = { viewModel.updateRoyalties(it) }
        )

        AttachmentSheet(
            isOpen = showAttachmentSheet,
            onDismiss = { showAttachmentSheet = false },
            attachments = uiState.attachments,
            onAdd = { uri -> viewModel.addAttachment(uri) },
            onRemove = { id -> viewModel.removeAttachment(id) },
            protectDownload = uiState.protectDownload,
            onProtectDownloadChange = if (isEdition) {
                { viewModel.updateProtectDownload(it) }
            } else null
        )

        CopyrightSheet(
            isOpen = showCopyrightSheet,
            onDismiss = { showCopyrightSheet = false },
            licensePreset = uiState.copyrightLicensePreset,
            licenseCustom = uiState.copyrightLicenseCustom,
            copyrightHolder = uiState.copyrightHolder,
            copyrightRights = uiState.copyrightRights,
            enabled = uiState.fieldLocking.areNftFieldsEditable,
            accentColor = nftAccent,
            onPresetChange = { viewModel.updateCopyrightLicensePreset(it) },
            onCustomChange = { viewModel.updateCopyrightLicenseCustom(it) },
            onHolderChange = { viewModel.updateCopyrightHolder(it) },
            onRightsChange = { viewModel.updateCopyrightRights(it) }
        )
    }

    if (isEdition) {
        EditionDetailsSheet(
            isOpen = showEditionDetailsSheet,
            onDismiss = { showEditionDetailsSheet = false },
            priceDisplay = uiState.priceDisplay,
            currency = uiState.currency,
            maxSupplyEnabled = uiState.maxSupplyEnabled,
            maxSupplyDisplay = uiState.maxSupplyDisplay,
            pricingEnabled = uiState.fieldLocking.arePricingEditable,
            onPriceChange = { viewModel.updatePrice(it) },
            onCurrencyChange = { viewModel.updateCurrency(it) },
            onMaxSupplyToggle = { viewModel.toggleMaxSupply(it) },
            onMaxSupplyChange = { viewModel.updateMaxSupply(it) }
        )

        TimedEditionSheet(
            isOpen = showTimedEditionSheet,
            onDismiss = { showTimedEditionSheet = false },
            enabled = uiState.mintWindowEnabled,
            startMode = uiState.mintWindowStartMode,
            startTime = uiState.mintWindowStartTime,
            durationHours = uiState.mintWindowDurationHours,
            isLocked = uiState.fieldLocking.areTimeWindowFieldsLocked,
            onToggle = { viewModel.toggleMintWindow(it) },
            onStartModeChange = { viewModel.updateMintWindowStartMode(it) },
            onStartTimeChange = { viewModel.updateMintWindowStartTime(it) },
            onDurationChange = { viewModel.updateMintWindowDurationHours(it) },
            isDoneEnabled = viewModel.isMintWindowValid()
        )

        if (!uiState.isEditMode || uiState.storageType == "arweave") {
            PermanentStorageSheet(
                isOpen = showStorageSheet,
                onDismiss = { showStorageSheet = false },
                storageType = uiState.storageType,
                fundingState = uiState.arweaveFundingState,
                isLocked = uiState.fieldLocking.isStorageTypeLocked,
                isEditMode = uiState.isEditMode,
                onStorageTypeChange = { viewModel.updateStorageType(it) },
                onRetryCheck = { viewModel.checkArweaveFunding() },
                isDoneEnabled = viewModel.isStorageValid()
            )
        }
    }

    // --- Screen ---

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
                    if (onBack != null) {
                        DesperseBackButton(onClick = onBack)
                    } else {
                        androidx.compose.material3.IconButton(onClick = onClose) {
                            FaIcon(FaIcons.Xmark, size = 20.dp)
                        }
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
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
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = DesperseSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
                ) {
                    // Post Type Selector (hidden in edit mode)
                    if (!uiState.isEditMode) {
                        PostTypeSelector(
                            selectedType = uiState.postType,
                            onTypeSelected = { viewModel.updatePostType(it) }
                        )
                    }

                    // Sheet rows for collectible/edition
                    if (isNftType) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // NFT Details
                        SheetRow(
                            title = "NFT Details",
                            subtitle = uiState.nftName.ifBlank { null },
                            required = uiState.nftName.isBlank(),
                            destructiveColor = destructiveColor,
                            onClick = { showNftDetailsSheet = true }
                        )
                    }

                    if (isEdition) {
                        // Edition Details (price, supply)
                        val priceSubtitle = uiState.priceDisplay.toDoubleOrNull()?.let {
                            "${uiState.priceDisplay} ${uiState.currency}"
                        }
                        SheetRow(
                            title = "Edition Details",
                            subtitle = priceSubtitle,
                            required = uiState.priceDisplay.isBlank(),
                            destructiveColor = destructiveColor,
                            onClick = { showEditionDetailsSheet = true }
                        )
                    }

                    // Attachments (after edition details for editions, after NFT details for collectibles)
                    if (isNftType) {
                        SheetRow(
                            title = "Attachments",
                            subtitle = uiState.attachments.firstOrNull()?.let { it.fileName.ifBlank { "Uploading..." } },
                            onClick = { showAttachmentSheet = true }
                        )
                    }

                    if (isEdition) {
                        // Timed Edition
                        val durationHours = uiState.mintWindowDurationHours
                        val startTime = uiState.mintWindowStartTime
                        val timedSubtitle = if (uiState.mintWindowEnabled && durationHours != null) {
                            val start = if (uiState.mintWindowStartMode == "scheduled" && startTime != null) {
                                app.desperse.ui.util.MintWindowUtils.formatDateTime(startTime)
                            } else "On publish"
                            val durationMs = (durationHours * 3600_000).toLong()
                            val duration = app.desperse.ui.util.MintWindowUtils.formatDuration(durationMs)
                            "$start · $duration"
                        } else if (uiState.mintWindowEnabled) {
                            "Enabled"
                        } else null
                        SheetRow(
                            title = "Timed Edition",
                            subtitle = timedSubtitle,
                            onClick = { showTimedEditionSheet = true }
                        )

                        // Permanent Storage
                        if (!uiState.isEditMode || uiState.storageType == "arweave") {
                            val storageSubtitle = if (uiState.storageType == "arweave") {
                                if (viewModel.isStorageValid()) "Arweave" else "Setup required"
                            } else null
                            SheetRow(
                                title = "Permanent Storage",
                                subtitle = storageSubtitle,
                                onClick = { showStorageSheet = true }
                            )
                        }
                    }

                    if (isNftType) {
                        // Copyright & Licensing
                        SheetRow(
                            title = "Copyright & Licensing",
                            subtitle = uiState.copyrightLicensePreset,
                            onClick = { showCopyrightSheet = true }
                        )
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

                    Spacer(modifier = Modifier.height(DesperseSpacing.lg))
                }

                // Post type description above publish button
                if (!uiState.isEditMode) {
                    val typeDescription = when (uiState.postType) {
                        "post" -> "Share your work without minting"
                        "collectible" -> "Followers can collect this as a free NFT"
                        "edition" -> "Sell limited or open NFT editions"
                        else -> null
                    }
                    if (typeDescription != null) {
                        Text(
                            typeDescription,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Button(
                    onClick = { viewModel.submit() },
                    enabled = viewModel.isValid() && !uiState.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.md)
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

/**
 * Reusable tappable row that opens a sheet. Shows title, optional subtitle, and chevron.
 */
@Composable
private fun SheetRow(
    title: String,
    subtitle: String? = null,
    required: Boolean = false,
    destructiveColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (required) {
                Text(
                    "Required",
                    style = MaterialTheme.typography.bodySmall,
                    color = destructiveColor
                )
            }
        }
        FaIcon(
            icon = FaIcons.ChevronRight,
            size = 14.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
