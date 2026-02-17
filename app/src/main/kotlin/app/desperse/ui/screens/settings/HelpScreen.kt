package app.desperse.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun sendEmail(email: String) {
        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        FaIcon(FaIcons.ArrowLeft, size = 20.dp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Find answers, support, and important information.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 2x2 Grid of help cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Support Card
                HelpCard(
                    title = "Support",
                    description = "Get help with bugs, account issues, or general questions.",
                    icon = FaIcons.CircleQuestion,
                    modifier = Modifier.weight(1f)
                ) {
                    HelpLinkRow(
                        text = "support@desperse.app",
                        icon = FaIcons.Envelope,
                        onClick = { sendEmail("support@desperse.app") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HelpLinkRow(
                        text = "@DesperseApp",
                        icon = FaIcons.X,
                        iconStyle = FaIconStyle.Brands,
                        onClick = { openUrl("https://x.com/DesperseApp") }
                    )
                }

                // Fees Card
                HelpCard(
                    title = "Fees & Pricing",
                    description = "Understand platform fees, minting costs, and pricing.",
                    icon = FaIcons.Tag,
                    modifier = Modifier.weight(1f)
                ) {
                    HelpLinkRow(
                        text = "View fees",
                        icon = FaIcons.ArrowRight,
                        onClick = { openUrl("https://desperse.com/fees") }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // About Card
                HelpCard(
                    title = "About Desperse",
                    description = "Learn what Desperse is, who it's for, and how it works.",
                    icon = FaIcons.CircleInfo,
                    modifier = Modifier.weight(1f)
                ) {
                    HelpLinkRow(
                        text = "Visit about page",
                        icon = FaIcons.ArrowRight,
                        onClick = { openUrl("https://desperse.com/about") }
                    )
                }

                // Changelog Card
                HelpCard(
                    title = "Changelog",
                    description = "See what's new and what we've been working on.",
                    icon = FaIcons.List,
                    modifier = Modifier.weight(1f)
                ) {
                    HelpLinkRow(
                        text = "View changelog",
                        icon = FaIcons.ArrowRight,
                        onClick = { openUrl("https://desperse.com/changelog") }
                    )
                }
            }

            // Legal Card (full width)
            HelpCard(
                title = "Legal",
                description = "Review our terms and privacy policy.",
                icon = FaIcons.Shield,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HelpLinkRow(
                        text = "Terms of Service",
                        icon = FaIcons.ArrowRight,
                        onClick = { openUrl("https://desperse.com/terms") },
                        modifier = Modifier.weight(1f)
                    )
                    HelpLinkRow(
                        text = "Privacy Policy",
                        icon = FaIcons.ArrowRight,
                        onClick = { openUrl("https://desperse.com/privacy") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpCard(
    title: String,
    description: String,
    icon: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                FaIcon(
                    icon = icon,
                    size = 16.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = FaIconStyle.Regular
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
private fun HelpLinkRow(
    text: String,
    icon: String,
    onClick: () -> Unit,
    iconStyle: FaIconStyle = FaIconStyle.Regular,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            FaIcon(
                icon = icon,
                size = 12.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                style = iconStyle
            )
        }
    }
}
