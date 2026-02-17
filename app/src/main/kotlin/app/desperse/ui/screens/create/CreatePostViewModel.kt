package app.desperse.ui.screens.create

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.data.PostUpdateManager
import app.desperse.data.dto.request.CreateAssetRequest
import app.desperse.data.dto.request.CreatePostRequest
import app.desperse.data.dto.request.UpdatePostRequest
import app.desperse.data.dto.response.EditStateResult
import app.desperse.data.dto.response.MentionUser
import app.desperse.data.model.Categories
import app.desperse.data.model.MediaConstants
import app.desperse.data.model.Post
import app.desperse.data.repository.PostRepository
import app.desperse.data.upload.MediaUploadService
import app.desperse.data.upload.UploadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class UploadedMediaItem(
    val id: String = UUID.randomUUID().toString(),
    val localUri: Uri? = null,
    val url: String? = null,
    val mediaType: String = "image",
    val mimeType: String = "",
    val fileSize: Long = 0L,
    val fileName: String = "",
    val sortOrder: Int = 0,
    val uploadState: UploadState = UploadState.Idle
)

data class CreatePostUiState(
    // Post type
    val postType: String = "post", // "post", "collectible", "edition"

    // Content
    val caption: String = "",
    val selectedCategories: List<String> = emptyList(),

    // Media (consolidated: all items in one list)
    val mediaItems: List<UploadedMediaItem> = emptyList(),
    val coverMedia: UploadedMediaItem? = null,

    // NFT fields
    val nftName: String = "",
    val nftDescription: String = "",
    val nftSymbol: String = "",
    val royalties: String = "", // Display string (e.g. "5" for 5%)
    val isMutable: Boolean = true,

    // Edition fields
    val priceDisplay: String = "",
    val currency: String = "SOL",
    val maxSupplyEnabled: Boolean = false,
    val maxSupplyDisplay: String = "",
    val protectDownload: Boolean = false,

    // State
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val isLoading: Boolean = false,

    // Edit mode
    val isEditMode: Boolean = false,
    val editPostId: String? = null,
    val editState: EditStateResult? = null,
    val fieldLocking: FieldLocking = FieldLocking(),

    // Delete
    val showDeleteConfirmation: Boolean = false,
    val isDeleting: Boolean = false
)

data class FieldLocking(
    val isMediaLocked: Boolean = false,
    val isTypeLocked: Boolean = false,
    val isCaptionEditable: Boolean = true,
    val areCategoriesEditable: Boolean = true,
    val areNftFieldsEditable: Boolean = true,
    val isMutabilityEditable: Boolean = true,
    val arePricingEditable: Boolean = true
)

