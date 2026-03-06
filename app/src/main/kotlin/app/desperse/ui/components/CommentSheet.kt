package app.desperse.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.desperse.data.dto.response.Comment
import app.desperse.ui.screens.feed.CommentSheetViewModel
import app.desperse.ui.theme.DesperseSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit,
    onReportComment: (Comment) -> Unit,
    viewModel: CommentSheetViewModel
) {
    if (!isOpen) return

    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var commentText by remember { mutableStateOf("") }
    var previousCommentsSize by remember { mutableStateOf(0) }
    var commentToDelete by remember { mutableStateOf<Comment?>(null) }

    // Clear comment text after successful submission
    LaunchedEffect(uiState.comments.size, uiState.isSubmitting) {
        if (!uiState.isSubmitting && uiState.comments.size > previousCommentsSize) {
            commentText = ""
        }
        previousCommentsSize = uiState.comments.size
    }

    DesperseBottomSheet(
        isOpen = true,
        onDismiss = onDismiss,
        onDismissRequest = {
            focusManager.clearFocus()
            onDismiss()
        },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        // ~60% when keyboard is hidden, expand when keyboard appears
        val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
        val isKeyboardOpen = imeBottom > 0
        val heightFraction = if (isKeyboardOpen) 0.85f else 0.55f

        Column(
            modifier = Modifier.fillMaxHeight(heightFraction)
        ) {
            // Header
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = DesperseSpacing.sm),
                textAlign = TextAlign.Center
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // Comments list area (shrinks when keyboard appears)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                    uiState.error != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(DesperseSpacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = uiState.error ?: "Failed to load comments",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(DesperseSpacing.sm))
                            TextButton(
                                onClick = {
                                    uiState.postId?.let {
                                        viewModel.openForPost(it, uiState.commentCount)
                                    }
                                }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                    uiState.comments.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No comments yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = uiState.comments,
                                key = { it.id }
                            ) { comment ->
                                val onUserClickStable = remember(comment.user.slug) { { onUserClick(comment.user.slug) } }
                                val onDeleteStable = remember(comment.id) { { commentToDelete = comment } }
                                val onReportStable = remember(comment.id) { { onReportComment(comment) } }
                                SwipeableCommentItem(
                                    comment = comment,
                                    isOwnComment = comment.user.id == uiState.currentUserId,
                                    isDeleting = uiState.deletingCommentId == comment.id,
                                    onUserClick = onUserClickStable,
                                    onMentionClick = onUserClick,
                                    onDelete = onDeleteStable,
                                    onReport = onReportStable
                                )
                            }
                        }
                    }
                }
            }

            // Comment input bar
            SheetCommentInputBar(
                text = commentText,
                onTextChange = { commentText = it },
                onSubmit = {
                    if (commentText.trim().isNotEmpty()) {
                        viewModel.createComment(commentText)
                        commentText = ""
                        focusManager.clearFocus()
                    }
                },
                onSearch = { query -> viewModel.searchMentionUsers(query) },
                isSubmitting = uiState.isSubmitting,
                error = uiState.commentError,
                onErrorDismiss = { viewModel.clearCommentError() },
                avatarUrl = uiState.currentUserAvatarUrl,
                avatarIdentityInput = uiState.currentUserSlug,
                modifier = Modifier
                    .imePadding()
                    .navigationBarsPadding()
            )
        }

        // Delete confirmation dialog
        commentToDelete?.let { comment ->
            AlertDialog(
                onDismissRequest = { commentToDelete = null },
                title = { Text("Delete comment?") },
                text = { Text("This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteComment(comment.id)
                            commentToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { commentToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * Comment input bar for the sheet — duplicated from PostDetailScreen to avoid touching it.
 * Avatar on left, expandable input with send button inside (bottom-right).
 */
@Composable
private fun SheetCommentInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSearch: suspend (String) -> List<app.desperse.data.dto.response.MentionUser>,
    isSubmitting: Boolean,
    error: String?,
    onErrorDismiss: () -> Unit,
    avatarUrl: String? = null,
    avatarIdentityInput: String? = null,
    modifier: Modifier = Modifier
) {
    val maxLength = 280
    val isOverLimit = text.length > maxLength
    val canSubmit = text.trim().isNotEmpty() && !isOverLimit && !isSubmitting

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Error message
        if (error != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = DesperseSpacing.md, vertical = DesperseSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onErrorDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    FaIcon(
                        icon = FaIcons.Xmark,
                        size = 14.dp,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesperseSpacing.sm, vertical = DesperseSpacing.sm),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
        ) {
            // Avatar — sized to match the input pill height (32dp text + 12dp padding + 2dp border)
            DesperseAvatar(
                imageUrl = avatarUrl,
                contentDescription = "Your avatar",
                identityInput = avatarIdentityInput,
                size = AvatarSize.Large  // 48dp, closest to 44dp
            )

            // Input container with send button inside
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(
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
                    MentionTextField(
                        value = text,
                        onValueChange = onTextChange,
                        onSearch = onSearch,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 32.dp, max = 120.dp)
                            .padding(vertical = 6.dp),
                        placeholder = "Add a comment...",
                        enabled = !isSubmitting,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send,
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { if (canSubmit) onSubmit() }
                        )
                    )

                    // Send button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSubmit) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .then(
                                if (canSubmit && !isSubmitting)
                                    Modifier.clickable(onClick = onSubmit)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            FaIcon(
                                icon = FaIcons.ArrowUp,
                                size = 14.dp,
                                tint = if (canSubmit)
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
                text = "Comment must be $maxLength characters or less.",
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
