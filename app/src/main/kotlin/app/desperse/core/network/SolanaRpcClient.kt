package app.desperse.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SolanaRpcClient"

/**
 * Lightweight Solana RPC client for transaction broadcasting.
 * Uses Helius RPC when HELIUS_API_KEY is configured, falls back to public mainnet RPC.
 */
@Singleton
class SolanaRpcClient @Inject constructor() {

    private val rpcUrl: String = run {
        val apiKey = app.desperse.BuildConfig.HELIUS_API_KEY
        if (apiKey.isNotBlank()) "https://mainnet.helius-rpc.com/?api-key=$apiKey"
        else "https://api.mainnet-beta.solana.com"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // Longer timeout for tx confirmation
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Send a signed transaction to the Solana network.
     *
     * @param signedTxBase64 The signed transaction serialized as base64
     * @return Result containing the transaction signature (base58) or an error
     */
    suspend fun sendTransaction(signedTxBase64: String): Result<String> = withContext(Dispatchers.IO) {
        val maxRetries = 3
        val retryDelayMs = 2_000L

        for (attempt in 1..maxRetries) {
            try {
                Log.d(TAG, "Sending transaction to Solana RPC (attempt $attempt/$maxRetries)...")

                val request = RpcRequest(
                    jsonrpc = "2.0",
                    id = 1,
                    method = "sendTransaction",
                    params = listOf(
                        signedTxBase64,
                        SendTransactionConfig(
                            encoding = "base64",
                            skipPreflight = false,
                            preflightCommitment = "confirmed",
                            maxRetries = 3
                        )
                    )
                )

                val requestBody = json.encodeToString(request)
                    .toRequestBody("application/json".toMediaType())

                val httpRequest = Request.Builder()
                    .url(rpcUrl)
                    .post(requestBody)
                    .build()

                client.newCall(httpRequest).execute().use { response ->
                    val body = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response from RPC"))

                    Log.d(TAG, "RPC response: ${body.take(500)}...")

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("RPC request failed: ${response.code} - $body")
                        )
                    }

                    val rpcResponse = json.decodeFromString<RpcResponse>(body)

                    if (rpcResponse.error != null) {
                        val errorMessage = rpcResponse.error.message
                        Log.e(TAG, "RPC error: $errorMessage (code: ${rpcResponse.error.code})")

                        // Check for specific error types
                        return@withContext when {
                            errorMessage.contains("blockhash not found", ignoreCase = true) ||
                            errorMessage.contains("block height exceeded", ignoreCase = true) ->
                                Result.failure(BlockhashExpiredException(errorMessage))

                            errorMessage.contains("insufficient funds", ignoreCase = true) ||
                            errorMessage.contains("insufficient lamports", ignoreCase = true) ->
                                Result.failure(InsufficientFundsException(errorMessage))

                            else -> Result.failure(RpcException(errorMessage, rpcResponse.error.code))
                        }
                    }

                    val signature = rpcResponse.result
                        ?: return@withContext Result.failure(Exception("No signature in response"))

                    Log.d(TAG, "Transaction sent successfully, signature: $signature")
                    return@withContext Result.success(signature)
                }
            } catch (e: Exception) {
                val isNetworkError = e is java.net.UnknownHostException ||
                    e is java.net.SocketTimeoutException ||
                    e is java.net.ConnectException ||
                    e.message?.contains("resolve host", ignoreCase = true) == true

                if (isNetworkError && attempt < maxRetries) {
                    Log.w(TAG, "Network error on attempt $attempt, retrying in ${retryDelayMs}ms...", e)
                    kotlinx.coroutines.delay(retryDelayMs)
                    continue
                }

                Log.e(TAG, "Failed to send transaction (attempt $attempt)", e)
                return@withContext Result.failure(e)
            }
        }
        Result.failure(Exception("Failed to send transaction after $maxRetries attempts"))
    }

    /**
     * Confirm a transaction by polling for its status.
     * Returns when the transaction is confirmed or times out.
     */
    suspend fun confirmTransaction(
        signature: String,
        commitment: String = "confirmed",
        timeoutMs: Long = 60_000
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val pollInterval = 2_000L

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                val request = RpcRequest(
                    jsonrpc = "2.0",
                    id = 1,
                    method = "getSignatureStatuses",
                    params = listOf(listOf(signature))
                )

                val requestBody = json.encodeToString(request)
                    .toRequestBody("application/json".toMediaType())

                val httpRequest = Request.Builder()
                    .url(rpcUrl)
                    .post(requestBody)
                    .build()

                client.newCall(httpRequest).execute().use { response ->
                    val body = response.body?.string()

                    if (body != null && response.isSuccessful) {
                        val statusResponse = json.decodeFromString<SignatureStatusResponse>(body)
                        val status = statusResponse.result?.value?.firstOrNull()

                        if (status != null) {
                            val confirmations = status.confirmations ?: 0
                            val confirmationStatus = status.confirmationStatus

                            Log.d(TAG, "Tx status: confirmations=$confirmations, status=$confirmationStatus")

                            // Check if confirmed based on commitment level
                            val isConfirmed = when (commitment) {
                                "finalized" -> confirmationStatus == "finalized"
                                "confirmed" -> confirmationStatus in listOf("confirmed", "finalized")
                                else -> confirmations > 0 || confirmationStatus != null
                            }

                            if (isConfirmed) {
                                // Check for errors
                                if (status.err != null) {
                                    return@withContext Result.failure(
                                        TransactionFailedException("Transaction failed on-chain")
                                    )
                                }
                                return@withContext Result.success(true)
                            }
                        }
                    }
                }

                kotlinx.coroutines.delay(pollInterval)
            } catch (e: Exception) {
                Log.w(TAG, "Error checking tx status, retrying...", e)
                kotlinx.coroutines.delay(pollInterval)
            }
        }

        Result.failure(TransactionTimeoutException("Transaction confirmation timed out"))
    }
}

