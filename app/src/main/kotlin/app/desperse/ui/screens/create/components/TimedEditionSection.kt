package app.desperse.ui.screens.create.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.DesperseTones
import app.desperse.ui.theme.toneDestructive
import app.desperse.ui.theme.toneEdition
import app.desperse.ui.theme.toneSuccess
import app.desperse.ui.util.MintWindowUtils
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

/**
 * Timed Edition section — matches web EditionOptions > MintWindowSection layout.
 *
 * Layout:
 *   [Timed Edition]  ........  [Switch]
 *   [Launch type ▼]  [Set duration ▼]      ← side-by-side dropdowns
 *   ┌─────────────────────────────────────┐
 *   │ Start schedule      Calculated end  │  ← detail panel
 *   │ [On publish / date] → [End date]    │
 *   └─────────────────────────────────────┘
 *   Info note
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimedEditionSection(
    enabled: Boolean,
    startMode: String, // "now" | "scheduled"
    startTime: Long?, // epoch millis
    durationHours: Double?,
    isLocked: Boolean,
    onToggle: (Boolean) -> Unit,
    onStartModeChange: (String) -> Unit,
    onStartTimeChange: (Long) -> Unit,
    onDurationChange: (Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    val editionColor = toneEdition()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
    ) {
        // Toggle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Timed Edition",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                enabled = !isLocked
            )
        }

        if (!enabled) return@Column

        // Locked state: show read-only summary (matches web locked state)
        if (isLocked) {
            LockedSummary(startMode, startTime, durationHours)
            return@Column
        }

        // --- Editable state ---

        // Launch type + Duration — side-by-side dropdowns
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
        ) {
            // Launch type dropdown
            DropdownSelector(
                label = "Launch type",
                value = if (startMode == "now") "Start Now" else "Scheduled Launch",
                modifier = Modifier.weight(1f)
            ) { onDismiss ->
                DropdownMenuItem(
                    text = { Text("Start Now") },
                    onClick = { onStartModeChange("now"); onDismiss() }
                )
                DropdownMenuItem(
                    text = { Text("Scheduled Launch") },
                    onClick = { onStartModeChange("scheduled"); onDismiss() }
                )
            }

            // Duration dropdown
            var isCustomMode by remember {
                mutableStateOf(durationHours != null && durationHours !in MintWindowUtils.DURATION_PRESETS)
            }

            if (isCustomMode) {
                // Custom hours input
                CustomDurationInput(
                    durationHours = durationHours,
                    editionColor = editionColor,
                    onDurationChange = onDurationChange,
                    onBackToPresets = {
                        isCustomMode = false
                        onDurationChange(null)
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                val durationLabel = when {
                    durationHours == null -> "Select..."
                    durationHours in MintWindowUtils.DURATION_PRESETS -> {
                        val idx = MintWindowUtils.DURATION_PRESETS.indexOf(durationHours)
                        MintWindowUtils.DURATION_PRESET_LABELS[idx]
                    }
                    else -> "${durationHours}h"
                }
                DropdownSelector(
                    label = "Set duration",
                    value = durationLabel,
                    modifier = Modifier.weight(1f)
                ) { onDismiss ->
                    MintWindowUtils.DURATION_PRESETS.forEachIndexed { index, preset ->
                        DropdownMenuItem(
                            text = { Text(MintWindowUtils.DURATION_PRESET_LABELS[index]) },
                            onClick = { onDurationChange(preset); onDismiss() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Custom") },
                        onClick = { isCustomMode = true; onDurationChange(null); onDismiss() }
                    )
                }
            }
        }

        // Detail panel: Start schedule → Calculated end result
        DetailPanel(
            startMode = startMode,
            startTime = startTime,
            durationHours = durationHours,
            editionColor = editionColor,
            onStartTimeChange = onStartTimeChange
        )

        // Info note
        Text(
            "The system will automatically switch the listing status to \u2018Closed\u2019 once the end time is reached. Users will no longer be able to purchase or bid.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// === Locked summary (matches web: Start / End / Status rows) ===

@Composable
private fun LockedSummary(
    startMode: String,
    startTime: Long?,
    durationHours: Double?
) {
    val startLabel = if (startMode == "scheduled" && startTime != null) {
        MintWindowUtils.formatDateTime(startTime)
    } else "On publish"

    val endLabel = if (durationHours != null && startTime != null) {
        val endMs = startTime + (durationHours * 3600_000).toLong()
        MintWindowUtils.formatDateTime(endMs)
    } else null

    val now = System.currentTimeMillis()
    val isEnded = if (durationHours != null && startTime != null) {
        now >= startTime + (durationHours * 3600_000).toLong()
    } else false
    val isActive = !isEnded && startTime != null && now >= startTime

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)
    ) {
        Text(
            "Timed Edition",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FaIcon(icon = FaIcons.Lock, size = 10.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, style = FaIconStyle.Regular)
                Text("Locked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(DesperseRadius.sm),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesperseSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Start:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(startLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
            if (endLabel != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("End:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(endLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Status:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    when {
                        isEnded -> "Ended"
                        isActive -> "Active"
                        else -> "Scheduled"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isEnded -> MaterialTheme.colorScheme.onSurfaceVariant
                        isActive -> toneSuccess()
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

// === Dropdown selector ===

@Composable
private fun DropdownSelector(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    content: @Composable (onDismiss: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                shape = RoundedCornerShape(DesperseRadius.sm),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (value == "Select...") MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                    FaIcon(FaIcons.ChevronDown, size = 12.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                content { expanded = false }
            }
        }
    }
}

// === Custom duration input with "hrs" suffix and X button ===

@Composable
private fun CustomDurationInput(
    durationHours: Double?,
    editionColor: Color,
    onDurationChange: (Double?) -> Unit,
    onBackToPresets: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(durationHours?.toString() ?: "") }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Set duration", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { v ->
                    val filtered = v.filter { it.isDigit() || it == '.' }
                    text = filtered
                    filtered.toDoubleOrNull()?.let { if (it >= 1) onDurationChange(it) }
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Hours") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = editionColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = editionColor
                ),
                shape = RoundedCornerShape(DesperseRadius.sm)
            )
            Text("hrs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBackToPresets)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                FaIcon(FaIcons.Xmark, size = 14.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, style = FaIconStyle.Regular)
            }
        }
    }
}

// === Detail panel: Start → End ===

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailPanel(
    startMode: String,
    startTime: Long?,
    durationHours: Double?,
    editionColor: Color,
    onStartTimeChange: (Long) -> Unit
) {
    // Compute preview
    val preview = remember(startMode, startTime, durationHours) {
        if (durationHours == null) return@remember null
        val startMs = when (startMode) {
            "scheduled" -> startTime ?: return@remember null
            else -> System.currentTimeMillis()
        }
        val endMs = startMs + (durationHours * 3600_000).toLong()
        Triple(
            MintWindowUtils.formatDateTime(startMs),
            MintWindowUtils.formatDateTime(endMs),
            endMs <= System.currentTimeMillis() // endInPast
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
    ) {
        // Start schedule
        Text("Start schedule", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        if (startMode == "now") {
            // "On publish" pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("On publish", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (preview != null) {
                        Text(
                            "~ ${preview.first}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Date/time picker
            ScheduleDatePicker(
                startTime = startTime,
                editionColor = editionColor,
                onStartTimeChange = onStartTimeChange
            )
        }

        // Arrow
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FaIcon(
                icon = FaIcons.ArrowDown,
                size = 14.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                style = FaIconStyle.Regular
            )
        }

        // Calculated end result
        Text("Calculated end result", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        if (preview != null) {
            val (_, endLabel, endInPast) = preview
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (endInPast) toneDestructive().copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.onBackground,
                border = if (endInPast) BorderStroke(1.dp, toneDestructive().copy(alpha = 0.2f)) else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        endLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (endInPast) toneDestructive() else MaterialTheme.colorScheme.background,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        if (endInPast) "End time is in the past" else "Sale auto-closes",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (endInPast) toneDestructive().copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Select a duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// === Schedule date picker button + dialogs ===

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDatePicker(
    startTime: Long?,
    editionColor: Color,
    onStartTimeChange: (Long) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDateMillis by remember { mutableStateOf(startTime) }

    val displayText = if (startTime != null) {
        MintWindowUtils.formatDateTime(startTime)
    } else "Pick start date"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDatePicker = true },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
        ) {
            FaIcon(icon = FaIcons.Calendar, size = 14.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, style = FaIconStyle.Regular)
            Text(
                displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (startTime != null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startTime ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply {
            if (startTime != null) timeInMillis = startTime
        }
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE)
        )

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val dateMs = pickedDateMillis ?: System.currentTimeMillis()
                    val localDate = Instant.ofEpochMilli(dateMs)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    val localTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    val combined = localDate.atTime(localTime)
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onStartTimeChange(combined)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}
