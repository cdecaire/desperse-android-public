package app.desperse.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

/**
 * MentionText - Renders text with @mentions as clickable links
 *
 * Features:
 * - Parses @username patterns in text
 * - Renders mentions with primary color styling
 * - Calls onMentionClick with the username when tapped
 */

// Match @username patterns: @[a-z0-9_.-]{1,32}
private val MENTION_REGEX = Regex("""(^|[\s(\[{"'])(@[a-zA-Z0-9_.-]{1,32})(?=$|[^a-zA-Z0-9_.-])""")

private const val MENTION_TAG = "mention"

/**
 * Parses text and creates an AnnotatedString with clickable mention spans
 */
@Composable
fun parseMentionText(
    text: String,
    textColor: Color = LocalContentColor.current,
    mentionColor: Color = MaterialTheme.colorScheme.primary
): AnnotatedString {
    return remember(text, textColor, mentionColor) {
        buildAnnotatedString {
            var lastIndex = 0

            MENTION_REGEX.findAll(text).forEach { matchResult ->
                val precedingChar = matchResult.groupValues[1]
                val mention = matchResult.groupValues[2]
                val matchStart = matchResult.range.first + precedingChar.length

                // Add text before this match
                if (matchResult.range.first > lastIndex) {
                    withStyle(SpanStyle(color = textColor)) {
                        append(text.substring(lastIndex, matchResult.range.first))
                    }
                }

                // Add preceding character
                if (precedingChar.isNotEmpty()) {
                    withStyle(SpanStyle(color = textColor)) {
                        append(precedingChar)
                    }
                }

                // Add the mention with styling and annotation
                val usernameSlug = mention.substring(1).lowercase() // Remove @ and lowercase
                pushStringAnnotation(tag = MENTION_TAG, annotation = usernameSlug)
                withStyle(SpanStyle(color = mentionColor, fontWeight = FontWeight.Medium)) {
                    append(mention)
                }
                pop()

                lastIndex = matchStart + mention.length
            }

            // Add remaining text
            if (lastIndex < text.length) {
                withStyle(SpanStyle(color = textColor)) {
                    append(text.substring(lastIndex))
                }
            }
        }
    }
}

/**
 * MentionText - Text component that renders @mentions as clickable
 *
 * @param text The text containing @mentions
 * @param onMentionClick Callback when a mention is clicked, receives the username (without @)
 * @param modifier Modifier for the text
 * @param style TextStyle for the text
 * @param textColor Color for regular text
 * @param mentionColor Color for mentions
 * @param maxLines Maximum number of lines
 * @param overflow Text overflow behavior
 */
@Composable
fun MentionText(
    text: String,
    onMentionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    textColor: Color = LocalContentColor.current,
    mentionColor: Color = MaterialTheme.colorScheme.primary,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val annotatedText = parseMentionText(
        text = text,
        textColor = textColor,
        mentionColor = mentionColor
    )

    ClickableText(
        text = annotatedText,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        onClick = { offset ->
            annotatedText
                .getStringAnnotations(tag = MENTION_TAG, start = offset, end = offset)
                .firstOrNull()
                ?.let { annotation ->
                    onMentionClick(annotation.item)
                }
        }
    )
}

/**
 * Check if text contains any @mentions
 */
fun containsMentions(text: String): Boolean {
    return MENTION_REGEX.containsMatchIn(text)
}

/**
 * Extract all @mentions from text (returns usernames without @)
 */
fun extractMentions(text: String): List<String> {
    return MENTION_REGEX.findAll(text)
        .map { it.groupValues[2].substring(1).lowercase() }
        .distinct()
        .toList()
}
