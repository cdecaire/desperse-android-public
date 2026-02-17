package app.desperse.core.realtime

import android.util.Log
import app.desperse.data.repository.MessageRepository
import com.google.gson.JsonObject
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.realtime.ConnectionStateListener
import io.ably.lib.rest.Auth
import io.ably.lib.types.AblyException
import io.ably.lib.types.ClientOptions
import io.ably.lib.types.ErrorInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

sealed class AblyConnectionState {
    data object Disconnected : AblyConnectionState()
    data object Connecting : AblyConnectionState()
    data object Connected : AblyConnectionState()
    data class Failed(val error: String?) : AblyConnectionState()
}

sealed class AblyEvent {
    data class NewMessage(
        val threadId: String,
        val messageId: String,
        val senderId: String,
        val createdAt: String
    ) : AblyEvent()

    data class MessageRead(
        val threadId: String,
        val readerId: String,
        val readAt: String
    ) : AblyEvent()
}

@Singleton
class AblyManager @Inject constructor(
    private val messageRepository: MessageRepository
) {
    companion object {
        private const val TAG = "AblyManager"
        private const val MAX_AUTH_FAILURES = 3
        private const val AUTH_COOLDOWN_MS = 60_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var ablyClient: AblyRealtime? = null
    private var userChannel: Channel? = null
    private var currentUserId: String? = null

    private var authFailureCount = 0
    private var lastAuthFailureTime = 0L

    private val _connectionState = MutableStateFlow<AblyConnectionState>(AblyConnectionState.Disconnected)
    val connectionState: StateFlow<AblyConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<AblyEvent>(extraBufferCapacity = 20)
    val events: SharedFlow<AblyEvent> = _events.asSharedFlow()

    fun connect(userId: String) {
        if (currentUserId == userId && ablyClient != null) {
            Log.d(TAG, "Already connected for user $userId")
            return
        }

        disconnect()
        currentUserId = userId
        _connectionState.value = AblyConnectionState.Connecting

        try {
            val options = ClientOptions().apply {
                autoConnect = true
                echoMessages = false
                authCallback = Auth.TokenCallback { _ ->
                    // Check cooldown to prevent rapid token request loops
                    if (authFailureCount >= MAX_AUTH_FAILURES) {
                        val elapsed = System.currentTimeMillis() - lastAuthFailureTime
                        if (elapsed < AUTH_COOLDOWN_MS) {
                            throw AblyException.fromErrorInfo(
                                ErrorInfo("Auth cooldown active", 401)
                            )
                        }
                        authFailureCount = 0
                    }

                    // Fetch token synchronously (Ably callback runs on a background thread)
                    try {
                        val tokenResponse = runBlocking {
                            messageRepository.getAblyToken()
                        }
                        val token = tokenResponse.getOrThrow()
                        authFailureCount = 0

                        // Build an Auth.TokenRequest from the server response
                        val tokenRequest = Auth.TokenRequest()
                        tokenRequest.keyName = token.keyName
                        tokenRequest.clientId = token.clientId
                        tokenRequest.nonce = token.nonce
                        tokenRequest.mac = token.mac
                        tokenRequest.timestamp = token.timestamp
                        if (token.ttl != null) tokenRequest.ttl = token.ttl
                        if (token.capability != null) tokenRequest.capability = token.capability
                        tokenRequest
                    } catch (e: Exception) {
                        authFailureCount++
                        lastAuthFailureTime = System.currentTimeMillis()
                        Log.e(TAG, "Auth callback failed ($authFailureCount/$MAX_AUTH_FAILURES)", e)
                        throw AblyException.fromErrorInfo(
                            ErrorInfo("Token fetch failed: ${e.message}", 401)
                        )
                    }
                }
            }

            ablyClient = AblyRealtime(options).also { client ->
                // Monitor connection state changes
                client.connection.on(object : ConnectionStateListener {
                    override fun onConnectionStateChanged(stateChange: ConnectionStateListener.ConnectionStateChange) {
                        Log.d(TAG, "Connection state: ${stateChange.previous} -> ${stateChange.current}")
                        _connectionState.value = when (stateChange.current) {
                            ConnectionState.connected -> AblyConnectionState.Connected
                            ConnectionState.connecting -> AblyConnectionState.Connecting
                            ConnectionState.disconnected -> AblyConnectionState.Disconnected
                            ConnectionState.closed -> AblyConnectionState.Disconnected
                            ConnectionState.failed -> AblyConnectionState.Failed(stateChange.reason?.message)
                            ConnectionState.suspended -> AblyConnectionState.Disconnected
                            else -> AblyConnectionState.Disconnected
                        }
                    }
                })

                // Subscribe to user-specific channel
                subscribeToUserChannel(client, userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Ably client", e)
            _connectionState.value = AblyConnectionState.Failed(e.message)
        }
    }

    private fun subscribeToUserChannel(client: AblyRealtime, userId: String) {
        try {
            val channel = client.channels.get("user:$userId")
            userChannel = channel

            // Subscribe to new_message events
            channel.subscribe("new_message") { message ->
                try {
                    val data = message.data
                    val threadId: String
                    val messageId: String
                    val senderId: String
                    val createdAt: String

                    when (data) {
                        is JsonObject -> {
                            threadId = data.get("threadId")?.asString ?: return@subscribe
                            messageId = data.get("messageId")?.asString ?: return@subscribe
                            senderId = data.get("senderId")?.asString ?: return@subscribe
                            createdAt = data.get("createdAt")?.asString ?: return@subscribe
                        }
                        is String -> {
                            // Fallback: parse JSON string via Gson
                            val parsed = com.google.gson.JsonParser.parseString(data).asJsonObject
                            threadId = parsed.get("threadId")?.asString ?: return@subscribe
                            messageId = parsed.get("messageId")?.asString ?: return@subscribe
                            senderId = parsed.get("senderId")?.asString ?: return@subscribe
                            createdAt = parsed.get("createdAt")?.asString ?: return@subscribe
                        }
                        else -> {
                            Log.w(TAG, "Unexpected new_message data type: ${data?.javaClass?.name}")
                            return@subscribe
                        }
                    }

                    scope.launch {
                        _events.emit(
                            AblyEvent.NewMessage(
                                threadId = threadId,
                                messageId = messageId,
                                senderId = senderId,
                                createdAt = createdAt
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse new_message event", e)
                }
            }

            // Subscribe to message_read events
            channel.subscribe("message_read") { message ->
                try {
                    val data = message.data
                    val threadId: String
                    val readerId: String
                    val readAt: String

                    when (data) {
                        is JsonObject -> {
                            threadId = data.get("threadId")?.asString ?: return@subscribe
                            readerId = data.get("readerId")?.asString ?: return@subscribe
                            readAt = data.get("readAt")?.asString ?: return@subscribe
                        }
                        is String -> {
                            val parsed = com.google.gson.JsonParser.parseString(data).asJsonObject
                            threadId = parsed.get("threadId")?.asString ?: return@subscribe
                            readerId = parsed.get("readerId")?.asString ?: return@subscribe
                            readAt = parsed.get("readAt")?.asString ?: return@subscribe
                        }
                        else -> {
                            Log.w(TAG, "Unexpected message_read data type: ${data?.javaClass?.name}")
                            return@subscribe
                        }
                    }

                    scope.launch {
                        _events.emit(
                            AblyEvent.MessageRead(
                                threadId = threadId,
                                readerId = readerId,
                                readAt = readAt
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse message_read event", e)
                }
            }

            Log.d(TAG, "Subscribed to channel user:$userId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to user channel", e)
        }
    }

    fun disconnect() {
        try {
            userChannel?.unsubscribe()
            userChannel = null
            ablyClient?.close()
            ablyClient = null
            currentUserId = null
            _connectionState.value = AblyConnectionState.Disconnected
            Log.d(TAG, "Disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }
    }
}
