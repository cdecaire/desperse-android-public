package app.desperse.ui.screens.create.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.desperse.ui.components.DesperseBottomSheet
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIcons
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSpacing

/**
 * Bottom sheet for editing NFT details: name, description, mutable toggle,
 * and optional details (symbol, royalties).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NftDetailsSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    nftName: String,
    nftDescription: String,
    isMutable: Boolean,
    nftSymbol: String,
    royalties: String,
    enabled: Boolean,
    mutabilityEditable: Boolean,
    accentColor: Color,
    isEdition: Boolean,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onMutableChange: (Boolean) -> Unit,
    onSymbolChange: (String) -> Unit,
    onRoyaltiesChange: (String) -> Unit
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = accentColor,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        cursorColor = accentColor,
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    DesperseBottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesperseSpacing.lg)
                .padding(bottom = DesperseSpacing.xxl)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DesperseSpacing.lg)
        ) {
            Text(
                text = "NFT Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // NFT Name
            Column(verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)) {
                Text(
                    text = "Name" + if (isEdition) " *" else "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = nftName,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter NFT name") },
                    singleLine = true,
                    enabled = enabled,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(DesperseRadius.sm)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "${nftName.length} / 32",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // NFT Description
            Column(verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)) {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = nftDescription,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    placeholder = { Text("Enter NFT description") },
                    minLines = 3,
                    maxLines = 5,
                    enabled = enabled,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(DesperseRadius.sm)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "${nftDescription.length} / 5000",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Mutable toggle
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Mutable",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Allow metadata updates after minting",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isMutable,
                    onCheckedChange = onMutableChange,
                    enabled = mutabilityEditable
                )
            }

            // Optional Details (collapsible)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            OptionalDetailsSection(
                nftSymbol = nftSymbol,
                royalties = royalties,
                enabled = enabled,
                textFieldColors = textFieldColors,
                onSymbolChange = onSymbolChange,
                onRoyaltiesChange = onRoyaltiesChange
            )

            // Done button
            Spacer(modifier = Modifier.height(DesperseSpacing.sm))
            androidx.compose.material3.Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    "Done",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun OptionalDetailsSection(
    nftSymbol: String,
    royalties: String,
    enabled: Boolean,
    textFieldColors: androidx.compose.material3.TextFieldColors,
    onSymbolChange: (String) -> Unit,
    onRoyaltiesChange: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = DesperseSpacing.xs),
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
            Column(verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)) {
                Spacer(modifier = Modifier.height(DesperseSpacing.xs))

                Text(
                    "Optional metadata shown in wallets and marketplaces.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
                        enabled = enabled,
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
                        enabled = enabled,
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
