package app.desperse.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.desperse.R
import app.desperse.ui.components.media.ImageContext
import app.desperse.ui.components.media.ImageOptimization
import app.desperse.ui.theme.DesperseColors
import app.desperse.ui.theme.DesperseSpacing
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay

/**
 * Tip state machine for the tip flow.
 * States: idle → preparing → signing → confirming → success
 *                ↘──────────────→ failed ←──────────┘
 */
sealed class TipState {
    data object Idle : TipState()
    data object Preparing : TipState()
    data object Signing : TipState()
    data class Confirming(val tipId: String) : TipState()
    data object Success : TipState()
    data class Failed(val error: String, val canRetry: Boolean = true) : TipState()
}

private val PRESET_AMOUNTS = listOf(50, 100, 250, 500)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipSheet(
    creatorDisplayName: String?,
    creatorAvatarUrl: String?,
    tipState: TipState,
    skrBalance: Double?,
    defaultAmount: Int? = null,
    onSendTip: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var selectedPreset by remember { mutableStateOf(defaultAmount ?: PRESET_AMOUNTS[0]) }
    var useCustomAmount by remember { mutableStateOf(false) }
    var customAmountText by remember { mutableStateOf("") }

    val currentAmount = if (useCustomAmount) {
        customAmountText.toDoubleOrNull() ?: 0.0
    } else {
        selectedPreset.toDouble()
    }

    val isInProgress = tipState is TipState.Preparing ||
        tipState is TipState.Signing ||
        tipState is TipState.Confirming

    val hasInsufficientFunds = skrBalance != null && currentAmount > skrBalance
    val canSend = currentAmount > 0 && !isInProgress && !hasInsufficientFunds &&
        tipState !is TipState.Success

    // Auto-dismiss on success
    LaunchedEffect(tipState) {
        if (tipState is TipState.Success) {
            delay(1500)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isInProgress) onDismiss()
        },
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
                .padding(horizontal = DesperseSpacing.lg)
                .padding(bottom = DesperseSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: SKR icon + creator avatar overlapping
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.ic_token_skr),
                    contentDescription = "SKR",
                    modifier = Modifier
                        .offset(x = (-17).dp)
                        .size(44.dp)
                        .clip(CircleShape)
                )
                if (creatorAvatarUrl != null) {
                    val optimizedAvatar = remember(creatorAvatarUrl) {
                        ImageOptimization.getOptimizedUrlForContext(
                            creatorAvatarUrl, ImageContext.AVATAR
                        )
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(optimizedAvatar)
                            .crossfade(true)
                            .build(),
                        contentDescription = creatorDisplayName,
                        modifier = Modifier
                            .offset(x = 17.dp)
                            .size(44.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesperseSpacing.md))

            Text(
                text = "Tip ${creatorDisplayName ?: "Creator"}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.xs))

            Text(
                text = "Send Seeker tokens to show your appreciation",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.lg))

            // Success state
            if (tipState is TipState.Success) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = DesperseSpacing.xl)
                ) {
                    FaIcon(
                        icon = FaIcons.CircleCheck,
                        size = 48.dp,
                        tint = Color(0xFF4CAF50),
                        style = FaIconStyle.Solid
                    )
                    Spacer(modifier = Modifier.height(DesperseSpacing.md))
                    Text(
                        text = "Tip sent!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                return@Column
            }

            // Preset amount pills
            if (!useCustomAmount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
                ) {
                    PRESET_AMOUNTS.forEach { amount ->
                        val isSelected = selectedPreset == amount
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable(enabled = !isInProgress) {
                                    selectedPreset = amount
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$amount",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Custom amount input
            if (useCustomAmount) {
                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = { text ->
                        // Allow digits and a single decimal point
                        if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d*$"))) {
                            customAmountText = text
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter amount") },
                    suffix = { Text("SKR", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = !isInProgress,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(DesperseSpacing.sm))

            // Custom/preset toggle
            Text(
                text = if (useCustomAmount) "Use preset amounts" else "Custom amount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(enabled = !isInProgress) {
                        useCustomAmount = !useCustomAmount
                        if (!useCustomAmount) customAmountText = ""
                    }
                    .padding(vertical = DesperseSpacing.xs)
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.md))

            // Balance display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_token_skr),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (skrBalance != null) {
                        "Balance: ${formatSkrAmount(skrBalance)} SKR"
                    } else {
                        "Balance: --"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasInsufficientFunds) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (hasInsufficientFunds) {
                Spacer(modifier = Modifier.height(DesperseSpacing.xs))
                Text(
                    text = "Insufficient SKR balance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Error state
            if (tipState is TipState.Failed) {
                Spacer(modifier = Modifier.height(DesperseSpacing.sm))
                Text(
                    text = tipState.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(DesperseSpacing.lg))

            // Send button - high contrast white bg + black text
            DesperseButton(
                onClick = { if (canSend) onSendTip(currentAmount) },
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Default,
                enabled = canSend || isInProgress
            ) {
                if (isInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = DesperseColors.Zinc950
                    )
                    Spacer(modifier = Modifier.width(DesperseSpacing.sm))
                    Text(
                        text = when (tipState) {
                            is TipState.Preparing -> "Preparing..."
                            is TipState.Signing -> "Signing..."
                            is TipState.Confirming -> "Confirming..."
                            else -> "Sending..."
                        }
                    )
                } else {
                    Text(
                        text = if (currentAmount > 0) "Send ${formatSkrAmount(currentAmount)} SKR"
                        else "Send Tip"
                    )
                }
            }

            // Cancel button (only when not in signing/confirming)
            if (!isInProgress || tipState is TipState.Preparing) {
                Spacer(modifier = Modifier.height(DesperseSpacing.sm))
                DesperseTextButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = ButtonVariant.Ghost,
                    enabled = !isInProgress,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun formatSkrAmount(amount: Double): String {
    return if (amount == amount.toLong().toDouble()) {
        amount.toLong().toString()
    } else {
        String.format("%.2f", amount)
    }
}
