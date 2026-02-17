package app.desperse.ui.components.media

/**
 * Media type categories for content display
 */
enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    MODEL_3D
}

/**
 * Detect media type from URL extension
 */
fun detectMediaType(url: String?): MediaType {
    if (url == null) return MediaType.IMAGE
    val extension = url.substringAfterLast('.').lowercase().substringBefore('?')
    return when (extension) {
        "jpg", "jpeg", "png", "webp", "gif", "svg" -> MediaType.IMAGE
        "mp4", "webm", "mov" -> MediaType.VIDEO
        "mp3", "wav", "ogg", "aac" -> MediaType.AUDIO
        "pdf", "zip", "epub" -> MediaType.DOCUMENT
        "glb", "gltf" -> MediaType.MODEL_3D
        else -> MediaType.IMAGE // Default to image for unknown types
    }
}

/**
 * Detect media type from MIME type
 */
fun detectMediaTypeFromMime(mimeType: String): MediaType {
    return when {
        mimeType.startsWith("image/") -> MediaType.IMAGE
        mimeType.startsWith("video/") -> MediaType.VIDEO
        mimeType.startsWith("audio/") -> MediaType.AUDIO
        mimeType == "application/pdf" || mimeType == "application/zip" || mimeType == "application/epub+zip" -> MediaType.DOCUMENT
        mimeType.startsWith("model/") || mimeType == "application/octet-stream" -> MediaType.MODEL_3D
        else -> MediaType.IMAGE
    }
}

/**
 * Check if media type supports blurred background treatment
 * (for portrait/tall content)
 */
fun MediaType.supportsBlurredBackground(): Boolean = when (this) {
    MediaType.IMAGE, MediaType.VIDEO -> true
    else -> false
}

/**
 * Check if media type can be played inline
 */
fun MediaType.isPlayable(): Boolean = when (this) {
    MediaType.VIDEO, MediaType.AUDIO -> true
    else -> false
}

/**
 * Get human-readable file type label from URL extension
 */
fun getFileTypeLabel(url: String?): String {
    if (url == null) return "File"
    val extension = url.substringAfterLast('.').lowercase().substringBefore('?')
    return when (extension) {
        "pdf" -> "PDF"
        "zip" -> "ZIP"
        "epub" -> "EPUB"
        else -> "File"
    }
}

/**
 * Get human-readable file type label from MIME type, falling back to URL extension.
 */
fun getFileTypeLabelFromMime(mimeType: String?, url: String?): String {
    if (mimeType != null) {
        return when {
            mimeType == "application/pdf" -> "PDF"
            mimeType == "application/zip" -> "ZIP"
            mimeType == "application/epub+zip" -> "EPUB"
            mimeType.startsWith("audio/") -> "Audio"
            mimeType.contains("gltf") || mimeType == "model/gltf-binary" -> "3D Model"
            else -> getFileTypeLabel(url)
        }
    }
    return getFileTypeLabel(url)
}

/**
 * Get file extension icon for documents
 */
fun getDocumentIcon(url: String): String {
    val extension = url.substringAfterLast('.').lowercase().substringBefore('?')
    return when (extension) {
        "pdf" -> "file-pdf"
        "zip" -> "file-zipper"
        "epub" -> "book-open"
        else -> "file"
    }
}
