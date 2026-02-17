package app.desperse.data.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateThreadRequest(
    val otherUserId: String,
    val contextCreatorId: String
)

@Serializable
data class SendMessageRequest(
    val content: String
)

@Serializable
data class BlockRequest(
    val blocked: Boolean
)

@Serializable
data class ArchiveRequest(
    val archived: Boolean
)

@Serializable
data class UpdateDmPreferencesRequest(
    val dmEnabled: Boolean? = null,
    val allowBuyers: Boolean? = null,
    val allowCollectors: Boolean? = null,
    val collectorMinCount: Int? = null,
    val allowTippers: Boolean? = null,
    val tipMinAmount: Int? = null
)
