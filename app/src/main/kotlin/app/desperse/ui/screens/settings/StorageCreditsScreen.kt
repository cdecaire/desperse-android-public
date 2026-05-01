package app.desperse.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.desperse.core.arweave.ArweaveUtils
import app.desperse.ui.components.DesperseBackButton
import app.desperse.core.arweave.CreditApproval
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.StorageCreditsSkeleton
import app.desperse.ui.components.rememberShimmerBrush
import app.desperse.ui.theme.toneDestructive
import app.desperse.ui.theme.toneStandard
import app.desperse.ui.theme.toneWarning
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageCreditsScreen(
    onBack: () -> Unit,
    viewModel: StorageCreditsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StorageCreditsEvent.TopUpComplete -> { /* snackbar handled by state */ }
                is StorageCreditsEvent.AuthorizationComplete -> { /* snackbar handled by state */ }
                is StorageCreditsEvent.RevocationComplete -> { /* snackbar handled by state */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Credits") },
                navigationIcon = {
                    DesperseBackButton(onClick = onBack)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            StorageCreditsSkeleton(
                brush = rememberShimmerBrush(),
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Error
                if (uiState.error != null) {
                    ErrorBanner(uiState.error!!) { viewModel.refresh() }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Pending Top-Up Recovery Banner
                if (uiState.pendingTopUpTx != null && uiState.topUpState !is TopUpState.Submitting) {
                    PendingTopUpBanner(
                        txId = uiState.pendingTopUpTx!!,
                        onRetry = { viewModel.retryPendingTopUp() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Balance Card
                BalanceCard(
                    sharedRemainingWinc = uiState.sharedRemainingWinc,
                    walletWinc = uiState.walletWinc,
                    rateWinc = uiState.rateWinc,
                    fiatRates = uiState.fiatRates
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Authorization
                AuthorizationSection(
                    hasActiveApproval = uiState.hasActiveApproval,
                    shareState = uiState.shareState,
                    revokeState = uiState.revokeState,
                    walletWinc = uiState.walletWinc,
                    onAuthorize = { wincAmount ->
                        activity?.let { viewModel.authorizeCredits(it, wincAmount) }
                    },
                    onRevoke = {
                        activity?.let { viewModel.revokeCredits(it) }
                    },
                    onDismissShareState = { viewModel.dismissShareState() },
                    onDismissRevokeState = { viewModel.dismissRevokeState() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Top-Up
                TopUpSection(
                    selectedAmount = uiState.topUpAmountSol,
                    topUpState = uiState.topUpState,
                    onSelectAmount = { viewModel.setTopUpAmount(it) },
                    onPurchase = {
                        activity?.let { viewModel.purchaseCredits(it) }
                    },
                    onDismiss = { viewModel.dismissTopUpState() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Approval History
                if (uiState.approvals.isNotEmpty()) {
                    ApprovalHistorySection(uiState.approvals)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BalanceCard(
    sharedRemainingWinc: String,
    walletWinc: String,
    rateWinc: String?,
    fiatRates: Map<String, Double>?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Balance",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Shared with Desperse (primary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Shared with Desperse",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ArweaveUtils.formatCredits(sharedRemainingWinc),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (rateWinc != null) {
                    Text(
                        text = ArweaveUtils.formatCreditsUsd(sharedRemainingWinc, rateWinc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Personal wallet balance (informational)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Personal Turbo Balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ArweaveUtils.formatCredits(walletWinc),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (rateWinc != null) {
                    Text(
                        text = ArweaveUtils.formatCreditsUsd(walletWinc, rateWinc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthorizationSection(
    hasActiveApproval: Boolean,
    shareState: ShareState,
    revokeState: RevokeState,
    walletWinc: String,
    onAuthorize: (wincAmount: String) -> Unit,
    onRevoke: () -> Unit,
    onDismissShareState: () -> Unit,
    onDismissRevokeState: () -> Unit
) {
    val successColor = toneStandard()
    val warningColor = toneWarning()
    val standardColor = toneStandard()
    val destructiveColor = toneDestructive()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Authorization",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Status chip
                val (chipColor, chipText) = if (hasActiveApproval) {
                    successColor to "Active"
                } else {
                    warningColor to "Required"
                }
                Surface(
                    color = chipColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = chipText,
                        style = MaterialTheme.typography.labelSmall,
                        color = chipColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Authorize Desperse to use your Turbo credits for permanent storage uploads when collectors mint your editions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            if (hasActiveApproval) {
                OutlinedButton(
                    onClick = onRevoke,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = revokeState is RevokeState.Idle || revokeState is RevokeState.Failed,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = destructiveColor
                    )
                ) {
                    when (revokeState) {
                        is RevokeState.Idle, is RevokeState.Failed -> Text("Revoke Authorization")
                        is RevokeState.Preparing -> {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Preparing...")
                        }
                        is RevokeState.Signing -> {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Sign in wallet...")
                        }
                        is RevokeState.Submitting -> {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Submitting...")
                        }
                        is RevokeState.Success -> Text("Revoked")
                    }
                }
            } else {
                Button(
                    onClick = { onAuthorize(walletWinc) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = shareState is ShareState.Idle || shareState is ShareState.Failed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    when (shareState) {
                        is ShareState.Idle, is ShareState.Failed -> Text("Authorize Credits")
                        is ShareState.Preparing -> {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Preparing...")
                        }
                        is ShareState.Signing -> {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Sign in wallet...")
                        }
                        is ShareState.Submitting -> {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Submitting...")
                        }
                        is ShareState.Success -> Text("Authorized")
                    }
                }
            }

            // Error messages
            if (shareState is ShareState.Failed) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = shareState.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = destructiveColor
                )
            }
            if (revokeState is RevokeState.Failed) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = revokeState.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = destructiveColor
                )
            }
        }
    }
}

@Composable
private fun TopUpSection(
    selectedAmount: Double,
    topUpState: TopUpState,
    onSelectAmount: (Double) -> Unit,
    onPurchase: () -> Unit,
    onDismiss: () -> Unit
) {
    val presetAmounts = listOf(0.01, 0.05, 0.1, 0.5)
    val standardColor = toneStandard()
    val successColor = toneStandard()
    val destructiveColor = toneDestructive()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Purchase Credits",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Preset amount chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetAmounts.forEach { amount ->
                    val isSelected = selectedAmount == amount
                    Surface(
                        onClick = { onSelectAmount(amount) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${amount} SOL",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Purchase button
            val isInProgress = topUpState is TopUpState.Building ||
                topUpState is TopUpState.Signing ||
                topUpState is TopUpState.Broadcasting ||
                topUpState is TopUpState.Submitting

            Button(
                onClick = onPurchase,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isInProgress && topUpState !is TopUpState.Success,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                when (topUpState) {
                    is TopUpState.Idle -> Text("Purchase $selectedAmount SOL in Credits")
                    is TopUpState.Building -> {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Building transaction...")
                    }
                    is TopUpState.Signing -> {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign in wallet...")
                    }
                    is TopUpState.Broadcasting -> {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Broadcasting...")
                    }
                    is TopUpState.Submitting -> {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Registering payment...")
                    }
                    is TopUpState.Pending -> {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Processing...")
                    }
                    is TopUpState.Success -> Text("Credits Purchased")
                    is TopUpState.Failed -> Text("Retry Purchase")
                }
            }

            // Status messages
            if (topUpState is TopUpState.Failed) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = topUpState.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = destructiveColor
                )
            }

            if (topUpState is TopUpState.Success) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Credits will appear in your balance shortly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = successColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Credits are non-refundable. Unused shared credits return to you if authorization is revoked or expires.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PendingTopUpBanner(
    txId: String,
    onRetry: () -> Unit
) {
    val warningColor = toneWarning()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = warningColor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FaIcon(
                FaIcons.TriangleExclamation,
                size = 16.dp,
                tint = warningColor
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pending top-up",
                    style = MaterialTheme.typography.labelMedium,
                    color = warningColor
                )
                Text(
                    text = "tx: ${txId.take(8)}...${txId.takeLast(8)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onRetry) {
                Text("Retry", color = warningColor)
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    error: String,
    onRetry: () -> Unit
) {
    val destructiveColor = toneDestructive()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = destructiveColor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FaIcon(
                FaIcons.CircleExclamation,
                size = 16.dp,
                tint = destructiveColor
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = destructiveColor,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text("Retry", color = destructiveColor)
            }
        }
    }
}

@Composable
private fun ApprovalHistorySection(approvals: List<CreditApproval>) {
    Text(
        text = "APPROVAL HISTORY",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )

    approvals.forEach { approval ->
        ApprovalRow(approval)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ApprovalRow(approval: CreditApproval) {
    val successColor = toneStandard()
    val destructiveColor = toneDestructive()
    val standardColor = toneStandard()

    val approved = approval.approvedWincAmount.toBigIntegerOrNull() ?: BigInteger.ZERO
    val used = approval.usedWincAmount.toBigIntegerOrNull() ?: BigInteger.ZERO
    val remaining = (approved - used).coerceAtLeast(BigInteger.ZERO)
    val progress = if (approved > BigInteger.ZERO) {
        used.toFloat() / approved.toFloat()
    } else 0f

    val isExpired = approval.expirationDate?.let {
        runCatching { Instant.parse(it).isBefore(Instant.now()) }.getOrDefault(false)
    } ?: false

    val dateStr = runCatching {
        val instant = Instant.parse(approval.createdDate)
        DateTimeFormatter.ofPattern("MMM d, yyyy")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }.getOrDefault(approval.createdDate)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isExpired) {
                    Text(
                        text = "Expired",
                        style = MaterialTheme.typography.labelSmall,
                        color = destructiveColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Approved: ${ArweaveUtils.formatCredits(approval.approvedWincAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Remaining: ${ArweaveUtils.formatCredits(remaining.toString())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (remaining > BigInteger.ZERO) successColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = standardColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
    }
}
