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
import app.desperse.data.dto.response.Comment
import app.desperse.data.dto.response.MentionUser
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
    val comments: List<Comment> = emptyList(),
    val isLoadingPost: Boolean = true,
    val isLoadingComments: Boolean = true,
    val isSubmittingComment: Boolean = false,
    val commentError: String? = null,
    val error: String? = null,
    val collectState: CollectState = CollectState.Idle,
    val purchaseState: PurchaseState = PurchaseState.Idle,
    val currentUserAvatarUrl: String? = null,
    val currentUserId: String? = null,
    val deletingCommentId: String? = null,
    val lastFetchTime: Long = 0L,
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
        loadPost()
        loadComments()
        observePostUpdates()
        observeCurrentUser()
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
                                collectCount = update.collectCount
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
            _uiState.value = _uiState.value.copy(isLoadingPost = true, error = null)
            postRepository.getPost(postId)
                .onSuccess { post ->
                    _uiState.value = _uiState.value.copy(
                        post = post,
                        isLoadingPost = false,
                        lastFetchTime = System.currentTimeMillis(),
                        // If already collected, update state
                        collectState = if (post.isCollected) CollectState.Success else _uiState.value.collectState
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingPost = false,
                        error = error.message ?: "Failed to load post"
                    )
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

    fun loadComments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingComments = true)
            postRepository.getComments(postId)
                .onSuccess { comments ->
                    _uiState.value = _uiState.value.copy(
                        comments = comments,
                        isLoadingComments = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingComments = false)
                }
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

    fun createComment(content: String) {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty() || trimmedContent.length > 280) return
        if (_uiState.value.isSubmittingComment) return

        _uiState.value = _uiState.value.copy(isSubmittingComment = true, commentError = null)

        viewModelScope.launch {
            postRepository.createComment(postId, trimmedContent)
                .onSuccess { newComment ->
                    // Add the new comment to the beginning of the list
                    val updatedComments = listOf(newComment) + _uiState.value.comments
                    val newCommentCount = (_uiState.value.post?.commentCount ?: 0) + 1

                    _uiState.value = _uiState.value.copy(
                        comments = updatedComments,
                        isSubmittingComment = false,
                        post = _uiState.value.post?.copy(commentCount = newCommentCount)
                    )

                    // Broadcast comment count update to other screens
                    postUpdateManager.emitCommentCountUpdate(postId, newCommentCount)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmittingComment = false,
                        commentError = error.message ?: "Failed to post comment"
                    )
                }
        }
    }

    fun clearCommentError() {
        _uiState.value = _uiState.value.copy(commentError = null)
    }

    /**
     * Delete a comment (only own comments).
     * Uses optimistic update with rollback on failure.
     */
    fun deleteComment(commentId: String) {
        val currentUserId = _uiState.value.currentUserId ?: return
        val comments = _uiState.value.comments
        val commentToDelete = comments.find { it.id == commentId } ?: return

        // Verify ownership
        if (commentToDelete.user.id != currentUserId) {
            Log.w(TAG, "Attempted to delete comment owned by another user")
            return
        }

        // Skip if already deleting
        if (_uiState.value.deletingCommentId != null) return

        // Optimistic update: remove comment immediately
        val updatedComments = comments.filter { it.id != commentId }
        val newCommentCount = (_uiState.value.post?.commentCount ?: 1) - 1

        _uiState.value = _uiState.value.copy(
            comments = updatedComments,
            deletingCommentId = commentId,
            post = _uiState.value.post?.copy(commentCount = newCommentCount.coerceAtLeast(0))
        )

        viewModelScope.launch {
            // Broadcast comment count update to other screens
            postUpdateManager.emitCommentCountUpdate(postId, newCommentCount.coerceAtLeast(0))

            postRepository.deleteComment(postId, commentId)
                .onSuccess {
                    // Successfully deleted - just clear deleting state
                    _uiState.value = _uiState.value.copy(deletingCommentId = null)
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to delete comment: ${error.message}")
                    // Rollback: restore the comment
                    val restoredComments = comments // Original list
                    val originalCommentCount = (_uiState.value.post?.commentCount ?: 0) + 1

                    _uiState.value = _uiState.value.copy(
                        comments = restoredComments,
                        deletingCommentId = null,
                        post = _uiState.value.post?.copy(commentCount = originalCommentCount),
                        commentError = error.message ?: "Failed to delete comment"
                    )

                    // Broadcast rollback to other screens
                    postUpdateManager.emitCommentCountUpdate(postId, originalCommentCount)
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
                            _uiState.value = _uiState.value.copy(
                                collectState = CollectState.Failed(
                                    result.error ?: result.message ?: "Collection failed"
                                )
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        collectState = CollectState.Failed(error.message ?: "Collection failed")
                    )
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
            _uiState.value = _uiState.value.copy(
                purchaseState = PurchaseState.Failed(
                    "No compatible wallet app found. Please install a Solana wallet.",
                    canRetry = false
                )
            )
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
                        }
                }
                .onFailure { error ->
                    Log.e(TAG, "Buy request failed: ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        purchaseState = PurchaseState.Failed(
                            error.message ?: "Failed to initiate purchase"
                        )
                    )
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
                                _uiState.value = _uiState.value.copy(
                                    purchaseState = PurchaseState.Success,
                                    post = _uiState.value.post?.copy(
                                        isCollected = true,
                                        collectCount = newCollectCount
                                    )
                                )
                                // Broadcast update to other screens
                                postUpdateManager.emitCollectUpdate(postId, true, newCollectCount)
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
     * Search users for @mention autocomplete
     */
    suspend fun searchMentionUsers(query: String): List<MentionUser> {
        return postRepository.searchMentionUsers(query.ifEmpty { null })
            .getOrElse { emptyList() }
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
