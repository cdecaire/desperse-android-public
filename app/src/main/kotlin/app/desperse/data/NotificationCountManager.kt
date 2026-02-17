package app.desperse.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.feedDataStore: DataStore<Preferences> by preferencesDataStore(name = "feed_preferences")

/**
 * Creator info for new posts toast
 */
data class NewPostCreator(
    val id: String,
    val avatarUrl: String?,
    val displayName: String?,
    val slug: String
)

/**
 * Notification and new post counts for UI indicators
 */
data class NotificationCounters(
    val unreadNotifications: Int = 0,
    val forYouNewPostsCount: Int = 0,
    val forYouCreators: List<NewPostCreator> = emptyList(),
    val followingNewPostsCount: Int = 0,
    val followingCreators: List<NewPostCreator> = emptyList()
)

private const val TAG = "NotificationCountMgr"

/**
 * Global manager for notification and new post counts.
 * Polls the server periodically and provides state flows for UI consumption.
 */
@Singleton
class NotificationCountManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: DesperseApi
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val POLL_INTERVAL_MS = 60_000L // 60 seconds

        // DataStore keys for lastSeen timestamps
        private val FOR_YOU_LAST_SEEN_KEY = stringPreferencesKey("feed:lastSeen:forYou")
        private val FOLLOWING_LAST_SEEN_KEY = stringPreferencesKey("feed:lastSeen:following")
    }

    private val _counters = MutableStateFlow(NotificationCounters())
    val counters: StateFlow<NotificationCounters> = _counters.asStateFlow()

    /** Convenience flow for whether there are unread notifications */
    private val _hasUnreadNotifications = MutableStateFlow(false)
    val hasUnreadNotifications: StateFlow<Boolean> = _hasUnreadNotifications.asStateFlow()

    init {
        // Keep hasUnreadNotifications in sync with counters
        scope.launch {
            _counters.collect { counters ->
                _hasUnreadNotifications.value = counters.unreadNotifications > 0
            }
        }
    }

    private var pollingJob: Job? = null
    private var isPolling = false
    private var isAppForeground = true
    private var lifecycleRegistered = false

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            // App came to foreground
            isAppForeground = true
            if (isPolling) {
                Log.d(TAG, "App foregrounded, resuming polling")
                startPollingInternal()
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            // App went to background
            isAppForeground = false
            Log.d(TAG, "App backgrounded, pausing polling")
            pollingJob?.cancel()
            pollingJob = null
        }
    }

    /**
     * Start polling for notification counts.
     * Automatically pauses when app is backgrounded and resumes on foreground.
     */
    fun startPolling() {
        if (isPolling) return
        isPolling = true

        // Register lifecycle observer once
        if (!lifecycleRegistered) {
            lifecycleRegistered = true
            scope.launch(Dispatchers.Main) {
                ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
            }
        }

        if (isAppForeground) {
            startPollingInternal()
        }
    }

    private fun startPollingInternal() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            Log.d(TAG, "Starting notification count polling")
            // Immediate fetch
            fetchCounters()

            // Then poll periodically
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                fetchCounters()
            }
        }
    }

    /**
     * Stop polling completely.
     * Call on logout.
     */
    fun stopPolling() {
        Log.d(TAG, "Stopping notification count polling")
        isPolling = false
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * Fetch notification counters from server.
     * Called by polling and can be called manually for immediate refresh.
     */
    suspend fun fetchCounters() {
        try {
            // Get lastSeen timestamps
            val prefs = context.feedDataStore.data.first()
            val forYouLastSeen = prefs[FOR_YOU_LAST_SEEN_KEY]
            val followingLastSeen = prefs[FOLLOWING_LAST_SEEN_KEY]

            when (val result = safeApiCall {
                api.getNotificationCounters(
                    forYouLastSeen = forYouLastSeen,
                    followingLastSeen = followingLastSeen
                )
            }) {
                is ApiResult.Success -> {
                    val data = result.data
                    _counters.value = NotificationCounters(
                        unreadNotifications = data.unreadNotifications,
                        forYouNewPostsCount = data.forYou?.newPostsCount ?: 0,
                        forYouCreators = data.forYou?.creators?.map { creator ->
                            NewPostCreator(
                                id = creator.id,
                                avatarUrl = creator.avatarUrl,
                                displayName = creator.displayName,
                                slug = creator.slug
                            )
                        } ?: emptyList(),
                        followingNewPostsCount = data.following?.newPostsCount ?: 0,
                        followingCreators = data.following?.creators?.map { creator ->
                            NewPostCreator(
                                id = creator.id,
                                avatarUrl = creator.avatarUrl,
                                displayName = creator.displayName,
                                slug = creator.slug
                            )
                        } ?: emptyList()
                    )
                }
                is ApiResult.Error -> {
                    Log.w(TAG, "Failed to fetch notification counters: ${result.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching notification counters", e)
        }
    }

    /**
     * Update lastSeen timestamp for a feed tab.
     * Call after successfully loading/refreshing a feed.
     */
    suspend fun updateLastSeen(tab: String, timestamp: String) {
        val key = when (tab) {
            "for-you" -> FOR_YOU_LAST_SEEN_KEY
            "following" -> FOLLOWING_LAST_SEEN_KEY
            else -> return
        }

        context.feedDataStore.edit { prefs ->
            prefs[key] = timestamp
        }

        // Also update for the other tab to prevent duplicate badges
        // (same behavior as web app)
        val otherKey = when (tab) {
            "for-you" -> FOLLOWING_LAST_SEEN_KEY
            "following" -> FOR_YOU_LAST_SEEN_KEY
            else -> return
        }
        context.feedDataStore.edit { prefs ->
            // Only update if current is older
            val current = prefs[otherKey]
            if (current == null || current < timestamp) {
                prefs[otherKey] = timestamp
            }
        }

        // Clear counts for the tab that was just viewed
        _counters.value = when (tab) {
            "for-you" -> _counters.value.copy(forYouNewPostsCount = 0, forYouCreators = emptyList())
            "following" -> _counters.value.copy(followingNewPostsCount = 0, followingCreators = emptyList())
            else -> _counters.value
        }
    }

    /**
     * Clear unread notification count.
     * Call after viewing notifications screen.
     */
    fun clearUnreadNotifications() {
        _counters.value = _counters.value.copy(unreadNotifications = 0)
    }

    /**
     * Manually set the unread notification count.
     * Useful when NotificationsViewModel knows the actual count.
     */
    fun setUnreadNotificationCount(count: Int) {
        _counters.value = _counters.value.copy(unreadNotifications = count)
    }
}
