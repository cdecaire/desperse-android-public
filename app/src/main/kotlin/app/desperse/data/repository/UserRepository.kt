package app.desperse.data.repository

import android.os.Trace
import android.util.Log
import app.desperse.core.auth.PrivyAuthManager
import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.core.wallet.MwaManager
import app.desperse.core.wallet.WalletPreferences
import app.desperse.core.wallet.WalletType
import app.desperse.data.dto.request.InitAuthRequest
import app.desperse.data.dto.request.UpdateProfileRequest
import app.desperse.data.dto.request.UploadImageRequest
import app.desperse.data.model.Post
import app.desperse.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for current user data.
 *
 * Provides a centralized source of truth for the authenticated user's profile.
 * This is used throughout the app for:
 * - Bottom navigation avatar
 * - Profile screen (own profile)
 * - Settings
 * - Creating posts (author info)
 * - Any feature that needs current user data
 */
@Singleton
class UserRepository @Inject constructor(
    private val api: DesperseApi,
    private val privyAuthManager: PrivyAuthManager,
    private val mwaManager: MwaManager,
    private val walletPreferences: WalletPreferences,
    private val walletRepository: WalletRepository
) {
    companion object {
        private const val TAG = "UserRepository"
        /** Delay before starting wallet registration to let feed render first */
        private const val WALLET_REG_DELAY_MS = 3_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _currentUser = MutableStateFlow<User?>(null)
    private val initAuthMutex = Mutex()

    /**
     * Defers wallet registration and Seeker auto-set to a background coroutine.
     * This avoids 2-3 extra network calls blocking the critical startup path,
     * letting the feed render first.
     */
    private fun deferWalletRegistration() {
        scope.launch {
            delay(WALLET_REG_DELAY_MS)
            Log.d(TAG, "Starting deferred wallet registration")
            ensureWalletsRegistered()
            autoSetMwaOnSeeker()
        }
    }

    /**
     * Auto-set MWA wallet as primary on Seeker devices if user hasn't already set a preference.
     * Called after successful auth init to provide a seamless experience on Seeker hardware.
     */
    private suspend fun autoSetMwaOnSeeker() {
        try {
            if (!mwaManager.isSeekerDevice()) return
            if (walletPreferences.hasUserSetPreference()) {
                Log.d(TAG, "User already has a wallet preference set, skipping Seeker auto-set")
                return
            }

            val wallets = walletPreferences.wallets.value
            val mwaWallet = wallets.firstOrNull {
                it.type == WalletType.EXTERNAL && it.connector == "mwa"
            }

            if (mwaWallet != null) {
                walletPreferences.setActiveWallet(mwaWallet)
                Log.d(TAG, "Auto-set MWA wallet as primary on Seeker device: ${mwaWallet.address.take(8)}...")
            } else {
                Log.d(TAG, "Seeker device detected but no MWA wallet found in wallet list")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to auto-set MWA wallet on Seeker", e)
        }
    }

    /**
     * Register wallets in user_wallets table after login.
     * Ensures embedded wallet is registered as 'embedded' and SIWS signing wallet as 'external'.
     * Fire-and-forget: failures are logged but never block login.
     */
    private suspend fun ensureWalletsRegistered() {
        Trace.beginSection("UserRepo.walletReg")
        try {
            val authInfo = privyAuthManager.getAuthInitInfo() ?: return

            // 1. Fetch existing wallets first to avoid redundant add calls
            val existingAddresses = when (val result = walletRepository.getUserWallets()) {
                is ApiResult.Success -> result.data.map { it.address }.toSet()
                is ApiResult.Error -> {
                    Log.w(TAG, "Failed to fetch wallets, will attempt registration anyway")
                    emptySet()
                }
            }

            // 2. Get/create embedded wallet and register as 'embedded' (if not already registered)
            val embeddedAddress = try {
                privyAuthManager.getOrCreateEmbeddedWallet().getOrNull()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get/create embedded wallet for registration", e)
                null
            }

            if (embeddedAddress != null && embeddedAddress !in existingAddresses) {
                walletRepository.ensureWalletExists(embeddedAddress, "embedded", "privy", "Desperse Wallet")
            }

            // 3. For SIWS logins, register signing wallet as 'external' (if not already registered)
            if (authInfo.isSiwsLogin && authInfo.siwsWalletAddress != null) {
                val walletName = authInfo.siwsWalletClientType
                    ?.replaceFirstChar { it.uppercase() }
                val label = if (walletName != null) "$walletName Wallet" else "External Wallet"
                if (authInfo.siwsWalletAddress != embeddedAddress &&
                    authInfo.siwsWalletAddress !in existingAddresses) {
                    walletRepository.ensureWalletExists(
                        authInfo.siwsWalletAddress, "external", "mwa", label
                    )
                }
            }

            // 4. Re-sync wallet list if we added any new wallets
            if ((embeddedAddress != null && embeddedAddress !in existingAddresses) ||
                (authInfo.isSiwsLogin && authInfo.siwsWalletAddress != null &&
                    authInfo.siwsWalletAddress !in existingAddresses)) {
                walletRepository.getUserWallets()
            }
        } catch (e: Exception) {
            Log.w(TAG, "ensureWalletsRegistered failed (non-blocking)", e)
        } finally {
            Trace.endSection() // UserRepo.walletReg
        }
    }

    /**
     * The currently authenticated user's profile.
     * Null if not logged in or profile hasn't been fetched yet.
     */
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    /**
     * Whether we're currently fetching the user profile.
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Initialize auth with the backend after login.
     * This syncs the user with the backend and returns the user profile.
     * Call this after successful authentication (Privy login, including SIWS).
     *
     * Strategy:
     * 1. First try GET /users/me to check if user already exists
     * 2. If user exists, we're done
     * 3. If not, try POST /auth/init to create/sync the user
     *
     * @return Result containing the User or an error
     */
    suspend fun initAuth(): Result<User> {
        // Prevent concurrent initAuth calls (e.g., from duplicate AuthGateViewModel
        // instances during activity recreation on deeplink callbacks)
        if (!initAuthMutex.tryLock()) {
            Log.d(TAG, "initAuth() already in progress, waiting for completion...")
            initAuthMutex.lock()
            initAuthMutex.unlock()
            val user = _currentUser.value
            return if (user != null) {
                Log.d(TAG, "initAuth() completed by other caller, returning cached user: ${user.slug}")
                Result.success(user)
            } else {
                Result.failure(Exception("initAuth completed without user"))
            }
        }

        _isLoading.value = true
        Log.d(TAG, "initAuth() called")
        Trace.beginSection("UserRepo.initAuth")

        return try {
            // First, try to fetch existing user profile
            Log.d(TAG, "Checking if user already exists via /users/me...")
            Trace.beginSection("UserRepo.fetchMe")
            val meResult = safeApiCall { api.getCurrentUser() }
            Trace.endSection() // UserRepo.fetchMe
            when (meResult) {
                is ApiResult.Success -> {
                    val user = meResult.data.user
                    Log.d(TAG, "GET /users/me response: user=${user?.slug ?: "null"}")
                    if (user != null) {
                        _currentUser.value = user
                        Log.d(TAG, "User already exists: ${user.slug}")
                        deferWalletRegistration()
                        return Result.success(user)
                    }
                    Log.d(TAG, "No existing user found, will try initAuth")
                }
                is ApiResult.Error -> {
                    Log.d(TAG, "Failed to fetch current user: ${meResult.message}, will try initAuth")
                }
            }

            // Get wallet address and email from Privy SDK
            var authInfo = privyAuthManager.getAuthInitInfo()

            // If no wallet exists yet, try to create one
            if (authInfo == null) {
                Log.d(TAG, "No wallet found, attempting to create embedded wallet...")
                val walletResult = privyAuthManager.getOrCreateEmbeddedWallet()
                if (walletResult.isFailure) {
                    Log.e(TAG, "Failed to create wallet: ${walletResult.exceptionOrNull()?.message}")
                    return Result.failure(walletResult.exceptionOrNull() ?: Exception("Failed to create wallet"))
                }
                // Now try to get auth info again
                authInfo = privyAuthManager.getAuthInitInfo()
            }

            if (authInfo == null) {
                Log.e(TAG, "No auth init info available after wallet creation attempt")
                return Result.failure(Exception("Wallet not available"))
            }

            val request = InitAuthRequest(
                walletAddress = authInfo.walletAddress,
                email = authInfo.email
            )
            Log.d(TAG, "Calling POST /auth/init with wallet: ${authInfo.walletAddress}")

            when (val result = safeApiCall { api.initAuth(request) }) {
                is ApiResult.Success -> {
                    val user = result.data.user
                    if (user != null) {
                        _currentUser.value = user
                        Log.d(TAG, "Auth init successful, user: ${user.slug}")
                        deferWalletRegistration()
                        Result.success(user)
                    } else {
                        Log.e(TAG, "Auth init returned null user in response")
                        Result.failure(Exception("User not found"))
                    }
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "POST /auth/init failed: code=${result.code}, message=${result.message}, httpCode=${result.httpCode}")
                    Result.failure(Exception(result.message))
                }
            }
        } finally {
            Trace.endSection() // UserRepo.initAuth
            _isLoading.value = false
            initAuthMutex.unlock()
        }
    }

    /**
     * Fetch the current user's profile from the API.
     * Use initAuth() after login; use this for refreshing profile data.
     *
     * @return Result containing the User or an error
     */
    suspend fun fetchCurrentUser(): Result<User> {
        _isLoading.value = true

        return try {
            when (val result = safeApiCall { api.getCurrentUser() }) {
                is ApiResult.Success -> {
                    val user = result.data.user
                    if (user != null) {
                        _currentUser.value = user
                        Log.d(TAG, "Fetched current user: ${user.slug}")
                        Result.success(user)
                    } else {
                        Log.e(TAG, "API returned null user")
                        Result.failure(Exception("User not found"))
                    }
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Failed to fetch current user: ${result.message}")
                    Result.failure(Exception(result.message))
                }
            }
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Update the cached current user.
     * Use this after the user updates their profile.
     */
    fun updateCurrentUser(user: User) {
        _currentUser.value = user
        Log.d(TAG, "Updated current user: ${user.slug}")
    }

    /**
     * Update the current user's profile.
     */
    suspend fun updateProfile(
        displayName: String? = null,
        bio: String? = null,
        usernameSlug: String? = null,
        website: String? = null,
        avatarUrl: String? = null,
        headerUrl: String? = null
    ): Result<User> {
        Log.d(TAG, "updateProfile called - displayName=$displayName, bio=$bio, usernameSlug=$usernameSlug")

        val request = UpdateProfileRequest(
            displayName = displayName,
            bio = bio,
            usernameSlug = usernameSlug,
            website = website,
            avatarUrl = avatarUrl,
            headerUrl = headerUrl
        )

        return when (val result = safeApiCall { api.updateProfile(request) }) {
            is ApiResult.Success -> {
                val updatedUser = result.data.user
                // Update the current user with the new profile data
                val currentProfile = _currentUser.value
                if (currentProfile != null) {
                    val newUser = currentProfile.copy(
                        slug = updatedUser.slug,
                        displayName = updatedUser.displayName,
                        bio = updatedUser.bio,
                        avatarUrl = updatedUser.avatarUrl,
                        headerUrl = updatedUser.headerBgUrl,
                        website = updatedUser.link
                    )
                    _currentUser.value = newUser
                    Log.d(TAG, "Profile updated successfully: ${newUser.slug}")
                    Result.success(newUser)
                } else {
                    Result.failure(Exception("No current user"))
                }
            }
            is ApiResult.Error -> {
                Log.e(TAG, "updateProfile failed: ${result.message}")
                Result.failure(Exception(result.message))
            }
        }
    }

    /**
     * Upload a new avatar image.
     * @param base64Data Base64-encoded image data
     * @param fileName Original file name
     * @param mimeType MIME type (e.g., "image/jpeg")
     * @param fileSize Size in bytes
     * @return URL of the uploaded avatar
     */
    suspend fun uploadAvatar(
        base64Data: String,
        fileName: String,
        mimeType: String,
        fileSize: Int
    ): Result<String> {
        Log.d(TAG, "uploadAvatar called - fileName=$fileName, mimeType=$mimeType, fileSize=$fileSize")

        val request = UploadImageRequest(
            fileData = base64Data,
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileSize
        )

        return when (val result = safeApiCall { api.uploadAvatar(request) }) {
            is ApiResult.Success -> {
                val url = result.data.url
                // Update cached user with new avatar
                _currentUser.value?.let { user ->
                    _currentUser.value = user.copy(avatarUrl = url)
                }
                Log.d(TAG, "Avatar uploaded successfully: $url")
                Result.success(url)
            }
            is ApiResult.Error -> {
                Log.e(TAG, "uploadAvatar failed: ${result.message}")
                Result.failure(Exception(result.message))
            }
        }
    }

    /**
     * Upload a new header background image.
     * @param base64Data Base64-encoded image data
     * @param fileName Original file name
     * @param mimeType MIME type (e.g., "image/jpeg")
     * @param fileSize Size in bytes
     * @return URL of the uploaded header
     */
    suspend fun uploadHeader(
        base64Data: String,
        fileName: String,
        mimeType: String,
        fileSize: Int
    ): Result<String> {
        Log.d(TAG, "uploadHeader called - fileName=$fileName, mimeType=$mimeType, fileSize=$fileSize")

        val request = UploadImageRequest(
            fileData = base64Data,
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileSize
        )

        return when (val result = safeApiCall { api.uploadHeader(request) }) {
            is ApiResult.Success -> {
                val url = result.data.url
                // Update cached user with new header
                _currentUser.value?.let { user ->
                    _currentUser.value = user.copy(headerUrl = url)
                }
                Log.d(TAG, "Header uploaded successfully: $url")
                Result.success(url)
            }
            is ApiResult.Error -> {
                Log.e(TAG, "uploadHeader failed: ${result.message}")
                Result.failure(Exception(result.message))
            }
        }
    }

    /**
     * Clear the current user on logout.
     */
    fun clearCurrentUser() {
        _currentUser.value = null
        Log.d(TAG, "Cleared current user")
    }

    /**
     * Get a user profile by slug with stats and follow info.
     */
    suspend fun getUserProfile(slug: String): Result<ProfileData> {
        return when (val result = safeApiCall { api.getUserProfile(slug) }) {
            is ApiResult.Success -> {
                val data = result.data
                Result.success(ProfileData(
                    user = data.user,
                    stats = data.stats,
                    followersCount = data.followersCount,
                    followingCount = data.followingCount,
                    collectorsCount = data.collectorsCount,
                    isFollowing = data.isFollowing
                ))
            }
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    /**
     * Get paginated posts created by a user.
     */
    suspend fun getUserPosts(slug: String, cursor: String? = null, limit: Int = 20): Result<UserPostsData> {
        Log.d(TAG, "getUserPosts called for slug=$slug, cursor=$cursor")
        return when (val result = safeApiCall { api.getUserPosts(slug, cursor, limit) }) {
            is ApiResult.Success -> {
                Log.d(TAG, "getUserPosts success: ${result.data.posts.size} posts")
                Result.success(UserPostsData(
                    posts = result.data.posts,
                    hasMore = result.meta?.hasMore ?: false,
                    nextCursor = result.meta?.nextCursor
                ))
            }
            is ApiResult.Error -> {
                Log.e(TAG, "getUserPosts error: code=${result.code}, message=${result.message}")
                Result.failure(Exception(result.message))
            }
        }
    }

    /**
     * Get paginated items collected by a user.
     */
    suspend fun getUserCollected(slug: String, cursor: String? = null, limit: Int = 20): Result<UserPostsData> {
        Log.d(TAG, "getUserCollected called for slug=$slug, cursor=$cursor")
        return when (val result = safeApiCall { api.getUserCollected(slug, cursor, limit) }) {
            is ApiResult.Success -> {
                Log.d(TAG, "getUserCollected success: ${result.data.posts.size} posts")
                Result.success(UserPostsData(
                    posts = result.data.posts,
                    hasMore = result.meta?.hasMore ?: false,
                    nextCursor = result.meta?.nextCursor
                ))
            }
            is ApiResult.Error -> {
                Log.e(TAG, "getUserCollected error: code=${result.code}, message=${result.message}")
                Result.failure(Exception(result.message))
            }
        }
    }

    /**
     * Get paginated editions for sale by a user.
     */
    suspend fun getUserForSale(slug: String, cursor: String? = null, limit: Int = 20): Result<UserPostsData> {
        Log.d(TAG, "getUserForSale called for slug=$slug, cursor=$cursor")
        return when (val result = safeApiCall { api.getUserForSale(slug, cursor, limit) }) {
            is ApiResult.Success -> {
                Log.d(TAG, "getUserForSale success: ${result.data.posts.size} posts")
                Result.success(UserPostsData(
                    posts = result.data.posts,
                    hasMore = result.meta?.hasMore ?: false,
                    nextCursor = result.meta?.nextCursor
                ))
            }
            is ApiResult.Error -> {
                Log.e(TAG, "getUserForSale error: code=${result.code}, message=${result.message}")
                Result.failure(Exception(result.message))
            }
        }
    }

    /**
     * Follow a user.
     */
    suspend fun followUser(userId: String): Result<Boolean> {
        return when (val result = safeApiCall { api.followUser(userId) }) {
            is ApiResult.Success -> Result.success(result.data.isFollowing)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    /**
     * Unfollow a user.
     */
    suspend fun unfollowUser(userId: String): Result<Boolean> {
        return when (val result = safeApiCall { api.unfollowUser(userId) }) {
            is ApiResult.Success -> Result.success(result.data.isFollowing)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    /**
     * Get paginated list of followers for a user.
     */
    suspend fun getUserFollowers(slug: String, cursor: String? = null, limit: Int = 50): Result<FollowListData> {
        Log.d(TAG, "getUserFollowers called for slug=$slug, cursor=$cursor")
        return when (val result = safeApiCall { api.getUserFollowers(slug, cursor, limit) }) {
            is ApiResult.Success -> {
                Log.d(TAG, "getUserFollowers success: ${result.data.users.size} users")
                Result.success(FollowListData(
                    users = result.data.users,
                    hasMore = result.meta?.hasMore ?: false,
                    nextCursor = result.meta?.nextCursor
                ))
            }
            is ApiResult.Error -> {
                Log.e(TAG, "getUserFollowers error: code=${result.code}, message=${result.message}")
                Result.failure(Exception(result.message))
            }
        }
    }

    /**
     * Get paginated list of users that a user is following.
     */
    suspend fun getUserFollowing(slug: String, cursor: String? = null, limit: Int = 50): Result<FollowListData> {
        Log.d(TAG, "getUserFollowing called for slug=$slug, cursor=$cursor")
        return when (val result = safeApiCall { api.getUserFollowing(slug, cursor, limit) }) {
            is ApiResult.Success -> {
                Log.d(TAG, "getUserFollowing success: ${result.data.users.size} users")
                Result.success(FollowListData(
                    users = result.data.users,
                    hasMore = result.meta?.hasMore ?: false,
                    nextCursor = result.meta?.nextCursor
                ))
            }
            is ApiResult.Error -> {
                Log.e(TAG, "getUserFollowing error: code=${result.code}, message=${result.message}")
                Result.failure(Exception(result.message))
            }
        }
    }

    /**
     * Get paginated list of collectors for a user's creations.
     */
    suspend fun getUserCollectors(slug: String, cursor: String? = null, limit: Int = 50): Result<FollowListData> {
        Log.d(TAG, "getUserCollectors called for slug=$slug, cursor=$cursor")
        return when (val result = safeApiCall { api.getUserCollectors(slug, cursor, limit) }) {
            is ApiResult.Success -> {
                Log.d(TAG, "getUserCollectors success: ${result.data.users.size} users")
                Result.success(FollowListData(
                    users = result.data.users,
                    hasMore = result.meta?.hasMore ?: false,
                    nextCursor = result.meta?.nextCursor
                ))
            }
            is ApiResult.Error -> {
                Log.e(TAG, "getUserCollectors error: code=${result.code}, message=${result.message}")
                Result.failure(Exception(result.message))
            }
        }
    }

    /**
     * Get user's activity feed (own user only).
     */
    suspend fun getUserActivity(cursor: String? = null, limit: Int = 50): Result<ActivityData> {
        Log.d(TAG, "getUserActivity called, cursor=$cursor")
        return when (val result = safeApiCall { api.getUserActivity(cursor, limit) }) {
            is ApiResult.Success -> {
                Log.d(TAG, "getUserActivity success: ${result.data.activities.size} activities")
                Result.success(ActivityData(
                    activities = result.data.activities,
                    hasMore = result.meta?.hasMore ?: false,
                    nextCursor = result.meta?.nextCursor
                ))
            }
            is ApiResult.Error -> {
                Log.e(TAG, "getUserActivity error: code=${result.code}, message=${result.message}")
                Result.failure(Exception(result.message))
            }
        }
    }
}

/**
 * Combined profile data for a user.
 */
data class ProfileData(
    val user: app.desperse.data.dto.response.ProfileUser,
    val stats: app.desperse.data.dto.response.ProfileStats,
    val followersCount: Int,
    val followingCount: Int,
    val collectorsCount: Int,
    val isFollowing: Boolean
)

/**
 * Paginated user posts result.
 */
data class UserPostsData(
    val posts: List<Post>,
    val hasMore: Boolean,
    val nextCursor: String?
)

/**
 * Paginated follow list result (followers, following, collectors).
 */
data class FollowListData(
    val users: List<app.desperse.data.dto.response.FollowUser>,
    val hasMore: Boolean,
    val nextCursor: String?
)

/**
 * Paginated activity result.
 */
data class ActivityData(
    val activities: List<app.desperse.data.dto.response.ActivityItem>,
    val hasMore: Boolean,
    val nextCursor: String?
)
