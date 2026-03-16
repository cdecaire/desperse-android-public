package app.desperse.ui.components

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.desperse.R
import app.desperse.data.dto.response.TokenBalance
import app.desperse.ui.screens.wallet.SendState
import app.desperse.ui.screens.wallet.SendViewModel
import app.desperse.ui.theme.DesperseColors
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.toneSuccess
import kotlinx.coroutines.delay

private val BASE58_REGEX = Regex("^[1-9A-HJ-NP-Za-km-z]{32,44}$")

// SOL reserve kept for fees when using "Max" on SOL sends
private const val SOL_FEE_RESERVE = 0.005

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendSheet(
    token: TokenBalance,
    asset: String,
    senderAddress: String,
    sendState: SendState,
    onSend: (toAddress: String, amount: String) -> Unit,
    onDismiss: () -> Unit
) {
    var toAddress by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    val context = LocalContext.current

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val balance = token.balance
    val maxDecimals = token.decimals
    val symbol = token.symbol.uppercase()

    // UI validation
    val isValidAddress = toAddress.isNotBlank() && BASE58_REGEX.matches(toAddress.trim())
    val isSelfSend = toAddress.trim() == senderAddress
    val exceedsBalance = amount > balance
    val tooManyDecimals = amountText.contains(".") &&
        (amountText.split(".").getOrNull(1)?.length ?: 0) > maxDecimals

    val isInProgress = sendState is SendState.Preparing ||
        sendState is SendState.Signing ||
        sendState is SendState.Submitting

    val canSend = isValidAddress && !isSelfSend && amount > 0 && !exceedsBalance &&
        !tooManyDecimals && !isInProgress && sendState !is SendState.Submitted

    // Validation error text
    val validationError = when {
        toAddress.isNotBlank() && !isValidAddress -> "Invalid Solana address"
        isSelfSend -> "Cannot send to your own address"
        exceedsBalance -> "Amount exceeds balance"
        tooManyDecimals -> "Maximum $maxDecimals decimal places for $symbol"
        else -> null
    }

    // Auto-dismiss on success
    LaunchedEffect(sendState) {
        if (sendState is SendState.Submitted) {
            delay(1500)
            onDismiss()
        }
    }

    DesperseBottomSheet(
        isOpen = true,
        onDismiss = onDismiss,
        onDismissRequest = {
            if (!isInProgress) onDismiss()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesperseSpacing.lg)
                .padding(bottom = DesperseSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: token icon + title
            val localIconRes = remember(symbol) {
                when (symbol) {
                    "SOL" -> R.drawable.ic_token_sol
                    "USDC" -> R.drawable.ic_token_usdc
                    "SKR" -> R.drawable.ic_token_skr
                    else -> null
                }
            }

            if (localIconRes != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (symbol == "SOL") {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = localIconRes),
                                contentDescription = symbol,
                                modifier = Modifier.size(28.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    } else {
                        Image(
                            painter = painterResource(id = localIconRes),
                            contentDescription = symbol,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesperseSpacing.md))

            Text(
                text = "Send $symbol",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.xs))

            // From address
            Text(
                text = "From: ${senderAddress.take(6)}...${senderAddress.takeLast(4)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.lg))

            // Success state
            if (sendState is SendState.Submitted) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = DesperseSpacing.xl)
                ) {
                    FaIcon(
                        icon = FaIcons.CircleCheck,
                        size = 48.dp,
                        tint = toneSuccess(),
                        style = FaIconStyle.Solid
                    )
                    Spacer(modifier = Modifier.height(DesperseSpacing.md))
                    Text(
                        text = "Transaction submitted",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = toneSuccess()
                    )
                }
                return@Column
            }

            // Recipient address field
            OutlinedTextField(
                value = toAddress,
                onValueChange = { toAddress = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Recipient address") },
                trailingIcon = {
                    Text(
                        text = "Paste",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable(enabled = !isInProgress) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    toAddress = clip.getItemAt(0).text?.toString()?.trim() ?: ""
                                }
                            }
                            .padding(horizontal = DesperseSpacing.sm)
                    )
                },
                singleLine = true,
                enabled = !isInProgress,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.md))

            // Amount field
            OutlinedTextField(
                value = amountText,
                onValueChange = { text ->
                    if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d*$"))) {
                        amountText = text
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Amount") },
                suffix = { Text(symbol, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = !isInProgress,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.sm))

            // Balance + Max row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Available: ${formatTokenBalance(balance, maxDecimals)} $symbol",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (exceedsBalance) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Max",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(enabled = !isInProgress) {
                            val maxAmount = if (asset == "sol") {
                                (balance - SOL_FEE_RESERVE).coerceAtLeast(0.0)
                            } else {
                                balance
                            }
                            amountText = formatTokenBalance(maxAmount, maxDecimals)
                        }
                        .padding(vertical = DesperseSpacing.xs)
                )
            }

            if (asset == "sol" && amountText.isNotBlank() && balance > 0 && balance - amount < SOL_FEE_RESERVE && !exceedsBalance) {
                Text(
                    text = "A small SOL balance is kept for network fees",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Validation error
            if (validationError != null) {
                Spacer(modifier = Modifier.height(DesperseSpacing.xs))
                Text(
                    text = validationError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Server error
            if (sendState is SendState.Failed) {
                Spacer(modifier = Modifier.height(DesperseSpacing.sm))
                Text(
                    text = sendState.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(DesperseSpacing.sm))

            // Irreversibility warning
            Text(
                text = "Transfers cannot be reversed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.lg))

            // Send button
            DesperseButton(
                onClick = { if (canSend) onSend(toAddress.trim(), amountText) },
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
                        text = when (sendState) {
                            is SendState.Preparing -> "Preparing..."
                            is SendState.Signing -> "Approve in wallet..."
                            is SendState.Submitting -> "Submitting..."
                            is SendState.Idle, is SendState.Submitted, is SendState.Failed -> "Sending..."
                        }
                    )
                } else {
                    Text(
                        text = if (amount > 0) "Send ${formatTokenBalance(amount, maxDecimals)} $symbol"
                        else "Send"
                    )
                }
            }

            // Cancel button
            if (!isInProgress || sendState is SendState.Preparing) {
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

private fun formatTokenBalance(amount: Double, decimals: Int): String {
    if (amount == 0.0) return "0"
    // Show up to the asset's decimal places, but trim trailing zeros
    val formatted = String.format("%.${decimals}f", amount)
    return formatted.trimEnd('0').trimEnd('.')
}
