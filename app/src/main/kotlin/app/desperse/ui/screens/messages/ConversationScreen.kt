package app.desperse.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.desperse.data.dto.response.MessageResponse
import app.desperse.ui.components.AvatarSize
import app.desperse.ui.components.DesperseAvatar
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.ReportContentPreview
import app.desperse.ui.components.ReportSheet
import app.desperse.ui.theme.DesperseSizes
import app.desperse.ui.theme.DesperseSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    threadId: String,
    otherUserName: String? = null,
    otherUserSlug: String? = null,
    otherUserAvatarUrl: String? = null,
    isBlocked: Boolean = false,
    isBlockedBy: Boolean = false,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: ConversationViewModel = hiltViewModel()
) {
    // Pass nav data to ViewModel on first composition
    LaunchedEffect(Unit) {
        if (otherUserSlug != null) {
            viewModel.setThreadInfo(
                otherUserName = otherUserName,
                otherUserSlug = otherUserSlug,
                otherUserAvatarUrl = otherUserAvatarUrl,
                isBlocked = isBlocked,
                isBlockedBy = isBlockedBy
            )
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Lifecycle observer for mark-read
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onScreenVisible()
                Lifecycle.Event.ON_PAUSE -> viewModel.onScreenHidden()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-scroll to bottom when new messages arrive
    val messageCount = uiState.messages.size
    LaunchedEffect(messageCount) {
        if (messageCount > 0) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            ConversationTopBar(
                otherUserName = uiState.otherUserName,
                otherUserSlug = uiState.otherUserSlug,
                otherUserAvatarUrl = uiState.otherUserAvatarUrl,
                onBack = onBack,
                onUserClick = {
                    val slug = uiState.otherUserSlug
                    if (!slug.isNullOrBlank()) onUserClick(slug)
                },
                onMenuClick = { showMenu = true }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {
            // Message list
            when {
                uiState.isLoading && uiState.messages.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.messages.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.error ?: "Failed to load messages",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(DesperseSpacing.md))
                            Button(onClick = { viewModel.loadMessages() }) {
                                Text("Try Again")
                            }
                        }
                    }
                }
                else -> {
                    MessageList(
                        messages = uiState.messages,
                        currentUserId = uiState.currentUserId,
                        otherLastReadAt = uiState.otherLastReadAt,
                        isLoadingMore = uiState.isLoadingMore,
                        hasMore = uiState.hasMore,
                        listState = listState,
                        onLoadMore = { viewModel.loadOlderMessages() },
                        onDeleteMessage = { messageId -> messageToDelete = messageId },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Input area or blocked status
            if (uiState.isBlockedBy) {
                BlockedStatusBar(message = "You can no longer message this user.")
            } else if (uiState.isBlocked) {
                BlockedStatusBar(message = "You have blocked this user. Unblock to send messages.")
            } else {
                MessageInput(
                    text = messageText,
                    onTextChange = { messageText = it },
                    onSend = {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    },
                    isSending = uiState.isSending,
                    avatarUrl = uiState.currentUserAvatarUrl,
                    avatarIdentity = uiState.currentUserSlug,
                    modifier = Modifier
                        .imePadding()
                        .navigationBarsPadding()
                )
            }
        }

        // Conversation menu bottom sheet
        ConversationMenuSheet(
            isOpen = showMenu,
            isBlocked = uiState.isBlocked,
            onDismiss = { showMenu = false },
            onGoToProfile = {
                showMenu = false
                val slug = uiState.otherUserSlug
                if (!slug.isNullOrBlank()) onUserClick(slug)
            },
            onBlock = {
                showMenu = false
                viewModel.blockUser(!uiState.isBlocked)
            },
            onReport = {
                showMenu = false
                showReportSheet = true
            }
        )

        // Report user sheet
        ReportSheet(
            open = showReportSheet,
            onDismiss = { showReportSheet = false },
            contentType = "user",
            contentPreview = ReportContentPreview(
                userName = uiState.otherUserName ?: uiState.otherUserSlug ?: "",
                userAvatarUrl = uiState.otherUserAvatarUrl
            ),
            onSubmit = { reasons, details ->
                viewModel.createReport(reasons, details)
            }
        )

        // Delete message confirmation dialog
        messageToDelete?.let { messageId ->
            AlertDialog(
                onDismissRequest = { messageToDelete = null },
                title = { Text("Delete message?") },
                text = { Text("This message will be removed for everyone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteMessage(messageId)
                            messageToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { messageToDelete = null }) {
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
private fun ConversationTopBar(
    otherUserName: String?,
    otherUserSlug: String?,
    otherUserAvatarUrl: String?,
    onBack: () -> Unit,
    onUserClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onUserClick)
                    .padding(vertical = DesperseSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                DesperseAvatar(
                    imageUrl = otherUserAvatarUrl,
                    contentDescription = otherUserName ?: otherUserSlug,
                    identityInput = otherUserSlug,
                    size = AvatarSize.Small
                )

                Spacer(modifier = Modifier.width(DesperseSpacing.sm))

                // Name
                Column {
                    Text(
                        text = otherUserName ?: otherUserSlug ?: "Conversation",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (otherUserSlug != null && otherUserName != null) {
                        Text(
                            text = "@$otherUserSlug",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                FaIcon(FaIcons.ArrowLeft, size = 20.dp)
            }
        },
        actions = {
            IconButton(onClick = onMenuClick) {
                FaIcon(FaIcons.EllipsisVertical, size = 20.dp)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationMenuSheet(
    isOpen: Boolean,
    isBlocked: Boolean,
    onDismiss: () -> Unit,
    onGoToProfile: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (isOpen) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DesperseSpacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = DesperseSpacing.xxl)
            ) {
                // Go to profile
                ConversationMenuItem(
                    icon = FaIcons.User,
                    label = "Go to profile",
                    onClick = onGoToProfile
                )

                // Separator
                HorizontalDivider(
                    modifier = Modifier.padding(
                        horizontal = DesperseSpacing.lg,
                        vertical = DesperseSpacing.xs
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Block / Unblock
                ConversationMenuItem(
                    icon = if (isBlocked) FaIcons.LockOpen else FaIcons.Lock,
                    label = if (isBlocked) "Unblock" else "Block",
                    tint = if (!isBlocked) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                    onClick = onBlock
                )

                // Report user
                ConversationMenuItem(
                    icon = FaIcons.Flag,
                    label = "Report user",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onReport
                )
            }
        }
    }
}

@Composable
private fun ConversationMenuItem(
    icon: String,
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(
                horizontal = DesperseSpacing.lg,
                vertical = DesperseSpacing.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FaIcon(
            icon = icon,
            size = DesperseSizes.iconMd,
            style = FaIconStyle.Regular,
            tint = tint
        )

        Spacer(modifier = Modifier.width(DesperseSpacing.md))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = tint
        )
    }
}

@Composable
private fun MessageList(
    messages: List<MessageResponse>,
    currentUserId: String?,
    otherLastReadAt: String?,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    listState: LazyListState,
    onLoadMore: () -> Unit,
    onDeleteMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Build items with date separators
    // Messages are in chronological order (oldest first).
    // We use reverseLayout = true so the newest messages appear at the bottom.
    val displayItems = remember(messages) {
        buildDisplayItems(messages)
    }

    // Find the index of the last own message for "Seen" indicator
    val lastOwnMessageId = remember(messages, currentUserId) {
        messages.lastOrNull { it.senderId == currentUserId }?.id
    }

    // Check if other user has seen the last own message
    val isLastOwnMessageSeen = remember(lastOwnMessageId, otherLastReadAt, messages) {
        if (lastOwnMessageId == null || otherLastReadAt == null) false
        else {
            val lastOwnMessage = messages.find { it.id == lastOwnMessageId }
            if (lastOwnMessage != null) {
                try {
                    val messageInstant = Instant.parse(lastOwnMessage.createdAt)
                    val readInstant = Instant.parse(otherLastReadAt)
                    !readInstant.isBefore(messageInstant)
                } catch (e: Exception) {
                    false
                }
            } else false
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(
            horizontal = DesperseSpacing.md,
            vertical = DesperseSpacing.sm
        ),
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.micro)
    ) {
        // Items are reversed because of reverseLayout = true.
        // displayItems is chronological, so we iterate in reverse.
        itemsIndexed(
            items = displayItems.reversed(),
            key = { _, item ->
                when (item) {
                    is DisplayItem.MessageItem -> item.message.id
                    is DisplayItem.DateSeparator -> "date_${item.label}"
                }
            },
            contentType = { _, item ->
                when (item) {
                    is DisplayItem.MessageItem -> "message"
                    is DisplayItem.DateSeparator -> "separator"
                }
            }
        ) { index, item ->
            when (item) {
                is DisplayItem.DateSeparator -> {
                    DateSeparator(label = item.label)
                }
                is DisplayItem.MessageItem -> {
                    val isOwn = item.message.senderId == currentUserId
                    val showSeen = isOwn &&
                            item.message.id == lastOwnMessageId &&
                            isLastOwnMessageSeen

                    MessageBubble(
                        message = item.message,
                        isOwn = isOwn,
                        showSeen = showSeen,
                        onDelete = if (isOwn && !item.message.isDeleted) {
                            { onDeleteMessage(item.message.id) }
                        } else null
                    )
                }
            }

            // Load more trigger: when near the end of reversed list (top of screen)
            val reversedIndex = displayItems.size - 1 - index
            if (reversedIndex <= 3 && hasMore && !isLoadingMore) {
                LaunchedEffect(Unit) {
                    onLoadMore()
                }
            }
        }

        // Loading more indicator at the top
        if (isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(DesperseSpacing.sm),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageResponse,
    isOwn: Boolean,
    showSeen: Boolean,
    onDelete: (() -> Unit)?
) {
    var showDeleteMenu by remember { mutableStateOf(false) }

    val alignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isOwn) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isOwn) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isOwn) 16.dp else 4.dp,
        bottomEnd = if (isOwn) 4.dp else 16.dp
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
        ) {
            Box {
                Surface(
                    color = if (message.isDeleted) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    } else bubbleColor,
                    shape = bubbleShape,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .then(
                            if (onDelete != null && !message.isDeleted) {
                                Modifier.clickable(
                                    onClickLabel = "Message options",
                                    onClick = { showDeleteMenu = true }
                                )
                            } else Modifier
                        )
                ) {
                    if (message.isDeleted) {
                        Text(
                            text = "This message was deleted",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                horizontal = DesperseSpacing.md,
                                vertical = DesperseSpacing.sm
                            )
                        )
                    } else {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = DesperseSpacing.md,
                                vertical = DesperseSpacing.sm
                            )
                        ) {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatMessageTime(message.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Delete dropdown menu
                DropdownMenu(
                    expanded = showDeleteMenu,
                    onDismissRequest = { showDeleteMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            FaIcon(
                                icon = FaIcons.Trash,
                                size = 16.dp,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showDeleteMenu = false
                            onDelete?.invoke()
                        }
                    )
                }
            }

            // "Seen" indicator
            if (showSeen) {
                Text(
                    text = "Seen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        top = 2.dp,
                        end = if (isOwn) DesperseSpacing.xs else 0.dp,
                        start = if (!isOwn) DesperseSpacing.xs else 0.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun DateSeparator(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DesperseSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
    avatarUrl: String? = null,
    avatarIdentity: String? = null,
    modifier: Modifier = Modifier
) {
    val maxLength = 2000
    val isOverLimit = text.length > maxLength
    val canSend = text.trim().isNotEmpty() && !isOverLimit && !isSending

    Column(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        // Top border
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Input row: Avatar + Input container with embedded send button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesperseSpacing.sm, vertical = DesperseSpacing.sm),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
        ) {
            // Avatar
            DesperseAvatar(
                imageUrl = avatarUrl,
                contentDescription = "Your avatar",
                identityInput = avatarIdentity,
                size = AvatarSize.Small
            )

            // Input container with send button inside
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isOverLimit) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 32.dp, max = 120.dp)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                "Message...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = text,
                            onValueChange = onTextChange,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send,
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = { if (canSend) onSend() }
                            ),
                            enabled = !isSending,
                            maxLines = 6
                        )
                    }

                    // Send button - circular, anchored to bottom-right
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .then(
                                if (canSend && !isSending)
                                    Modifier.clickable(onClick = onSend)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            FaIcon(
                                icon = FaIcons.ArrowUp,
                                size = 14.dp,
                                tint = if (canSend)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Over-limit warning
        if (isOverLimit) {
            Text(
                text = "Message must be $maxLength characters or less.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(
                    start = DesperseSpacing.md,
                    end = DesperseSpacing.md,
                    bottom = DesperseSpacing.xs
                )
            )
        }
    }
}

@Composable
private fun BlockedStatusBar(message: String) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesperseSpacing.lg)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -- Display item model for interleaving messages and date separators --

private sealed class DisplayItem {
    data class MessageItem(val message: MessageResponse) : DisplayItem()
    data class DateSeparator(val label: String) : DisplayItem()
}

private fun buildDisplayItems(messages: List<MessageResponse>): List<DisplayItem> {
    if (messages.isEmpty()) return emptyList()

    val items = mutableListOf<DisplayItem>()
    var lastDate: LocalDate? = null

    for (message in messages) {
        val messageDate = try {
            Instant.parse(message.createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        } catch (e: Exception) {
            null
        }

        if (messageDate != null && messageDate != lastDate) {
            items.add(DisplayItem.DateSeparator(formatDateLabel(messageDate)))
            lastDate = messageDate
        }

        items.add(DisplayItem.MessageItem(message))
    }

    return items
}

private fun formatDateLabel(date: LocalDate): String {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    return when (date) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> {
            if (date.year == today.year) {
                date.format(DateTimeFormatter.ofPattern("MMM d"))
            } else {
                date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            }
        }
    }
}

private fun formatMessageTime(timestamp: String): String {
    return try {
        val instant = Instant.parse(timestamp)
        val zonedTime = instant.atZone(ZoneId.systemDefault())
        zonedTime.format(DateTimeFormatter.ofPattern("h:mm a"))
    } catch (e: Exception) {
        ""
    }
}
