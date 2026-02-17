package app.desperse.core.network

import android.util.Log
import app.desperse.core.auth.PrivyAuthManager
import app.desperse.core.auth.TokenStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles 401 responses by refreshing the token via Privy SDK and retrying.
 *
 * When the server returns 401 (token expired), this authenticator:
 * 1. Gets a fresh token from Privy SDK (which handles refresh internally)
 * 2. Updates TokenStorage with the new token
 * 3. Retries the request with the new token
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val privyAuthManager: PrivyAuthManager,
    private val tokenStorage: TokenStorage
) : Authenticator {

    companion object {
        private const val TAG = "TokenAuthenticator"
        private const val MAX_RETRY_COUNT = 2
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d(TAG, "401 received for ${response.request.url.encodedPath}, attempting token refresh")

        // Prevent infinite retry loops
        val retryCount = response.request.header("X-Retry-Count")?.toIntOrNull() ?: 0
        if (retryCount >= MAX_RETRY_COUNT) {
            Log.w(TAG, "Max retry count reached, giving up")
            return null
        }

        // Get fresh token from Privy SDK
        // Note: runBlocking is used here because Authenticator.authenticate() is not suspending
        // This is acceptable for token refresh as it's infrequent and quick
        val newToken = runBlocking {
            try {
                val token = privyAuthManager.getAccessToken()
                if (token != null) {
                    Log.d(TAG, "Got fresh token from Privy (length=${token.length})")
                    tokenStorage.saveAccessToken(token)
                    token
                } else {
                    Log.w(TAG, "Privy returned null token - user may need to re-login")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh token", e)
                null
            }
        }

        if (newToken == null) {
            Log.w(TAG, "Token refresh failed, cannot retry request")
            return null
        }

        // Retry the request with the new token
        Log.d(TAG, "Retrying request with fresh token")
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .header("X-Retry-Count", (retryCount + 1).toString())
            .build()
    }
}
