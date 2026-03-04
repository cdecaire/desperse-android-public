package app.desperse.core.arweave

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.time.Instant

object ArweaveUtils {

    /** 1 AR = 10^12 winston (winc) */
    private val WINC_PER_AR = BigDecimal("1000000000000")

    /** Desperse's Turbo wallet address — only shared credits to this address are usable */
    const val DESPERSE_TURBO_WALLET = "G6QNEtkriCzNJM1CrsB2xGukmijLP9TENSNPtJQyT9nR"

    /** Format winc string as AR with 4 decimal places, e.g. "0.0042 AR" */
    fun formatCredits(winc: String): String {
        val wincBd = winc.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val ar = wincBd.divide(WINC_PER_AR, 4, RoundingMode.HALF_UP)
        return "${ar.stripTrailingZeros().toPlainString()} AR"
    }

    /** Format winc as USD using fiat rate, e.g. "$0.04" */
    fun formatCreditsUsd(winc: String, rateWincPerUsd: String): String {
        val wincBd = winc.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val ratePerUsd = rateWincPerUsd.toBigDecimalOrNull() ?: return "$0.00"
        if (ratePerUsd.compareTo(BigDecimal.ZERO) == 0) return "$0.00"
        val usd = wincBd.divide(ratePerUsd, 2, RoundingMode.HALF_UP)
        return "$${usd.toPlainString()}"
    }

    /** Arweave gateway URL for a transaction */
    fun arweaveTxUrl(txId: String): String = "https://arweave.net/$txId"

    /**
     * Calculate remaining shared credits to Desperse from approval list.
     * Only counts non-expired approvals where approvedAddress matches DESPERSE_TURBO_WALLET.
     */
    fun calculateSharedRemaining(
        approvals: List<CreditApproval>,
        desperseWallet: String = DESPERSE_TURBO_WALLET
    ): BigInteger {
        val now = Instant.now()
        return approvals
            .filter { it.approvedAddress.equals(desperseWallet, ignoreCase = true) }
            .filter { approval ->
                approval.expirationDate == null ||
                    runCatching { Instant.parse(approval.expirationDate).isAfter(now) }.getOrDefault(false)
            }
            .fold(BigInteger.ZERO) { acc, approval ->
                val approved = approval.approvedWincAmount.toBigIntegerOrNull() ?: BigInteger.ZERO
                val used = approval.usedWincAmount.toBigIntegerOrNull() ?: BigInteger.ZERO
                acc + (approved - used).coerceAtLeast(BigInteger.ZERO)
            }
    }

    /**
     * Check if shared remaining credits are sufficient for the estimated cost with 20% buffer.
     */
    fun hasSufficientShared(sharedRemaining: BigInteger, estimatedCostWinc: String): Boolean {
        val cost = estimatedCostWinc.toBigIntegerOrNull() ?: return false
        val costWithBuffer = cost * BigInteger("120") / BigInteger("100")
        return sharedRemaining >= costWithBuffer
    }

    /**
     * Check if there is at least one active (non-expired, remaining > 0) approval to Desperse.
     */
    fun hasActiveApproval(
        approvals: List<CreditApproval>,
        desperseWallet: String = DESPERSE_TURBO_WALLET
    ): Boolean {
        val now = Instant.now()
        return approvals.any { approval ->
            approval.approvedAddress.equals(desperseWallet, ignoreCase = true) &&
                (approval.expirationDate == null ||
                    runCatching { Instant.parse(approval.expirationDate).isAfter(now) }.getOrDefault(false)) &&
                run {
                    val approved = approval.approvedWincAmount.toBigIntegerOrNull() ?: BigInteger.ZERO
                    val used = approval.usedWincAmount.toBigIntegerOrNull() ?: BigInteger.ZERO
                    (approved - used) > BigInteger.ZERO
                }
        }
    }

    /** Apply 20% buffer to an estimated cost in winc */
    fun withBuffer(estimatedCostWinc: String): BigInteger {
        val cost = estimatedCostWinc.toBigIntegerOrNull() ?: BigInteger.ZERO
        return cost * BigInteger("120") / BigInteger("100")
    }
}
