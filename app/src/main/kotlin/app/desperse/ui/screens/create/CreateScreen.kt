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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.desperse.data.model.MediaConstants
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.MentionTextField
import app.desperse.ui.screens.create.components.CategorySelector
import app.desperse.ui.screens.create.components.CoverPickerCard
import app.desperse.ui.screens.create.components.EditionOptionsCard
import app.desperse.ui.screens.create.components.MultiMediaPicker
import app.desperse.ui.screens.create.components.NftMetadataCard
import app.desperse.ui.screens.create.components.PostTypeSelector
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.toneCollectible
import app.desperse.ui.theme.toneDestructive
import app.desperse.ui.theme.toneEdition
import app.desperse.ui.theme.toneStandard
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material3.OutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    editPostId: String? = null,
    onPostCreated: (String) -> Unit,
    onClose: () -> Unit,
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
    val typeTone = when (uiState.postType) {
        "collectible" -> collectibleColor
        "edition" -> editionColor
        else -> standardColor
    }

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

                    // Submit button
                    Button(
                        onClick = { viewModel.submit() },
                        enabled = viewModel.isValid() && !uiState.isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (uiState.isEditMode) "Save" else "Publish")
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
                verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xl)
            ) {
                Spacer(modifier = Modifier.height(DesperseSpacing.xs))

                // 1. Multi-Media Picker
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

                // 2. Post Type Selector (hidden in edit mode)
                if (!uiState.isEditMode) {
                    PostTypeSelector(
                        selectedType = uiState.postType,
                        onTypeSelected = { viewModel.updatePostType(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // 3. Caption with @mention autocomplete
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(DesperseRadius.xs)
                            )
                            .padding(DesperseSpacing.md)
                    ) {
                        MentionTextField(
                            value = uiState.caption,
                            onValueChange = { viewModel.updateCaption(it) },
                            onSearch = { query -> viewModel.searchMentionUsers(query) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "What's on your mind?",
                            enabled = uiState.fieldLocking.isCaptionEditable,
                            maxLines = 8,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    // Character counter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = DesperseSpacing.xs, start = DesperseSpacing.lg),
                    ) {
                        Text(
                            "${uiState.caption.length}/2000",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 4. Categories
                CategorySelector(
                    selectedCategories = uiState.selectedCategories,
                    onToggle = { viewModel.toggleCategory(it) },
                    enabled = uiState.fieldLocking.areCategoriesEditable
                )

                // 5. NFT Name & Description (for collectible/edition)
                if (isNftType) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    val nftTone = if (isEdition) editionColor else collectibleColor

                    OutlinedTextField(
                        value = uiState.nftName,
                        onValueChange = { viewModel.updateNftName(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("NFT Name${if (isEdition) " *" else ""}") },
                        placeholder = { Text("My Artwork") },
                        singleLine = true,
                        enabled = uiState.fieldLocking.areNftFieldsEditable,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = nftTone,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = nftTone,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        supportingText = {
                            Text("${uiState.nftName.length}/32", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    )

                    OutlinedTextField(
                        value = uiState.nftDescription,
                        onValueChange = { viewModel.updateNftDescription(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("NFT Description") },
                        placeholder = { Text("Description for on-chain metadata") },
                        minLines = 2,
                        maxLines = 4,
                        enabled = uiState.fieldLocking.areNftFieldsEditable,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = nftTone,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = nftTone,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // 6. Edition Options (edition only)
                if (isEdition) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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

                // 7. NFT Metadata (collectible/edition, collapsible)
                if (isNftType) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
