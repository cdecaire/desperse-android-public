package app.desperse.ui.screens.feed

import android.app.Activity
import android.os.Trace
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.core.network.BlockhashExpiredException
import app.desperse.core.network.InsufficientFundsException
import app.desperse.core.wallet.InstalledMwaWallet
import app.desperse.core.wallet.MwaError
import app.desperse.core.wallet.TransactionWalletManager
import app.desperse.data.NotificationCountManager
import app.desperse.data.PostUpdate
import app.desperse.data.PostUpdateManager
import app.desperse.data.model.CollectState
import app.desperse.data.model.Post
import app.desperse.data.model.PurchaseState
import app.desperse.data.repository.FeedPage
import app.desperse.data.repository.PostRepository
import app.desperse.data.repository.UserRepository
import app.desperse.ui.components.ToastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "FeedViewModel"

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val nextCursor: String? = null,
    val error: String? = null,
    /** Timestamp of last successful fetch per tab - used for stale time logic */
    val lastFetchTimeByTab: Map<String, Long> = emptyMap(),
    val currentUserId: String? = null,
    // Wallet picker for external wallet selection (edition purchases)
    val showWalletPicker: Boolean = false,
    val installedWallets: List<InstalledMwaWallet> = emptyList(),
    val pendingPurchasePostId: String? = null
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val postUpdateManager: PostUpdateManager,
    private val notificationCountManager: NotificationCountManager,
    private val toastManager: ToastManager,
    private val userRepository: UserRepository,
    private val transactionWalletManager: TransactionWalletManager
) : ViewModel() {

    companion object {
        /** How long before data is considered stale (30 seconds, matching web app) */
        private const val STALE_TIME_MS = 30_000L
        /** Interval for periodic silent refresh when screen is active (45 seconds) */
        private const val REFRESH_INTERVAL_MS = 45_000L
        /** Minimum time between fetches to prevent duplicate loads (e.g. init + onScreenVisible race) */
        private const val MIN_FETCH_INTERVAL_MS = 2_000L
    }

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    /** Separate flows for collect/purchase states — changes only recompose affected items */
    private val _collectStates = MutableStateFlow<Map<String, CollectState>>(emptyMap())
    val collectStates: StateFlow<Map<String, CollectState>> = _collectStates.asStateFlow()

    private val _purchaseStates = MutableStateFlow<Map<String, PurchaseState>>(emptyMap())
    val purchaseStates: StateFlow<Map<String, PurchaseState>> = _purchaseStates.asStateFlow()

    private val _selectedTab = MutableStateFlow("for-you")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    /** Notification counters for badges */
    val notificationCounters = notificationCountManager.counters

    private val pollingJobs = mutableMapOf<String, Job>()
    private val purchasePollingJobs = mutableMapOf<String, Job>()
    private var periodicRefreshJob: Job? = null

    /** Tracks whether a loadFeed() call is currently in flight to prevent duplicate fetches */
    private var isFetchInFlight = false

    /** Per-tab cache: posts, hasMore, nextCursor */
    private data class TabCache(
        val posts: List<Post> = emptyList(),
        val hasMore: Boolean = false,
        val nextCursor: String? = null
    )
    private val tabCaches = mutableMapOf<String, TabCache>()

    init {
        loadFeed()
        observePostUpdates()
        observeCurrentUser()
        observeBlockUpdates()
    }

    private fun observeBlockUpdates() {
        viewModelScope.launch {
            postUpdateManager.blockUpdates.collect { update ->
                if (update.isBlocked) {
                    // Drop posts authored by the now-blocked user from the active UI state
                    _uiState.update { state ->
                        state.copy(posts = state.posts.filter { it.user.id != update.userId })
                    }
                    // Also purge from all tab caches so switching tabs doesn't resurface them
                    tabCaches.entries.forEach { (key, cache) ->
                        val filtered = cache.posts.filter { it.user.id != update.userId }
                        if (filtered.size != cache.posts.size) {
                            tabCaches[key] = cache.copy(posts = filtered)
                        }
                    }
                } else {
                    // Unblock — refetch to surface their posts again
                    silentRefresh()
                }
            }
        }
    }

    fun blockUser(userId: String, displayName: String) {
        viewModelScope.launch {
            userRepository.blockUser(userId)
                .onSuccess {
                    postUpdateManager.emitBlockUpdate(userId, isBlocked = true)
                    toastManager.showSuccess("Blocked @$displayName")
                }
                .onFailure { error ->
                    toastManager.showError(error.message ?: "Failed to block user")
                }
        }
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            userRepository.currentUser.collect { user ->
                _uiState.update { it.copy(currentUserId = user?.id) }
            }
        }
    }

    /**
     * Called when the feed screen becomes visible.
     * Starts periodic refresh and does an immediate refresh if data is stale.
     */
    fun onScreenVisible() {
        // Skip if a fetch is already in flight (prevents duplicate load from init + onScreenVisible race)
        if (isFetchInFlight) {
            startPeriodicRefresh()
            return
        }

        val lastFetch = _uiState.value.lastFetchTimeByTab[_selectedTab.value] ?: 0L
        val timeSinceLastFetch = System.currentTimeMillis() - lastFetch

        // Skip if we just fetched
        if (timeSinceLastFetch < MIN_FETCH_INTERVAL_MS) {
            startPeriodicRefresh()
            return
        }

        // Check if data is stale and refresh if needed
        if (timeSinceLastFetch > STALE_TIME_MS && _uiState.value.posts.isNotEmpty()) {
            silentRefresh()
        }

        // Start periodic refresh
        startPeriodicRefresh()
    }

    /**
     * Called when the feed screen is no longer visible.
     * Stops periodic refresh to save resources.
     */
    fun onScreenHidden() {
        stopPeriodicRefresh()
    }

    private fun startPeriodicRefresh() {
        // Cancel any existing job
        periodicRefreshJob?.cancel()

        periodicRefreshJob = viewModelScope.launch {
            while (true) {
                delay(REFRESH_INTERVAL_MS)
                // Only refresh if we have posts and not already loading
                if (_uiState.value.posts.isNotEmpty() && !_uiState.value.isLoading && !_uiState.value.isRefreshing) {
                    silentRefresh()
                }
            }
        }
    }

    private fun stopPeriodicRefresh() {
        periodicRefreshJob?.cancel()
        periodicRefreshJob = null
    }

    /**
     * Silent refresh - updates data without showing loading indicators.
     * Used for periodic background updates to keep the feed feeling alive.
     */
    private fun silentRefresh() {
        viewModelScope.launch {
            postRepository.getFeed(_selectedTab.value)
                .onSuccess { feedPage ->
                    // Clear stale collect/purchase states for posts now marked collected
                    val collectedPostIds = feedPage.posts.filter { p -> p.isCollected }.map { p -> p.id }.toSet()
                    _collectStates.update { it.filterKeys { id -> id !in collectedPostIds } }
                    _purchaseStates.update { it.filterKeys { id -> id !in collectedPostIds } }

                    _uiState.update { currentState ->
                        // Merge fresh server data with local optimistic state.
                        // If the user liked/collected a post locally (optimistic update)
                        // but the server hasn't persisted it yet, preserve the local state.
                        val currentPostsById = currentState.posts.associateBy { it.id }
                        val mergedPosts = feedPage.posts.map { freshPost ->
                            val currentPost = currentPostsById[freshPost.id]
                            if (currentPost != null) {
                                freshPost.copy(
                                    isLiked = freshPost.isLiked || currentPost.isLiked,
                                    isCollected = freshPost.isCollected || currentPost.isCollected,
                                    likeCount = if (currentPost.isLiked && !freshPost.isLiked)
                                        currentPost.likeCount else freshPost.likeCount,
                                    collectCount = if (currentPost.isCollected && !freshPost.isCollected)
                                        currentPost.collectCount else freshPost.collectCount
                                )
                            } else {
                                freshPost
                            }
                        }

                        currentState.copy(
                            posts = mergedPosts,
                            hasMore = feedPage.hasMore,
                            nextCursor = feedPage.nextCursor,
                            error = null,
                            lastFetchTimeByTab = currentState.lastFetchTimeByTab +
                                (_selectedTab.value to System.currentTimeMillis())
                        )
                    }
                }
            // Silently ignore errors - don't disrupt the UI
        }
    }

    /**
     * Observe post updates from other screens (e.g., detail view)
     * and apply them to our local state
     */
    private fun observePostUpdates() {
        viewModelScope.launch {
            postUpdateManager.updates.collect { update ->
                when (update) {
                    is PostUpdate.LikeUpdate -> {
                        applyLikeUpdate(update.postId, update.isLiked, update.likeCount)
                    }
                    is PostUpdate.CollectUpdate -> {
                        applyCollectUpdate(update.postId, update.isCollected, update.collectCount, update.currentSupply)
                    }
                    is PostUpdate.CommentCountUpdate -> {
                        applyCommentCountUpdate(update.postId, update.commentCount)
                    }
                    is PostUpdate.PostEdited -> {
                        applyPostEdited(update.postId, update.post)
                    }
                    is PostUpdate.PostCreated, is PostUpdate.PostDeleted -> {
                        // Refresh feed when a post is created or deleted
                        silentRefresh()
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

    private fun applyCollectUpdate(postId: String, isCollected: Boolean, collectCount: Int, currentSupply: Int? = null) {
        _uiState.update { currentState ->
            val updatedPosts = currentState.posts.map { post ->
                if (post.id == postId) {
                    post.copy(
                        isCollected = isCollected,
                        collectCount = collectCount,
                        currentSupply = currentSupply ?: post.currentSupply
                    )
                } else {
                    post
                }
            }
            currentState.copy(posts = updatedPosts)
        }
        // Update collect state separately
        if (isCollected) {
            _collectStates.update { it + (postId to CollectState.Success) }
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

    fun switchTab(tab: String) {
        if (_selectedTab.value != tab) {
            // Save current tab's posts to cache
            val currentTab = _selectedTab.value
            val currentState = _uiState.value
            tabCaches[currentTab] = TabCache(
                posts = currentState.posts,
                hasMore = currentState.hasMore,
                nextCursor = currentState.nextCursor
            )

            _selectedTab.value = tab

            // Restore cached posts for the new tab (or clear if none)
            val cached = tabCaches[tab]
            if (cached != null) {
                _uiState.update {
                    it.copy(
                        posts = cached.posts,
                        hasMore = cached.hasMore,
                        nextCursor = cached.nextCursor,
                        isLoading = false
                    )
                }
            } else {
                // No cache — clear posts and show loading
                _uiState.update {
                    it.copy(posts = emptyList(), hasMore = false, nextCursor = null, isLoading = true)
                }
            }

            val lastFetch = _uiState.value.lastFetchTimeByTab[tab] ?: 0L
            val timeSinceLastFetch = System.currentTimeMillis() - lastFetch
            if (timeSinceLastFetch > STALE_TIME_MS || lastFetch == 0L) {
                loadFeed(isRefresh = cached != null)
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadFeed(isRefresh = true)
    }

    private fun loadFeed(isRefresh: Boolean = false) {
        isFetchInFlight = true
        viewModelScope.launch {
            if (!isRefresh) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            Trace.beginSection("Feed.getFeed")
            postRepository.getFeed(_selectedTab.value)
                .also { Trace.endSection() } // Feed.getFeed
                .onSuccess { feedPage ->
                    // Clear stale collect/purchase states for posts now marked collected
                    val collectedPostIds = feedPage.posts.filter { p -> p.isCollected }.map { p -> p.id }.toSet()
                    _collectStates.update { it.filterKeys { id -> id !in collectedPostIds } }
                    _purchaseStates.update { it.filterKeys { id -> id !in collectedPostIds } }

                    val tab = _selectedTab.value
                    _uiState.update {
                        it.copy(
                            posts = feedPage.posts,
                            isLoading = false,
                            isRefreshing = false,
                            hasMore = feedPage.hasMore,
                            nextCursor = feedPage.nextCursor,
                            error = null,
                            lastFetchTimeByTab = it.lastFetchTimeByTab +
                                (tab to System.currentTimeMillis())
                        )
                    }

                    // Update tab cache
                    tabCaches[tab] = TabCache(
                        posts = feedPage.posts,
                        hasMore = feedPage.hasMore,
                        nextCursor = feedPage.nextCursor
                    )

                    // Update lastSeen timestamp to clear new post badge
                    feedPage.posts.firstOrNull()?.createdAt?.let { timestamp ->
                        notificationCountManager.updateLastSeen(tab, timestamp)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error.message ?: "Failed to load feed"
                        )
                    }
                }
            isFetchInFlight = false
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.nextCursor == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val tab = _selectedTab.value
            postRepository.getFeed(tab, cursor = state.nextCursor)
                .onSuccess { feedPage ->
                    _uiState.update { current ->
                        // Deduplicate by post ID
                        val existingIds = current.posts.map { it.id }.toSet()
                        val newPosts = feedPage.posts.filter { it.id !in existingIds }

                        current.copy(
                            posts = current.posts + newPosts,
                            isLoadingMore = false,
                            hasMore = feedPage.hasMore,
                            nextCursor = feedPage.nextCursor
                        )
                    }
                    // Update tab cache
                    val updated = _uiState.value
                    tabCaches[tab] = TabCache(
                        posts = updated.posts,
                        hasMore = updated.hasMore,
                        nextCursor = updated.nextCursor
                    )
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch {
            // Find the post and toggle like state optimistically
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
                    // Broadcast revert to other screens
                    postUpdateManager.emitLikeUpdate(postId, post.isLiked, post.likeCount)
                }
            }
        }
    }

    fun collectPost(postId: String) {
        // Skip if already in progress or collected
        val currentState = _collectStates.value[postId]
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
                                val message = result.message ?: "Failed to collect"
                                updateCollectState(postId, CollectState.Failed(message))
                                toastManager.showError(message)
                            }
                        }
                        else -> {
                            val message = result.message ?: result.error ?: "Failed to collect"
                            updateCollectState(postId, CollectState.Failed(message))
                            toastManager.showError(message)
                        }
                    }
                }
                .onFailure { error ->
                    val message = error.message ?: "Failed to collect"
                    updateCollectState(postId, CollectState.Failed(message))
                    toastManager.showError(message)
                }
        }
    }

    private fun startCollectionPolling(postId: String, collectionId: String) {
        // Cancel any existing polling job for this post
        pollingJobs[postId]?.cancel()

        pollingJobs[postId] = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val maxPollTime = 60_000L // 60 seconds
            val pollInterval = 5_000L // 5 seconds

            while (System.currentTimeMillis() - startTime < maxPollTime) {
                delay(pollInterval)

                // Check if we're still in confirming state
                val currentCollectState = _collectStates.value[postId]
                if (currentCollectState !is CollectState.Confirming) {
                    return@launch
                }

                postRepository.checkCollectionStatus(collectionId)
                    .onSuccess { status ->
                        when (status.status) {
                            "confirmed" -> {
                                updateCollectState(postId, CollectState.Success)
                                updatePostCollected(postId, true)
                                toastManager.showSuccess("Successfully collected!")
                                pollingJobs.remove(postId)
                                return@launch
                            }
                            "failed" -> {
                                updateCollectState(postId, CollectState.Failed(
                                    status.error ?: "Collection failed"
                                ))
                                toastManager.showError(status.error ?: "Collection failed. You can try again.")
                                pollingJobs.remove(postId)
                                return@launch
                            }
                            // "pending" - continue polling
                        }
                    }
                    // On network error during poll, continue polling (don't fail)
            }

            // Timeout - still confirming
            updateCollectState(postId, CollectState.Failed(
                "Still confirming... Check back later.",
                canRetry = false
            ))
            toastManager.showWarning("Taking too long to confirm. Check back later.")
            pollingJobs.remove(postId)
        }
    }

    private fun updateCollectState(postId: String, state: CollectState) {
        _collectStates.update { it + (postId to state) }
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

        // Broadcast update to other screens
        viewModelScope.launch {
            val post = _uiState.value.posts.find { it.id == postId }
            if (post != null) {
                postUpdateManager.emitCollectUpdate(postId, post.isCollected, post.collectCount)
            }
        }
    }

    /**
     * Purchase an edition (paid NFT).
     * Uses TransactionWalletManager for wallet-aware signing (embedded + MWA).
     *
     * @param postId The post to purchase
     * @param activity Required for MWA wallets to launch the wallet app
     */
    private var pendingPurchaseActivity: Activity? = null

    fun purchasePost(postId: String, activity: Activity) {
        val post = _uiState.value.posts.find { it.id == postId } ?: return
        if (post.type != "edition") return

        // Skip if already in progress or purchased
        val currentState = _purchaseStates.value[postId]
        if (currentState is PurchaseState.Preparing ||
            currentState is PurchaseState.Signing ||
            currentState is PurchaseState.Broadcasting ||
            currentState is PurchaseState.Submitting ||
            currentState is PurchaseState.Confirming ||
            currentState is PurchaseState.Success ||
            post.isCollected) {
            return
        }

        // Check wallet availability before starting
        if (!transactionWalletManager.isActiveWalletAvailable()) {
            val message = "No compatible wallet app found. Please install a Solana wallet."
            updatePurchaseState(postId, PurchaseState.Failed(message, canRetry = false))
            toastManager.showError(message)
            return
        }

        // If external wallet package is unknown, show wallet picker
        if (transactionWalletManager.needsWalletSelection()) {
            pendingPurchaseActivity = activity
            val wallets = transactionWalletManager.getInstalledExternalWallets()
            _uiState.update { it.copy(
                showWalletPicker = true,
                installedWallets = wallets,
                pendingPurchasePostId = postId
            ) }
            return
        }

        val walletAddress = transactionWalletManager.getActiveWalletAddress()

        viewModelScope.launch {
            // Step 1: Get unsigned transaction from server
            updatePurchaseState(postId, PurchaseState.Preparing)
            Log.d(TAG, "Step 1: Requesting unsigned transaction for post $postId, wallet=${walletAddress?.take(8)}...")

            postRepository.buyEdition(postId, walletAddress)
                .onSuccess { buyResult ->
                    Log.d(TAG, "Got unsigned tx, purchaseId=${buyResult.purchaseId}")

                    // Step 2: Sign and broadcast via the active wallet
                    updatePurchaseState(postId, PurchaseState.Signing)
                    Log.d(TAG, "Step 2: Signing and broadcasting via active wallet")

                    transactionWalletManager.signAndSendTransaction(buyResult.unsignedTxBase64, activity)
                        .onSuccess { txSignature ->
                            Log.d(TAG, "Transaction signed and broadcast, signature=$txSignature")

                            // Step 3: Submit signature to server for tracking
                            updatePurchaseState(postId, PurchaseState.Submitting)
                            Log.d(TAG, "Step 3: Submitting signature to server")

                            postRepository.submitPurchaseSignature(buyResult.purchaseId, txSignature)
                                .onSuccess { submitResult ->
                                    Log.d(TAG, "Signature submitted, status=${submitResult.status}")
                                }
                                .onFailure { error ->
                                    // Transaction was already broadcast, so continue to polling
                                    Log.w(TAG, "Submit failed (${error.message}), polling anyway since tx was broadcast")
                                }

                            // Step 4: Poll for confirmation (regardless of submit result)
                            updatePurchaseState(postId, PurchaseState.Confirming(buyResult.purchaseId))
                            startPurchasePolling(postId, buyResult.purchaseId)
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Sign+broadcast failed: ${error.message}")
                            val errorMessage = when (error) {
                                is BlockhashExpiredException -> "Transaction expired. Please try again."
                                is InsufficientFundsException -> "Insufficient funds for this purchase."
                                is MwaError.UserCancelled -> "Transaction cancelled."
                                is MwaError.NoWalletInstalled -> "No compatible wallet app found."
                                is MwaError.Timeout -> "Wallet connection timed out. Please try again."
                                is MwaError.WalletRejected -> "Wallet rejected the transaction."
                                is MwaError.SessionTerminated -> "Wallet session ended. Please try again."
                                else -> error.message ?: "Failed to sign transaction"
                            }
                            updatePurchaseState(postId, PurchaseState.Failed(
                                errorMessage,
                                canRetry = error !is MwaError.NoWalletInstalled
                            ))
                            toastManager.showError(errorMessage)
                        }
                }
                .onFailure { error ->
                    Log.e(TAG, "Buy request failed: ${error.message}")
                    val message = error.message ?: "Failed to initiate purchase"
                    updatePurchaseState(postId, PurchaseState.Failed(message))
                    toastManager.showError(message)
                }
        }
    }

    fun onWalletSelectedForTransaction(packageName: String) {
        val activity = pendingPurchaseActivity
        val postId = _uiState.value.pendingPurchasePostId
        viewModelScope.launch {
            transactionWalletManager.setWalletPackage(packageName)
            _uiState.update { it.copy(showWalletPicker = false, pendingPurchasePostId = null) }
            if (activity != null && postId != null) {
                purchasePost(postId, activity)
            }
        }
    }

    fun dismissWalletPicker() {
        _uiState.update { it.copy(showWalletPicker = false, pendingPurchasePostId = null) }
        pendingPurchaseActivity = null
    }

    private fun startPurchasePolling(postId: String, purchaseId: String) {
        // Cancel any existing polling job for this post
        purchasePollingJobs[postId]?.cancel()

        purchasePollingJobs[postId] = viewModelScope.launch {
            val maxPollTime = 90_000L  // 90 seconds for blockchain confirmation
            val pollInterval = 5_000L
            val startTime = System.currentTimeMillis()

            Log.d(TAG, "Starting purchase polling for $purchaseId")

            while (System.currentTimeMillis() - startTime < maxPollTime) {
                delay(pollInterval)

                // Check if we're still in confirming state
                val currentPurchaseState = _purchaseStates.value[postId]
                if (currentPurchaseState !is PurchaseState.Confirming) {
                    return@launch
                }

                postRepository.checkPurchaseStatus(purchaseId)
                    .onSuccess { status ->
                        Log.d(TAG, "Poll status: ${status.status}")
                        when (status.status) {
                            "confirmed" -> {
                                Log.d(TAG, "Purchase confirmed! nftMint=${status.nftMint}")
                                updatePurchaseState(postId, PurchaseState.Success)
                                updatePostPurchased(postId)
                                toastManager.showSuccess("Purchase complete!")
                                purchasePollingJobs.remove(postId)
                                return@launch
                            }
                            "failed", "abandoned" -> {
                                Log.e(TAG, "Purchase failed: ${status.error}")
                                updatePurchaseState(postId, PurchaseState.Failed(
                                    status.error ?: "Purchase failed",
                                    canRetry = true
                                ))
                                toastManager.showError(status.error ?: "Purchase failed. You can try again.")
                                purchasePollingJobs.remove(postId)
                                return@launch
                            }
                            // "reserved", "submitted", "minting" - continue polling
                        }
                    }
                // On network error, continue polling
            }

            // Timeout
            Log.w(TAG, "Purchase polling timed out for $purchaseId")
            updatePurchaseState(postId, PurchaseState.Failed(
                "Confirmation is taking longer than expected. Your purchase may still be processing.",
                canRetry = true
            ))
            toastManager.showWarning("Taking too long to confirm. Your purchase may still be processing.")
            purchasePollingJobs.remove(postId)
        }
    }

    private fun updatePurchaseState(postId: String, state: PurchaseState) {
        _purchaseStates.update { it + (postId to state) }
    }

    private fun updatePostPurchased(postId: String) {
        _uiState.update { currentState ->
            val post = currentState.posts.find { it.id == postId }
            val newCurrentSupply = (post?.currentSupply ?: 0) + 1
            val updatedPosts = currentState.posts.map { p ->
                if (p.id == postId) {
                    p.copy(
                        isCollected = true,
                        currentSupply = newCurrentSupply
                    )
                } else {
                    p
                }
            }
            currentState.copy(posts = updatedPosts)
        }

        // Broadcast update to other screens (including currentSupply for editions)
        viewModelScope.launch {
            val post = _uiState.value.posts.find { it.id == postId }
            if (post != null) {
                postUpdateManager.emitCollectUpdate(postId, post.isCollected, post.collectCount, post.currentSupply)
            }
        }
    }

    /**
     * Report a post
     */
    suspend fun createReport(
        contentType: String,
        contentId: String,
        reasons: List<String>,
        details: String?
    ): Result<Unit> {
        return postRepository.createReport(contentType, contentId, reasons, details)
            .map { }  // Convert Result<String> to Result<Unit>
            .also { result ->
                result.onSuccess {
                    toastManager.showSuccess("Report submitted. Thanks for helping keep the community safe.")
                }
            }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            postRepository.deletePost(postId)
                .onSuccess {
                    postUpdateManager.emitPostDeleted(postId)
                }
                .onFailure { error ->
                    toastManager.showError(error.message ?: "Failed to delete post")
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pendingPurchaseActivity = null
        stopPeriodicRefresh()
        pollingJobs.values.forEach { it.cancel() }
        pollingJobs.clear()
        purchasePollingJobs.values.forEach { it.cancel() }
        purchasePollingJobs.clear()
    }
}
