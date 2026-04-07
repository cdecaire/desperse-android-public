package app.desperse.ui.screens.create.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIcons
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.toneCollectible

/**
 * Collapsible "Optional Details" section with symbol and royalties fields.
 */
@Composable
fun NftMetadataCard(
    nftSymbol: String,
    royalties: String,
    nftFieldsEditable: Boolean,
    onSymbolChange: (String) -> Unit,
    onRoyaltiesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val collectibleColor = toneCollectible()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = collectibleColor,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        cursorColor = collectibleColor,
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Collapsible header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = DesperseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Optional Details",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            FaIcon(
                icon = if (isExpanded) FaIcons.ChevronUp else FaIcons.ChevronDown,
                size = 14.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(DesperseSpacing.md))

                Text(
                    "Optional metadata shown in wallets and marketplaces.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(DesperseSpacing.lg))

                Column(
                    verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
                ) {
                    // Symbol
                    Column(verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)) {
                        Text(
                            "Symbol",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = nftSymbol,
                            onValueChange = onSymbolChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("DSPRS") },
                            singleLine = true,
                            enabled = nftFieldsEditable,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(DesperseRadius.sm)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                "${nftSymbol.length} / 10",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Royalties
                    Column(verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)) {
                        Text(
                            "Royalties",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = royalties,
                            onValueChange = onRoyaltiesChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("0.00") },
                            suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            enabled = nftFieldsEditable,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(DesperseRadius.sm)
                        )
                        Text(
                            "0-10% creator royalties on resales",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
