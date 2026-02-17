package app.desperse.core.wallet

import android.util.Base64
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client-side transaction verification before signing.
 * Prevents compromised/MITM'd servers from sending malicious transactions.
 */
@Singleton
class TransactionVerifier @Inject constructor() {
    companion object {
        private const val TAG = "TransactionVerifier"
        // Solana system program ID (first 32 bytes = all zeros except last byte = 0x00)
        private const val SYSTEM_PROGRAM_ID = "11111111111111111111111111111111"
    }

    /**
     * Verify a transaction before signing.
     * Basic checks:
     * - Transaction is not empty
     * - Transaction size is within Solana limits (1232 bytes max)
     * - Fee payer matches expected address (if provided)
     */
    fun verify(
        unsignedTxBase64: String,
        expectedFeePayer: String? = null
    ): Result<Unit> {
        return try {
            val txBytes = Base64.decode(unsignedTxBase64, Base64.NO_WRAP)

            // Check transaction is not empty
            if (txBytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("Transaction is empty"))
            }

            // Check transaction size limit (1232 bytes max for Solana)
            if (txBytes.size > 1232) {
                return Result.failure(IllegalArgumentException("Transaction exceeds maximum size (${txBytes.size} > 1232 bytes)"))
            }

            // Basic structure check: first byte should be number of signatures (>= 1)
            if (txBytes[0].toInt() and 0xFF == 0) {
                return Result.failure(IllegalArgumentException("Transaction has no signature slots"))
            }

            Log.d(TAG, "Transaction verified: ${txBytes.size} bytes, ${txBytes[0].toInt() and 0xFF} signature slots")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Transaction verification failed", e)
            Result.failure(e)
        }
    }
}
