package app.desperse.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.desperse.data.dto.response.MentionUser
import app.desperse.data.dto.response.ThreadResponse
import app.desperse.ui.components.AvatarSize
import app.desperse.ui.components.DesperseAvatar
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.theme.DesperseSizes
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.DesperseTones
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onBack: (() -> Unit)? = null,
    onThreadClick: (ThreadResponse) -> Unit,
    onNewConversation: (threadId: String, name: String?, slug: String?, avatar: String?) -> Unit = { _, _, _, _ -> },
    viewModel: ThreadListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val newMessageState by viewModel.newMessageState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    var showNewMessageSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Lifecycle observer for stale time refresh
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onScreenVisible()
                Lifecycle.Event.ON_PAUSE -> viewModel.onScreenHidden()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // New Message Sheet
    if (showNewMessageSheet) {
        NewMessageSheet(
            state = newMessageState,
            sheetState = sheetState,
            onDismiss = {
                showNewMessageSheet = false
                viewModel.resetNewMessage()
            },
            onSearchQueryChange = { viewModel.searchUsers(it) },
            onUserSelected = { viewModel.selectUser(it) },
            onBackToSearch = { viewModel.clearSelectedUser() },
            onStartConversation = {
                viewModel.startConversation { threadId, name, slug, avatar ->
                    scope.launch {
                        sheetState.hide()
                        showNewMessageSheet = false
                        viewModel.resetNewMessage()
                        onNewConversation(threadId, name, slug, avatar)
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Messages",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { scope.launch { listState.animateScrollToItem(0) } }
                    )
                },
                actions = {
                    IconButton(onClick = { showNewMessageSheet = true }) {
                        FaIcon(
                            FaIcons.MessagePlus,
                            tint = MaterialTheme.colorScheme.onSurface,
                            size = DesperseSizes.iconMd,
                            style = FaIconStyle.Regular,
                            contentDescription = "New message"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading && uiState.threads.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                uiState.error != null && uiState.threads.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(DesperseSpacing.lg)
                        ) {
                            Text(
                                text = uiState.error ?: "Something went wrong",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(onClick = { viewModel.loadThreads() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                uiState.threads.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
                        ) {
                            FaIcon(
                                FaIcons.Message,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                size = DesperseSizes.iconFeature
                            )
                            Spacer(Modifier.height(DesperseSpacing.xs))
                            Text(
                                text = "No conversations yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.threads,
                            key = { it.id },
                            contentType = { "thread" }
                        ) { thread ->
                            val onClickStable = remember(thread.id) {
                                { onThreadClick(thread) }
                            }
                            ThreadItem(
                                thread = thread,
                                onClick = onClickStable
                            )
                        }

                        // Load more trigger
                        if (uiState.hasMore) {
                            item(key = "load_more", contentType = "load_more") {
                                LaunchedEffect(Unit) {
                                    viewModel.loadMore()
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(DesperseSpacing.lg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadItem(
    thread: ThreadResponse,
    onClick: () -> Unit
) {
    val displayName = thread.otherUser.displayName
        ?: thread.otherUser.usernameSlug

    val relativeTime = remember(thread.lastMessageAt) {
        thread.lastMessageAt?.let { formatRelativeTime(it) } ?: ""
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = DesperseSpacing.lg,
                    vertical = DesperseSpacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            DesperseAvatar(
                imageUrl = thread.otherUser.avatarUrl,
                contentDescription = displayName,
                identityInput = thread.otherUser.usernameSlug,
                size = AvatarSize.Large
            )

            Spacer(Modifier.width(DesperseSpacing.md))

            // Thread info (name, preview, time)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (thread.hasUnread) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (relativeTime.isNotEmpty()) {
                        Spacer(Modifier.width(DesperseSpacing.sm))
                        Text(
                            text = relativeTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (thread.hasUnread) {
                                DesperseTones.Info
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.lastMessagePreview ?: "No messages yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (thread.hasUnread) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (thread.hasUnread) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Unread indicator dot
                    if (thread.hasUnread) {
                        Spacer(Modifier.width(DesperseSpacing.sm))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DesperseTones.Info)
                        )
                    }
                }
            }
        }

        // Divider between items
        HorizontalDivider(
            modifier = Modifier.padding(start = DesperseSpacing.lg + DesperseSizes.avatarLg + DesperseSpacing.md),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewMessageSheet(
    state: NewMessageUiState,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onUserSelected: (MentionUser) -> Unit,
    onBackToSearch: () -> Unit,
    onStartConversation: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = DesperseSpacing.lg)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.selectedUser != null) {
                    IconButton(onClick = onBackToSearch) {
                        FaIcon(
                            FaIcons.ArrowLeft,
                            tint = MaterialTheme.colorScheme.onSurface,
                            size = DesperseSizes.iconMd,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(Modifier.width(DesperseSpacing.xs))
                }
                Text(
                    text = "New Message",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            if (state.selectedUser == null) {
                // Search view
                NewMessageSearchView(
                    query = state.searchQuery,
                    results = state.searchResults,
                    isSearching = state.isSearching,
                    onQueryChange = onSearchQueryChange,
                    onUserSelected = onUserSelected
                )
            } else {
                // Selected user view
                NewMessageSelectedUserView(
                    user = state.selectedUser,
                    eligibility = state.eligibility,
                    isCheckingEligibility = state.isCheckingEligibility,
                    isCreatingThread = state.isCreatingThread,
                    error = state.error,
                    onStartConversation = onStartConversation
                )
            }
        }
    }
}

@Composable
private fun NewMessageSearchView(
    query: String,
    results: List<MentionUser>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onUserSelected: (MentionUser) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Search field
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.lg)
            .focusRequester(focusRequester),
        placeholder = { Text("Search users...") },
        leadingIcon = {
            FaIcon(
                FaIcons.MagnifyingGlass,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = DesperseSizes.iconSm,
                contentDescription = null
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    )

    Spacer(Modifier.height(DesperseSpacing.md))

    when {
        isSearching -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = DesperseSpacing.xxl),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        query.length >= 2 && results.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = DesperseSpacing.xxl),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No users found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        results.isNotEmpty() -> {
            LazyColumn {
                items(
                    items = results,
                    key = { it.id }
                ) { user ->
                    val onClick = remember(user.id) { { onUserSelected(user) } }
                    UserSearchResultItem(user = user, onClick = onClick)
                }
            }
        }

        query.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = DesperseSpacing.xxl),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Type to search for users",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UserSearchResultItem(
    user: MentionUser,
    onClick: () -> Unit
) {
    val displayName = user.displayName ?: user.usernameSlug

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DesperseAvatar(
            imageUrl = user.avatarUrl,
            contentDescription = displayName,
            identityInput = user.usernameSlug,
            size = AvatarSize.Medium
        )
        Spacer(Modifier.width(DesperseSpacing.md))
        Column {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${user.usernameSlug}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NewMessageSelectedUserView(
    user: MentionUser,
    eligibility: app.desperse.data.dto.response.DmEligibilityResponse?,
    isCheckingEligibility: Boolean,
    isCreatingThread: Boolean,
    error: String?,
    onStartConversation: () -> Unit
) {
    val displayName = user.displayName ?: user.usernameSlug

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.lg)
    ) {
        // User header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DesperseAvatar(
                imageUrl = user.avatarUrl,
                contentDescription = displayName,
                identityInput = user.usernameSlug,
                size = AvatarSize.Large
            )
            Spacer(Modifier.width(DesperseSpacing.md))
            Column {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "@${user.usernameSlug}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(DesperseSpacing.xl))

        when {
            isCheckingEligibility -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DesperseSpacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            eligibility != null && eligibility.allowed -> {
                Button(
                    onClick = onStartConversation,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreatingThread,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isCreatingThread) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(DesperseSpacing.sm))
                    }
                    Text(
                        text = if (isCreatingThread) "Starting..." else "Start Conversation",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            eligibility != null && eligibility.creatorDmsDisabled == true -> {
                // DMs disabled
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
                ) {
                    FaIcon(
                        FaIcons.Lock,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = DesperseSizes.iconLg,
                        contentDescription = null
                    )
                    Text(
                        text = "Messaging not available",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$displayName has DMs disabled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            eligibility != null && eligibility.unlockPaths.isNotEmpty() -> {
                // Unlock paths
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
                    ) {
                        FaIcon(
                            FaIcons.Lock,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = DesperseSizes.iconMd,
                            contentDescription = null
                        )
                        Text(
                            text = "Unlock messaging",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "To prevent spam, $displayName requires one of the following to message them:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(DesperseSpacing.xs))

                    eligibility.unlockPaths.forEach { path ->
                        val icon = when (path.method) {
                            "edition_purchase" -> FaIcons.BagShopping
                            "collect" -> FaIcons.Gem
                            else -> FaIcons.Lock
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(DesperseSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
                        ) {
                            FaIcon(
                                icon,
                                tint = MaterialTheme.colorScheme.primary,
                                size = DesperseSizes.iconMd,
                                contentDescription = null
                            )
                            Text(
                                text = path.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            error != null -> {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = DesperseSpacing.md)
                )
            }
        }
    }
}

/**
 * Formats an ISO timestamp to a relative time string (e.g., "2m", "1h", "3d").
 */
private fun formatRelativeTime(isoTimestamp: String): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        val now = Instant.now()
        val duration = Duration.between(instant, now)

        when {
            duration.seconds < 60 -> "now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m"
            duration.toHours() < 24 -> "${duration.toHours()}h"
            duration.toDays() < 7 -> "${duration.toDays()}d"
            duration.toDays() < 30 -> "${duration.toDays() / 7}w"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d")
                val zonedDateTime = instant.atZone(java.time.ZoneId.systemDefault())
                formatter.format(zonedDateTime)
            }
        }
    } catch (e: DateTimeParseException) {
        ""
    }
}
