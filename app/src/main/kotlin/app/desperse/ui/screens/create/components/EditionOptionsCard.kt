package app.desperse.ui.screens.create.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.toneEdition

@Composable
fun EditionOptionsCard(
    priceDisplay: String,
    currency: String,
    maxSupplyEnabled: Boolean,
    maxSupplyDisplay: String,
    protectDownload: Boolean,
    showProtectDownload: Boolean,
    pricingEnabled: Boolean,
    onPriceChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onMaxSupplyToggle: (Boolean) -> Unit,
    onMaxSupplyChange: (String) -> Unit,
    onProtectDownloadChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val editionColor = toneEdition()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = editionColor,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        cursorColor = editionColor,
        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
    ) {
        Text(
            "Pricing",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Price + Currency
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
        ) {
            OutlinedTextField(
                value = priceDisplay,
                onValueChange = onPriceChange,
                modifier = Modifier.weight(1f),
                label = { Text("Price") },
                placeholder = { Text(if (currency == "SOL") "0.1" else "15") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = pricingEnabled,
                colors = textFieldColors,
                supportingText = {
                    val minPrice = if (currency == "SOL") "0.1 SOL" else "$15 USDC"
                    Text("Min: $minPrice", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )

            // Currency toggle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)
            ) {
                Text("Currency", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)) {
                    listOf("SOL", "USDC").forEach { cur ->
                        val isSelected = cur == currency
                        val shape = RoundedCornerShape(DesperseRadius.sm)
                        Surface(
                            modifier = Modifier
                                .clip(shape)
                                .border(
                                    1.dp,
                                    if (isSelected) editionColor else MaterialTheme.colorScheme.outline,
                                    shape
                                )
                                .clickable(enabled = pricingEnabled) { onCurrencyChange(cur) },
                            color = if (isSelected) editionColor.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                cur,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) editionColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    horizontal = DesperseSpacing.md,
                                    vertical = DesperseSpacing.sm
                                )
                            )
                        }
                    }
                }
            }
        }

        // Max supply toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Limited Supply", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (maxSupplyEnabled) "Set a maximum number of editions" else "Unlimited editions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = maxSupplyEnabled,
                onCheckedChange = onMaxSupplyToggle,
                enabled = pricingEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = editionColor,
                    checkedTrackColor = editionColor.copy(alpha = 0.3f)
                )
            )
        }

        if (maxSupplyEnabled) {
            OutlinedTextField(
                value = maxSupplyDisplay,
                onValueChange = onMaxSupplyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Max Supply") },
                placeholder = { Text("100") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = pricingEnabled,
                colors = textFieldColors
            )
        }

        // Protect download
        if (showProtectDownload) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Protect Download", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Gate file downloads behind purchase",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = protectDownload,
                    onCheckedChange = onProtectDownloadChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = editionColor,
                        checkedTrackColor = editionColor.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}