// Request/Response models
@Serializable
private data class RpcRequest(
    val jsonrpc: String,
    val id: Int,
    val method: String,
    val params: List<@Serializable(with = AnySerializer::class) Any>
)

@Serializable
private data class SendTransactionConfig(
    val encoding: String,
    val skipPreflight: Boolean,
    val preflightCommitment: String,
    val maxRetries: Int? = null
)

@Serializable
private data class RpcResponse(
    val jsonrpc: String? = null,
    val id: Int? = null,
    val result: String? = null,
    val error: RpcError? = null
)

@Serializable
private data class RpcError(
    val code: Int,
    val message: String,
    val data: kotlinx.serialization.json.JsonElement? = null
)

@Serializable
private data class SignatureStatusResponse(
    val jsonrpc: String? = null,
    val id: Int? = null,
    val result: SignatureStatusResult? = null
)

@Serializable
private data class SignatureStatusResult(
    val context: SignatureContext? = null,
    val value: List<SignatureStatus?>? = null
)

@Serializable
private data class SignatureContext(
    val slot: Long? = null
)

@Serializable
private data class SignatureStatus(
    val slot: Long? = null,
    val confirmations: Int? = null,
    val confirmationStatus: String? = null,
    val err: kotlinx.serialization.json.JsonElement? = null
)

// Custom serializer for Any type in RPC params
private object AnySerializer : kotlinx.serialization.KSerializer<Any> {
    override val descriptor = kotlinx.serialization.descriptors.buildClassSerialDescriptor("Any")

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Any) {
        val jsonEncoder = encoder as kotlinx.serialization.json.JsonEncoder
        val element = when (value) {
            is String -> kotlinx.serialization.json.JsonPrimitive(value)
            is Number -> kotlinx.serialization.json.JsonPrimitive(value)
            is Boolean -> kotlinx.serialization.json.JsonPrimitive(value)
            is List<*> -> kotlinx.serialization.json.buildJsonArray {
                value.forEach { item ->
                    when (item) {
                        is String -> add(kotlinx.serialization.json.JsonPrimitive(item))
                        else -> add(kotlinx.serialization.json.JsonPrimitive(item.toString()))
                    }
                }
            }
            is SendTransactionConfig -> Json.encodeToJsonElement(SendTransactionConfig.serializer(), value)
            else -> kotlinx.serialization.json.JsonPrimitive(value.toString())
        }
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Any {
        throw UnsupportedOperationException("Deserialization not supported")
    }
}

// Custom exceptions for better error handling
class RpcException(message: String, val code: Int) : Exception(message)
class BlockhashExpiredException(message: String) : Exception(message)
class InsufficientFundsException(message: String) : Exception(message)
class TransactionFailedException(message: String) : Exception(message)
class TransactionTimeoutException(message: String) : Exception(message)
