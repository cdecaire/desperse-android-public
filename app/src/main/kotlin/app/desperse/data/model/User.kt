package app.desperse.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class User(
    val id: String,
    val slug: String,
    val displayName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    @SerialName("headerBgUrl")
    val headerUrl: String? = null,
    val walletAddress: String? = null,
    val website: String? = null,
    val twitterUsername: String? = null,
    val instagramUsername: String? = null,
    val isVerified: Boolean = false,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val postCount: Int = 0
)
