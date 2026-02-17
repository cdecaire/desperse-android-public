package app.desperse.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AddWalletRequest(
    val address: String,
    val type: String,        // "embedded" or "external"
    val connector: String? = null,
    val label: String? = null
)

@Serializable
data class UpdateWalletLabelRequest(
    val address: String,
    val label: String
)
