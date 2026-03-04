package app.desperse.core.arweave

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// === Turbo REST API DTOs ===

@Serializable
data class TurboPriceResponse(
    val winc: String,
    val adjustments: List<JsonElement> = emptyList()
)

@Serializable
data class TurboRatesResponse(
    val winc: String,
    val fiat: Map<String, Double>
)

@Serializable
data class TurboBalanceResponse(
    val winc: String,
    val balance: String? = null,
    val controlledWinc: String? = null,
    val effectiveBalance: String? = null,
    val givenApprovals: List<CreditApproval> = emptyList(),
    val receivedApprovals: List<CreditApproval> = emptyList()
)

@Serializable
data class CreditApproval(
    val approvalDataItemId: String,
    val approvedAddress: String,
    val approvedWincAmount: String,
    val usedWincAmount: String,
    @SerialName("creationDate") val createdDate: String,
    val payingAddress: String? = null,
    val expirationDate: String? = null
)

@Serializable
data class TurboApprovalsResponse(
    val givenApprovals: List<CreditApproval> = emptyList(),
    val receivedApprovals: List<CreditApproval> = emptyList()
)

@Serializable
data class TurboServiceInfoResponse(
    val version: String,
    val addresses: Map<String, String>,
    val gateway: String? = null,
    val freeUploadLimitBytes: Long? = null
)

@Serializable
data class SubmitFundTxBody(
    @SerialName("tx_id") val txId: String
)

@Serializable
data class TurboFundResponse(
    val creditedTransaction: FundTxDetail? = null,
    val pendingTransaction: FundTxDetail? = null,
    val failedTransaction: FundTxDetail? = null
)

@Serializable
data class FundTxDetail(
    val transactionId: String,
    val winstonCreditAmount: String? = null
)

// === Server Proxy DTOs (ANS-104 share/revoke) ===

@Serializable
data class PrepareShareCreditsRequest(
    val wincAmount: String
)

@Serializable
data class SubmitSignedDataItemRequest(
    val sessionId: String,
    val signatureBase64: String
)

@Serializable
data class PrepareSigningResponse(
    val sessionId: String,
    val deepHashBase64: String
)

@Serializable
data class ShareCreditsResult(
    val approvalDataItemId: String,
    val approvedWincAmount: String
)

@Serializable
data class RevokeCreditsResult(
    val success: Boolean = true
)
