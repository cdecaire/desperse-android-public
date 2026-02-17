package app.desperse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import app.desperse.ui.components.media.ImageContext
import app.desperse.ui.components.media.ImageOptimization
import app.desperse.ui.theme.DesperseColors
import app.desperse.ui.theme.DesperseSizes
import coil.compose.AsyncImage

/**
 * Avatar sizes matching style guide
 */
enum class AvatarSize(val size: Dp) {
    XSmall(DesperseSizes.avatarXs),      // 24dp
    Small(DesperseSizes.avatarSm),        // 32dp
    Medium(DesperseSizes.avatarMd),       // 40dp (default)
    Large(DesperseSizes.avatarLg),        // 48dp
    XLarge(DesperseSizes.avatarXl),       // 64dp
    Profile(DesperseSizes.avatarProfile)  // 96dp
}

/**
 * Desperse Avatar Component
 *
 * Displays a circular user avatar image with optional border.
 *
 * @param imageUrl URL of the avatar image
 * @param contentDescription Accessibility description
 * @param size Avatar size preset
 * @param showBorder Whether to show a border (used for profile overlap on banner)
 * @param borderColor Color of the border
 * @param borderWidth Width of the border
 * @param modifier Additional modifiers
 */
@Composable
fun DesperseAvatar(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    identityInput: String? = null,
    size: AvatarSize = AvatarSize.Medium,
    showBorder: Boolean = false,
    borderColor: Color = MaterialTheme.colorScheme.surface,
    borderWidth: Dp = DesperseSizes.avatarXs / 6  // ~4dp for profile size
) {
    val baseModifier = modifier
        .size(size.size)
        .clip(CircleShape)
        .background(DesperseColors.SurfaceVariant)

    val finalModifier = if (showBorder) {
        baseModifier.border(borderWidth, borderColor, CircleShape)
    } else {
        baseModifier
    }

    val optimizedUrl = remember(imageUrl) {
        imageUrl?.let { ImageOptimization.getOptimizedUrlForContext(it, ImageContext.AVATAR) }
    }

    Box(
        modifier = finalModifier,
        contentAlignment = Alignment.Center
    ) {
        if (optimizedUrl != null) {
            AsyncImage(
                model = optimizedUrl,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(size.size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else if (identityInput != null) {
            GeometricAvatar(
                input = identityInput,
                size = size.size
            )
        }
    }
}

/**
 * Avatar with online/status indicator
 */
@Composable
fun DesperseAvatarWithBadge(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    identityInput: String? = null,
    size: AvatarSize = AvatarSize.Medium,
    badgeContent: @Composable (() -> Unit)? = null
) {
    Box(modifier = modifier) {
        DesperseAvatar(
            imageUrl = imageUrl,
            contentDescription = contentDescription,
            identityInput = identityInput,
            size = size
        )
        if (badgeContent != null) {
            Box(
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                badgeContent()
            }
        }
    }
}
