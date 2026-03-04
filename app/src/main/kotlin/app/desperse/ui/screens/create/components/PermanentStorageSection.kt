package app.desperse.ui.screens.create.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.desperse.core.arweave.ArweaveUtils
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIcons
import app.desperse.ui.screens.create.ArweaveFundingState
import app.desperse.ui.theme.toneDestructive
import app.desperse.ui.theme.toneInfo
import app.desperse.ui.theme.toneStandard

@Composable
fun PermanentStorageSection(
    storageType: String,
    fundingState: ArweaveFundingState,
    isLocked: Boolean,
    isEditMode: Boolean,
    onStorageTypeChange: (String) -> Unit,
    onManageCreditsClick: () -> Unit,
    onRetryCheck: () -> Unit
) {
    val destructiveColor = toneDestructive()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Toggle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Permanent Storage",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Store media permanently on Arweave when first collector mints",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = storageType == "arweave",
                onCheckedChange = { checked ->
                    onStorageTypeChange(if (checked) "arweave" else "centralized")
                },
                enabled = !isLocked
            )
        }

        // Details when enabled
        if (storageType == "arweave") {
            Spacer(modifier = Modifier.height(12.dp))

            when (fundingState) {
                is ArweaveFundingState.NotChecked -> {
                    // Shouldn't normally happen when toggle is on
                }

                is ArweaveFundingState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Checking storage credits...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is ArweaveFundingState.Loaded -> {
                    FundingDetails(
                        state = fundingState,
                        onManageCreditsClick = onManageCreditsClick
                    )
                }

                is ArweaveFundingState.Error -> {
                    Surface(
                        color = destructiveColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FaIcon(
                                FaIcons.CircleExclamation,
                                size = 14.dp,
                                tint = destructiveColor
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = fundingState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = destructiveColor,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onRetryCheck) {
                                Text("Retry", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FundingDetails(
    state: ArweaveFundingState.Loaded,
    onManageCreditsClick: () -> Unit
) {
    val successColor = toneStandard()
    val destructiveColor = toneDestructive()
    val infoColor = toneInfo()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Est. cost
        val costUsdDisplay = when {
            state.estimatedCostUsd < 0.01 && state.estimatedCostUsd > 0 -> "<$0.01"
            else -> "$${String.format("%.2f", state.estimatedCostUsd)}"
        }
        FundingRow(
            label = "Est. cost",
            value = "${ArweaveUtils.formatCredits(state.estimatedCostWinc)} (~$costUsdDisplay)",
            valueColor = MaterialTheme.colorScheme.onSurface
        )

        // Shared with Desperse
        val sharedColor = if (state.hasSufficientSharedCredits) successColor else destructiveColor
        FundingRow(
            label = "Shared with Desperse",
            value = ArweaveUtils.formatCredits(state.sharedRemainingWinc),
            valueColor = sharedColor
        )

        // Authorization
        val (authColor, authText) = if (state.hasActiveApproval) {
            successColor to "Active"
        } else {
            destructiveColor to "Required"
        }
        FundingRow(
            label = "Authorization",
            value = authText,
            valueColor = authColor
        )

        // Link to manage credits if insufficient or unauthorized
        if (!state.hasSufficientSharedCredits || !state.hasActiveApproval) {
            TextButton(
                onClick = onManageCreditsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Manage Storage Credits",
                    style = MaterialTheme.typography.labelMedium,
                    color = infoColor
                )
                Spacer(Modifier.width(4.dp))
                FaIcon(
                    FaIcons.ArrowRight,
                    size = 12.dp,
                    tint = infoColor
                )
            }
        }
    }
}

@Composable
private fun FundingRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}
