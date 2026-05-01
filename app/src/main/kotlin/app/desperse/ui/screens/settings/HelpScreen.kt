package app.desperse.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.desperse.core.util.openInAppBrowser
import app.desperse.ui.components.DesperseBackButton
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onChangelogClick: () -> Unit = {}
) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.openInAppBrowser(url)
    }

    fun sendEmail(email: String) {
        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help") },
                navigationIcon = {
                    DesperseBackButton(onClick = onBack)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Find answers, support, and important information.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Support - Email
            HelpRow(
                icon = FaIcons.Envelope,
                label = "Email support",
                description = "Get help with bugs, account issues, or questions",
                onClick = { sendEmail("support@desperse.app") }
            )

            SettingsDivider()

            // Support - X/Twitter
            HelpRow(
                icon = FaIcons.X,
                iconStyle = FaIconStyle.Brands,
                label = "@DesperseApp",
                description = "Reach out on X",
                onClick = { openUrl("https://x.com/DesperseApp") }
            )

            SettingsDivider()

            // Fees & Pricing
            HelpRow(
                icon = FaIcons.Tag,
                label = "Fees & Pricing",
                description = "Platform fees, minting costs, and pricing",
                onClick = { openUrl("https://desperse.com/fees") }
            )

            SettingsDivider()

            // About
            HelpRow(
                icon = FaIcons.CircleInfo,
                label = "About Desperse",
                description = "What Desperse is, who it's for, and how it works",
                onClick = { openUrl("https://desperse.com/about") }
            )

            SettingsDivider()

            // Changelog
            HelpRow(
                icon = FaIcons.FileLines,
                label = "Changelog",
                description = "See what's new and what we've been working on",
                onClick = onChangelogClick
            )

            SettingsDivider()

            // Terms of Service
            HelpRow(
                icon = FaIcons.Shield,
                label = "Terms of Service",
                description = "Review our terms of use",
                onClick = { openUrl("https://desperse.com/terms") }
            )

            SettingsDivider()

            // Privacy Policy
            HelpRow(
                icon = FaIcons.Shield,
                iconStyle = FaIconStyle.Regular,
                label = "Privacy Policy",
                description = "Review our privacy policy",
                onClick = { openUrl("https://desperse.com/privacy") }
            )
        }
    }
}

@Composable
private fun HelpRow(
    icon: String,
    label: String,
    description: String,
    onClick: () -> Unit,
    iconStyle: FaIconStyle = FaIconStyle.Regular,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FaIcon(
                icon = icon,
                size = 18.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                style = iconStyle
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FaIcon(
                icon = FaIcons.ChevronRight,
                size = 14.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}
