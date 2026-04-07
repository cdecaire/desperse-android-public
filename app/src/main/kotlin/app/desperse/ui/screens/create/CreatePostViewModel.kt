package app.desperse.ui.screens.create

import android.net.Uri
import android.util.Log
import app.desperse.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.core.arweave.ArweaveUtils
import app.desperse.data.PostUpdateManager
import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.data.dto.request.CreateAssetRequest
import app.desperse.data.dto.request.CreatePostRequest
import app.desperse.data.dto.request.UpdatePostRequest
import app.desperse.data.dto.response.EditStateResult
import app.desperse.data.dto.response.MentionUser
import app.desperse.data.model.Categories
import app.desperse.ui.screens.settings.LICENSE_PRESETS
import app.desperse.data.model.MediaConstants
import app.desperse.data.model.Post
import app.desperse.data.repository.ArweaveRepository
import app.desperse.data.repository.DeviceAudioItem
import app.desperse.data.repository.DeviceMediaItem
import app.desperse.data.repository.DeviceMediaRepository
import app.desperse.data.repository.MediaAlbum
import app.desperse.data.repository.PostRepository
import app.desperse.core.wallet.TransactionWalletManager
import app.desperse.ui.util.MintWindowUtils
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

enum class MediaTab { Image, Video, Audio, ThreeD }

data class CreatePostUiState(
    // Post type
    val postType: String = "post", // "post", "collectible", "edition"

    // Content
    val caption: String = "",
    val selectedCategories: List<String> = emptyList(),

    // Media (consolidated: all items in one list)
    val mediaItems: List<UploadedMediaItem> = emptyList(),
    val coverMedia: UploadedMediaItem? = null,

    // Attachments (downloadable files: PDF, ZIP, EPUB — for collectible/edition)
    val attachments: List<UploadedMediaItem> = emptyList(),

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

    // Timed edition mint window
    val mintWindowEnabled: Boolean = false,
    val mintWindowStartMode: String = "now", // "now" | "scheduled"
    val mintWindowStartTime: Long? = null, // epoch millis for scheduled start
    val mintWindowDurationHours: Double? = null,

    // Arweave permanent storage
    val storageType: String = "centralized", // "centralized" or "arweave"
    val arweaveFundingState: ArweaveFundingState = ArweaveFundingState.NotChecked,

    // State
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val isLoading: Boolean = false,

    // Edit mode
    val isEditMode: Boolean = false,
    val editPostId: String? = null,
    val editState: EditStateResult? = null,
    val fieldLocking: FieldLocking = FieldLocking(),

    // Copyright & licensing (for collectible/edition)
    val copyrightLicensePreset: String? = null,
    val copyrightLicenseCustom: String = "",
    val copyrightHolder: String = "",
    val copyrightRights: String = "",
    val creatorDefaultsLoaded: Boolean = false,

    // Delete
    val showDeleteConfirmation: Boolean = false,
    val isDeleting: Boolean = false,

    // Gallery state (Step 1 - media selection)
    val mediaTab: MediaTab = MediaTab.Image,
    val galleryItems: List<DeviceMediaItem> = emptyList(),
    val galleryAlbums: List<MediaAlbum> = emptyList(),
    val selectedAlbumId: String? = null, // null = Recents
    val selectedMediaItems: List<DeviceMediaItem> = emptyList(),
    val galleryPermissionGranted: Boolean = false,
    val galleryPermissionDenied: Boolean = false,
    val isLoadingGallery: Boolean = false,
    val galleryHasMore: Boolean = true,
    val audioFile: UploadedMediaItem? = null,   // Audio tab: selected audio file
    val threeDFile: UploadedMediaItem? = null,   // 3D tab: selected 3D file
    val deviceAudioFiles: List<DeviceAudioItem> = emptyList(), // Audio files on device
    val isLoadingAudioFiles: Boolean = false
)

data class FieldLocking(
    val isMediaLocked: Boolean = false,
    val isTypeLocked: Boolean = false,
    val isCaptionEditable: Boolean = true,
    val areCategoriesEditable: Boolean = true,
    val areNftFieldsEditable: Boolean = true,
    val isMutabilityEditable: Boolean = true,
    val arePricingEditable: Boolean = true,
    val isStorageTypeLocked: Boolean = false,
    val areTimeWindowFieldsLocked: Boolean = false
)

