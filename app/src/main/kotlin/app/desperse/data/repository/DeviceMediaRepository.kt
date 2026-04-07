package app.desperse.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A single media item from the device gallery (image or video).
 */
data class DeviceMediaItem(
    val id: Long,
    val uri: Uri,
    val mimeType: String,
    val dateAdded: Long,
    val duration: Long = 0L,
    val bucketId: String? = null,
    val bucketDisplayName: String? = null,
    val isVideo: Boolean = false
)

/**
 * An audio file from the device.
 */
data class DeviceAudioItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val duration: Long = 0L,
    val fileSize: Long = 0L,
    val dateAdded: Long = 0L,
    val relativePath: String = "" // e.g. "Music/", "Download/", "Recordings/"
)

/**
 * An album/folder grouping from the device gallery.
 */
data class MediaAlbum(
    val id: String,
    val displayName: String,
    val count: Int,
    val thumbnailUri: Uri? = null
)

/**
 * Repository wrapping MediaStore queries for device images and videos.
 * Provides paginated media queries and album listing.
 */
@Singleton
class DeviceMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    companion object {
        const val ALBUM_RECENTS = "recents"
        const val ALBUM_ALL_PHOTOS = "all_photos"
        const val ALBUM_ALL_VIDEOS = "all_videos"

        private const val DEFAULT_PAGE_SIZE = 80

