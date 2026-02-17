package app.desperse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.desperse.data.dto.response.MentionUser
import app.desperse.ui.components.media.ImageContext
import app.desperse.ui.components.media.ImageOptimization
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/**
 * MentionTextField - TextField with @mention autocomplete
 *
 * Features:
 * - Detects @mentions while typing
 * - Shows dropdown with user suggestions
 * - Inserts selected user's username
 */

// Regex to find @mention being typed (partial or complete)
private val MENTION_TRIGGER_REGEX = Regex("""(^|[\s])@([a-zA-Z0-9_.-]{0,32})$""")

@Composable
fun MentionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: suspend (String) -> List<MentionUser>,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface
    ),
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = ImeAction.Default
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    var showAutocomplete by remember { mutableStateOf(false) }
    var mentionQuery by remember { mutableStateOf<String?>(null) }
    var mentionStartIndex by remember { mutableStateOf(0) }
    var users by remember { mutableStateOf<List<MentionUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    // Sync external value changes
    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }

    // Detect mention trigger
    LaunchedEffect(textFieldValue) {
        val text = textFieldValue.text
        val cursorPos = textFieldValue.selection.start

        // Get text up to cursor
        val textBeforeCursor = text.substring(0, cursorPos)

        // Check for mention trigger
        val match = MENTION_TRIGGER_REGEX.find(textBeforeCursor)
        if (match != null) {
            val query = match.groupValues[2]
            mentionQuery = query
            mentionStartIndex = match.range.first + match.groupValues[1].length
            showAutocomplete = true
            selectedIndex = 0

            // Search with debounce
            isLoading = true
            delay(150) // Debounce
            try {
                users = onSearch(query)
            } catch (e: Exception) {
                users = emptyList()
            }
            isLoading = false
        } else {
            showAutocomplete = false
            mentionQuery = null
            users = emptyList()
        }
    }

    // Insert mention
    fun insertMention(user: MentionUser) {
        val text = textFieldValue.text
        val mentionEnd = textFieldValue.selection.start

        // Build new text with mention
        val beforeMention = text.substring(0, mentionStartIndex)
        val afterMention = if (mentionEnd < text.length) text.substring(mentionEnd) else ""
        val mention = "@${user.usernameSlug} "
        val newText = beforeMention + mention + afterMention
        val newCursorPos = mentionStartIndex + mention.length

        textFieldValue = TextFieldValue(newText, TextRange(newCursorPos))
        onValueChange(newText)
        showAutocomplete = false
        users = emptyList()
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    onValueChange(newValue.text)
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
                enabled = enabled,
                textStyle = textStyle,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                maxLines = maxLines,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = textStyle.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            trailingIcon?.invoke()
        }

        // Autocomplete dropdown
        if (showAutocomplete && isFocused && (isLoading || users.isNotEmpty())) {
            Popup(
                alignment = Alignment.TopStart,
                properties = PopupProperties(focusable = false)
            ) {
                Surface(
                    modifier = Modifier
                        .width(280.dp)
                        .heightIn(max = 200.dp),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else if (users.isEmpty() && mentionQuery?.isNotEmpty() == true) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No users found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn {
                            items(users) { user ->
                                MentionUserItem(
                                    user = user,
                                    onClick = { insertMention(user) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MentionUserItem(
    user: MentionUser,
    onClick: () -> Unit
) {
    val displayName = user.displayName ?: user.usernameSlug

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        val avatarUrl = remember(user.avatarUrl) {
            user.avatarUrl?.let {
                ImageOptimization.getOptimizedUrlForContext(it, ImageContext.AVATAR)
            }
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                FaIcon(
                    icon = FaIcons.User,
                    size = 14.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Name and username
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Text(
                text = "@${user.usernameSlug}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
