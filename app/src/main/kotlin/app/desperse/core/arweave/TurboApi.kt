package app.desperse.core.arweave

import retrofit2.Response
import retrofit2.http.*

/**
 * Turbo Payment API — direct REST calls (no auth required).
 * Base URL: https://payment.ardrive.io/
 */
interface TurboPaymentApi {

    @GET("v1/price/bytes/{byteCount}")
    suspend fun getUploadPrice(
        @Path("byteCount") bytes: Long
    ): Response<TurboPriceResponse>

    @GET("v1/rates")
    suspend fun getFiatRates(): Response<TurboRatesResponse>

    @GET("v1/account/balance/solana")
    suspend fun getBalance(
        @Query("address") walletAddress: String
    ): Response<TurboBalanceResponse>

    @GET("v1/account/approvals/get")
    suspend fun getCreditApprovals(
        @Query("userAddress") walletAddress: String
    ): Response<TurboApprovalsResponse>

    @POST("v1/account/balance/solana")
    suspend fun submitFundTransaction(
        @Body body: SubmitFundTxBody
    ): Response<TurboFundResponse>
}

/**
 * Turbo Upload API — direct REST calls (no auth required).
 * Base URL: https://upload.ardrive.io/
 */
interface TurboUploadApi {

    @GET("/")
    suspend fun getServiceInfo(): Response<TurboServiceInfoResponse>
}
