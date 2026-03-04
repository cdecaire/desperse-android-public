package app.desperse.data.repository

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.desperse.core.arweave.*
import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArweaveRepository @Inject constructor(
    private val turboPaymentApi: TurboPaymentApi,
    private val turboUploadApi: TurboUploadApi,
    private val api: DesperseApi,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ArweaveRepository"
        private const val PREFS_NAME = "desperse_arweave_prefs"
        private const val KEY_PENDING_TX = "pending_top_up_tx"
        private const val SERVICE_INFO_CACHE_MS = 3_600_000L // 1 hour
        private const val RATES_CACHE_MS = 300_000L // 5 min
        private const val BALANCE_STALE_MS = 30_000L // 30 sec
    }

    // Caches
    private var cachedServiceInfo: TurboServiceInfoResponse? = null
    private var serviceInfoTimestamp = 0L

    private var cachedRates: TurboRatesResponse? = null
    private var ratesTimestamp = 0L

    private var cachedBalance: TurboBalanceResponse? = null
    private var cachedBalanceAddress: String? = null
    private var balanceTimestamp = 0L

    private val prefs by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context, PREFS_NAME, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences", e)
            null
        }
    }

    // === Turbo Service Info ===

    suspend fun getServiceInfo(forceRefresh: Boolean = false): Result<TurboServiceInfoResponse> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedServiceInfo != null && now - serviceInfoTimestamp < SERVICE_INFO_CACHE_MS) {
            return Result.success(cachedServiceInfo!!)
        }
        return try {
            val response = turboUploadApi.getServiceInfo()
            if (response.isSuccessful && response.body() != null) {
                cachedServiceInfo = response.body()!!
                serviceInfoTimestamp = now
                Result.success(cachedServiceInfo!!)
            } else {
                Result.failure(Exception("Failed to get Turbo service info: ${response.code()}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error fetching service info: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Get the Turbo Solana deposit wallet address */
    suspend fun getTurboSolanaAddress(): Result<String> {
        return getServiceInfo().mapCatching { info ->
            info.addresses["solana"] ?: throw Exception("Turbo Solana address not available")
        }
    }

    // === Upload Price ===

    suspend fun getUploadPrice(byteCount: Long): Result<TurboPriceResponse> {
        return try {
            val response = turboPaymentApi.getUploadPrice(byteCount)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get upload price: ${response.code()}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === Fiat Rates ===

    suspend fun getFiatRates(forceRefresh: Boolean = false): Result<TurboRatesResponse> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedRates != null && now - ratesTimestamp < RATES_CACHE_MS) {
            return Result.success(cachedRates!!)
        }
        return try {
            val response = turboPaymentApi.getFiatRates()
            if (response.isSuccessful && response.body() != null) {
                cachedRates = response.body()!!
                ratesTimestamp = now
                Result.success(cachedRates!!)
            } else {
                Result.failure(Exception("Failed to get fiat rates: ${response.code()}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === Balance & Approvals ===

    /**
     * Get balance and approvals for a wallet address.
     * Handles 404 (new user) as zero balance.
     */
    suspend fun getBalance(
        walletAddress: String,
        forceRefresh: Boolean = false
    ): Result<TurboBalanceResponse> {
        val now = System.currentTimeMillis()
        if (!forceRefresh
            && cachedBalance != null
            && cachedBalanceAddress == walletAddress
            && now - balanceTimestamp < BALANCE_STALE_MS
        ) {
            return Result.success(cachedBalance!!)
        }
        return try {
            val response = turboPaymentApi.getBalance(walletAddress)
            when {
                response.isSuccessful && response.body() != null -> {
                    cachedBalance = response.body()!!
                    cachedBalanceAddress = walletAddress
                    balanceTimestamp = now
                    Result.success(cachedBalance!!)
                }
                response.code() == 404 -> {
                    // New user — no Turbo account yet, treat as zero
                    Log.d(TAG, "404 for balance: new Turbo user ($walletAddress)")
                    val zero = TurboBalanceResponse(winc = "0")
                    cachedBalance = zero
                    cachedBalanceAddress = walletAddress
                    balanceTimestamp = now
                    Result.success(zero)
                }
                else -> {
                    Result.failure(Exception("Failed to get balance: ${response.code()}"))
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Invalidate cached balance so next call fetches fresh data */
    fun invalidateBalanceCache() {
        cachedBalance = null
        balanceTimestamp = 0
    }

    // === Fund Top-Up ===

    suspend fun submitFundTransaction(txId: String): Result<TurboFundResponse> {
        return try {
            val response = turboPaymentApi.submitFundTransaction(SubmitFundTxBody(txId))
            if (response.isSuccessful && response.body() != null) {
                clearPendingTopUp()
                Result.success(response.body()!!)
            } else {
                // Save for retry
                savePendingTopUp(txId)
                Result.failure(Exception("Failed to submit fund transaction: ${response.code()}"))
            }
        } catch (e: IOException) {
            savePendingTopUp(txId)
            Result.failure(Exception("Network error submitting fund tx: ${e.message}"))
        } catch (e: Exception) {
            savePendingTopUp(txId)
            Result.failure(e)
        }
    }

    // === Pending Top-Up Recovery ===

    fun savePendingTopUp(txId: String) {
        prefs?.edit()?.putString(KEY_PENDING_TX, txId)?.apply()
    }

    fun getPendingTopUp(): String? = prefs?.getString(KEY_PENDING_TX, null)

    fun clearPendingTopUp() {
        prefs?.edit()?.remove(KEY_PENDING_TX)?.apply()
    }

    // === Server Proxy: Share Credits ===

    suspend fun prepareShareCredits(wincAmount: String): Result<PrepareSigningResponse> {
        return when (val result = safeApiCall {
            api.prepareShareCredits(PrepareShareCreditsRequest(wincAmount))
        }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun submitShareCredits(
        sessionId: String,
        signatureBase64: String
    ): Result<ShareCreditsResult> {
        return when (val result = safeApiCall {
            api.submitShareCredits(SubmitSignedDataItemRequest(sessionId, signatureBase64))
        }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    // === Server Proxy: Revoke Credits ===

    suspend fun prepareRevokeCredits(): Result<PrepareSigningResponse> {
        return when (val result = safeApiCall { api.prepareRevokeCredits() }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun submitRevokeCredits(
        sessionId: String,
        signatureBase64: String
    ): Result<RevokeCreditsResult> {
        return when (val result = safeApiCall {
            api.submitRevokeCredits(SubmitSignedDataItemRequest(sessionId, signatureBase64))
        }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }
}
