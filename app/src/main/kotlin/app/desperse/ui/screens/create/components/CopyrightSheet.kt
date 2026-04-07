package app.desperse.ui.screens.create.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.desperse.ui.components.DesperseBottomSheet
import app.desperse.ui.screens.settings.LICENSE_PRESETS
import app.desperse.ui.screens.settings.SUGGESTED_STATEMENTS
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSpacing

/**
 * Bottom sheet for editing copyright and licensing fields.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyrightSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    licensePreset: String?,
    licenseCustom: String,
    copyrightHolder: String,
    copyrightRights: String,
    enabled: Boolean,
    accentColor: Color,
    onPresetChange: (String?) -> Unit,
    onCustomChange: (String) -> Unit,
    onHolderChange: (String) -> Unit,
    onRightsChange: (String) -> Unit
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
                text = "Copyright & Licensing",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Set copyright and licensing for this post",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // License dropdown
            var expanded by remember { mutableStateOf(false) }
            val displayText = licensePreset ?: "None"

            Column(verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)) {
                Text(
                    text = "License Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                ExposedDropdownMenuBox(
                    expanded = expanded && enabled,
                    onExpandedChange = { if (enabled) expanded = it }
                ) {
                    OutlinedTextField(
                        value = displayText,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(DesperseRadius.sm)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded && enabled,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                onPresetChange(null)
                                expanded = false
                            }
                        )
                        LICENSE_PRESETS.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset) },
                                onClick = {
                                    onPresetChange(preset)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Custom license text (only when CUSTOM)
            if (licensePreset == "CUSTOM") {
                Column(verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)) {
                    Text(
                        text = "Custom License",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedTextField(
                        value = licenseCustom,
                        onValueChange = onCustomChange,
                        placeholder = { Text("e.g., MIT, Apache-2.0") },
                        modifier = Modifier.fillMaxWidth(),
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
                            "${licenseCustom.length} / 100",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Rights Holder
            Column(verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)) {
                Text(
                    text = "Rights Holder",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = copyrightHolder,
                    onValueChange = onHolderChange,
                    placeholder = { Text("Legal name or entity") },
                    modifier = Modifier.fillMaxWidth(),
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
                        "${copyrightHolder.length} / 200",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Rights Statement
            Column(verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)) {
                Text(
                    text = "Rights Statement",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = copyrightRights,
                    onValueChange = onRightsChange,
                    placeholder = { Text("Describe usage rights and restrictions") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    enabled = enabled,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(DesperseRadius.sm)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "${copyrightRights.length} / 1000",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Suggested statement button
                val suggestedStatement = licensePreset?.let { SUGGESTED_STATEMENTS[it] }
                if (suggestedStatement != null && copyrightRights != suggestedStatement && enabled) {
                    TextButton(
                        onClick = { onRightsChange(suggestedStatement) },
                        colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                    ) {
                        Text("Use suggested statement")
                    }
                }
            }

            // Done button
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
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
