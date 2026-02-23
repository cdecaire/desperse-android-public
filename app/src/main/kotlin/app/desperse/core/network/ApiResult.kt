package app.desperse.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException

/**
 * API response envelope matching backend contract
 */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val meta: ApiMeta? = null,
    val requestId: String
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)

@Serializable
data class ApiMeta(
    val hasMore: Boolean? = null,
    val nextCursor: String? = null
)

@Serializable
data class ErrorEnvelope(
    val success: Boolean = false,
    val error: ApiError? = null,
    val requestId: String? = null
)

/**
 * Unified result type for API calls
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T, val requestId: String, val meta: ApiMeta? = null) : ApiResult<T>()
    data class Error(
        val code: ErrorCode,
        val message: String,
        val requestId: String?,
        val httpCode: Int? = null
    ) : ApiResult<Nothing>()
}

enum class ErrorCode {
    VALIDATION_ERROR,
    AUTH_REQUIRED,
    NOT_FOUND,
    RATE_LIMITED,
    SERVER_ERROR,
    NETWORK_ERROR,
    TX_EXPIRED_BLOCKHASH,
    TX_INVALID_SIGNATURE,
    TX_ALREADY_SUBMITTED,
    TX_BROADCAST_FAILED,
    PURCHASE_NOT_FOUND,
    EDITION_SOLD_OUT,
    INSUFFICIENT_BALANCE,
    INSUFFICIENT_FUNDS,
    DUPLICATE_WALLET,
    UNKNOWN
}

fun parseErrorCode(code: String): ErrorCode {
    return try {
        ErrorCode.valueOf(code)
    } catch (e: Exception) {
        ErrorCode.UNKNOWN
    }
}

/**
 * Safe API call wrapper with consistent error handling
 */
suspend fun <T> safeApiCall(
    block: suspend () -> Response<ApiEnvelope<T>>
): ApiResult<T> {
    val errorJson = Json { ignoreUnknownKeys = true }

    return try {
        val response = block()
        val body = response.body()
        val requestId = body?.requestId ?: response.headers()["X-Request-Id"]

        when {
            response.isSuccessful && body?.success == true && body.data != null -> {
                ApiResult.Success(body.data, requestId ?: "unknown", body.meta)
            }
            response.code() == 401 -> {
                ApiResult.Error(ErrorCode.AUTH_REQUIRED, "Authentication required", requestId, 401)
            }
            response.code() == 429 -> {
                ApiResult.Error(ErrorCode.RATE_LIMITED, "Too many requests", requestId, 429)
            }
            response.code() == 404 -> {
                ApiResult.Error(ErrorCode.NOT_FOUND, "Not found", requestId, 404)
            }
            body?.error != null -> {
                ApiResult.Error(
                    parseErrorCode(body.error.code),
                    body.error.message,
                    requestId,
                    response.code()
                )
            }
            else -> {
                // For non-2xx responses, Retrofit puts the body in errorBody()
                val errorBody = response.errorBody()?.string()
                if (errorBody != null) {
                    try {
                        val envelope = errorJson.decodeFromString<ErrorEnvelope>(errorBody)
                        val rid = envelope.requestId ?: requestId
                        if (envelope.error != null) {
                            ApiResult.Error(
                                parseErrorCode(envelope.error.code),
                                envelope.error.message,
                                rid,
                                response.code()
                            )
                        } else {
                            ApiResult.Error(ErrorCode.SERVER_ERROR, "Server error", rid, response.code())
                        }
                    } catch (_: Exception) {
                        ApiResult.Error(ErrorCode.SERVER_ERROR, "Server error", requestId, response.code())
                    }
                } else {
                    ApiResult.Error(ErrorCode.SERVER_ERROR, "Server error", requestId, response.code())
                }
            }
        }
    } catch (e: IOException) {
        ApiResult.Error(ErrorCode.NETWORK_ERROR, "Network error: ${e.message}", null)
    } catch (e: Exception) {
        ApiResult.Error(ErrorCode.UNKNOWN, e.message ?: "Unknown error", null)
    }
}