sealed class CreatePostEvent {
    data class PostCreated(val postId: String) : CreatePostEvent()
    data class PostUpdated(val postId: String) : CreatePostEvent()
    data object PostDeleted : CreatePostEvent()
    data class Error(val message: String) : CreatePostEvent()
}

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val uploadService: MediaUploadService,
    private val postUpdateManager: PostUpdateManager
) : ViewModel() {

    companion object {
        private const val TAG = "CreatePostViewModel"
        private const val MAX_ITEMS = 10
        private const val MAX_DOWNLOADABLE = 1

        // Price conversion constants
        const val SOL_DECIMALS = 9
        const val USDC_DECIMALS = 6
        const val MIN_PRICE_SOL = 0.1
        const val MIN_PRICE_USDC = 15.0
    }

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CreatePostEvent>(extraBufferCapacity = 5)
    val events: SharedFlow<CreatePostEvent> = _events.asSharedFlow()

    // === Post Type ===

    fun updatePostType(type: String) {
        if (_uiState.value.fieldLocking.isTypeLocked) return
        _uiState.update { it.copy(postType = type) }
    }

    // === Content ===

    fun updateCaption(text: String) {
        if (text.length <= 2000) {
            _uiState.update { it.copy(caption = text) }
        }
    }

    fun toggleCategory(category: String) {
        if (!_uiState.value.fieldLocking.areCategoriesEditable) return
        _uiState.update { state ->
            val current = state.selectedCategories.toMutableList()
            if (category in current) {
                current.remove(category)
            } else if (current.size < Categories.MAX_CATEGORIES) {
                current.add(category)
            }
            state.copy(selectedCategories = current)
        }
    }

    // === Media ===

    fun addMediaItem(uri: Uri) {
        if (_uiState.value.fieldLocking.isMediaLocked) return
        val currentItems = _uiState.value.mediaItems
        if (currentItems.size >= MAX_ITEMS) return

        val item = UploadedMediaItem(
            localUri = uri,
            sortOrder = currentItems.size
        )
        _uiState.update { it.copy(mediaItems = it.mediaItems + item) }
        uploadMediaItem(uri, item.id)
    }

    /**
     * Called after upload completes to handle downloadable constraints.
     * If the newly uploaded item is non-previewable (audio/document/3D),
     * replace any existing non-previewable item (max 1 downloadable).
     */
    private fun enforceDownloadableConstraint(itemId: String) {
        _uiState.update { state ->
            val item = state.mediaItems.find { it.id == itemId } ?: return@update state
            if (MediaConstants.isPreviewable(item.mediaType)) return@update state

            // Remove other non-previewable items (keep only this one)
            val filtered = state.mediaItems.filter { it.id == itemId || MediaConstants.isPreviewable(it.mediaType) }
                .mapIndexed { index, i -> i.copy(sortOrder = index) }
            state.copy(mediaItems = filtered)
        }
    }

    fun removeMediaItem(itemId: String) {
        if (_uiState.value.fieldLocking.isMediaLocked) return
        _uiState.update { state ->
            val filtered = state.mediaItems.filter { it.id != itemId }
                .mapIndexed { index, item -> item.copy(sortOrder = index) }
            state.copy(mediaItems = filtered)
        }
    }

    fun onCoverSelected(uri: Uri) {
        val item = UploadedMediaItem(localUri = uri, sortOrder = 0)
        _uiState.update { it.copy(coverMedia = item) }
        uploadCover(uri)
    }

    fun removeCover() {
        _uiState.update { it.copy(coverMedia = null) }
    }

    private fun uploadMediaItem(uri: Uri, itemId: String) {
        viewModelScope.launch {
            updateMediaItemState(itemId, UploadState.Uploading(0f))

            val result = uploadService.uploadFile(uri)
            result.onSuccess { uploaded ->
                _uiState.update { state ->
                    state.copy(
                        mediaItems = state.mediaItems.map { item ->
                            if (item.id == itemId) {
                                item.copy(
                                    url = uploaded.url,
                                    mimeType = uploaded.mimeType,
                                    fileSize = uploaded.fileSize,
                                    fileName = uploaded.fileName,
                                    mediaType = uploaded.mediaType,
                                    uploadState = UploadState.Success(uploaded.url, uploaded.mimeType, uploaded.fileSize)
                                )
                            } else item
                        }
                    )
                }
                // After upload, enforce max 1 downloadable constraint
                enforceDownloadableConstraint(itemId)
            }
            result.onFailure { error ->
                updateMediaItemState(itemId, UploadState.Failed(error.message ?: "Upload failed"))
            }
        }
    }

    private fun uploadCover(uri: Uri) {
        viewModelScope.launch {
            val result = uploadService.uploadFile(uri)
            result.onSuccess { uploaded ->
                _uiState.update { state ->
                    state.copy(
                        coverMedia = state.coverMedia?.copy(
                            url = uploaded.url,
                            mimeType = uploaded.mimeType,
                            fileSize = uploaded.fileSize,
                            fileName = uploaded.fileName,
                            mediaType = uploaded.mediaType,
                            uploadState = UploadState.Success(uploaded.url, uploaded.mimeType, uploaded.fileSize)
                        )
                    )
                }
            }
            result.onFailure { error ->
                val msg = error.message ?: "Upload failed"
                _uiState.update { state ->
                    state.copy(coverMedia = state.coverMedia?.copy(uploadState = UploadState.Failed(msg)))
                }
            }
        }
    }

    private fun updateMediaItemState(itemId: String, state: UploadState) {
        _uiState.update { s ->
            s.copy(mediaItems = s.mediaItems.map { if (it.id == itemId) it.copy(uploadState = state) else it })
        }
    }

    // === Mention Search ===

    suspend fun searchMentionUsers(query: String): List<MentionUser> {
        return postRepository.searchMentionUsers(query.ifEmpty { null })
            .getOrElse { emptyList() }
    }

    // === NFT Fields ===

    fun updateNftName(name: String) {
        if (!_uiState.value.fieldLocking.areNftFieldsEditable) return
        if (name.length <= 32) _uiState.update { it.copy(nftName = name) }
    }

    fun updateNftDescription(desc: String) {
        if (!_uiState.value.fieldLocking.areNftFieldsEditable) return
        if (desc.length <= 5000) _uiState.update { it.copy(nftDescription = desc) }
    }

    fun updateNftSymbol(symbol: String) {
        if (!_uiState.value.fieldLocking.areNftFieldsEditable) return
        if (symbol.length <= 10) _uiState.update { it.copy(nftSymbol = symbol.uppercase()) }
    }

    fun updateRoyalties(value: String) {
        if (!_uiState.value.fieldLocking.areNftFieldsEditable) return
        val filtered = value.filter { it.isDigit() || it == '.' }
        val num = filtered.toDoubleOrNull()
        if (num == null || num <= 10.0) _uiState.update { it.copy(royalties = filtered) }
    }

    fun updateIsMutable(mutable: Boolean) {
        if (!_uiState.value.fieldLocking.isMutabilityEditable) return
        _uiState.update { it.copy(isMutable = mutable) }
    }

    // === Edition Fields ===

    fun updatePrice(price: String) {
        if (!_uiState.value.fieldLocking.arePricingEditable) return
        val filtered = price.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(priceDisplay = filtered) }
    }

    fun updateCurrency(currency: String) {
        if (!_uiState.value.fieldLocking.arePricingEditable) return
        _uiState.update { it.copy(currency = currency) }
    }

    fun toggleMaxSupply(enabled: Boolean) {
        if (!_uiState.value.fieldLocking.arePricingEditable) return
        _uiState.update { it.copy(maxSupplyEnabled = enabled) }
    }

    fun updateMaxSupply(supply: String) {
        if (!_uiState.value.fieldLocking.arePricingEditable) return
        val filtered = supply.filter { it.isDigit() }
        _uiState.update { it.copy(maxSupplyDisplay = filtered) }
    }

    fun updateProtectDownload(protect: Boolean) {
        _uiState.update { it.copy(protectDownload = protect) }
    }

    // === Validation ===

    fun isValid(): Boolean {
        val state = _uiState.value
        // Must have at least one uploaded media item
        if (state.mediaItems.isEmpty()) return false
        if (!state.mediaItems.all { it.url != null }) return false

        // Edition validation
        if (state.postType == "edition") {
            val price = state.priceDisplay.toDoubleOrNull() ?: return false
            val minPrice = if (state.currency == "SOL") MIN_PRICE_SOL else MIN_PRICE_USDC
            if (price < minPrice) return false
            if (state.nftName.isBlank()) return false
        }

        return true
    }

    // === Submit ===

    fun submit() {
        if (_uiState.value.isSubmitting) return
        if (_uiState.value.isEditMode) {
            submitUpdate()
        } else {
            submitCreate()
        }
    }

    private fun submitCreate() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }

            try {
                val firstItem = state.mediaItems.first()
                val mediaUrl = firstItem.url!!
                val assets = state.mediaItems.map { item ->
                    CreateAssetRequest(
                        url = item.url!!,
                        mediaType = item.mediaType,
                        fileName = item.fileName,
                        mimeType = item.mimeType,
                        fileSize = item.fileSize,
                        sortOrder = item.sortOrder
                    )
                }

                val priceBaseUnits = if (state.postType == "edition") {
                    convertPriceToBaseUnits(state.priceDisplay, state.currency)
                } else null

                val maxSupply = if (state.maxSupplyEnabled) {
                    state.maxSupplyDisplay.toIntOrNull()
                } else null

                val royaltyBasisPoints = state.royalties.toDoubleOrNull()?.let {
                    (it * 100).toInt() // 5% = 500 basis points
                }

                val request = CreatePostRequest(
                    mediaUrl = mediaUrl,
                    coverUrl = state.coverMedia?.url,
                    caption = state.caption.ifBlank { null },
                    categories = state.selectedCategories.ifEmpty { null },
                    type = state.postType,
                    assets = assets,
                    price = priceBaseUnits,
                    currency = if (state.postType == "edition") state.currency else null,
                    maxSupply = maxSupply,
                    nftName = state.nftName.ifBlank { null },
                    nftSymbol = state.nftSymbol.ifBlank { null },
                    nftDescription = state.nftDescription.ifBlank { null },
                    sellerFeeBasisPoints = royaltyBasisPoints,
                    isMutable = state.isMutable,
                    protectDownload = state.protectDownload,
                    mediaMimeType = firstItem.mimeType.ifBlank { null },
                    mediaFileSize = firstItem.fileSize.takeIf { it > 0 }
                )

                val result = postRepository.createPost(request)
                result.onSuccess { post ->
                    Log.d(TAG, "Post created: ${post.id}")
                    _uiState.update { it.copy(isSubmitting = false) }
                    postUpdateManager.emitPostCreated(post.id)
                    _events.emit(CreatePostEvent.PostCreated(post.id))
                }
                result.onFailure { error ->
                    Log.e(TAG, "Create failed", error)
                    _uiState.update { it.copy(isSubmitting = false, submitError = error.message) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Create error", e)
                _uiState.update { it.copy(isSubmitting = false, submitError = e.message) }
            }
        }
    }

    private fun submitUpdate() {
        val state = _uiState.value
        val postId = state.editPostId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }

            try {
                val isNftType = state.postType == "collectible" || state.postType == "edition"
                val isEdition = state.postType == "edition"

                val priceBaseUnits = if (isEdition && state.fieldLocking.arePricingEditable) {
                    convertPriceToBaseUnits(state.priceDisplay, state.currency)
                } else null

                val maxSupply = if (isEdition && state.fieldLocking.arePricingEditable && state.maxSupplyEnabled) {
                    state.maxSupplyDisplay.toIntOrNull()
                } else null

                val royaltyBasisPoints = if (isNftType && state.fieldLocking.areNftFieldsEditable) {
                    state.royalties.toDoubleOrNull()?.let { (it * 100).toInt() }
                } else null

                val request = UpdatePostRequest(
                    caption = state.caption.ifBlank { null },
                    categories = state.selectedCategories.ifEmpty { null },
                    nftName = if (isNftType && state.fieldLocking.areNftFieldsEditable) state.nftName.ifBlank { null } else null,
                    nftSymbol = if (isNftType && state.fieldLocking.areNftFieldsEditable) state.nftSymbol.ifBlank { null } else null,
                    nftDescription = if (isNftType && state.fieldLocking.areNftFieldsEditable) state.nftDescription.ifBlank { null } else null,
                    sellerFeeBasisPoints = royaltyBasisPoints,
                    isMutable = if (isNftType && state.fieldLocking.isMutabilityEditable) state.isMutable else null,
                    price = priceBaseUnits,
                    currency = if (isEdition && state.fieldLocking.arePricingEditable) state.currency else null,
                    maxSupply = maxSupply
                )

                val result = postRepository.updatePost(postId, request)
                result.onSuccess { post ->
                    Log.d(TAG, "Post updated: ${post.id}")
                    postUpdateManager.emitPostEdited(post)
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.emit(CreatePostEvent.PostUpdated(post.id))
                }
                result.onFailure { error ->
                    Log.e(TAG, "Update failed", error)
                    _uiState.update { it.copy(isSubmitting = false, submitError = error.message) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update error", e)
                _uiState.update { it.copy(isSubmitting = false, submitError = e.message) }
            }
        }
    }

    // === Edit Mode ===

    fun loadPostForEdit(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEditMode = true, editPostId = postId) }

            try {
                // Fetch post and edit state in parallel
                val postResult = postRepository.getPost(postId)
                val editStateResult = postRepository.getEditState(postId)

                postResult.onSuccess { post ->
                    populateFromPost(post)
                }
                editStateResult.onSuccess { editState ->
                    applyEditState(editState)
                }

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load post for edit", e)
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(CreatePostEvent.Error(e.message ?: "Failed to load post"))
            }
        }
    }

    private fun populateFromPost(post: Post) {
        _uiState.update { state ->
            // Build mediaItems from existing post
            val items = mutableListOf<UploadedMediaItem>()
            post.mediaUrl?.let { url ->
                items.add(
                    UploadedMediaItem(
                        url = url,
                        mediaType = "image", // Media is locked in edit mode
                        uploadState = UploadState.Success(url, "", 0L)
                    )
                )
            }

            state.copy(
                postType = post.type,
                caption = post.caption ?: "",
                nftName = post.nftName ?: "",
                priceDisplay = post.price?.let { formatPriceFromBaseUnits(it, post.currency ?: "SOL") } ?: "",
                currency = post.currency ?: "SOL",
                maxSupplyEnabled = post.maxSupply != null,
                maxSupplyDisplay = post.maxSupply?.toString() ?: "",
                mediaItems = items
            )
        }
    }

    private fun applyEditState(editState: EditStateResult) {
        val state = _uiState.value
        val isCollectible = state.postType == "collectible"
        val isEdition = state.postType == "edition"

        val locking = FieldLocking(
            isMediaLocked = true, // Always locked in edit
            isTypeLocked = true, // Always locked in edit
            isCaptionEditable = true, // Always editable
            areCategoriesEditable = when {
                isCollectible && editState.isMinted -> false
                else -> true
            },
            areNftFieldsEditable = when {
                state.postType == "post" -> false
                editState.areNftFieldsLocked -> false
                else -> true
            },
            isMutabilityEditable = !editState.isMinted,
            arePricingEditable = when {
                !isEdition -> false
                editState.hasConfirmedPurchases -> false
                else -> true
            }
        )

        _uiState.update { it.copy(editState = editState, fieldLocking = locking) }
    }

    // === Delete ===

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun deletePost() {
        val postId = _uiState.value.editPostId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, showDeleteConfirmation = false) }
            try {
                val result = postRepository.deletePost(postId)
                result.onSuccess {
                    postUpdateManager.emitPostDeleted(postId)
                    _events.emit(CreatePostEvent.PostDeleted)
                }
                result.onFailure { error ->
                    _uiState.update { it.copy(isDeleting = false, submitError = error.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDeleting = false, submitError = e.message) }
            }
        }
    }

    // === Helpers ===

    private fun convertPriceToBaseUnits(priceDisplay: String, currency: String): Long? {
        val price = priceDisplay.toDoubleOrNull() ?: return null
        return when (currency) {
            "SOL" -> (price * 1_000_000_000).toLong() // 9 decimals
            "USDC" -> (price * 1_000_000).toLong() // 6 decimals
            else -> null
        }
    }

    private fun formatPriceFromBaseUnits(baseUnits: Double, currency: String): String {
        val display = when (currency) {
            "SOL" -> baseUnits / 1_000_000_000
            "USDC" -> baseUnits / 1_000_000
            else -> baseUnits
        }
        return if (display == display.toLong().toDouble()) {
            display.toLong().toString()
        } else {
            display.toString()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(submitError = null) }
    }
}
