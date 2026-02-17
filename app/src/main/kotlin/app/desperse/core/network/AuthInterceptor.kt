package app.desperse.core.network

import android.util.Log
import app.desperse.core.auth.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intercepts requests to add Authorization header with cached token.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.encodedPath
        val token = tokenStorage.getCachedAccessToken()

        val newRequest = if (token != null) {
            Log.d(TAG, "Adding auth header to $url (token length=${token.length})")
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            Log.w(TAG, "No auth token available for $url - request will be unauthenticated!")
            request
        }

        return chain.proceed(newRequest)
    }
}
