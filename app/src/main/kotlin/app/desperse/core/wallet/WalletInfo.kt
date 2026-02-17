package app.desperse.core.wallet

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Type of wallet - embedded (Privy-managed) or external (MWA, hardware).
 */
enum class WalletType {
    EMBEDDED,
    EXTERNAL
}

/**
 * Represents a user's linked wallet with metadata.
 * Uses @Immutable for Compose stability in lists.
 */
@Immutable
data class WalletInfo(
    val id: String,
    val address: String,
    val type: WalletType,
    val connector: String? = null,  // "mwa" for external wallets
    val label: String,              // "Desperse Wallet", "Seed Vault", wallet app name
    val isPrimary: Boolean = false
)

/**
 * Result of MWA authorization, containing the wallet address and auth token.
 */
data class MwaAuthResult(
    val address: String,              // Base58-encoded wallet public key
    val authToken: String?,           // Session token for future reauthorization
    val walletLabel: String?,         // Wallet app name (e.g., "Phantom Wallet", "Seed Vault")
    val walletClientType: String,     // Lowercase Privy identifier (e.g., "phantom", "solflare")
    val publicKeyBytes: ByteArray     // Raw 32-byte public key
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MwaAuthResult) return false
        return address == other.address && authToken == other.authToken
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + (authToken?.hashCode() ?: 0)
        return result
    }
}

/**
 * Wrapper for the /wallet/wallets endpoint response.
 * Server returns { wallets: [...] } inside the ApiEnvelope data field.
 */
@Serializable
data class UserWalletsResponse(
    val wallets: List<UserWalletDto>
)

/**
 * Wrapper for the POST /wallet/add response.
 * Server returns { data: { wallet: {...} } } inside the ApiEnvelope.
 */
@Serializable
data class AddWalletResponse(
    val wallet: UserWalletDto
)

/**
 * DTO matching server response for user wallet entries.
 */
@Serializable
data class UserWalletDto(
    val id: String,
    val address: String,
    val type: String,           // "embedded" or "external"
    val connector: String? = null,
    val label: String? = null,
    val isPrimary: Boolean = false
) {
    fun toWalletInfo(): WalletInfo = WalletInfo(
        id = id,
        address = address,
        type = if (type == "embedded") WalletType.EMBEDDED else WalletType.EXTERNAL,
        connector = connector,
        label = label ?: if (type == "embedded") "Desperse Wallet" else "External Wallet",
        isPrimary = isPrimary
    )
}