sealed class ArweaveFundingState {
    data object NotChecked : ArweaveFundingState()
    data object Loading : ArweaveFundingState()
    data class Loaded(
        val estimatedCostWinc: String,
        val estimatedCostUsd: Double,
        val walletWinc: String,
        val sharedRemainingWinc: String,
        val hasSufficientSharedCredits: Boolean,
        val hasActiveApproval: Boolean
    ) : ArweaveFundingState()
    data class Error(val message: String) : ArweaveFundingState()
}

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
    private val postUpdateManager: PostUpdateManager,
    private val arweaveRepository: ArweaveRepository,
    private val transactionWalletManager: TransactionWalletManager,
    private val api: DesperseApi,
    private val deviceMediaRepository: DeviceMediaRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CreatePostViewModel"
        private const val MAX_ITEMS = 10
        private const val MAX_DOWNLOADABLE = 1
        private const val GALLERY_PAGE_SIZE = 80

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

    private var galleryOffset = 0

    // === Gallery (Step 1) ===

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(
            galleryPermissionGranted = granted,
            galleryPermissionDenied = !granted
        ) }
        if (granted) {
            loadGalleryPage(reset = true)
        }
    }

    fun loadGalleryPage(reset: Boolean = false) {
        if (!reset && (_uiState.value.isLoadingGallery || !_uiState.value.galleryHasMore)) return

        if (reset) galleryOffset = 0

        _uiState.update { it.copy(isLoadingGallery = true) }

        viewModelScope.launch {
            val tab = _uiState.value.mediaTab
            val items = withContext(Dispatchers.IO) {
                when (tab) {
                    MediaTab.Video -> deviceMediaRepository.queryMedia(
                        albumId = DeviceMediaRepository.ALBUM_ALL_VIDEOS,
                        offset = galleryOffset,
                        limit = GALLERY_PAGE_SIZE
                    )
                    // Image, Audio (cover selection) — images only
                    else -> deviceMediaRepository.queryMedia(
                        albumId = DeviceMediaRepository.ALBUM_ALL_PHOTOS,
                        offset = galleryOffset,
                        limit = GALLERY_PAGE_SIZE
                    )
                }
            }

            galleryOffset += items.size

            _uiState.update { state ->
                val newItems = if (reset) items else state.galleryItems + items
                state.copy(
                    galleryItems = newItems,
                    galleryHasMore = items.size >= GALLERY_PAGE_SIZE,
                    isLoadingGallery = false
                )
            }
        }
    }

    fun switchMediaTab(tab: MediaTab) {
        if (_uiState.value.mediaTab == tab) return
        _uiState.update { it.copy(
            mediaTab = tab,
            selectedMediaItems = emptyList(),
            galleryItems = emptyList(),
            galleryHasMore = true
        ) }
        galleryOffset = 0
        when (tab) {
            MediaTab.Audio -> {
                loadGalleryPage(reset = true) // Images for cover
                loadAudioFiles()
            }
            MediaTab.ThreeD -> loadGalleryPage(reset = true) // Images for cover
            else -> loadGalleryPage(reset = true)
        }
    }

    fun loadAudioFiles() {
        if (_uiState.value.isLoadingAudioFiles) return
        _uiState.update { it.copy(isLoadingAudioFiles = true) }
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) {
                deviceMediaRepository.queryAudioFiles()
            }
            _uiState.update { it.copy(
                deviceAudioFiles = files,
                isLoadingAudioFiles = false
            ) }
        }
    }

    fun selectAudioFile(audioItem: DeviceAudioItem) {
        val item = UploadedMediaItem(
            localUri = audioItem.uri,
            mediaType = "audio",
            mimeType = audioItem.mimeType,
            fileName = audioItem.displayName,
            fileSize = audioItem.fileSize,
            sortOrder = 0
        )
        _uiState.update { it.copy(audioFile = item) }
    }

    fun toggleMediaSelection(item: DeviceMediaItem) {
        _uiState.update { state ->
            when (state.mediaTab) {
                MediaTab.Image -> {
                    // Always multi-select for images
                    val current = state.selectedMediaItems.toMutableList()
                    val existing = current.indexOfFirst { it.id == item.id }
                    if (existing >= 0) {
                        current.removeAt(existing)
                    } else if (current.size < MAX_ITEMS) {
                        current.add(item)
                    }
                    state.copy(selectedMediaItems = current)
                }
                MediaTab.Video, MediaTab.Audio, MediaTab.ThreeD -> {
                    // Single-select for video, audio cover, or 3D cover
                    state.copy(selectedMediaItems = listOf(item))
                }
            }
        }
    }

    // === Audio/3D file picking ===

    fun addAudioFile(uri: Uri, fileName: String = "") {
        val item = UploadedMediaItem(
            localUri = uri,
            mediaType = "audio",
            fileName = fileName,
            sortOrder = 0
        )
        _uiState.update { it.copy(audioFile = item) }
    }

    fun removeAudioFile() {
        _uiState.update { it.copy(audioFile = null) }
    }

    fun addThreeDFile(uri: Uri, fileName: String = "") {
        val item = UploadedMediaItem(
            localUri = uri,
            mediaType = "3d",
            fileName = fileName,
            sortOrder = 0
        )
        _uiState.update { it.copy(threeDFile = item) }
    }

    fun removeThreeDFile() {
        _uiState.update { it.copy(threeDFile = null) }
    }

    /**
     * Convert gallery selections to UploadedMediaItems and start uploads.
     * Called when user taps "Next" from media selection.
     * Behavior varies by media tab.
     */
    fun onNextFromMediaSelect() {
        val state = _uiState.value
        when (state.mediaTab) {
            MediaTab.Image -> {
                val selected = state.selectedMediaItems
                if (selected.isEmpty()) return
                val items = selected.mapIndexed { index, mediaItem ->
                    UploadedMediaItem(
                        localUri = mediaItem.uri,
                        mediaType = "image",
                        mimeType = mediaItem.mimeType,
                        sortOrder = index
                    )
                }
                _uiState.update { it.copy(mediaItems = items) }
                items.forEach { item ->
                    item.localUri?.let { uri -> uploadMediaItem(uri, item.id) }
                }
            }
            MediaTab.Video -> {
                val selected = state.selectedMediaItems.firstOrNull() ?: return
                val item = UploadedMediaItem(
                    localUri = selected.uri,
                    mediaType = "video",
                    mimeType = selected.mimeType,
                    sortOrder = 0
                )
                _uiState.update { it.copy(mediaItems = listOf(item)) }
                item.localUri?.let { uploadMediaItem(it, item.id) }
            }
            MediaTab.Audio -> {
                val audio = state.audioFile ?: return
                val audioItem = audio.copy(sortOrder = 0)
                _uiState.update { it.copy(mediaItems = listOf(audioItem)) }
                audio.localUri?.let { uploadMediaItem(it, audioItem.id) }
                uploadCoverFromSelection(state)
            }
            MediaTab.ThreeD -> {
                val file = state.threeDFile ?: return
                val item = file.copy(sortOrder = 0)
                _uiState.update { it.copy(mediaItems = listOf(item)) }
                file.localUri?.let { uploadMediaItem(it, item.id, file.fileName) }
                uploadCoverFromSelection(state)
            }
        }
    }

    /** Upload cover image from the first selected gallery item (used by Audio and 3D tabs). */
    private fun uploadCoverFromSelection(state: CreatePostUiState) {
        val coverSelection = state.selectedMediaItems.firstOrNull() ?: return
        val cover = UploadedMediaItem(
            localUri = coverSelection.uri,
            mediaType = "image",
            mimeType = coverSelection.mimeType,
            sortOrder = 0
        )
        _uiState.update { it.copy(coverMedia = cover) }
        uploadCover(coverSelection.uri)
    }

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

    // === Attachments (downloadable files for collectible/edition) ===

    fun addAttachment(uri: Uri) {
        val item = UploadedMediaItem(
            localUri = uri,
            sortOrder = _uiState.value.attachments.size
        )
        _uiState.update { it.copy(attachments = it.attachments + item) }
        uploadAttachment(uri, item.id)
    }

    fun removeAttachment(itemId: String) {
        _uiState.update { state ->
            val filtered = state.attachments.filter { it.id != itemId }
                .mapIndexed { index, item -> item.copy(sortOrder = index) }
            state.copy(attachments = filtered)
        }
    }

    private fun uploadAttachment(uri: Uri, itemId: String) {
        viewModelScope.launch {
            updateAttachmentState(itemId, UploadState.Uploading(0f))

            val result = uploadService.uploadFile(uri)
            result.onSuccess { uploaded ->
                _uiState.update { state ->
                    state.copy(
                        attachments = state.attachments.map { item ->
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
                if (_uiState.value.storageType == "arweave") {
                    checkArweaveFunding()
                }
            }
            result.onFailure { error ->
                updateAttachmentState(itemId, UploadState.Failed(error.message ?: "Upload failed"))
            }
        }
    }

    private fun updateAttachmentState(itemId: String, state: UploadState) {
        _uiState.update { s ->
            s.copy(attachments = s.attachments.map { if (it.id == itemId) it.copy(uploadState = state) else it })
        }
    }

    private fun uploadMediaItem(uri: Uri, itemId: String, fileName: String? = null) {
        viewModelScope.launch {
            updateMediaItemState(itemId, UploadState.Uploading(0f))

            val result = uploadService.uploadFile(uri, fileName)
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

                // Recalculate Arweave cost if permanent storage is enabled
                if (_uiState.value.storageType == "arweave") {
                    checkArweaveFunding()
                }
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

    // === Timed Edition ===

    fun toggleMintWindow(enabled: Boolean) {
        if (_uiState.value.fieldLocking.areTimeWindowFieldsLocked) return
        _uiState.update { it.copy(mintWindowEnabled = enabled) }
    }

    fun updateMintWindowStartMode(mode: String) {
        if (_uiState.value.fieldLocking.areTimeWindowFieldsLocked) return
        _uiState.update { it.copy(mintWindowStartMode = mode) }
    }

    fun updateMintWindowStartTime(millis: Long) {
        if (_uiState.value.fieldLocking.areTimeWindowFieldsLocked) return
        _uiState.update { it.copy(mintWindowStartTime = millis) }
    }

    fun updateMintWindowDurationHours(hours: Double?) {
        if (_uiState.value.fieldLocking.areTimeWindowFieldsLocked) return
        _uiState.update { it.copy(mintWindowDurationHours = hours) }
    }

    // === Copyright & Licensing ===

    fun updateCopyrightLicensePreset(preset: String?) {
        if (!_uiState.value.fieldLocking.areNftFieldsEditable) return
        _uiState.update { it.copy(copyrightLicensePreset = preset) }
    }

    fun updateCopyrightLicenseCustom(text: String) {
        if (!_uiState.value.fieldLocking.areNftFieldsEditable) return
        if (text.length <= 100) _uiState.update { it.copy(copyrightLicenseCustom = text) }
    }

    fun updateCopyrightHolder(text: String) {
        if (!_uiState.value.fieldLocking.areNftFieldsEditable) return
        if (text.length <= 200) _uiState.update { it.copy(copyrightHolder = text) }
    }

    fun updateCopyrightRights(text: String) {
        if (!_uiState.value.fieldLocking.areNftFieldsEditable) return
        if (text.length <= 1000) _uiState.update { it.copy(copyrightRights = text) }
    }

    fun loadCreatorDefaults() {
        val state = _uiState.value
        // Only load once, only for create mode, only for NFT types
        if (state.creatorDefaultsLoaded || state.isEditMode) return
        val type = state.postType
        if (type != "collectible" && type != "edition") return

        _uiState.update { it.copy(creatorDefaultsLoaded = true) }

        viewModelScope.launch {
            when (val result = safeApiCall { api.getCreatorSettings() }) {
                is ApiResult.Success -> {
                    val data = result.data
                    // Only apply defaults if user hasn't touched copyright fields
                    _uiState.update { current ->
                        if (current.copyrightLicensePreset != null ||
                            current.copyrightLicenseCustom.isNotBlank() ||
                            current.copyrightHolder.isNotBlank() ||
                            current.copyrightRights.isNotBlank()
                        ) {
                            current // User already touched fields, don't overwrite
                        } else {
                            current.copy(
                                copyrightLicensePreset = data.copyrightLicensePreset,
                                copyrightLicenseCustom = data.copyrightLicenseCustom ?: "",
                                copyrightHolder = data.copyrightHolder ?: "",
                                copyrightRights = data.copyrightRights ?: ""
                            )
                        }
                    }
                }
                is ApiResult.Error -> {
                    // Non-critical, silently ignore
                    Log.w(TAG, "Failed to load creator defaults: ${result.message}")
                }
            }
        }
    }

    // === Arweave Storage ===

    fun updateStorageType(type: String) {
        if (_uiState.value.fieldLocking.isStorageTypeLocked) return
        _uiState.update { it.copy(storageType = type) }
        if (type == "arweave") {
            checkArweaveFunding()
        } else {
            _uiState.update { it.copy(arweaveFundingState = ArweaveFundingState.NotChecked) }
        }
    }

    fun checkArweaveFunding() {
        if (_uiState.value.arweaveFundingState is ArweaveFundingState.Loading) return

        _uiState.update { it.copy(arweaveFundingState = ArweaveFundingState.Loading) }

        viewModelScope.launch {
            try {
                // Estimate total media size for price calculation
                val totalBytes = _uiState.value.mediaItems.sumOf { it.fileSize }.coerceAtLeast(1024L)
                val mediaCount = _uiState.value.mediaItems.size
                Log.d(TAG, "checkArweaveFunding: totalBytes=$totalBytes, mediaItems=$mediaCount")

                // Fetch price, balance, and rates
                val priceResult = arweaveRepository.getUploadPrice(totalBytes)
                val walletAddress = transactionWalletManager.getActiveWalletAddress()
                if (walletAddress == null) {
                    Log.w(TAG, "checkArweaveFunding: no wallet connected")
                    _uiState.update {
                        it.copy(arweaveFundingState = ArweaveFundingState.Error("No wallet connected"))
                    }
                    return@launch
                }

                // Use balance endpoint which includes approvals
                val balanceResult = arweaveRepository.getBalance(
                    walletAddress = walletAddress,
                    forceRefresh = false
                )
                val ratesResult = arweaveRepository.getFiatRates()

                val price = priceResult.getOrNull()
                val balance = balanceResult.getOrNull()
                val rates = ratesResult.getOrNull()

                Log.d(TAG, "checkArweaveFunding: price=${price?.winc}, balance=${balance?.winc}, rates=${rates?.winc}")

                if (price == null) {
                    Log.e(TAG, "checkArweaveFunding: price is null, error=${priceResult.exceptionOrNull()?.message}")
                    _uiState.update {
                        it.copy(arweaveFundingState = ArweaveFundingState.Error("Failed to get upload price"))
                    }
                    return@launch
                }

                val sharedRemaining = balance?.let {
                    ArweaveUtils.calculateSharedRemaining(it.givenApprovals)
                } ?: java.math.BigInteger.ZERO

                val hasActive = balance?.let {
                    ArweaveUtils.hasActiveApproval(it.givenApprovals)
                } ?: false

                val hasSufficient = ArweaveUtils.hasSufficientShared(sharedRemaining, price.winc)

                val costUsd = if (rates != null) {
                    val wincPerDollar = rates.winc.toBigDecimalOrNull() ?: java.math.BigDecimal.ONE
                    val costWinc = price.winc.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
                    if (wincPerDollar.compareTo(java.math.BigDecimal.ZERO) != 0) {
                        costWinc.divide(wincPerDollar, 4, java.math.RoundingMode.HALF_UP).toDouble()
                    } else 0.0
                } else 0.0

                Log.d(TAG, "checkArweaveFunding LOADED: costWinc=${price.winc}, costUsd=$costUsd, " +
                    "sharedRemainingWinc=$sharedRemaining, hasActiveApproval=$hasActive, hasSufficient=$hasSufficient")

                _uiState.update {
                    it.copy(
                        arweaveFundingState = ArweaveFundingState.Loaded(
                            estimatedCostWinc = price.winc,
                            estimatedCostUsd = costUsd,
                            walletWinc = balance?.winc ?: "0",
                            sharedRemainingWinc = sharedRemaining.toString(),
                            hasSufficientSharedCredits = hasSufficient,
                            hasActiveApproval = hasActive
                        )
                    )
                }

                Log.d(TAG, "checkArweaveFunding: state updated to Loaded, current storageType=${_uiState.value.storageType}")
            } catch (e: Exception) {
                Log.e(TAG, "Arweave funding check failed", e)
                _uiState.update {
                    it.copy(arweaveFundingState = ArweaveFundingState.Error(e.message ?: "Unknown error"))
                }
            }
        }
    }

    // === Validation ===

    fun isStorageValid(): Boolean {
        val state = _uiState.value
        if (state.storageType != "arweave") return true
        val funding = state.arweaveFundingState
        if (funding !is ArweaveFundingState.Loaded) return false
        return funding.hasSufficientSharedCredits && funding.hasActiveApproval
    }

    fun isMintWindowValid(): Boolean {
        val state = _uiState.value
        if (!state.mintWindowEnabled) return true
        if (state.mintWindowDurationHours == null) return false
        if (state.mintWindowStartMode == "scheduled" && state.mintWindowStartTime == null) return false
        return true
    }

    fun isValid(): Boolean {
        val state = _uiState.value
        // Must have at least one uploaded media item
        if (state.mediaItems.isEmpty()) return false
        if (!state.mediaItems.all { it.url != null }) return false
        // All attachments must be fully uploaded
        if (state.attachments.any { it.url == null }) return false

        // Collectible validation
        if (state.postType == "collectible") {
            if (state.nftName.isBlank()) return false
        }

        // Edition validation
        if (state.postType == "edition") {
            val price = state.priceDisplay.toDoubleOrNull() ?: return false
            val minPrice = if (state.currency == "SOL") MIN_PRICE_SOL else MIN_PRICE_USDC
            if (price < minPrice) return false
            if (state.nftName.isBlank()) return false

            // Timed edition: if enabled, must have a duration set
            if (state.mintWindowEnabled) {
                if (state.mintWindowDurationHours == null) return false
                if (state.mintWindowStartMode == "scheduled" && state.mintWindowStartTime == null) return false
            }

            // Arweave validation: must have sufficient shared credits + active approval
            if (state.storageType == "arweave") {
                val funding = state.arweaveFundingState
                if (funding !is ArweaveFundingState.Loaded) return false
                if (!funding.hasSufficientSharedCredits || !funding.hasActiveApproval) return false
            }
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
                val mediaAssets = state.mediaItems.map { item ->
                    CreateAssetRequest(
                        url = item.url!!,
                        mediaType = item.mediaType,
                        fileName = item.fileName,
                        mimeType = item.mimeType,
                        fileSize = item.fileSize,
                        sortOrder = item.sortOrder
                    )
                }
                val attachmentAssets = state.attachments.mapIndexed { index, item ->
                    CreateAssetRequest(
                        url = item.url!!,
                        mediaType = item.mediaType,
                        fileName = item.fileName,
                        mimeType = item.mimeType,
                        fileSize = item.fileSize,
                        sortOrder = state.mediaItems.size + index
                    )
                }
                val assets = mediaAssets + attachmentAssets

                val priceBaseUnits = if (state.postType == "edition") {
                    convertPriceToBaseUnits(state.priceDisplay, state.currency)
                } else null

                val maxSupply = if (state.maxSupplyEnabled) {
                    state.maxSupplyDisplay.toIntOrNull()
                } else null

                val royaltyBasisPoints = state.royalties.toDoubleOrNull()?.let {
                    (it * 100).toInt() // 5% = 500 basis points
                }

                // Mint window fields (edition only)
                val mintWindowEnabled = if (state.postType == "edition" && state.mintWindowEnabled) true else null
                val mintWindowStartMode = if (mintWindowEnabled == true) state.mintWindowStartMode else null
                val mintWindowStartTime = if (mintWindowEnabled == true && state.mintWindowStartMode == "scheduled" && state.mintWindowStartTime != null) {
                    MintWindowUtils.epochMsToIso(state.mintWindowStartTime)
                } else null
                val mintWindowDurationHours = if (mintWindowEnabled == true) state.mintWindowDurationHours else null

                // Copyright fields (collectible/edition only)
                val isNftType = state.postType == "collectible" || state.postType == "edition"
                val copyrightLicense = if (isNftType) {
                    resolveCopyrightLicense(state.copyrightLicensePreset, state.copyrightLicenseCustom)
                } else null
                val copyrightHolder = if (isNftType) state.copyrightHolder.ifBlank { null } else null
                val copyrightStatement = if (isNftType) state.copyrightRights.ifBlank { null } else null

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
                    mediaFileSize = firstItem.fileSize.takeIf { it > 0 },
                    storageType = if (state.postType == "edition" && state.storageType == "arweave") "arweave" else null,
                    isDev = if (BuildConfig.DEBUG) true else null,
                    mintWindowEnabled = mintWindowEnabled,
                    mintWindowStartMode = mintWindowStartMode,
                    mintWindowStartTime = mintWindowStartTime,
                    mintWindowDurationHours = mintWindowDurationHours,
                    copyrightLicense = copyrightLicense,
                    copyrightHolder = copyrightHolder,
                    copyrightStatement = copyrightStatement
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

                // Mint window fields (edition only, when not locked)
                // null = don't modify, false = explicitly disable, true = enable
                val mintWindowEnabled = if (isEdition && !state.fieldLocking.areTimeWindowFieldsLocked) state.mintWindowEnabled else null
                val mintWindowStartMode = if (mintWindowEnabled == true) state.mintWindowStartMode else null
                val mintWindowStartTimeIso = if (mintWindowEnabled == true && state.mintWindowStartMode == "scheduled" && state.mintWindowStartTime != null) {
                    MintWindowUtils.epochMsToIso(state.mintWindowStartTime)
                } else null
                val mintWindowDuration = if (mintWindowEnabled == true) state.mintWindowDurationHours else null

                // Copyright fields (only when NFT fields are editable)
                val copyrightLicense = if (isNftType && state.fieldLocking.areNftFieldsEditable) {
                    resolveCopyrightLicense(state.copyrightLicensePreset, state.copyrightLicenseCustom)
                } else null
                val copyrightHolder = if (isNftType && state.fieldLocking.areNftFieldsEditable) state.copyrightHolder.ifBlank { null } else null
                val copyrightStatement = if (isNftType && state.fieldLocking.areNftFieldsEditable) state.copyrightRights.ifBlank { null } else null

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
                    maxSupply = maxSupply,
                    mintWindowEnabled = mintWindowEnabled,
                    mintWindowStartMode = mintWindowStartMode,
                    mintWindowStartTime = mintWindowStartTimeIso,
                    mintWindowDurationHours = mintWindowDuration,
                    copyrightLicense = copyrightLicense,
                    copyrightHolder = copyrightHolder,
                    copyrightStatement = copyrightStatement
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

            // Determine mint window state from post
            val mintWindowStartMs = post.mintWindowStart?.let { MintWindowUtils.parseIsoToEpochMs(it) }
            val mintWindowEndMs = post.mintWindowEnd?.let { MintWindowUtils.parseIsoToEpochMs(it) }
            val hasMintWindow = mintWindowStartMs != null && mintWindowEndMs != null
            val mintWindowDuration = if (mintWindowStartMs != null && mintWindowEndMs != null) {
                (mintWindowEndMs - mintWindowStartMs).toDouble() / 3600_000.0
            } else null

            // Reverse-resolve copyright license to preset/custom
            val resolvedPreset = if (post.copyrightLicense != null && post.copyrightLicense in LICENSE_PRESETS) {
                post.copyrightLicense
            } else if (post.copyrightLicense != null) {
                "CUSTOM"
            } else null
            val resolvedCustom = if (resolvedPreset == "CUSTOM") post.copyrightLicense ?: "" else ""

            state.copy(
                postType = post.type,
                caption = post.caption ?: "",
                nftName = post.nftName ?: "",
                priceDisplay = post.price?.let { formatPriceFromBaseUnits(it, post.currency ?: "SOL") } ?: "",
                currency = post.currency ?: "SOL",
                maxSupplyEnabled = post.maxSupply != null,
                maxSupplyDisplay = post.maxSupply?.toString() ?: "",
                mediaItems = items,
                storageType = post.storageType ?: "centralized",
                mintWindowEnabled = hasMintWindow,
                mintWindowStartMode = if (hasMintWindow) "scheduled" else "now",
                mintWindowStartTime = mintWindowStartMs,
                mintWindowDurationHours = mintWindowDuration,
                copyrightLicensePreset = resolvedPreset,
                copyrightLicenseCustom = resolvedCustom,
                copyrightHolder = post.copyrightHolder ?: "",
                copyrightRights = post.copyrightStatement ?: "",
                creatorDefaultsLoaded = true // Don't load creator defaults in edit mode
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
            },
            isStorageTypeLocked = state.storageType == "arweave", // Can't downgrade from arweave
            areTimeWindowFieldsLocked = editState.areTimeWindowFieldsLocked
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

    private fun resolveCopyrightLicense(preset: String?, custom: String): String? {
        return when {
            preset == null -> null
            preset == "CUSTOM" -> custom.ifBlank { null }
            else -> preset
        }
    }
}
