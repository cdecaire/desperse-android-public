package app.desperse.ui.screens.create

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.desperse.ui.components.DesperseBackButton
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.MentionTextField
import app.desperse.ui.screens.create.components.CategorySheet
import app.desperse.ui.screens.create.components.MediaThumbnailStrip
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.toneStandard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptionScreen(
    viewModel: CreatePostViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val standardColor = toneStandard()

    var showCategorySheet by remember { mutableStateOf(false) }

    CategorySheet(
        isOpen = showCategorySheet,
        onDismiss = { showCategorySheet = false },
        selectedCategories = uiState.selectedCategories,
        enabled = uiState.fieldLocking.areCategoriesEditable,
        onToggle = { viewModel.toggleCategory(it) }
    )

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
                    DesperseBackButton(onClick = onBack)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DesperseSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xxl)
            ) {
                // Media thumbnail strip (read-only)
                if (uiState.mediaItems.isNotEmpty()) {
                    MediaThumbnailStrip(
                        mediaItems = uiState.mediaItems,
                        coverMedia = uiState.coverMedia
                    )
                }

                // Caption section
                var captionFocused by remember { mutableStateOf(false) }
                val captionFocusRequester = remember { FocusRequester() }
                val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

                @OptIn(ExperimentalFoundationApi::class)
                val bringIntoViewRequester = remember { BringIntoViewRequester() }
                LaunchedEffect(imeBottom, captionFocused) {
                    if (captionFocused && imeBottom > 0) {
                        bringIntoViewRequester.bringIntoView()
                    }
                }

                @OptIn(ExperimentalFoundationApi::class)
                Column(
                    verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
                ) {
                    Text(
                        text = "Caption",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val captionBorderColor = if (captionFocused) standardColor
                        else MaterialTheme.colorScheme.outlineVariant
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (captionFocused) 2.dp else 1.dp,
                                color = captionBorderColor,
                                shape = RoundedCornerShape(DesperseRadius.sm)
                            )
                            .clickable { captionFocusRequester.requestFocus() }
                            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.md)
                    ) {
                        MentionTextField(
                            value = uiState.caption,
                            onValueChange = { viewModel.updateCaption(it) },
                            onSearch = { query -> viewModel.searchMentionUsers(query) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp)
                                .focusRequester(captionFocusRequester),
                            placeholder = "Write a caption...",
                            enabled = uiState.fieldLocking.isCaptionEditable,
                            maxLines = 8,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            onFocusChanged = { focused -> captionFocused = focused }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(bringIntoViewRequester),
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
                    Spacer(modifier = Modifier.height(DesperseSpacing.md))
                }

                // Categories — tappable row, opens sheet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategorySheet = true }
                        .padding(vertical = DesperseSpacing.sm),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Categories",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (uiState.selectedCategories.isNotEmpty()) {
                            Text(
                                uiState.selectedCategories.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    FaIcon(
                        icon = FaIcons.ChevronRight,
                        size = 14.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(DesperseSpacing.lg))
            }

            // Next button (hidden when keyboard is open)
            val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            if (!isKeyboardOpen) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.md)
                        .height(44.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        "Next",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
