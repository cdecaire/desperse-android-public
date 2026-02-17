package app.desperse.data.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class ThreadResponse(
    val id: String,
    val otherUser: ThreadUser,
    val lastMessageAt: String? = null,
    val lastMessagePreview: String? = null,
    val hasUnread: Boolean = false,
    val isBlocked: Boolean = false,
    val isBlockedBy: Boolean = false,
    val createdAt: String
)

@Serializable
data class ThreadUser(
    val id: String,
    val usernameSlug: String,
    val displayName: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class ThreadListResponse(
    val threads: List<ThreadResponse>,
    val nextCursor: String? = null
)

@Serializable
data class CreateThreadResponse(
    val thread: CreateThreadData,
    val otherUser: ThreadUser,
    val created: Boolean = false
)

@Serializable
data class CreateThreadData(
    val id: String,
    val userAId: String,
    val userBId: String,
    val lastMessageAt: String? = null,
    val lastMessagePreview: String? = null,
    val createdAt: String
)

@Serializable
data class MessageResponse(
    val id: String,
    val threadId: String,
    val senderId: String,
    val content: String,
    val isDeleted: Boolean = false,
    val createdAt: String
)

@Serializable
data class MessagesListResponse(
    val messages: List<MessageResponse>,
    val otherLastReadAt: String? = null,
    val nextCursor: String? = null
)

@Serializable
data class SendMessageResponse(
    val message: MessageResponse
)

@Serializable
data class MarkReadResponse(
    val readAt: String
)

@Serializable
data class BlockResponse(
    val blocked: Boolean
)

@Serializable
data class ArchiveResponse(
    val archived: Boolean
)

@Serializable
data class DmEligibilityResponse(
    val allowed: Boolean = false,
    val eligibleVia: List<String> = emptyList(),
    val unlockPaths: List<UnlockPath> = emptyList(),
    val creatorDmsDisabled: Boolean? = null,
    val tipMinAmount: Int? = null,
    val creatorId: String? = null
)

@Serializable
data class UnlockPath(
    val method: String,
    val message: String
)

@Serializable
data class DmPreferencesResponse(
    val dmEnabled: Boolean = true,
    val allowBuyers: Boolean = true,
    val allowCollectors: Boolean = true,
    val collectorMinCount: Int = 3,
    val allowTippers: Boolean = true,
    val tipMinAmount: Int = 50
)

@Serializable
data class AblyTokenResponse(
    val keyName: String,
    val clientId: String? = null,
    val nonce: String,
    val mac: String,
    val timestamp: Long,
    val ttl: Long? = null,
    val capability: String? = null
)