        private val IMAGE_PROJECTION = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        private val VIDEO_PROJECTION = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        private val AUDIO_PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.RELATIVE_PATH
        )
    }

    /**
     * Query paginated media items from the device.
     *
     * @param albumId Album filter: null or ALBUM_RECENTS for all, ALBUM_ALL_PHOTOS for images only,
     *                ALBUM_ALL_VIDEOS for videos only, or a bucket ID for a specific album.
     * @param offset Number of items to skip.
     * @param limit Max items to return.
     */
    fun queryMedia(
        albumId: String? = null,
        offset: Int = 0,
        limit: Int = DEFAULT_PAGE_SIZE
    ): List<DeviceMediaItem> {
        return when (albumId) {
            null, ALBUM_RECENTS -> {
                // All media (images + videos) merged by date
                val images = queryImages(bucketId = null, offset = 0, limit = offset + limit)
                val videos = queryVideos(bucketId = null, offset = 0, limit = offset + limit)
                (images + videos)
                    .sortedByDescending { it.dateAdded }
                    .drop(offset)
                    .take(limit)
            }
            ALBUM_ALL_PHOTOS -> queryImages(bucketId = null, offset = offset, limit = limit)
            ALBUM_ALL_VIDEOS -> queryVideos(bucketId = null, offset = offset, limit = limit)
            else -> {
                // Specific album bucket — query both and merge
                val images = queryImages(bucketId = albumId, offset = 0, limit = offset + limit)
                val videos = queryVideos(bucketId = albumId, offset = 0, limit = offset + limit)
                (images + videos)
                    .sortedByDescending { it.dateAdded }
                    .drop(offset)
                    .take(limit)
            }
        }
    }

    /**
     * Query all albums from the device with item counts.
     * Returns synthetic albums (Recents, All Photos, All Videos) followed by device folders.
     */
    fun queryAlbums(): List<MediaAlbum> {
        val buckets = mutableMapOf<String, MutableList<DeviceMediaItem>>()
        var totalImages = 0
        var totalVideos = 0

        // Query all images for bucket info
        queryAllBuckets(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION, isVideo = false).forEach { item ->
            totalImages++
            val bid = item.bucketId ?: return@forEach
            buckets.getOrPut(bid) { mutableListOf() }.add(item)
        }

        // Query all videos for bucket info
        queryAllBuckets(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VIDEO_PROJECTION, isVideo = true).forEach { item ->
            totalVideos++
            val bid = item.bucketId ?: return@forEach
            buckets.getOrPut(bid) { mutableListOf() }.add(item)
        }

        val totalAll = totalImages + totalVideos

        val result = mutableListOf<MediaAlbum>()

        // Synthetic albums
        if (totalAll > 0) {
            result.add(MediaAlbum(ALBUM_RECENTS, "Recents", totalAll))
        }
        if (totalImages > 0) {
            result.add(MediaAlbum(ALBUM_ALL_PHOTOS, "All Photos", totalImages))
        }
        if (totalVideos > 0) {
            result.add(MediaAlbum(ALBUM_ALL_VIDEOS, "All Videos", totalVideos))
        }

        // Device albums sorted by count descending
        buckets.entries
            .sortedByDescending { it.value.size }
            .forEach { (bucketId, items) ->
                val name = items.firstOrNull()?.bucketDisplayName ?: "Unknown"
                val thumbnail = items.maxByOrNull { it.dateAdded }?.uri
                result.add(MediaAlbum(bucketId, name, items.size, thumbnail))
            }

        return result
    }

    private fun queryImages(
        bucketId: String?,
        offset: Int,
        limit: Int
    ): List<DeviceMediaItem> {
        return queryMediaStore(
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection = IMAGE_PROJECTION,
            bucketId = bucketId,
            offset = offset,
            limit = limit,
            isVideo = false
        )
    }

    private fun queryVideos(
        bucketId: String?,
        offset: Int,
        limit: Int
    ): List<DeviceMediaItem> {
        return queryMediaStore(
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection = VIDEO_PROJECTION,
            bucketId = bucketId,
            offset = offset,
            limit = limit,
            isVideo = true
        )
    }

    private fun queryMediaStore(
        uri: Uri,
        projection: Array<String>,
        bucketId: String?,
        offset: Int,
        limit: Int,
        isVideo: Boolean
    ): List<DeviceMediaItem> {
        val items = mutableListOf<DeviceMediaItem>()

        val selection = if (bucketId != null) "${MediaStore.MediaColumns.BUCKET_ID} = ?" else null
        val selectionArgs = if (bucketId != null) arrayOf(bucketId) else null

        val queryArgs = Bundle().apply {
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.MediaColumns.DATE_ADDED))
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            if (selection != null) {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            }
        }

        contentResolver.query(uri, projection, queryArgs, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val bucketIdCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val durationCol = if (isVideo) cursor.getColumnIndex(MediaStore.Video.Media.DURATION) else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(uri, id)
                val mimeType = cursor.getString(mimeCol) ?: continue
                val dateAdded = cursor.getLong(dateCol)
                val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                val bid = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) else null
                val bName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) else null

                items.add(
                    DeviceMediaItem(
                        id = id,
                        uri = contentUri,
                        mimeType = mimeType,
                        dateAdded = dateAdded,
                        duration = duration,
                        bucketId = bid,
                        bucketDisplayName = bName,
                        isVideo = isVideo
                    )
                )
            }
        }

        return items
    }

    /**
     * Query all items for bucket/album listing (no pagination).
     * Only fetches ID, date, and bucket columns for efficiency.
     */
    private fun queryAllBuckets(
        uri: Uri,
        projection: Array<String>,
        isVideo: Boolean
    ): List<DeviceMediaItem> {
        val items = mutableListOf<DeviceMediaItem>()
        val minProjection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )

        contentResolver.query(
            uri,
            minProjection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val bucketIdCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(uri, id)
                items.add(
                    DeviceMediaItem(
                        id = id,
                        uri = contentUri,
                        mimeType = "",
                        dateAdded = cursor.getLong(dateCol),
                        bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) else null,
                        bucketDisplayName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) else null,
                        isVideo = isVideo
                    )
                )
            }
        }

        return items
    }

    /**
     * Query audio files from the device, sorted by date added descending.
     */
    fun queryAudioFiles(offset: Int = 0, limit: Int = 100): List<DeviceAudioItem> {
        val items = mutableListOf<DeviceAudioItem>()

        val queryArgs = Bundle().apply {
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Audio.Media.DATE_ADDED))
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
        }

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            AUDIO_PROJECTION,
            queryArgs,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val pathCol = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val mimeType = cursor.getString(mimeCol) ?: continue

                items.add(
                    DeviceAudioItem(
                        id = id,
                        uri = contentUri,
                        displayName = cursor.getString(nameCol) ?: "Unknown",
                        mimeType = mimeType,
                        duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L,
                        fileSize = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L,
                        dateAdded = cursor.getLong(dateCol),
                        relativePath = if (pathCol >= 0) cursor.getString(pathCol) ?: "" else ""
                    )
                )
            }
        }

        return items
    }
}
