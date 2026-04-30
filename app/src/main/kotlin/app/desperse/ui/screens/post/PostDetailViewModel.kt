package app.desperse.ui.screens.post

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
import app.desperse.data.PostUpdateManager
import app.desperse.data.model.CollectState
import app.desperse.data.dto.response.FollowUser
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
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PostDetailViewModel"

/** How long before data is considered stale (30 seconds) */
private const val STALE_TIME_MS = 30_000L
/** Interval for periodic silent refresh when screen is active (45 seconds) */
private const val REFRESH_INTERVAL_MS = 45_000L

/**
 * UI state for Post Detail screen
 */
data class PostDetailUiState(
    val post: Post? = null,
    val isLoadingPost: Boolean = true,
    val error: String? = null,
    val collectState: CollectState = CollectState.Idle,
    val purchaseState: PurchaseState = PurchaseState.Idle,
    val currentUserAvatarUrl: String? = null,
    val currentUserId: String? = null,
    val lastFetchTime: Long = 0L,
    // Collectors tab
    val collectors: List<FollowUser> = emptyList(),
    val isLoadingCollectors: Boolean = false,
    val isLoadingMoreCollectors: Boolean = false,
    val hasMoreCollectors: Boolean = false,
    val collectorsNextCursor: String? = null,
    val collectorsError: String? = null,
    val collectorsFollowLoadingIds: Set<String> = emptySet(),
    // Wallet picker for external wallet selection
    val showWalletPicker: Boolean = false,
    val installedWallets: List<InstalledMwaWallet> = emptyList()
)

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val postRepository: PostRepository,
    private val postUpdateManager: PostUpdateManager,
    private val transactionWalletManager: TransactionWalletManager,
    private val userRepository: UserRepository,
    private val toastManager: ToastManager
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle["postId"])

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var purchasePollingJob: Job? = null
    private var periodicRefreshJob: Job? = null

    init {
        // Show cached post instantly (from feed/profile) while fetching fresh data
        val cached = postRepository.getCachedPost(postId)
        if (cached != null) {
            _uiState.value = PostDetailUiState(
                post = cached,
                isLoadingPost = false,
                collectState = if (cached.isCollected) CollectState.Success else CollectState.Idle
            )
        }
        loadPost()
        observePostUpdates()
        observeFollowUpdates()
        observeCurrentUser()
    }

    fun loadCollectors() {
        if (_uiState.value.isLoadingCollectors || _uiState.value.collectors.isNotEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCollectors = true, collectorsError = null)
            postRepository.getPostCollectors(postId)
                .onSuccess { page ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingCollectors = false,
                        collectors = page.users,
                        hasMoreCollectors = page.hasMore,
                        collectorsNextCursor = page.nextCursor
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingCollectors = false,
                        collectorsError = error.message ?: "Failed to load collectors"
                    )
                }
        }
    }

    fun loadMoreCollectors() {
        val cursor = _uiState.value.collectorsNextCursor ?: return
        if (_uiState.value.isLoadingMoreCollectors || !_uiState.value.hasMoreCollectors) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMoreCollectors = true)
            postRepository.getPostCollectors(postId, cursor)
                .onSuccess { page ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMoreCollectors = false,
                        collectors = _uiState.value.collectors + page.users,
                        hasMoreCollectors = page.hasMore,
                        collectorsNextCursor = page.nextCursor
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingMoreCollectors = false)
                }
        }
    }

    fun toggleFollowCollector(userId: String) {
        val currentState = _uiState.value
        if (currentState.collectorsFollowLoadingIds.contains(userId)) return

        val user = currentState.collectors.find { it.id == userId } ?: return
        val isCurrentlyFollowing = user.isFollowing

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                collectorsFollowLoadingIds = currentState.collectorsFollowLoadingIds + userId,
                collectors = currentState.collectors.map { u ->
                    if (u.id == userId) u.copy(isFollowing = !isCurrentlyFollowing) else u
                }
            )

            val result = if (isCurrentlyFollowing) {
                userRepository.unfollowUser(userId)
            } else {
                userRepository.followUser(userId)
            }

            result
                .onSuccess { newIsFollowing ->
                    _uiState.value = _uiState.value.copy(
                        collectorsFollowLoadingIds = _uiState.value.collectorsFollowLoadingIds - userId,
                        collectors = _uiState.value.collectors.map { u ->
                            if (u.id == userId) u.copy(isFollowing = newIsFollowing) else u
                        }
                    )
                    // Broadcast to other screens
                    postUpdateManager.emitFollowUpdate(userId, newIsFollowing)
                    // Show confirmation
                    val displayName = user.displayName ?: user.slug
                    if (newIsFollowing) {
                        toastManager.showSuccess("Following $displayName")
                    } else {
                        toastManager.showInfo("Unfollowed $displayName")
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        collectorsFollowLoadingIds = _uiState.value.collectorsFollowLoadingIds - userId,
                        collectors = _uiState.value.collectors.map { u ->
                            if (u.id == userId) u.copy(isFollowing = isCurrentlyFollowing) else u
                        }
                    )
                    toastManager.showError("Failed to update follow")
                }
        }
    }

    /**
     * Observe current user for avatar display in comment input and ownership checks
     */
    private fun observeCurrentUser() {
        viewModelScope.launch {
            userRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(
                    currentUserAvatarUrl = user?.avatarUrl,
                    currentUserId = user?.id
                )
            }
        }
    }

    private fun observeFollowUpdates() {
        viewModelScope.launch {
            postUpdateManager.followUpdates.collect { update ->
                _uiState.value = _uiState.value.copy(
                    collectors = _uiState.value.collectors.map { u ->
                        if (u.id == update.userId) u.copy(isFollowing = update.isFollowing) else u
                    }
                )
            }
        }
    }

    /**
     * Observe post updates from other screens (e.g., feed)
     * and apply them to our local state
     */
    private fun observePostUpdates() {
        viewModelScope.launch {
            postUpdateManager.updates.collect { update ->
                if (update.postId != postId) return@collect

                val currentPost = _uiState.value.post ?: return@collect

                when (update) {
                    is app.desperse.data.PostUpdate.LikeUpdate -> {
                        _uiState.value = _uiState.value.copy(
                            post = currentPost.copy(
                                isLiked = update.isLiked,
                                likeCount = update.likeCount
                            )
                        )
                    }
                    is app.desperse.data.PostUpdate.CollectUpdate -> {
                        _uiState.value = _uiState.value.copy(
                            post = currentPost.copy(
                                isCollected = update.isCollected,
                                collectCount = update.collectCount,
                                currentSupply = update.currentSupply ?: currentPost.currentSupply
                            ),
                            collectState = if (update.isCollected) CollectState.Success else _uiState.value.collectState
                        )
                    }
                    is app.desperse.data.PostUpdate.CommentCountUpdate -> {
                        _uiState.value = _uiState.value.copy(
                            post = currentPost.copy(commentCount = update.commentCount)
                        )
                    }
                    is app.desperse.data.PostUpdate.PostEdited -> {
                        _uiState.value = _uiState.value.copy(
                            post = currentPost.copy(
                                caption = update.post.caption,
                                nftName = update.post.nftName,
                                price = update.post.price,
                                currency = update.post.currency,
                                maxSupply = update.post.maxSupply
                            )
                        )
                    }
                    is app.desperse.data.PostUpdate.PostCreated,
                    is app.desperse.data.PostUpdate.PostDeleted -> { /* Not relevant for detail */ }
                }
            }
        }
    }

    fun loadPost() {
        viewModelScope.launch {
            val hasCachedPost = _uiState.value.post != null
            // Only show loading spinner if we have no cached data to display
            if (!hasCachedPost) {
                _uiState.value = _uiState.value.copy(isLoadingPost = true, error = null)
            }
            postRepository.getPost(postId)
                .onSuccess { post ->
                    _uiState.value = _uiState.value.copy(
                        post = post,
                        isLoadingPost = false,
                        lastFetchTime = System.currentTimeMillis(),
                        collectState = if (post.isCollected) CollectState.Success else _uiState.value.collectState
                    )
                }
                .onFailure { error ->
                    // Only show error if we have no cached data to fall back on
                    if (!hasCachedPost) {
                        _uiState.value = _uiState.value.copy(
                            isLoadingPost = false,
                            error = error.message ?: "Failed to load post"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoadingPost = false)
                    }
                }
        }
    }

    /**
     * Called when the post detail screen becomes visible.
     * Starts periodic refresh and does an immediate refresh if data is stale.
     */
    fun onScreenVisible() {
        val timeSinceLastFetch = System.currentTimeMillis() - _uiState.value.lastFetchTime
        if (timeSinceLastFetch > STALE_TIME_MS && _uiState.value.post != null) {
            silentRefresh()
        }
        startPeriodicRefresh()
    }

    /**
     * Called when the post detail screen is no longer visible.
     * Stops periodic refresh to save resources.
     */
    fun onScreenHidden() {
        stopPeriodicRefresh()
    }

    private fun startPeriodicRefresh() {
        stopPeriodicRefresh()
        periodicRefreshJob = viewModelScope.launch {
            while (true) {
                delay(REFRESH_INTERVAL_MS)
                if (_uiState.value.post != null && !_uiState.value.isLoadingPost) {
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
     * Silent refresh - fetches updated post data without showing loading state.
     * Used for periodic background updates to keep counts fresh.
     */
    private fun silentRefresh() {
        viewModelScope.launch {
            postRepository.getPost(postId)
                .onSuccess { freshPost ->
                    val currentState = _uiState.value
                    val currentPost = currentState.post ?: return@onSuccess

                    // Update counts from server while preserving local state
                    _uiState.value = currentState.copy(
                        post = currentPost.copy(
                            likeCount = freshPost.likeCount,
                            commentCount = freshPost.commentCount,
                            collectCount = freshPost.collectCount,
                            // Only update isLiked/isCollected if server says true (don't overwrite optimistic updates)
                            isLiked = freshPost.isLiked || currentPost.isLiked,
                            isCollected = freshPost.isCollected || currentPost.isCollected
                        ),
                        lastFetchTime = System.currentTimeMillis()
                    )
                }
            // Silently ignore errors for background refresh
        }
    }

    fun toggleLike() {
        val post = _uiState.value.post ?: return

        // Optimistic update
        val newIsLiked = !post.isLiked
        val newLikeCount = if (newIsLiked) post.likeCount + 1 else post.likeCount - 1
        _uiState.value = _uiState.value.copy(
            post = post.copy(isLiked = newIsLiked, likeCount = newLikeCount)
        )

        viewModelScope.launch {
            // Broadcast update to other screens
            postUpdateManager.emitLikeUpdate(postId, newIsLiked, newLikeCount)

            val result = if (newIsLiked) {
                postRepository.likePost(postId)
            } else {
                postRepository.unlikePost(postId)
            }

            result.onFailure {
                // Revert on failure
                _uiState.value = _uiState.value.copy(
                    post = post.copy(isLiked = post.isLiked, likeCount = post.likeCount)
                )
                // Broadcast revert to other screens
                postUpdateManager.emitLikeUpdate(postId, post.isLiked, post.likeCount)
            }
        }
    }

    fun collect() {
        val post = _uiState.value.post ?: return
        if (post.type != "collectible") return

        // Skip if already in progress or collected
        val currentState = _uiState.value.collectState
        if (currentState is CollectState.Preparing ||
            currentState is CollectState.Confirming ||
            currentState is CollectState.Success ||
            post.isCollected) {
            return
        }

        _uiState.value = _uiState.value.copy(collectState = CollectState.Preparing)

        // Send active wallet address so the cNFT is minted to the selected wallet
        val walletAddress = transactionWalletManager.getActiveWalletAddress()

        viewModelScope.launch {
            postRepository.collectPost(postId, walletAddress)
                .onSuccess { result ->
                    when {
                        result.status == "already_collected" || result.assetId != null -> {
                            // Already collected or immediately confirmed
                            val newCollectCount = (_uiState.value.post?.collectCount ?: 0) + 1
                            _uiState.value = _uiState.value.copy(
                                collectState = CollectState.Success,
                                post = _uiState.value.post?.copy(
                                    isCollected = true,
                                    collectCount = newCollectCount
                                )
                            )
                            // Broadcast update to other screens
                            postUpdateManager.emitCollectUpdate(postId, true, newCollectCount)
                        }
                        result.collectionId != null -> {
                            // Need to poll for confirmation
                            _uiState.value = _uiState.value.copy(
                                collectState = CollectState.Confirming(result.collectionId)
                            )
                            startPolling(result.collectionId)
                        }
                        else -> {
                            val message = result.error ?: result.message ?: "Collection failed"
                            _uiState.value = _uiState.value.copy(
                                collectState = CollectState.Failed(message)
                            )
                            toastManager.showError(message)
                        }
                    }
                }
                .onFailure { error ->
                    val message = error.message ?: "Collection failed"
                    _uiState.value = _uiState.value.copy(
                        collectState = CollectState.Failed(message)
                    )
                    toastManager.showError(message)
                }
        }
    }

    private fun startPolling(collectionId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            val maxPollTime = 60_000L
            val pollInterval = 5_000L
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < maxPollTime) {
                delay(pollInterval)

                postRepository.checkCollectionStatus(collectionId)
                    .onSuccess { status ->
                        when (status.status) {
                            "confirmed" -> {
                                val newCollectCount = (_uiState.value.post?.collectCount ?: 0) + 1
                                _uiState.value = _uiState.value.copy(
                                    collectState = CollectState.Success,
                                    post = _uiState.value.post?.copy(
                                        isCollected = true,
                                        collectCount = newCollectCount
                                    )
                                )
                                // Broadcast update to other screens
                                postUpdateManager.emitCollectUpdate(postId, true, newCollectCount)
                                toastManager.showSuccess("Successfully collected!")
                                return@launch
                            }
                            "failed" -> {
                                _uiState.value = _uiState.value.copy(
                                    collectState = CollectState.Failed(
                                        status.error ?: "Minting failed",
                                        canRetry = true
                                    )
                                )
                                toastManager.showError(status.error ?: "Collection failed. You can try again.")
                                return@launch
                            }
                            // "pending" - continue polling
                        }
                    }
                // On network error, continue polling
            }

            // Timeout
            _uiState.value = _uiState.value.copy(
                collectState = CollectState.Failed(
                    "Confirmation is taking longer than expected. Your collectible may still be processing.",
                    canRetry = true
                )
            )
            toastManager.showWarning("Taking too long to confirm. Check back later.")
        }
    }

    /**
     * Purchase an edition (paid NFT).
     * Flow: prepare → sign+broadcast (via active wallet) → submit → poll for confirmation.
     *
     * Uses TransactionWalletManager which routes to the correct wallet:
     * - Embedded (Privy): sign locally → broadcast via RPC → return signature
     * - External (MWA): wallet app signs AND broadcasts → return signature
     *
     * @param activity Required for MWA wallets to launch the wallet app
     */
    // Pending purchase info for wallet picker flow
    private var pendingPurchaseActivity: Activity? = null

    fun purchase(activity: Activity) {
        val post = _uiState.value.post ?: return
        if (post.type != "edition") return

        // Skip if already in progress or purchased
        val currentState = _uiState.value.purchaseState
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
            _uiState.value = _uiState.value.copy(
                purchaseState = PurchaseState.Failed(message, canRetry = false)
            )
            toastManager.showError(message)
            return
        }

        // If external wallet package is unknown, show our custom wallet picker
        if (transactionWalletManager.needsWalletSelection()) {
            pendingPurchaseActivity = activity
            val wallets = transactionWalletManager.getInstalledExternalWallets()
            _uiState.value = _uiState.value.copy(showWalletPicker = true, installedWallets = wallets)
            return
        }

        val walletAddress = transactionWalletManager.getActiveWalletAddress()

        viewModelScope.launch {
            // Step 1: Get unsigned transaction from server (built for the active wallet)
            _uiState.value = _uiState.value.copy(purchaseState = PurchaseState.Preparing)
            Log.d(TAG, "Step 1: Requesting unsigned transaction for post $postId, wallet=${walletAddress?.take(8)}...")

            postRepository.buyEdition(postId, walletAddress)
                .onSuccess { buyResult ->
                    Log.d(TAG, "Got unsigned tx, purchaseId=${buyResult.purchaseId}")

                    // Step 2: Sign and broadcast via the active wallet
                    _uiState.value = _uiState.value.copy(purchaseState = PurchaseState.Signing)
                    Log.d(TAG, "Step 2: Signing and broadcasting via active wallet")

                    transactionWalletManager.signAndSendTransaction(buyResult.unsignedTxBase64, activity)
                        .onSuccess { txSignature ->
                            Log.d(TAG, "Transaction signed and broadcast, signature=$txSignature")

                            // Step 3: Submit signature to server for tracking
                            _uiState.value = _uiState.value.copy(purchaseState = PurchaseState.Submitting)
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
                            _uiState.value = _uiState.value.copy(
                                purchaseState = PurchaseState.Confirming(buyResult.purchaseId)
                            )
                            startPurchasePolling(buyResult.purchaseId)
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
                            _uiState.value = _uiState.value.copy(
                                purchaseState = PurchaseState.Failed(
                                    errorMessage,
                                    canRetry = error !is MwaError.NoWalletInstalled
                                )
                            )
                            toastManager.showError(errorMessage)
                        }
                }
                .onFailure { error ->
                    Log.e(TAG, "Buy request failed: ${error.message}")
                    val message = error.message ?: "Failed to initiate purchase"
                    _uiState.value = _uiState.value.copy(
                        purchaseState = PurchaseState.Failed(message)
                    )
                    toastManager.showError(message)
                }
        }
    }

    private fun startPurchasePolling(purchaseId: String) {
        purchasePollingJob?.cancel()
        purchasePollingJob = viewModelScope.launch {
            val maxPollTime = 90_000L  // 90 seconds for blockchain confirmation
            val pollInterval = 5_000L
            val startTime = System.currentTimeMillis()

            Log.d(TAG, "Starting purchase polling for $purchaseId")

            while (System.currentTimeMillis() - startTime < maxPollTime) {
                delay(pollInterval)

                postRepository.checkPurchaseStatus(purchaseId)
                    .onSuccess { status ->
                        Log.d(TAG, "Poll status: ${status.status}")
                        when (status.status) {
                            "confirmed" -> {
                                Log.d(TAG, "Purchase confirmed! nftMint=${status.nftMint}")
                                val newCollectCount = (_uiState.value.post?.collectCount ?: 0) + 1
                                val newCurrentSupply = (_uiState.value.post?.currentSupply ?: 0) + 1
                                _uiState.value = _uiState.value.copy(
                                    purchaseState = PurchaseState.Success,
                                    post = _uiState.value.post?.copy(
                                        isCollected = true,
                                        collectCount = newCollectCount,
                                        currentSupply = newCurrentSupply
                                    )
                                )
                                // Broadcast update to other screens (include currentSupply for editions)
                                postUpdateManager.emitCollectUpdate(postId, true, newCollectCount, newCurrentSupply)
                                toastManager.showSuccess("Purchase complete!")
                                return@launch
                            }
                            "failed", "abandoned" -> {
                                Log.e(TAG, "Purchase failed: ${status.error}")
                                _uiState.value = _uiState.value.copy(
                                    purchaseState = PurchaseState.Failed(
                                        status.error ?: "Purchase failed",
                                        canRetry = true
                                    )
                                )
                                toastManager.showError(status.error ?: "Purchase failed. You can try again.")
                                return@launch
                            }
                            // "reserved", "submitted", "minting" - continue polling
                        }
                    }
                // On network error, continue polling
            }

            // Timeout
            Log.w(TAG, "Purchase polling timed out for $purchaseId")
            _uiState.value = _uiState.value.copy(
                purchaseState = PurchaseState.Failed(
                    "Confirmation is taking longer than expected. Your purchase may still be processing.",
                    canRetry = true
                )
            )
            toastManager.showWarning("Taking too long to confirm. Your purchase may still be processing.")
        }
    }

    fun resetPurchaseState() {
        _uiState.value = _uiState.value.copy(purchaseState = PurchaseState.Idle)
    }

    /**
     * Report a post or comment
     * @param contentType "post" or "comment"
     * @param contentId The ID of the content being reported
     * @param reasons List of reason strings
     * @param details Optional additional details
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

    fun blockUser(userId: String, displayName: String, onBlocked: () -> Unit = {}) {
        viewModelScope.launch {
            userRepository.blockUser(userId)
                .onSuccess {
                    postUpdateManager.emitBlockUpdate(userId, isBlocked = true)
                    toastManager.showSuccess("Blocked @$displayName")
                    onBlocked()
                }
                .onFailure { error ->
                    toastManager.showError(error.message ?: "Failed to block user")
                }
        }
    }

    fun deletePost(onDeleted: () -> Unit = {}) {
        val postId = _uiState.value.post?.id ?: return
        viewModelScope.launch {
            postRepository.deletePost(postId)
                .onSuccess {
                    postUpdateManager.emitPostDeleted(postId)
                    toastManager.showSuccess("Post deleted")
                    onDeleted()
                }
                .onFailure { error ->
                    toastManager.showError(error.message ?: "Failed to delete post")
                }
        }
    }

    fun onWalletSelectedForTransaction(packageName: String) {
        // Capture pending state before coroutine launch — dismissWalletPicker() may clear it
        val activity = pendingPurchaseActivity
        viewModelScope.launch {
            transactionWalletManager.setWalletPackage(packageName)
            _uiState.value = _uiState.value.copy(showWalletPicker = false)
            if (activity != null) {
                purchase(activity)
            }
        }
    }

    fun dismissWalletPicker() {
        _uiState.value = _uiState.value.copy(showWalletPicker = false)
        pendingPurchaseActivity = null
    }

    override fun onCleared() {
        super.onCleared()
        pendingPurchaseActivity = null
        pollingJob?.cancel()
        purchasePollingJob?.cancel()
        periodicRefreshJob?.cancel()
    }
}
