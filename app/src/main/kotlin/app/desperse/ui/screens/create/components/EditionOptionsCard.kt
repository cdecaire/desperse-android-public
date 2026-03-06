package app.desperse.ui.screens.create.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIcons
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.toneDestructive
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
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        cursorColor = editionColor,
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
    ) {
        // Header row: label + conditional min-price warning
        val minAmount = if (currency == "SOL") 0.1 else 15.0
        val enteredAmount = priceDisplay.toDoubleOrNull()
        val isBelowMin = enteredAmount != null && enteredAmount > 0 && enteredAmount < minAmount
        val destructiveColor = toneDestructive()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Price per edition *",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isBelowMin) {
                val minLabel = if (currency == "SOL") "Min: 0.1 SOL" else "Min: \$15 USDC"
                Text(
                    minLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = destructiveColor
                )
            }
        }

        // Price input with currency dropdown inside
        var currencyMenuExpanded by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = priceDisplay,
            onValueChange = onPriceChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            enabled = pricingEnabled,
            isError = isBelowMin,
            colors = textFieldColors,
            shape = RoundedCornerShape(DesperseRadius.sm),
            trailingIcon = {
                Box {
                    Row(
                        modifier = Modifier
                            .clickable(enabled = pricingEnabled) {
                                currencyMenuExpanded = true
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            currency,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(4.dp))
                        FaIcon(
                            FaIcons.ChevronDown,
                            size = 12.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(DesperseSpacing.sm))
                    }

                    DropdownMenu(
                        expanded = currencyMenuExpanded,
                        onDismissRequest = { currencyMenuExpanded = false }
                    ) {
                        listOf("SOL", "USDC").forEach { cur ->
                            DropdownMenuItem(
                                text = { Text(cur) },
                                onClick = {
                                    onCurrencyChange(cur)
                                    currencyMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        )

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
                enabled = pricingEnabled
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
                colors = textFieldColors,
                shape = RoundedCornerShape(DesperseRadius.sm)
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
                    onCheckedChange = onProtectDownloadChange
                )
            }
        }
    }
}
