package app.desperse.data.model

/**
 * Media upload constants matching server-side validation.
 */
object MediaConstants {
    const val MAX_FILE_SIZE_BYTES = 25L * 1024 * 1024 // 25MB
    const val MAX_FILE_SIZE_MB = 25

    val SUPPORTED_IMAGE_TYPES = setOf(
        "image/jpeg", "image/png", "image/webp", "image/gif", "image/svg+xml"
    )

    val SUPPORTED_VIDEO_TYPES = setOf("video/mp4", "video/webm")

    val SUPPORTED_AUDIO_TYPES = setOf("audio/mpeg", "audio/wav", "audio/ogg", "audio/mp3")

    val SUPPORTED_DOCUMENT_TYPES = setOf("application/pdf", "application/zip", "application/epub+zip")

    val SUPPORTED_3D_TYPES = setOf(
        "model/gltf-binary", "model/gltf+json", "application/octet-stream"
    )

    val ALL_SUPPORTED_TYPES = SUPPORTED_IMAGE_TYPES +
        SUPPORTED_VIDEO_TYPES +
        SUPPORTED_AUDIO_TYPES +
        SUPPORTED_DOCUMENT_TYPES +
        SUPPORTED_3D_TYPES

    fun isSupported(mimeType: String): Boolean = mimeType in ALL_SUPPORTED_TYPES

    /** Determine media type category from MIME type */
    fun getMediaType(mimeType: String): String {
        return when {
            mimeType in SUPPORTED_IMAGE_TYPES -> "image"
            mimeType in SUPPORTED_VIDEO_TYPES -> "video"
            mimeType in SUPPORTED_AUDIO_TYPES -> "audio"
            mimeType in SUPPORTED_DOCUMENT_TYPES -> "document"
            mimeType in SUPPORTED_3D_TYPES -> "3d"
            mimeType.startsWith("image/") -> "image"
            mimeType.startsWith("video/") -> "video"
            mimeType.startsWith("audio/") -> "audio"
            else -> "image"
        }
    }

    /** Check if media type needs a cover image (audio, document, 3D) */
    fun needsCoverImage(mediaType: String): Boolean {
        return mediaType in setOf("audio", "document", "3d")
    }

    /** MIME type to file extension mapping */
    private val MIME_TO_EXTENSION = mapOf(
        "image/jpeg" to "jpg",
        "image/png" to "png",
        "image/webp" to "webp",
        "image/gif" to "gif",
        "image/svg+xml" to "svg",
        "video/mp4" to "mp4",
        "video/webm" to "webm",
        "audio/mpeg" to "mp3",
        "audio/mp3" to "mp3",
        "audio/wav" to "wav",
        "audio/ogg" to "ogg",
        "application/pdf" to "pdf",
        "application/zip" to "zip",
        "application/epub+zip" to "epub",
        "model/gltf-binary" to "glb",
        "model/gltf+json" to "gltf"
    )

    fun getExtension(mimeType: String): String = MIME_TO_EXTENSION[mimeType] ?: "bin"

    /** Whether a media type can be visually previewed (image or video) */
    fun isPreviewable(mediaType: String): Boolean = mediaType in setOf("image", "video")
}
