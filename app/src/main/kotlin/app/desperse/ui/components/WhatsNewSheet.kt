package app.desperse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.desperse.BuildConfig
import app.desperse.ui.theme.DesperseSpacing

private const val MAX_RECENT_VERSIONS = 2

/**
 * Changelog content for a specific version.
 */
data class WhatsNewContent(
    val version: String,
    val items: List<String>
)

/**
 * Changelog entries keyed by versionCode.
 * Add a new entry here each time you release a version.
 */
val changelogs = mapOf(
    7 to WhatsNewContent(
        version = "1.0.6",
        items = listOf(
            "Permanent storage on Arweave for editions",
            "Manage Arweave storage credits in settings",
            "Improved analytics and notification tracking",
        )
    ),
    6 to WhatsNewContent(
        version = "1.0.5",
        items = listOf(
            "Added support for Backpack wallet",
            "Add social links to profiles (X and Instagram)",
            "In-app browser for external links",
            "New help & changelog screens",
            "What's new notification after updates",
        )
    ),
    5 to WhatsNewContent(
        version = "1.0.4",
        items = listOf(
            "Added support for Seeker wallet",
            "Added support for Jupiter wallet",
            "Improved Phantom & Solflare sign in",
        )
    )
)

/**
 * "What's New" bottom sheet shown after an app update.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val recentVersions = remember {
        changelogs.entries
            .sortedByDescending { it.key }
            .take(MAX_RECENT_VERSIONS)
    }

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
                .padding(horizontal = DesperseSpacing.lg)
                .padding(bottom = DesperseSpacing.xxl)
        ) {
            // Header
            Text(
                text = "What's new",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.lg))

            if (recentVersions.isNotEmpty()) {
                recentVersions.forEachIndexed { index, (_, content) ->
                    // Version header
                    Text(
                        text = "v${content.version}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(DesperseSpacing.sm))

                    Column(verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)) {
                        content.items.forEach { item ->
                            ChangelogItem(text = item)
                        }
                    }

                    if (index < recentVersions.lastIndex) {
                        Spacer(modifier = Modifier.height(DesperseSpacing.lg))
                    }
                }
            } else {
                Text(
                    text = "Bug fixes and improvements.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(DesperseSpacing.xl))

            // Dismiss button
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Got it")
            }
        }
    }
}

@Composable
private fun ChangelogItem(text: String) {
    Text(
        text = "\u2014 $text",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}
