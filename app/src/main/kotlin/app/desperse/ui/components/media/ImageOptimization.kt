package app.desperse.ui.components.media

import java.net.URLEncoder
import kotlin.math.abs

/**
 * Image optimization utility using Vercel's image optimization API
 * Matches the web app's approach for consistent image delivery
 */
object ImageOptimization {
    private const val BASE_URL = "https://www.desperse.com"

    // Allowed widths for Vercel image optimization (must match next.config.js)
    private val ALLOWED_WIDTHS = listOf(320, 480, 640, 800, 1200, 1600)

    /**
     * Get an optimized image URL using Vercel's image optimization
     *
     * @param originalUrl The original image URL
     * @param targetWidth Desired width in pixels (will be rounded to nearest allowed width)
     * @param quality Image quality 1-100 (default 75)
     * @return Optimized image URL
     */
    fun getOptimizedUrl(
        originalUrl: String,
        targetWidth: Int = 640,
        quality: Int = 75
    ): String {
        // Skip optimization for non-http URLs or already optimized URLs
        if (!originalUrl.startsWith("http") || originalUrl.contains("/_next/image")) {
            return originalUrl
        }

        // Find closest allowed width
        val width = ALLOWED_WIDTHS.minByOrNull { abs(it - targetWidth) } ?: 640

        val encoded = URLEncoder.encode(originalUrl, "UTF-8")
        return "$BASE_URL/_next/image?url=$encoded&w=$width&q=$quality"
    }

    /**
     * Get appropriate width for a given context
     */
    fun getWidthForContext(context: ImageContext): Int = when (context) {
        ImageContext.FEED_THUMBNAIL -> 640    // Feed images (covers most phone widths)
        ImageContext.CAROUSEL -> 800          // Carousel full width
        ImageContext.DETAIL -> 1200           // Post detail view
        ImageContext.AVATAR -> 320            // User avatars
        ImageContext.COVER -> 480             // Cover images (audio/document posts)
        ImageContext.WALLET_NFT_GRID -> 320   // NFT grid thumbnails (small squares)
        ImageContext.WALLET_NFT_LIST -> 320   // NFT list thumbnails
        ImageContext.WALLET_ACTIVITY -> 320   // Activity thumbnails
        ImageContext.TOKEN_ICON -> 320        // Token icons (small)
        ImageContext.PROFILE_HEADER -> 800    // Profile header/banner images
        ImageContext.PROFILE_GRID -> 480      // Profile grid post thumbnails
    }

    /**
     * Get optimized URL for a specific context
     */
    fun getOptimizedUrlForContext(
        originalUrl: String,
        context: ImageContext,
        quality: Int = 75
    ): String {
        return getOptimizedUrl(originalUrl, getWidthForContext(context), quality)
    }
}

/**
 * Image context for determining appropriate optimization settings
 */
enum class ImageContext {
    /** Feed images - standard mobile width */
    FEED_THUMBNAIL,
    /** Carousel images - full width swipeable */
    CAROUSEL,
    /** Post detail view - high quality */
    DETAIL,
    /** User avatars - small, circular */
    AVATAR,
    /** Cover images for audio/document posts */
    COVER,
    /** NFT grid thumbnails in wallet */
    WALLET_NFT_GRID,
    /** NFT list thumbnails in wallet */
    WALLET_NFT_LIST,
    /** Activity thumbnails in wallet */
    WALLET_ACTIVITY,
    /** Token icons in wallet */
    TOKEN_ICON,
    /** Profile header/banner images */
    PROFILE_HEADER,
    /** Profile grid post thumbnails */
    PROFILE_GRID
}
