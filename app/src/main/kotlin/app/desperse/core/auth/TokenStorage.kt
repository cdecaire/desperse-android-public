package app.desperse.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Nullable: null if EncryptedSharedPreferences failed to initialize.
    // In that case, tokens are only kept in-memory and will not persist across restarts.
    private val prefs: SharedPreferences? by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "desperse_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences; " +
                    "tokens will be in-memory only", e)
            null
        }
    }

    // In-memory cache for interceptor (sync read)
    @Volatile
    private var cachedToken: String? = null

    fun getCachedAccessToken(): String? = cachedToken

    suspend fun saveAccessToken(token: String) {
        cachedToken = token
        withContext(Dispatchers.IO) {
            prefs?.edit()?.putString(KEY_ACCESS_TOKEN, token)?.apply()
        }
    }

    suspend fun loadCachedToken() {
        cachedToken = withContext(Dispatchers.IO) {
            prefs?.getString(KEY_ACCESS_TOKEN, null)
        }
    }

    suspend fun clearTokens() {
        cachedToken = null
        withContext(Dispatchers.IO) {
            prefs?.edit()?.clear()?.apply()
        }
    }

    // --- MWA auth tokens, keyed per wallet address ---

    suspend fun saveMwaAuthToken(walletAddress: String, token: String) {
        withContext(Dispatchers.IO) {
            prefs?.edit()
                ?.putString("${KEY_MWA_AUTH_TOKEN_PREFIX}$walletAddress", token)
                ?.apply()
        }
    }

    suspend fun getMwaAuthToken(walletAddress: String): String? {
        return withContext(Dispatchers.IO) {
            prefs?.getString("${KEY_MWA_AUTH_TOKEN_PREFIX}$walletAddress", null)
        }
    }

    suspend fun clearMwaAuthTokens() {
        withContext(Dispatchers.IO) {
            val sharedPrefs = prefs ?: return@withContext
            val editor = sharedPrefs.edit()
            sharedPrefs.all.keys
                .filter { it.startsWith(KEY_MWA_AUTH_TOKEN_PREFIX) }
                .forEach { editor.remove(it) }
            editor.apply()
        }
    }

    companion object {
        private const val TAG = "TokenStorage"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_MWA_AUTH_TOKEN_PREFIX = "mwa_auth_token_"
    }
}
