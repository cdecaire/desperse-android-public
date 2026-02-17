package app.desperse.data.upload

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import app.desperse.data.model.MediaConstants
import app.desperse.data.repository.PostRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class UploadState {
    data object Idle : UploadState()
    data class Uploading(val progress: Float) : UploadState()
    data class Success(val url: String, val mimeType: String, val fileSize: Long) : UploadState()
    data class Failed(val error: String) : UploadState()
}

data class UploadedFile(
    val url: String,
    val mimeType: String,
    val fileSize: Long,
    val fileName: String,
    val mediaType: String // "image", "video", "audio", "document", "3d"
)

@Singleton
class MediaUploadService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val postRepository: PostRepository
) {
    companion object {
        private const val TAG = "MediaUploadService"
    }

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    fun resetState() {
        _uploadState.value = UploadState.Idle
    }

    /**
     * Upload a file via the server API (same pattern as avatar/header uploads).
     * 1. Read file and encode as base64
     * 2. Send to server via Retrofit
     * 3. Server uploads to Vercel Blob and returns URL
     */
    suspend fun uploadFile(uri: Uri, fileName: String? = null): Result<UploadedFile> =
        withContext(Dispatchers.IO) {
            try {
                _uploadState.value = UploadState.Uploading(0f)

                // Resolve file info
                val resolvedName = fileName ?: getFileName(uri) ?: "upload"
                val mimeType = context.contentResolver.getType(uri) ?: guessMimeType(resolvedName)
                val fileSize = getFileSize(uri)

                // Validate MIME type
                if (!MediaConstants.isSupported(mimeType)) {
                    val error = "Unsupported file type: $mimeType"
                    _uploadState.value = UploadState.Failed(error)
                    return@withContext Result.failure(Exception(error))
                }

                // Validate file size
                if (fileSize > MediaConstants.MAX_FILE_SIZE_BYTES) {
                    val error = "File too large. Maximum size is ${MediaConstants.MAX_FILE_SIZE_MB}MB"
                    _uploadState.value = UploadState.Failed(error)
                    return@withContext Result.failure(Exception(error))
                }

                _uploadState.value = UploadState.Uploading(0.1f)

                // Read file and encode as base64
                val base64Data = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                } ?: run {
                    val error = "Cannot read file"
                    _uploadState.value = UploadState.Failed(error)
                    return@withContext Result.failure(Exception(error))
                }

                _uploadState.value = UploadState.Uploading(0.4f)

                // Upload via server API (server handles Vercel Blob upload)
                val result = postRepository.uploadMedia(
                    base64Data = base64Data,
                    fileName = resolvedName,
                    mimeType = mimeType,
                    fileSize = fileSize
                )

                _uploadState.value = UploadState.Uploading(0.95f)

                if (result.isFailure) {
                    val error = result.exceptionOrNull()?.message ?: "Upload failed"
                    _uploadState.value = UploadState.Failed(error)
                    return@withContext Result.failure(Exception(error))
                }

                val uploadResult = result.getOrThrow()
                val uploadedFile = UploadedFile(
                    url = uploadResult.url,
                    mimeType = mimeType,
                    fileSize = fileSize,
                    fileName = resolvedName,
                    mediaType = uploadResult.mediaType
                )

                _uploadState.value = UploadState.Success(uploadResult.url, mimeType, fileSize)
                Log.d(TAG, "Upload success: ${uploadResult.url}")
                Result.success(uploadedFile)
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed", e)
                val error = e.message ?: "Upload failed"
                _uploadState.value = UploadState.Failed(error)
                Result.failure(e)
            }
        }

    private fun getFileName(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                cursor.getString(nameIndex)
            } else null
        }
    }

    private fun getFileSize(uri: Uri): Long {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex >= 0) {
                cursor.getLong(sizeIndex)
            } else 0L
        } ?: 0L
    }

    private fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "epub" -> "application/epub+zip"
            "glb" -> "model/gltf-binary"
            "gltf" -> "model/gltf+json"
            else -> "application/octet-stream"
        }
    }
}
