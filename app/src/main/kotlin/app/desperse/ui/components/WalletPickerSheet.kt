package app.desperse.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.desperse.R
import app.desperse.ui.theme.DesperseSizes
import app.desperse.ui.theme.DesperseSpacing

/**
 * Represents an installed Solana wallet app on the device.
 */
data class InstalledWallet(
    val packageName: String,
    val displayName: String,
    val walletClientType: String  // e.g. "phantom", "solflare", "seeker"
)

/**
 * Wallet Picker Bottom Sheet
 *
 * Shows installed Solana wallet apps for the user to select from.
 * Used during SIWS (Sign In With Solana) login flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletPickerSheet(
    wallets: List<InstalledWallet>,
    onWalletSelected: (InstalledWallet) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

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
            // Title
            Text(
                text = "Select Wallet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    horizontal = DesperseSpacing.lg,
                    vertical = DesperseSpacing.md
                )
            )

            // Wallet list
            wallets.forEach { wallet ->
                WalletPickerItem(
                    wallet = wallet,
                    onClick = {
                        onWalletSelected(wallet)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun WalletPickerItem(
    wallet: InstalledWallet,
    onClick: () -> Unit
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
        WalletIcon(walletClientType = wallet.walletClientType)

        Spacer(modifier = Modifier.width(DesperseSpacing.md))

        Text(
            text = wallet.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        FaIcon(
            icon = FaIcons.ChevronRight,
            size = DesperseSizes.iconSm,
            style = FaIconStyle.Solid,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WalletIcon(walletClientType: String) {
    val iconSize = 36.dp
    val iconPadding = 6.dp

    when (walletClientType) {
        "phantom" -> {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(Color(0xFFAB9FF2)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_phantom),
                    contentDescription = "Phantom",
                    modifier = Modifier
                        .size(iconSize - iconPadding * 2),
                    contentScale = ContentScale.Fit
                )
            }
        }
        "solflare" -> {
            Image(
                painter = painterResource(R.drawable.ic_solflare),
                contentDescription = "Solflare",
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        "jupiter" -> {
            Image(
                painter = painterResource(R.drawable.ic_jupiter),
                contentDescription = "Jupiter",
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        "seeker" -> {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(Color(0xFF9945FF)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_seeker_s),
                    contentDescription = "Seeker",
                    modifier = Modifier
                        .size(iconSize - iconPadding * 2),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }
        "backpack" -> {
            Image(
                painter = painterResource(R.drawable.ic_backpack),
                contentDescription = "Backpack",
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        else -> {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                FaIcon(
                    icon = FaIcons.Wallet,
                    size = DesperseSizes.iconMd,
                    style = FaIconStyle.Solid,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
