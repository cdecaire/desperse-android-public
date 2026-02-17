package app.desperse.ui.screens.feed

import android.os.Trace
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.core.auth.PrivyAuthManager
import app.desperse.core.network.BlockhashExpiredException
import app.desperse.core.network.InsufficientFundsException
import app.desperse.core.network.SolanaRpcClient
import app.desperse.core.wallet.TransactionWalletManager
import app.desperse.data.NotificationCountManager
import app.desperse.data.PostUpdate
import app.desperse.data.PostUpdateManager
import app.desperse.data.model.CollectState
import app.desperse.data.model.Post
import app.desperse.data.model.PurchaseState
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
    val error: String? = null,
    val collectStates: Map<String, CollectState> = emptyMap(),
    val purchaseStates: Map<String, PurchaseState> = emptyMap(),
    /** Timestamp of last successful fetch per tab - used for stale time logic */
    val lastFetchTimeByTab: Map<String, Long> = emptyMap(),
    val currentUserId: String? = null
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val postUpdateManager: PostUpdateManager,
    private val privyAuthManager: PrivyAuthManager,
    private val solanaRpcClient: SolanaRpcClient,
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

    private val _selectedTab = MutableStateFlow("for-you")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    /** Notification counters for badges */
    val notificationCounters = notificationCountManager.counters

    private val pollingJobs = mutableMapOf<String, Job>()
    private val purchasePollingJobs = mutableMapOf<String, Job>()
    private var periodicRefreshJob: Job? = null

    init {
        loadFeed()
        observePostUpdates()
        observeCurrentUser()
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
        val lastFetch = _uiState.value.lastFetchTimeByTab[_selectedTab.value] ?: 0L
        val timeSinceLastFetch = System.currentTimeMillis() - lastFetch

        // Skip if we just fetched (prevents duplicate load from init + onScreenVisible race)
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
                .onSuccess { posts ->
                    _uiState.update { currentState ->
                        // Merge new data while preserving local state (collect states, etc.)
                        val collectedPostIds = posts.filter { p -> p.isCollected }.map { p -> p.id }.toSet()
                        val updatedCollectStates = currentState.collectStates.filterKeys { id ->
                            id !in collectedPostIds
                        }
                        val updatedPurchaseStates = currentState.purchaseStates.filterKeys { id ->
                            id !in collectedPostIds
                        }

                        currentState.copy(
                            posts = posts,
                            error = null,
                            collectStates = updatedCollectStates,
                            purchaseStates = updatedPurchaseStates,
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
                        applyCollectUpdate(update.postId, update.isCollected, update.collectCount)
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

    private fun applyCollectUpdate(postId: String, isCollected: Boolean, collectCount: Int) {
        _uiState.update { currentState ->
            val updatedPosts = currentState.posts.map { post ->
                if (post.id == postId) {
                    post.copy(isCollected = isCollected, collectCount = collectCount)
                } else {
                    post
                }
            }
            // Also update collect state to Success if collected
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

    fun switchTab(tab: String) {
        if (_selectedTab.value != tab) {
            _selectedTab.value = tab
            val lastFetch = _uiState.value.lastFetchTimeByTab[tab] ?: 0L
            val timeSinceLastFetch = System.currentTimeMillis() - lastFetch
            if (timeSinceLastFetch > STALE_TIME_MS || lastFetch == 0L) {
                loadFeed()
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadFeed(isRefresh = true)
    }

    private fun loadFeed(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isRefresh) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            Trace.beginSection("Feed.getFeed")
            postRepository.getFeed(_selectedTab.value)
                .also { Trace.endSection() } // Feed.getFeed
                .onSuccess { posts ->
                    _uiState.update {
                        // Clear collectStates/purchaseStates for posts that are now marked as collected
                        // This prevents stale "failed" states from persisting after refresh
                        val collectedPostIds = posts.filter { p -> p.isCollected }.map { p -> p.id }.toSet()
                        val updatedCollectStates = it.collectStates.filterKeys { id ->
                            id !in collectedPostIds
                        }
                        val updatedPurchaseStates = it.purchaseStates.filterKeys { id ->
                            id !in collectedPostIds
                        }

                        it.copy(
                            posts = posts,
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            collectStates = updatedCollectStates,
                            purchaseStates = updatedPurchaseStates,
                            lastFetchTimeByTab = it.lastFetchTimeByTab +
                                (_selectedTab.value to System.currentTimeMillis())
                        )
                    }

                    // Update lastSeen timestamp to clear new post badge
                    posts.firstOrNull()?.createdAt?.let { timestamp ->
                        notificationCountManager.updateLastSeen(_selectedTab.value, timestamp)
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
        // Cancel any existing polling job for this post
        pollingJobs[postId]?.cancel()

        pollingJobs[postId] = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val maxPollTime = 60_000L // 60 seconds
            val pollInterval = 5_000L // 5 seconds

            while (System.currentTimeMillis() - startTime < maxPollTime) {
                delay(pollInterval)

                // Check if we're still in confirming state
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
     * Flow: prepare → sign → broadcast → submit → poll for confirmation
     */
    fun purchasePost(postId: String) {
        val post = _uiState.value.posts.find { it.id == postId } ?: return
        if (post.type != "edition") return

        // Skip if already in progress or purchased
        val currentState = _uiState.value.purchaseStates[postId]
        if (currentState is PurchaseState.Preparing ||
            currentState is PurchaseState.Signing ||
            currentState is PurchaseState.Broadcasting ||
            currentState is PurchaseState.Submitting ||
            currentState is PurchaseState.Confirming ||
            currentState is PurchaseState.Success ||
            post.isCollected) {
            return
        }

        viewModelScope.launch {
            // Step 1: Get unsigned transaction from server
            updatePurchaseState(postId, PurchaseState.Preparing)
            Log.d(TAG, "Step 1: Requesting unsigned transaction for post $postId")

            postRepository.buyEdition(postId)
                .onSuccess { buyResult ->
                    Log.d(TAG, "Got unsigned tx, purchaseId=${buyResult.purchaseId}")

                    // Step 2: Sign the transaction with Privy
                    updatePurchaseState(postId, PurchaseState.Signing)
                    Log.d(TAG, "Step 2: Signing transaction with Privy wallet")

                    privyAuthManager.signTransaction(buyResult.unsignedTxBase64)
                        .onSuccess { signedTxBase64 ->
                            Log.d(TAG, "Transaction signed successfully")

                            // Step 3: Broadcast signed transaction to Solana
                            updatePurchaseState(postId, PurchaseState.Broadcasting)
                            Log.d(TAG, "Step 3: Broadcasting transaction to Solana")

                            solanaRpcClient.sendTransaction(signedTxBase64)
                                .onSuccess { txSignature ->
                                    Log.d(TAG, "Transaction broadcast, signature=$txSignature")

                                    // Step 4: Submit signature to server
                                    updatePurchaseState(postId, PurchaseState.Submitting)
                                    Log.d(TAG, "Step 4: Submitting signature to server")

                                    postRepository.submitPurchaseSignature(buyResult.purchaseId, txSignature)
                                        .onSuccess { submitResult ->
                                            Log.d(TAG, "Signature submitted, status=${submitResult.status}")

                                            // Step 5: Poll for confirmation
                                            updatePurchaseState(postId, PurchaseState.Confirming(buyResult.purchaseId))
                                            startPurchasePolling(postId, buyResult.purchaseId)
                                        }
                                        .onFailure { error ->
                                            Log.e(TAG, "Submit failed: ${error.message}")
                                            // Transaction was already broadcast, so we should still poll
                                            Log.w(TAG, "Starting polling anyway since tx was broadcast")
                                            updatePurchaseState(postId, PurchaseState.Confirming(buyResult.purchaseId))
                                            startPurchasePolling(postId, buyResult.purchaseId)
                                        }
                                }
                                .onFailure { error ->
                                    Log.e(TAG, "Broadcast failed: ${error.message}")
                                    val errorMessage = when (error) {
                                        is BlockhashExpiredException -> "Transaction expired. Please try again."
                                        is InsufficientFundsException -> "Insufficient funds for this purchase."
                                        else -> error.message ?: "Failed to broadcast transaction"
                                    }
                                    updatePurchaseState(postId, PurchaseState.Failed(errorMessage, canRetry = true))
                                }
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Signing failed: ${error.message}")
                            updatePurchaseState(postId, PurchaseState.Failed(
                                error.message ?: "Failed to sign transaction"
                            ))
                        }
                }
                .onFailure { error ->
                    Log.e(TAG, "Buy request failed: ${error.message}")
                    updatePurchaseState(postId, PurchaseState.Failed(
                        error.message ?: "Failed to initiate purchase"
                    ))
                }
        }
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
                val currentState = _uiState.value.purchaseStates[postId]
                if (currentState !is PurchaseState.Confirming) {
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
        _uiState.update { currentState ->
            currentState.copy(
                purchaseStates = currentState.purchaseStates + (postId to state)
            )
        }
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

        // Broadcast update to other screens
        viewModelScope.launch {
            val post = _uiState.value.posts.find { it.id == postId }
            if (post != null) {
                postUpdateManager.emitCollectUpdate(postId, post.isCollected, post.collectCount)
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
        stopPeriodicRefresh()
        pollingJobs.values.forEach { it.cancel() }
        pollingJobs.clear()
        purchasePollingJobs.values.forEach { it.cancel() }
        purchasePollingJobs.clear()
    }
}
