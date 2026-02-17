package io.privy.auth.siwe

import io.privy.wallet.WalletClientType

/**
 * Metadata for wallet login, providing optional details about the wallet client and connection
 * method.
 */
public data class WalletLoginMetadata(
    val walletClientType: WalletClientType? = null,
    val connectorType: String? = null
)
