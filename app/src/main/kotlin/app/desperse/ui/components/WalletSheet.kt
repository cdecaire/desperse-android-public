package app.desperse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.desperse.data.dto.response.NFTAsset
import app.desperse.data.dto.response.TokenBalance
import app.desperse.data.dto.response.WalletActivityItem
import app.desperse.ui.components.media.ImageContext
import app.desperse.ui.components.media.ImageOptimization
import app.desperse.ui.screens.wallet.WalletUiState
import app.desperse.ui.screens.wallet.WalletViewModel
import app.desperse.ui.theme.DesperseSizes
import app.desperse.ui.theme.DesperseSpacing
import coil.compose.AsyncImage
import coil.request.ImageRequest
import app.desperse.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Wallet Bottom Sheet
 *
 * Shows wallet balance, tokens, NFTs, and activity.
 * Matches the web app's wallet popover/sheet design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var activeTab by remember { mutableStateOf("tokens") }
    var nftLayout by remember { mutableStateOf("grid") } // "grid" or "list"
    var showDepositSheet by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val primaryAddress = uiState.wallets.firstOrNull()?.address

    // Load wallet data when sheet opens
    LaunchedEffect(isOpen) {
        if (isOpen) {
            viewModel.loadWalletData()
        }
    }

    if (isOpen) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
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
                    .padding(bottom = DesperseSpacing.xxl)
            ) {
                // Balance Header
                WalletBalanceHeader(uiState)

                Spacer(modifier = Modifier.height(DesperseSpacing.md))

                // Tabs
                WalletTabs(
                    activeTab = activeTab,
                    onTabSelected = { activeTab = it }
                )

                Spacer(modifier = Modifier.height(DesperseSpacing.md))

                // Tab Content
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    uiState.error != null -> {
                        ErrorContent(
                            message = uiState.error!!,
                            onRetry = { viewModel.refresh() }
                        )
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .height(400.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            when (activeTab) {
                                "tokens" -> {
                                    TokensContent(uiState)
                                    // Deposit Button
                                    Spacer(modifier = Modifier.height(DesperseSpacing.lg))
                                    DesperseTextButton(
                                        text = "Deposit",
                                        onClick = { if (primaryAddress != null) showDepositSheet = true },
                                        variant = ButtonVariant.Default,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                "nfts" -> NFTsContent(
                                    uiState = uiState,
                                    layout = nftLayout,
                                    onLayoutChange = { nftLayout = it }
                                )
                                "activity" -> ActivityContent(uiState)
                            }
                        }
                    }
                }
            }
        }

        // Deposit Sheet
        if (showDepositSheet && primaryAddress != null) {
            DepositSheet(
                walletAddress = primaryAddress,
                onDismiss = { showDepositSheet = false }
            )
        }
    }
}

