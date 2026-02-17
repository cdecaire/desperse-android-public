package app.desperse.ui.screens.create.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.toneCollectible

@Composable
fun NftMetadataCard(
    nftSymbol: String,
    royalties: String,
    isMutable: Boolean,
    nftFieldsEditable: Boolean,
    mutabilityEditable: Boolean,
    onSymbolChange: (String) -> Unit,
    onRoyaltiesChange: (String) -> Unit,
    onMutableChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val collectibleColor = toneCollectible()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = collectibleColor,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        cursorColor = collectibleColor,
        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
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
                "Additional Details",
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
            Column(
                verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
            ) {
                // Symbol
                OutlinedTextField(
                    value = nftSymbol,
                    onValueChange = onSymbolChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Symbol") },
                    placeholder = { Text("DSPRS") },
                    singleLine = true,
                    enabled = nftFieldsEditable,
                    colors = textFieldColors,
                    supportingText = { Text("Max 10 characters", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )

                // Royalties
                OutlinedTextField(
                    value = royalties,
                    onValueChange = onRoyaltiesChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Royalties (%)") },
                    placeholder = { Text("5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = nftFieldsEditable,
                    colors = textFieldColors,
                    supportingText = { Text("0-10% creator royalties on resales", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )

                // Mutable toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mutable Metadata", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (isMutable) "NFT metadata can be updated after minting"
                            else "NFT metadata will be frozen after minting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isMutable,
                        onCheckedChange = onMutableChange,
                        enabled = mutabilityEditable,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = collectibleColor,
                            checkedTrackColor = collectibleColor.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }
}
