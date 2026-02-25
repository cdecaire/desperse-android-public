package app.desperse.core.wallet

/**
 * Sealed class representing all MWA (Mobile Wallet Adapter) failure modes.
 * Used by MwaManager to provide typed error handling.
 */
sealed class MwaError : Exception() {
    data object NoWalletInstalled : MwaError() {
        private fun readResolve(): Any = NoWalletInstalled
    }

    data object UserCancelled : MwaError() {
        private fun readResolve(): Any = UserCancelled
    }

    data object SessionTerminated : MwaError() {
        private fun readResolve(): Any = SessionTerminated
    }

    data object Timeout : MwaError() {
        private fun readResolve(): Any = Timeout
    }

    data class WalletRejected(val code: Int, override val message: String) : MwaError()

    data class Unknown(override val cause: Throwable) : MwaError()

    /**
     * Authorization succeeded inside a single-session authorizeAndSignMessage flow,
     * but the messageProvider failed (e.g., Privy foreground requirement).
     * Carries the [authResult] so the caller can fall back to a two-step flow
     * without re-authorizing.
     */
    data class MessageProviderFailed(
        val authResult: MwaAuthResult,
        override val cause: Throwable
    ) : MwaError()
}

/**
 * Returns a user-facing error message for display in UI.
 */
fun MwaError.userFacingMessage(): String = when (this) {
    is MwaError.NoWalletInstalled -> "No Solana wallet app found. Please install Phantom or Solflare."
    is MwaError.UserCancelled -> "" // Should be handled silently
    is MwaError.SessionTerminated -> "Wallet connection lost. Please try again."
    is MwaError.Timeout -> "Wallet did not respond. Please ensure your wallet app is open."
    is MwaError.WalletRejected -> "Wallet rejected the request: $message"
    is MwaError.MessageProviderFailed -> "Sign-in preparation failed. Retrying..."
    is MwaError.Unknown -> "An unexpected error occurred. Please try again."
}