@Composable
private fun WalletBalanceHeader(uiState: WalletUiState) {
    Column {
        Text(
            text = "Balance",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
        ) {
            Text(
                text = "$${formatCurrency(uiState.totalUsd)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val changePrefix = if (uiState.isPositiveChange) "+" else "-"
            val changeColor = if (uiState.isPositiveChange) {
                Color(0xFF22C55E) // green-500
            } else {
                MaterialTheme.colorScheme.error
            }
            Text(
                text = "$changePrefix$${formatCurrency(abs(uiState.totalChangeUsd))} (${changePrefix}${String.format("%.2f", abs(uiState.solChangePct24h))}%)",
                style = MaterialTheme.typography.bodySmall,
                color = changeColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun WalletTabs(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.lg)
    ) {
        WalletTab(
            text = "Tokens",
            isSelected = activeTab == "tokens",
            onClick = { onTabSelected("tokens") }
        )
        WalletTab(
            text = "NFTs",
            isSelected = activeTab == "nfts",
            onClick = { onTabSelected("nfts") }
        )
        WalletTab(
            text = "Activity",
            isSelected = activeTab == "activity",
            onClick = { onTabSelected("activity") }
        )
    }
}

@Composable
private fun WalletTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(vertical = DesperseSpacing.sm)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.onSurface)
            )
        } else {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

/**
 * SKR token mint address on Solana mainnet.
 */
private const val SKR_MINT_ADDRESS = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"
private const val SOL_NATIVE_MINT = "So11111111111111111111111111111111111111112"
private const val SOL_NATIVE_MINT_HELIUS = "So11111111111111111111111111111111111111111"
private const val USDC_MAINNET_MINT = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"

/**
 * Check if a token is the SKR (Seeker) token by its mint address.
 */
private fun isSkrToken(token: TokenBalance): Boolean {
    return token.mint == SKR_MINT_ADDRESS
}

/**
 * Placeholder tokens for SOL, USDC, and SKR when they're not in the API response.
 */
private fun placeholderSol(solPriceUsd: Double) = TokenBalance(
    mint = SOL_NATIVE_MINT, symbol = "SOL", name = "Solana",
    iconUrl = null, balance = 0.0, decimals = 9,
    priceUsd = solPriceUsd, totalValueUsd = 0.0, isAppToken = true
)

private val placeholderUsdc = TokenBalance(
    mint = USDC_MAINNET_MINT, symbol = "USDC", name = "USD Coin",
    iconUrl = null, balance = 0.0, decimals = 6,
    priceUsd = 1.0, totalValueUsd = 0.0, isAppToken = true
)

private val placeholderSkr = TokenBalance(
    mint = SKR_MINT_ADDRESS, symbol = "SKR", name = "Seeker",
    iconUrl = null, balance = 0.0, decimals = 6,
    priceUsd = null, totalValueUsd = 0.0, isAppToken = true
)

@Composable
private fun TokensContent(uiState: WalletUiState) {
    // Deduplicate tokens by mint (keep the one with highest balance)
    val tokens = uiState.tokens
        .groupBy { it.mint }
        .map { (_, dupes) -> dupes.maxByOrNull { it.balance } ?: dupes.first() }
    val solPriceUsd = uiState.solPriceUsd

    // Pinned app token mints — always shown in the top section
    val pinnedMints = setOf(SOL_NATIVE_MINT, SOL_NATIVE_MINT_HELIUS, USDC_MAINNET_MINT, SKR_MINT_ADDRESS)

    // Ensure SOL, USDC, and SKR always appear (match either SOL mint variant)
    val skrToken = tokens.find { isSkrToken(it) } ?: placeholderSkr
    val solToken = tokens.find { it.mint == SOL_NATIVE_MINT || it.mint == SOL_NATIVE_MINT_HELIUS } ?: placeholderSol(solPriceUsd)
    val usdcToken = tokens.find { it.mint == USDC_MAINNET_MINT } ?: placeholderUsdc
    // Additional tokens not pinned at top
    val otherTokens = tokens.filter { it.mint !in pinnedMints }

    Column(
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
    ) {
        // App tokens - always shown, 24h change from server
        TokenCard(token = skrToken, priceChange = skrToken.changePct24h)
        TokenCard(token = solToken, priceChange = solToken.changePct24h)
        TokenCard(token = usdcToken, priceChange = usdcToken.changePct24h)

        // Any other tokens in the wallet
        otherTokens.forEach { token ->
            TokenCard(token = token)
        }
    }
}

@Composable
private fun TokenCard(
    token: TokenBalance,
    priceChange: Double? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(DesperseSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Token Icon
        val localIconRes = remember(token.symbol) {
            when (token.symbol.uppercase()) {
                "SOL" -> R.drawable.ic_token_sol
                "USDC" -> R.drawable.ic_token_usdc
                "SKR" -> R.drawable.ic_token_skr
                else -> null
            }
        }
        val symbolUpper = token.symbol.uppercase()
        if ((symbolUpper == "USDC" || symbolUpper == "SKR") && localIconRes != null) {
            // Circular logos (USDC, SKR) - render at full size without container
            Image(
                painter = painterResource(id = localIconRes),
                contentDescription = token.symbol,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )
        } else if (symbolUpper == "SOL" && localIconRes != null) {
            // SOL logo sits inside a circular container, slightly smaller
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = localIconRes),
                    contentDescription = token.symbol,
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (token.iconUrl != null) {
                    val context = LocalContext.current
                    val optimizedUrl = remember(token.iconUrl) {
                        ImageOptimization.getOptimizedUrlForContext(token.iconUrl, ImageContext.TOKEN_ICON)
                    }
                    AsyncImage(
                        model = remember(optimizedUrl) {
                            ImageRequest.Builder(context)
                                .data(optimizedUrl)
                                .crossfade(true)
                                .build()
                        },
                        contentDescription = token.symbol,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = token.symbol.take(2).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(DesperseSpacing.md))

        // Token Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = token.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)
            ) {
                token.priceUsd?.let { price ->
                    Text(
                        text = "$${formatPrice(price)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                priceChange?.let { change ->
                    val isPositive = change >= 0
                    val changeColor = if (isPositive) {
                        Color(0xFF22C55E)
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format("%.2f", change)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = changeColor
                    )
                }
            }
        }

        // Balance
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$${formatCurrency(token.totalValueUsd ?: 0.0)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${formatBalance(token.balance, token.decimals)} ${token.symbol}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NFTsContent(
    uiState: WalletUiState,
    layout: String,
    onLayoutChange: (String) -> Unit
) {
    val nfts = uiState.nfts

    if (nfts.isEmpty()) {
        EmptyContent(
            icon = FaIcons.Image,
            title = "No NFTs found",
            description = "Your NFTs will appear here"
        )
        return
    }

    Column {
        // Header with count and layout toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = DesperseSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${nfts.size} ${if (nfts.size == 1) "item" else "items"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Layout toggle buttons
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                LayoutToggleButton(
                    icon = FaIcons.Grid2,
                    isSelected = layout == "grid",
                    onClick = { onLayoutChange("grid") }
                )
                LayoutToggleButton(
                    icon = FaIcons.List,
                    isSelected = layout == "list",
                    onClick = { onLayoutChange("list") }
                )
            }
        }

        // NFT content based on layout
        when (layout) {
            "grid" -> NFTGridView(nfts)
            "list" -> NFTListView(nfts)
        }
    }
}

@Composable
private fun LayoutToggleButton(
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (isSelected) {
                    Modifier.background(MaterialTheme.colorScheme.surface)
                } else {
                    Modifier
                }
            )
            .clickable { onClick() }
            .padding(DesperseSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        FaIcon(
            icon = icon,
            size = 16.dp,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun NFTGridView(nfts: List<NFTAsset>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
    ) {
        nfts.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
            ) {
                row.forEach { nft ->
                    NFTCard(
                        nft = nft,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty space if odd number
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun NFTListView(nfts: List<NFTAsset>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
    ) {
        nfts.forEach { nft ->
            NFTListItem(nft = nft)
        }
    }
}

@Composable
private fun NFTListItem(nft: NFTAsset) {
    val context = LocalContext.current
    val name = nft.name ?: "Unnamed NFT"
    val collection = nft.collectionName
    val imageUrl = nft.imageUri

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = remember(imageUrl) {
                        ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .build()
                    },
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Low
                )
            } else {
                FaIcon(
                    icon = FaIcons.Image,
                    size = 20.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = FaIconStyle.Regular
                )
            }
        }

        Spacer(modifier = Modifier.width(DesperseSpacing.md))

        // Name and collection
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (collection != null) {
                Text(
                    text = collection,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Arrow icon (like web app's external link indicator)
        FaIcon(
            icon = FaIcons.ChevronRight,
            size = 16.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NFTCard(
    nft: NFTAsset,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val name = nft.name ?: "Unnamed NFT"
    val collection = nft.collectionName
    val imageUrl = nft.imageUri

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = remember(imageUrl) {
                        ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .build()
                    },
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Medium
                )
            } else {
                FaIcon(
                    icon = FaIcons.Image,
                    size = 32.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = FaIconStyle.Regular
                )
            }
        }

        Column(
            modifier = Modifier.padding(DesperseSpacing.sm)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (collection != null) {
                Text(
                    text = collection,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ActivityContent(uiState: WalletUiState) {
    val activity = uiState.activity
    val solPriceUsd = uiState.solPriceUsd
    val skrPriceUsd = uiState.tokens.find { it.mint == SKR_MINT_ADDRESS }?.priceUsd ?: 0.0

    if (activity.isEmpty()) {
        EmptyContent(
            icon = FaIcons.ClockRotateLeft,
            title = "No recent activity",
            description = "Your transactions will appear here"
        )
        return
    }

    // Group by date
    val groupedActivity = activity.groupBy { item ->
        val date = Date(item.timestamp)
        val today = Date()
        val yesterday = Date(today.time - 86400000)

        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayStr = dateFormat.format(today)
        val yesterdayStr = dateFormat.format(yesterday)
        val itemStr = dateFormat.format(date)

        when (itemStr) {
            todayStr -> "Today"
            yesterdayStr -> "Yesterday"
            else -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
    ) {
        groupedActivity.forEach { (dateLabel, items) ->
            Column {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = DesperseSpacing.sm)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
                ) {
                    items.forEach { item ->
                        ActivityCard(
                            item = item,
                            solPriceUsd = solPriceUsd,
                            skrPriceUsd = skrPriceUsd
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(
    item: WalletActivityItem,
    solPriceUsd: Double,
    skrPriceUsd: Double = 0.0
) {
    val context = LocalContext.current
    val isBasicTransfer = item.type == "transfer_in" || item.type == "transfer_out"
    val isTip = item.type == "tip_sent" || item.type == "tip_received"

    val title = item.context.post?.caption ?: when (item.type) {
        "edition_sale" -> "Edition Sale"
        "edition_purchase" -> "Edition Purchase"
        "collection" -> "Collected"
        "tip_sent" -> "Tipped ${item.context.counterparty?.displayName ?: item.context.counterparty?.usernameSlug ?: "someone"}"
        "tip_received" -> "Tip from ${item.context.counterparty?.displayName ?: item.context.counterparty?.usernameSlug ?: "someone"}"
        "transfer_in" -> "Received ${item.token}"
        "transfer_out" -> "Sent ${item.token}"
        else -> "Transaction"
    }

    val badge = when (item.type) {
        "edition_sale" -> "SOLD" to Color(0xFF22C55E) // green
        "edition_purchase" -> "MINTED" to Color(0xFF3B82F6) // blue-500
        "collection" -> "COLLECTED" to Color(0xFF8B5CF6) // violet-500
        "tip_sent" -> "TIPPED" to Color(0xFFF59E0B) // amber-500
        "tip_received" -> "TIP" to Color(0xFFF59E0B) // amber-500
        else -> null
    }

    val isPositive = item.direction == "in"
    val amountText = if (item.amount != null && item.token != null) {
        val sign = if (isPositive) "+" else "-"
        "$sign${formatBalance(item.amount, if (item.token == "USDC") 2 else 4)}"
    } else {
        "Free"
    }

    val usdValue = if (item.amount != null && item.token != null) {
        when (item.token) {
            "SOL" -> item.amount * solPriceUsd
            "USDC" -> item.amount
            "SKR" -> if (skrPriceUsd > 0) item.amount * skrPriceUsd else null
            else -> null
        }
    } else null

    val thumbnail = item.context.post?.coverUrl ?: item.context.post?.mediaUrl

    // Compact row for basic transfers (send/receive)
    if (isBasicTransfer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = DesperseSpacing.md, vertical = DesperseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle icon for transfers
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                FaIcon(
                    icon = if (item.direction == "in") FaIcons.ArrowDown else FaIcons.ArrowUp,
                    size = 14.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = FaIconStyle.Regular
                )
            }

            Spacer(modifier = Modifier.width(DesperseSpacing.md))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Amount with token on same line
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isPositive) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurface
                )
                if (item.token != null) {
                    Text(
                        text = item.token,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Explorer link
            if (item.signature != null) {
                Spacer(modifier = Modifier.width(DesperseSpacing.sm))
                FaIcon(
                    icon = FaIcons.ArrowUpRightSimple,
                    size = 12.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // Rich card for enriched entries (edition_sale, edition_purchase, collection)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(DesperseSpacing.md)
    ) {
        // Main row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Thumbnail or icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    val optimizedThumbnail = remember(thumbnail) {
                        ImageOptimization.getOptimizedUrlForContext(thumbnail, ImageContext.WALLET_ACTIVITY)
                    }
                    AsyncImage(
                        model = remember(optimizedThumbnail) {
                            ImageRequest.Builder(context)
                                .data(optimizedThumbnail)
                                .crossfade(true)
                                .build()
                        },
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    FaIcon(
                        icon = when {
                            item.type == "collection" -> FaIcons.Bookmark
                            isTip -> FaIcons.Coins
                            else -> FaIcons.Gem
                        },
                        size = 20.dp,
                        tint = if (isTip) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = FaIconStyle.Regular
                    )
                }
            }

            Spacer(modifier = Modifier.width(DesperseSpacing.md))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(DesperseSpacing.sm))

            // Amount with token on same line
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = amountText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPositive && item.amount != null) {
                            Color(0xFF22C55E)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (item.token != null) {
                        Text(
                            text = item.token,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                usdValue?.let {
                    Text(
                        text = "~$${formatCurrency(it)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Footer with badge, person, and explorer link
        if (badge != null || item.context.counterparty != null || item.context.creator != null) {
            Spacer(modifier = Modifier.height(DesperseSpacing.sm))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.height(DesperseSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: badge and person
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    badge?.let { (text, color) ->
                        // Pill-shaped badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(color.copy(alpha = 0.15f))
                                .padding(horizontal = DesperseSpacing.sm, vertical = 2.dp)
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = color
                            )
                        }
                    }

                    // Person (counterparty or creator)
                    val person = when (item.type) {
                        "edition_sale" -> item.context.counterparty?.let { "to" to it }
                        "edition_purchase" -> item.context.counterparty?.let { "from" to it }
                        "collection" -> item.context.creator?.let { "from" to it }
                        "tip_sent" -> item.context.counterparty?.let { "to" to it }
                        "tip_received" -> item.context.counterparty?.let { "from" to it }
                        else -> null
                    }

                    person?.let { (preposition, user) ->
                        if (badge != null) {
                            Text(
                                text = " · ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$preposition ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Avatar
                        if (user.avatarUrl != null) {
                            val optimizedAvatar = remember(user.avatarUrl) {
                                ImageOptimization.getOptimizedUrlForContext(user.avatarUrl, ImageContext.AVATAR)
                            }
                            AsyncImage(
                                model = remember(optimizedAvatar) {
                                    ImageRequest.Builder(context)
                                        .data(optimizedAvatar)
                                        .crossfade(true)
                                        .build()
                                },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "@${user.usernameSlug}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Right side: explorer link
                if (item.signature != null) {
                    FaIcon(
                        icon = FaIcons.ArrowUpRightSimple,
                        size = 12.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyContent(
    icon: String,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DesperseSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
    ) {
        FaIcon(
            icon = icon,
            size = DesperseSizes.iconXl,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            style = FaIconStyle.Regular
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DesperseSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
    ) {
        FaIcon(
            icon = FaIcons.TriangleExclamation,
            size = DesperseSizes.iconXl,
            tint = MaterialTheme.colorScheme.error,
            style = FaIconStyle.Solid
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        DesperseTextButton(
            text = "Retry",
            onClick = onRetry,
            variant = ButtonVariant.Default
        )
    }
}

// Format helpers
private fun formatCurrency(value: Double): String {
    return String.format("%.2f", value)
}

private fun formatPrice(value: Double): String {
    return if (value < 1) {
        String.format("%.4f", value)
    } else {
        String.format("%.2f", value)
    }
}

private fun formatBalance(value: Double, decimals: Int): String {
    val absValue = kotlin.math.abs(value)
    return when {
        value == 0.0 -> "0"
        absValue < 0.0001 -> "~0.0001"
        absValue < 1 -> String.format("%.${minOf(decimals, 6)}f", value)
        absValue < 100 -> String.format("%.4f", value)
        else -> String.format("%,.2f", value)
    }
}
