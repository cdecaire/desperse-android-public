package app.desperse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class UserRole {
    Admin,
    Moderator;

    val label: String
        get() = when (this) {
            Admin -> "Admin"
            Moderator -> "Moderator"
        }

    val accessibilityLabel: String
        get() = when (this) {
            Admin -> "Official Desperse account"
            Moderator -> "Desperse moderator"
        }

    val icon: String
        get() = when (this) {
            Admin -> FaIcons.CircleCheck
            Moderator -> FaIcons.ShieldCheck
        }
}

fun parseUserRole(role: String?): UserRole? = when (role) {
    "admin" -> UserRole.Admin
    "moderator" -> UserRole.Moderator
    else -> null
}

/**
 * Inline icon badge for compact contexts (post header, comment author).
 * Renders nothing when role is null/unknown.
 */
@Composable
fun RoleBadgeInline(
    role: String?,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val parsed = parseUserRole(role) ?: return
    FaIcon(
        icon = parsed.icon,
        size = size,
        tint = tint,
        style = FaIconStyle.Solid,
        contentDescription = parsed.accessibilityLabel,
        modifier = modifier
    )
}

/**
 * Text pill for prominent contexts (profile header).
 * Solid pill with inverse colors. Renders nothing when role is null/unknown.
 */
@Composable
fun RoleBadgePill(
    role: String?,
    modifier: Modifier = Modifier
) {
    val parsed = parseUserRole(role) ?: return
    Text(
        text = parsed.label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .semantics { contentDescription = parsed.accessibilityLabel }
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.onBackground)
            .padding(PaddingValues(horizontal = 8.dp, vertical = 2.dp))
    )
}
