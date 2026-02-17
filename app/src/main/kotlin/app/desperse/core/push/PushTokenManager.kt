package app.desperse.core.push

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.data.dto.request.RegisterPushTokenRequest
import app.desperse.data.dto.request.UnregisterPushTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pushTokenDataStore by preferencesDataStore(name = "push_token_prefs")

@Singleton
class PushTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: DesperseApi
) {
    companion object {
        private const val TAG = "PushTokenManager"
        private val KEY_REGISTERED_TOKEN = stringPreferencesKey("registered_token")
        private val KEY_PENDING_TOKEN = stringPreferencesKey("pending_token")
    }

    /**
     * Saves a token as pending for registration after auth completes.
     * Called from FirebaseMessagingService.onNewToken() which may fire before auth.
     */
    suspend fun savePendingToken(token: String) {
        context.pushTokenDataStore.edit { prefs ->
            prefs[KEY_PENDING_TOKEN] = token
        }
        Log.d(TAG, "Saved pending FCM token: ${token.take(10)}...")
    }

    /**
     * Ensures the current FCM token is registered with the server.
     * Called after successful authentication.
     */
    suspend fun ensureTokenRegistered() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM token obtained: ${token.take(10)}...")
            registerToken(token)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get FCM token", e)
        }
    }

    /**
     * Registers an FCM token with the server.
     * Caches the registered token to avoid redundant API calls.
     */
    suspend fun registerToken(token: String) {
        // Check if already registered
        val lastRegistered = context.pushTokenDataStore.data.first()[KEY_REGISTERED_TOKEN]
        if (lastRegistered == token) {
            Log.d(TAG, "Token already registered, skipping")
            return
        }

        val result = safeApiCall {
            api.registerPushToken(RegisterPushTokenRequest(token = token, platform = "android"))
        }

        when (result) {
            is ApiResult.Success -> {
                Log.d(TAG, "Push token registered successfully")
                context.pushTokenDataStore.edit { prefs ->
                    prefs[KEY_REGISTERED_TOKEN] = token
                    prefs.remove(KEY_PENDING_TOKEN)
                }
            }
            is ApiResult.Error -> {
                Log.w(TAG, "Failed to register push token: ${result.message}")
                // Store as pending to retry after next login
                context.pushTokenDataStore.edit { prefs ->
                    prefs[KEY_PENDING_TOKEN] = token
                }
            }
        }
    }

    /**
     * Unregisters the current FCM token from the server.
     * Called on logout.
     */
    suspend fun unregisterToken() {
        try {
            val token = context.pushTokenDataStore.data.first()[KEY_REGISTERED_TOKEN]
            if (token != null) {
                safeApiCall {
                    api.unregisterPushToken(UnregisterPushTokenRequest(token = token))
                }
                context.pushTokenDataStore.edit { prefs ->
                    prefs.remove(KEY_REGISTERED_TOKEN)
                    prefs.remove(KEY_PENDING_TOKEN)
                }
                Log.d(TAG, "Push token unregistered")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister push token", e)
        }
    }
}
