package app.desperse.ui.screens.profile

import android.app.Activity
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.core.network.BlockhashExpiredException
import app.desperse.core.network.InsufficientFundsException
import app.desperse.core.wallet.InstalledMwaWallet
import app.desperse.core.wallet.MwaError
import app.desperse.core.wallet.TransactionWalletManager
import app.desperse.data.PostUpdate
import app.desperse.data.PostUpdateManager
import app.desperse.data.model.CollectState
import app.desperse.data.model.Post
import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.data.dto.response.ProfileStats
import app.desperse.data.dto.response.ProfileUser
import app.desperse.data.repository.MessageRepository
import app.desperse.data.repository.TipRepository
import app.desperse.data.repository.UserRepository
import app.desperse.data.repository.PostRepository
import app.desperse.ui.components.TipState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Profile tabs for displaying different content
 */
enum class ProfileTab {
    Posts,
    Collected,
    ForSale
}

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    // Profile data
    val user: ProfileUser? = null,
    val stats: ProfileStats? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val collectorsCount: Int = 0,
    val isFollowing: Boolean = false,
    val isOwnProfile: Boolean = false,
    // Posts tab
    val posts: List<Post> = emptyList(),
    val postsHasMore: Boolean = false,
    val postsNextCursor: String? = null,
    val isLoadingPosts: Boolean = false,
    // Collected tab
    val collected: List<Post> = emptyList(),
    val collectedHasMore: Boolean = false,
    val collectedNextCursor: String? = null,
    val isLoadingCollected: Boolean = false,
    // For Sale tab
    val forSale: List<Post> = emptyList(),
    val forSaleHasMore: Boolean = false,
    val forSaleNextCursor: String? = null,
    val isLoadingForSale: Boolean = false,
    // Tab state
    val selectedTab: ProfileTab = ProfileTab.Posts,
    // Collect states for posts
    val collectStates: Map<String, CollectState> = emptyMap(),
    // Follow action state
    val isFollowLoading: Boolean = false,
    // Tip state
    val tipState: TipState = TipState.Idle,
    val showTipSheet: Boolean = false,
    val skrBalance: Double? = null,
    // Wallet picker for external wallet selection (when package not known)
    val showWalletPicker: Boolean = false,
    val installedWallets: List<InstalledMwaWallet> = emptyList()
) {
    /** Get the current tab's items */
    val currentItems: List<Post>
        get() = when (selectedTab) {
            ProfileTab.Posts -> posts
            ProfileTab.Collected -> collected
            ProfileTab.ForSale -> forSale
        }

    /** Check if current tab has more items to load */
    val currentHasMore: Boolean
        get() = when (selectedTab) {
            ProfileTab.Posts -> postsHasMore
            ProfileTab.Collected -> collectedHasMore
            ProfileTab.ForSale -> forSaleHasMore
        }

    /** Check if current tab is loading more */
    val isLoadingMore: Boolean
        get() = when (selectedTab) {
            ProfileTab.Posts -> isLoadingPosts
            ProfileTab.Collected -> isLoadingCollected
            ProfileTab.ForSale -> isLoadingForSale
        }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val postUpdateManager: PostUpdateManager,
    private val messageRepository: MessageRepository,
    private val transactionWalletManager: TransactionWalletManager,
    private val tipRepository: TipRepository,
    private val api: DesperseApi
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    // Get slug from saved state (navigation argument)
    private val slug: String? = savedStateHandle.get<String>("slug")

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val pollingJobs = mutableMapOf<String, Job>()

    init {
        loadProfile()
        observePostUpdates()
        observeCurrentUserUpdates()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Determine if this is own profile or other user's profile
            val currentUser = userRepository.currentUser.value
            val profileSlug = slug ?: currentUser?.slug

            if (profileSlug == null) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "No profile to load"
                ) }
                return@launch
            }

            val isOwnProfile = currentUser?.slug == profileSlug

            userRepository.getUserProfile(profileSlug)
                .onSuccess { profileData ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        user = profileData.user,
                        stats = profileData.stats,
                        followersCount = profileData.followersCount,
                        followingCount = profileData.followingCount,
                        collectorsCount = profileData.collectorsCount,
                        isFollowing = profileData.isFollowing,
                        isOwnProfile = isOwnProfile,
                        error = null
                    ) }

                    // Load posts after profile is loaded
                    loadPosts(profileSlug)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load profile"
                    ) }
                }
        }
    }

    private fun loadPosts(profileSlug: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPosts = true) }
            android.util.Log.d(TAG, "Loading posts for profile: $profileSlug")
            userRepository.getUserPosts(profileSlug)
                .onSuccess { result ->
                    android.util.Log.d(TAG, "Loaded ${result.posts.size} posts, hasMore=${result.hasMore}")
                    _uiState.update { it.copy(
                        posts = result.posts,
                        postsHasMore = result.hasMore,
                        postsNextCursor = result.nextCursor,
                        isLoadingPosts = false
                    ) }
                }
                .onFailure { error ->
                    android.util.Log.e(TAG, "Failed to load posts: ${error.message}", error)
                    _uiState.update { it.copy(
                        posts = emptyList(),
                        postsHasMore = false,
                        isLoadingPosts = false
                    ) }
                }
        }
    }

    private fun loadCollected(profileSlug: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCollected = true) }
            android.util.Log.d(TAG, "Loading collected for profile: $profileSlug")
            userRepository.getUserCollected(profileSlug)
                .onSuccess { result ->
                    android.util.Log.d(TAG, "Loaded ${result.posts.size} collected, hasMore=${result.hasMore}")
                    _uiState.update { it.copy(
                        collected = result.posts,
                        collectedHasMore = result.hasMore,
                        collectedNextCursor = result.nextCursor,
                        isLoadingCollected = false
                    ) }
                }
                .onFailure { error ->
                    android.util.Log.e(TAG, "Failed to load collected: ${error.message}", error)
                    _uiState.update { it.copy(
                        collected = emptyList(),
                        collectedHasMore = false,
                        isLoadingCollected = false
                    ) }
                }
        }
    }

    private fun loadForSale(profileSlug: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingForSale = true) }
            android.util.Log.d(TAG, "Loading for-sale for profile: $profileSlug")
            userRepository.getUserForSale(profileSlug)
                .onSuccess { result ->
                    android.util.Log.d(TAG, "Loaded ${result.posts.size} for-sale, hasMore=${result.hasMore}")
                    _uiState.update { it.copy(
                        forSale = result.posts,
                        forSaleHasMore = result.hasMore,
                        forSaleNextCursor = result.nextCursor,
                        isLoadingForSale = false
                    ) }
                }
                .onFailure { error ->
                    android.util.Log.e(TAG, "Failed to load for-sale: ${error.message}", error)
                    _uiState.update { it.copy(
                        forSale = emptyList(),
                        forSaleHasMore = false,
                        isLoadingForSale = false
                    ) }
                }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        val profileSlug = slug ?: userRepository.currentUser.value?.slug ?: return

        if (currentState.isLoadingMore || !currentState.currentHasMore) return

        viewModelScope.launch {
            when (currentState.selectedTab) {
                ProfileTab.Posts -> {
                    _uiState.update { it.copy(isLoadingPosts = true) }
                    userRepository.getUserPosts(profileSlug, currentState.postsNextCursor)
                        .onSuccess { result ->
                            _uiState.update { it.copy(
                                posts = it.posts + result.posts,
                                postsHasMore = result.hasMore,
                                postsNextCursor = result.nextCursor,
                                isLoadingPosts = false
                            ) }
                        }
                        .onFailure { _uiState.update { it.copy(isLoadingPosts = false) } }
                }
                ProfileTab.Collected -> {
                    _uiState.update { it.copy(isLoadingCollected = true) }
                    userRepository.getUserCollected(profileSlug, currentState.collectedNextCursor)
                        .onSuccess { result ->
                            _uiState.update { it.copy(
                                collected = it.collected + result.posts,
                                collectedHasMore = result.hasMore,
                                collectedNextCursor = result.nextCursor,
                                isLoadingCollected = false
                            ) }
                        }
                        .onFailure { _uiState.update { it.copy(isLoadingCollected = false) } }
                }
                ProfileTab.ForSale -> {
                    _uiState.update { it.copy(isLoadingForSale = true) }
                    userRepository.getUserForSale(profileSlug, currentState.forSaleNextCursor)
                        .onSuccess { result ->
                            _uiState.update { it.copy(
                                forSale = it.forSale + result.posts,
                                forSaleHasMore = result.hasMore,
                                forSaleNextCursor = result.nextCursor,
                                isLoadingForSale = false
                            ) }
                        }
                        .onFailure { _uiState.update { it.copy(isLoadingForSale = false) } }
                }
            }
        }
    }

    fun refresh() {
        val profileSlug = slug ?: userRepository.currentUser.value?.slug ?: return

        // Avoid duplicate refresh calls
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            try {
                // Fetch profile data
                userRepository.getUserProfile(profileSlug)
                    .onSuccess { profileData ->
                        val currentUser = userRepository.currentUser.value
                        val isOwnProfile = currentUser?.slug == profileSlug

                        _uiState.update { it.copy(
                            user = profileData.user,
                            stats = profileData.stats,
                            followersCount = profileData.followersCount,
                            followingCount = profileData.followingCount,
                            collectorsCount = profileData.collectorsCount,
                            isFollowing = profileData.isFollowing,
                            isOwnProfile = isOwnProfile
                        ) }
                    }

                // Reload current tab's content
                when (_uiState.value.selectedTab) {
                    ProfileTab.Posts -> {
                        userRepository.getUserPosts(profileSlug)
                            .onSuccess { result ->
                                _uiState.update { it.copy(
                                    posts = result.posts,
                                    postsHasMore = result.hasMore,
                                    postsNextCursor = result.nextCursor
                                ) }
                            }
                    }
                    ProfileTab.Collected -> {
                        userRepository.getUserCollected(profileSlug)
                            .onSuccess { result ->
                                _uiState.update { it.copy(
                                    collected = result.posts,
                                    collectedHasMore = result.hasMore,
                                    collectedNextCursor = result.nextCursor
                                ) }
                            }
                    }
                    ProfileTab.ForSale -> {
                        userRepository.getUserForSale(profileSlug)
                            .onSuccess { result ->
                                _uiState.update { it.copy(
                                    forSale = result.posts,
                                    forSaleHasMore = result.hasMore,
                                    forSaleNextCursor = result.nextCursor
                                ) }
                            }
                    }
                }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun selectTab(tab: ProfileTab) {
        if (_uiState.value.selectedTab != tab) {
            _uiState.update { it.copy(selectedTab = tab) }
            val profileSlug = slug ?: userRepository.currentUser.value?.slug ?: return
            // Load content for the selected tab if not already loaded
            when (tab) {
                ProfileTab.Posts -> {
                    if (_uiState.value.posts.isEmpty() && !_uiState.value.isLoadingPosts) {
                        loadPosts(profileSlug)
                    }
                }
                ProfileTab.Collected -> {
                    if (_uiState.value.collected.isEmpty() && !_uiState.value.isLoadingCollected) {
                        loadCollected(profileSlug)
                    }
                }
                ProfileTab.ForSale -> {
                    if (_uiState.value.forSale.isEmpty() && !_uiState.value.isLoadingForSale) {
                        loadForSale(profileSlug)
                    }
                }
            }
        }
    }

    fun toggleFollow() {
        val currentState = _uiState.value
        if (currentState.isFollowLoading || currentState.isOwnProfile) return

        val userId = currentState.user?.id ?: return
        val isCurrentlyFollowing = currentState.isFollowing

        viewModelScope.launch {
            _uiState.update { it.copy(isFollowLoading = true) }

            // Optimistic update
            _uiState.update { it.copy(
                isFollowing = !isCurrentlyFollowing,
                followersCount = if (isCurrentlyFollowing) it.followersCount - 1 else it.followersCount + 1
            ) }

            val result = if (isCurrentlyFollowing) {
                userRepository.unfollowUser(userId)
            } else {
                userRepository.followUser(userId)
            }

            result
                .onSuccess { newIsFollowing ->
                    _uiState.update { it.copy(
                        isFollowing = newIsFollowing,
                        isFollowLoading = false
                    ) }
                }
                .onFailure {
                    // Revert optimistic update
                    _uiState.update { it.copy(
                        isFollowing = isCurrentlyFollowing,
                        followersCount = if (isCurrentlyFollowing) it.followersCount + 1 else it.followersCount - 1,
                        isFollowLoading = false
                    ) }
                }
        }
    }

    /**
     * Start a conversation with the profile user.
     * Checks DM eligibility first, then creates or retrieves the thread.
     */
    data class ConversationInfo(
        val threadId: String,
        val otherName: String?,
        val otherSlug: String?,
        val otherAvatar: String?
    )

    fun startConversation(
        onSuccess: (ConversationInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _uiState.value.user ?: run {
            onError("User not loaded")
            return
        }

        viewModelScope.launch {
            messageRepository.checkDmEligibility(user.id)
                .onSuccess { eligibility ->
                    if (eligibility.allowed) {
                        messageRepository.getOrCreateThread(user.id, user.id)
                            .onSuccess { response ->
                                onSuccess(
                                    ConversationInfo(
                                        threadId = response.thread.id,
                                        otherName = user.displayName,
                                        otherSlug = user.slug,
                                        otherAvatar = user.avatarUrl
                                    )
                                )
                            }
                            .onFailure { onError(it.message ?: "Failed to create conversation") }
                    } else {
                        val msg = if (eligibility.creatorDmsDisabled == true) {
                            "This user has disabled messages"
                        } else {
                            eligibility.unlockPaths.firstOrNull()?.message
                                ?: "You can't message this user yet"
                        }
                        onError(msg)
                    }
                }
                .onFailure { onError(it.message ?: "Failed to check eligibility") }
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch {
            val currentPosts = _uiState.value.posts
            val postIndex = currentPosts.indexOfFirst { it.id == postId }
            if (postIndex >= 0) {
                val post = currentPosts[postIndex]
                val newIsLiked = !post.isLiked
                val newLikeCount = if (post.isLiked) post.likeCount - 1 else post.likeCount + 1
                val updatedPost = post.copy(
                    isLiked = newIsLiked,
                    likeCount = newLikeCount
                )
                val updatedPosts = currentPosts.toMutableList().apply {
                    set(postIndex, updatedPost)
                }
                _uiState.update { it.copy(posts = updatedPosts) }

                // Broadcast update to other screens
                postUpdateManager.emitLikeUpdate(postId, newIsLiked, newLikeCount)

                // Make API call
                val result = if (updatedPost.isLiked) {
                    postRepository.likePost(postId)
                } else {
                    postRepository.unlikePost(postId)
                }

                // Revert on failure
                result.onFailure {
                    _uiState.update { it.copy(posts = currentPosts) }
                    postUpdateManager.emitLikeUpdate(postId, post.isLiked, post.likeCount)
                }
            }
        }
    }

    fun collectPost(postId: String) {
        val currentState = _uiState.value.collectStates[postId]
        if (currentState is CollectState.Preparing ||
            currentState is CollectState.Confirming ||
            currentState is CollectState.Success) return

        val walletAddress = transactionWalletManager.getActiveWalletAddress()
        Log.d(TAG, "Collecting post $postId with wallet ${walletAddress?.take(8)}...")

        viewModelScope.launch {
            updateCollectState(postId, CollectState.Preparing)

            postRepository.collectPost(postId, walletAddress)
                .onSuccess { result ->
                    when (result.status) {
                        "already_collected" -> {
                            updateCollectState(postId, CollectState.Success)
                            updatePostCollected(postId, true)
                        }
                        "pending" -> {
                            val collectionId = result.collectionId
                            if (collectionId != null) {
                                updateCollectState(postId, CollectState.Confirming(collectionId))
                                startCollectionPolling(postId, collectionId)
                            } else {
                                updateCollectState(postId, CollectState.Failed(
                                    result.message ?: "Failed to collect"
                                ))
                            }
                        }
                        else -> {
                            updateCollectState(postId, CollectState.Failed(
                                result.message ?: result.error ?: "Failed to collect"
                            ))
                        }
                    }
                }
                .onFailure { error ->
                    updateCollectState(postId, CollectState.Failed(
                        error.message ?: "Failed to collect"
                    ))
                }
        }
    }

    private fun startCollectionPolling(postId: String, collectionId: String) {
        pollingJobs[postId]?.cancel()

        pollingJobs[postId] = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val maxPollTime = 60_000L
            val pollInterval = 5_000L

            while (System.currentTimeMillis() - startTime < maxPollTime) {
                delay(pollInterval)

                val currentState = _uiState.value.collectStates[postId]
                if (currentState !is CollectState.Confirming) {
                    return@launch
                }

                postRepository.checkCollectionStatus(collectionId)
                    .onSuccess { status ->
                        when (status.status) {
                            "confirmed" -> {
                                updateCollectState(postId, CollectState.Success)
                                updatePostCollected(postId, true)
                                pollingJobs.remove(postId)
                                return@launch
                            }
                            "failed" -> {
                                updateCollectState(postId, CollectState.Failed(
                                    status.error ?: "Collection failed"
                                ))
                                pollingJobs.remove(postId)
                                return@launch
                            }
                        }
                    }
            }

            updateCollectState(postId, CollectState.Failed(
                "Still confirming... Check back later.",
                canRetry = false
            ))
            pollingJobs.remove(postId)
        }
    }

    private fun updateCollectState(postId: String, state: CollectState) {
        _uiState.update { currentState ->
            currentState.copy(
                collectStates = currentState.collectStates + (postId to state)
            )
        }
    }

    private fun updatePostCollected(postId: String, isCollected: Boolean) {
        _uiState.update { currentState ->
            val post = currentState.posts.find { it.id == postId }
            val newCollectCount = if (isCollected) (post?.collectCount ?: 0) + 1 else (post?.collectCount ?: 0)
            val updatedPosts = currentState.posts.map { p ->
                if (p.id == postId) {
                    p.copy(
                        isCollected = isCollected,
                        collectCount = newCollectCount
                    )
                } else {
                    p
                }
            }
            currentState.copy(posts = updatedPosts)
        }

        viewModelScope.launch {
            val post = _uiState.value.posts.find { it.id == postId }
            if (post != null) {
                postUpdateManager.emitCollectUpdate(postId, post.isCollected, post.collectCount)
            }
        }
    }

    /**
     * Observe currentUser changes to sync profile updates for own profile.
     * When the user edits their profile, UserRepository updates currentUser,
     * and we need to reflect those changes in the ProfileUiState.
     */
    private fun observeCurrentUserUpdates() {
        viewModelScope.launch {
            userRepository.currentUser.collect { user ->
                if (user != null && _uiState.value.isOwnProfile) {
                    // Sync currentUser changes to ProfileUser in UI state
                    _uiState.update { state ->
                        state.user?.let { profileUser ->
                            state.copy(
                                user = profileUser.copy(
                                    displayName = user.displayName,
                                    bio = user.bio,
                                    avatarUrl = user.avatarUrl,
                                    headerBgUrl = user.headerUrl,
                                    link = user.website,
                                    slug = user.slug ?: profileUser.slug
                                )
                            )
                        } ?: state
                    }
                }
            }
        }
    }

    private fun observePostUpdates() {
        viewModelScope.launch {
            postUpdateManager.updates.collect { update ->
                when (update) {
                    is PostUpdate.LikeUpdate -> {
                        applyLikeUpdate(update.postId, update.isLiked, update.likeCount)
                    }
                    is PostUpdate.CollectUpdate -> {
                        applyCollectUpdate(update.postId, update.isCollected, update.collectCount)
                    }
                    is PostUpdate.CommentCountUpdate -> {
                        applyCommentCountUpdate(update.postId, update.commentCount)
                    }
                    is PostUpdate.PostEdited -> {
                        applyPostEdited(update.postId, update.post)
                    }
                    is PostUpdate.PostCreated, is PostUpdate.PostDeleted -> {
                        // Refresh profile posts when a post is created or deleted
                        loadProfile()
                    }
                }
            }
        }
    }

    private fun applyLikeUpdate(postId: String, isLiked: Boolean, likeCount: Int) {
        _uiState.update { currentState ->
            val updatedPosts = currentState.posts.map { post ->
                if (post.id == postId) {
                    post.copy(isLiked = isLiked, likeCount = likeCount)
                } else {
                    post
                }
            }
            currentState.copy(posts = updatedPosts)
        }
    }

    private fun applyCollectUpdate(postId: String, isCollected: Boolean, collectCount: Int) {
        _uiState.update { currentState ->
            val updatedPosts = currentState.posts.map { post ->
                if (post.id == postId) {
                    post.copy(isCollected = isCollected, collectCount = collectCount)
                } else {
                    post
                }
            }
            val updatedCollectStates = if (isCollected) {
                currentState.collectStates + (postId to CollectState.Success)
            } else {
                currentState.collectStates
            }
            currentState.copy(posts = updatedPosts, collectStates = updatedCollectStates)
        }
    }

    private fun applyCommentCountUpdate(postId: String, commentCount: Int) {
        _uiState.update { currentState ->
            val updatedPosts = currentState.posts.map { post ->
                if (post.id == postId) {
                    post.copy(commentCount = commentCount)
                } else {
                    post
                }
            }
            currentState.copy(posts = updatedPosts)
        }
    }

    private fun applyPostEdited(postId: String, updatedPost: app.desperse.data.model.Post) {
        _uiState.update { currentState ->
            val updatedPosts = currentState.posts.map { post ->
                if (post.id == postId) {
                    post.copy(
                        caption = updatedPost.caption,
                        nftName = updatedPost.nftName,
                        price = updatedPost.price,
                        currency = updatedPost.currency,
                        maxSupply = updatedPost.maxSupply
                    )
                } else {
                    post
                }
            }
            currentState.copy(posts = updatedPosts)
        }
    }

    // === Tip functionality ===

    fun showTipSheet() {
        _uiState.update { it.copy(showTipSheet = true, tipState = TipState.Idle) }
        loadSkrBalance()
    }

    fun dismissTipSheet() {
        if (_uiState.value.tipState is TipState.Signing ||
            _uiState.value.tipState is TipState.Confirming) return
        _uiState.update { it.copy(showTipSheet = false, tipState = TipState.Idle) }
    }

    private fun loadSkrBalance() {
        viewModelScope.launch {
            when (val result = safeApiCall { api.getWalletOverview() }) {
                is ApiResult.Success -> {
                    val skrToken = result.data.tokens.find {
                        it.symbol.equals("SKR", ignoreCase = true)
                    }
                    _uiState.update { it.copy(skrBalance = skrToken?.balance ?: 0.0) }
                }
                is ApiResult.Error -> {
                    Log.w(TAG, "Failed to load SKR balance: ${result.message}")
                }
            }
        }
    }

    // Pending tip info for wallet picker flow
    private var pendingTipAmount: Double = 0.0
    private var pendingTipContext: String = "profile"
    private var pendingTipActivity: Activity? = null

    fun sendTip(amount: Double, context: String, activity: Activity) {
        val userId = _uiState.value.user?.id ?: return

        // Skip if already in progress
        val currentState = _uiState.value.tipState
        if (currentState is TipState.Preparing ||
            currentState is TipState.Signing ||
            currentState is TipState.Confirming ||
            currentState is TipState.Success) return

        // Check wallet availability
        if (!transactionWalletManager.isActiveWalletAvailable()) {
            _uiState.update {
                it.copy(tipState = TipState.Failed(
                    "No compatible wallet app found. Please install a Solana wallet.",
                    canRetry = false
                ))
            }
            return
        }

        // If external wallet package is unknown, show our custom wallet picker
        if (transactionWalletManager.needsWalletSelection()) {
            pendingTipAmount = amount
            pendingTipContext = context
            pendingTipActivity = activity
            val wallets = transactionWalletManager.getInstalledExternalWallets()
            _uiState.update { it.copy(showWalletPicker = true, installedWallets = wallets) }
            return
        }

        val walletAddress = transactionWalletManager.getActiveWalletAddress()

        viewModelScope.launch {
            // Step 1: Prepare tip (get unsigned transaction)
            _uiState.update { it.copy(tipState = TipState.Preparing) }
            Log.d(TAG, "Tip Step 1: Preparing tip for user $userId, amount=$amount, wallet=${walletAddress?.take(8)}...")

            tipRepository.prepareTip(userId, amount, context, walletAddress)
                .onSuccess { prepareResult ->
                    Log.d(TAG, "Got unsigned tip tx, tipId=${prepareResult.tipId}")

                    // Step 2: Sign and broadcast via active wallet
                    _uiState.update { it.copy(tipState = TipState.Signing) }
                    Log.d(TAG, "Tip Step 2: Signing via active wallet")

                    transactionWalletManager.signAndSendTransaction(prepareResult.transaction, activity)
                        .onSuccess { txSignature ->
                            Log.d(TAG, "Tip tx signed and broadcast, signature=$txSignature")

                            // Step 3: Confirm tip on server
                            _uiState.update { it.copy(tipState = TipState.Confirming(prepareResult.tipId)) }
                            Log.d(TAG, "Tip Step 3: Confirming on server")

                            tipRepository.confirmTip(prepareResult.tipId, txSignature)
                                .onSuccess {
                                    Log.d(TAG, "Tip confirmed successfully!")
                                    _uiState.update { it.copy(tipState = TipState.Success) }
                                }
                                .onFailure { error ->
                                    // Transaction was broadcast, so the tip likely went through
                                    Log.w(TAG, "Tip confirm failed (${error.message}), but tx was broadcast")
                                    _uiState.update { it.copy(tipState = TipState.Success) }
                                }
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Tip sign+broadcast failed: ${error.message}")
                            val errorMessage = when (error) {
                                is BlockhashExpiredException -> "Transaction expired. Please try again."
                                is InsufficientFundsException -> "Insufficient funds for this tip."
                                is MwaError.UserCancelled -> "Transaction cancelled."
                                is MwaError.NoWalletInstalled -> "No compatible wallet app found."
                                is MwaError.Timeout -> "Wallet connection timed out. Please try again."
                                is MwaError.WalletRejected -> "Wallet rejected the transaction."
                                is MwaError.SessionTerminated -> "Wallet session ended. Please try again."
                                else -> error.message ?: "Failed to sign transaction"
                            }
                            _uiState.update {
                                it.copy(tipState = TipState.Failed(
                                    errorMessage,
                                    canRetry = error !is MwaError.NoWalletInstalled
                                ))
                            }
                        }
                }
                .onFailure { error ->
                    Log.e(TAG, "Tip prepare failed: ${error.message}")
                    _uiState.update {
                        it.copy(tipState = TipState.Failed(
                            error.message ?: "Failed to prepare tip"
                        ))
                    }
                }
        }
    }

    fun onWalletSelectedForTransaction(packageName: String) {
        // Capture pending values before the coroutine launches — dismissWalletPicker()
        // is called synchronously after this (by WalletPickerSheet's onClick) and clears them
        val tipAmount = pendingTipAmount
        val tipContext = pendingTipContext
        val activity = pendingTipActivity

        viewModelScope.launch {
            transactionWalletManager.setWalletPackage(packageName)
            _uiState.update { it.copy(showWalletPicker = false) }
            // Resume pending tip
            if (activity != null && tipAmount > 0) {
                sendTip(tipAmount, tipContext, activity)
            }
        }
    }

    fun dismissWalletPicker() {
        _uiState.update { it.copy(showWalletPicker = false) }
        pendingTipActivity = null
        pendingTipAmount = 0.0
    }

    override fun onCleared() {
        super.onCleared()
        pendingTipActivity = null
        pollingJobs.values.forEach { it.cancel() }
        pollingJobs.clear()
    }
}
