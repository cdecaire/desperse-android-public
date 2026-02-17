package app.desperse.data

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.core.realtime.AblyEvent
import app.desperse.core.realtime.AblyManager
import app.desperse.data.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UnreadMessageMgr"

/**
 * Global manager for unread message state.
 * Tracks whether the user has any unread message threads for bottom nav badge display.
 *
 * Uses a hybrid approach:
 * - Fetches initial state from threads API on start
 * - Observes Ably real-time events for instant updates
 * - Polls periodically as a safety net
 */
@Singleton
class UnreadMessageManager @Inject constructor(
    private val api: DesperseApi,
    private val ablyManager: AblyManager,
    private val messageRepository: MessageRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val POLL_INTERVAL_MS = 60_000L
    }

    private val _hasUnreadMessages = MutableStateFlow(false)
    val hasUnreadMessages: StateFlow<Boolean> = _hasUnreadMessages.asStateFlow()

    /** Track unread thread IDs for accurate local bookkeeping */
    private val unreadThreadIds = mutableSetOf<String>()

    private var pollingJob: Job? = null
    private var ablyJob: Job? = null
    private var isRunning = false
    private var isAppForeground = true
    private var lifecycleRegistered = false

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            // App came to foreground
            isAppForeground = true
            if (isRunning) {
                Log.d(TAG, "App foregrounded, resuming message polling")
                startPollingInternal()
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            // App went to background - pause polling but keep Ably
            isAppForeground = false
            Log.d(TAG, "App backgrounded, pausing message polling")
            pollingJob?.cancel()
            pollingJob = null
        }
    }

    /**
     * Start tracking unread messages.
     * Call when user authenticates.
     * Automatically pauses polling when app is backgrounded.
     */
    fun start() {
        if (isRunning) return
        isRunning = true
        Log.d(TAG, "Starting unread message tracking")

        // Register lifecycle observer once
        if (!lifecycleRegistered) {
            lifecycleRegistered = true
            scope.launch(Dispatchers.Main) {
                ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
            }
        }

        // Fetch initial state
        scope.launch { fetchUnreadStatus() }

        // Observe Ably events for real-time updates (always active)
        ablyJob?.cancel()
        ablyJob = scope.launch { observeAblyEvents() }

        // Start polling only if app is in foreground
        if (isAppForeground) {
            startPollingInternal()
        }
    }

    private fun startPollingInternal() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                fetchUnreadStatus()
            }
        }
    }

    /**
     * Stop tracking completely.
     * Call on logout.
     */
    fun stop() {
        Log.d(TAG, "Stopping unread message tracking")
        isRunning = false
        pollingJob?.cancel()
        pollingJob = null
        ablyJob?.cancel()
        ablyJob = null
        synchronized(unreadThreadIds) { unreadThreadIds.clear() }
        _hasUnreadMessages.value = false
    }

    /**
     * Notify that a new message arrived in a thread.
     * Called from Ably event observation — immediately shows badge.
     */
    fun onNewMessage(threadId: String) {
        synchronized(unreadThreadIds) {
            unreadThreadIds.add(threadId)
        }
        _hasUnreadMessages.value = true
    }

    /**
     * Notify that a thread was read by the current user.
     * Updates local state immediately (for badge).
     */
    fun onThreadRead(threadId: String) {
        synchronized(unreadThreadIds) {
            unreadThreadIds.remove(threadId)
        }
        _hasUnreadMessages.value = synchronized(unreadThreadIds) { unreadThreadIds.isNotEmpty() }
    }

    /**
     * Mark a thread as read on the server AND update local state.
     * Runs on the manager's own scope so it survives ViewModel destruction
     * (e.g., when user navigates back from a conversation).
     */
    fun markThreadRead(threadId: String) {
        onThreadRead(threadId)
        scope.launch {
            try {
                messageRepository.markThreadRead(threadId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to mark thread $threadId as read", e)
            }
        }
    }

    /**
     * Force refresh unread status from server.
     * Called when thread list loads or user navigates to messages.
     */
    suspend fun refreshUnreadStatus() {
        fetchUnreadStatus()
    }

    private suspend fun fetchUnreadStatus() {
        try {
            when (val result = safeApiCall { api.getThreads(limit = 50) }) {
                is ApiResult.Success -> {
                    synchronized(unreadThreadIds) {
                        unreadThreadIds.clear()
                        result.data.threads
                            .filter { it.hasUnread }
                            .forEach { unreadThreadIds.add(it.id) }
                    }
                    _hasUnreadMessages.value = synchronized(unreadThreadIds) { unreadThreadIds.isNotEmpty() }
                }
                is ApiResult.Error -> {
                    Log.w(TAG, "Failed to fetch thread unread status: ${result.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching unread status", e)
        }
    }

    private suspend fun observeAblyEvents() {
        ablyManager.events.collect { event ->
            when (event) {
                is AblyEvent.NewMessage -> {
                    onNewMessage(event.threadId)
                }
                is AblyEvent.MessageRead -> {
                    // This fires when the OTHER user reads our messages — not relevant for our unread state
                }
            }
        }
    }
}
