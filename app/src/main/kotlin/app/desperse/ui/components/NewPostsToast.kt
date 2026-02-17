package app.desperse.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.desperse.data.NewPostCreator
import app.desperse.ui.components.media.ImageOptimization
import app.desperse.ui.components.media.ImageContext
import coil.compose.AsyncImage

/**
 * X.com-style floating toast that appears when user scrolls down
 * and new posts are available. Shows creator avatars and scrolls
 * to top when tapped.
 *
 * @param visible Whether the toast should be visible
 * @param creators List of up to 3 creators who posted new content
 * @param onRefresh Called when the toast is tapped - should scroll to top and refresh
 * @param modifier Additional modifiers
 */
@Composable
fun NewPostsToast(
    visible: Boolean,
    creators: List<NewPostCreator>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && creators.isNotEmpty(),
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
    ) {
        Surface(
            onClick = onRefresh,
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Overlapping avatars (up to 3)
                val displayCreators = creators.take(3)
                Box {
                    displayCreators.forEachIndexed { index, creator ->
                        val optimizedUrl = remember(creator.avatarUrl) {
                            creator.avatarUrl?.let {
                                ImageOptimization.getOptimizedUrlForContext(it, ImageContext.AVATAR)
                            }
                        }
                        AsyncImage(
                            model = optimizedUrl,
                            contentDescription = creator.displayName ?: creator.slug,
                            modifier = Modifier
                                .offset(x = (index * 16).dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .zIndex((displayCreators.size - index).toFloat()),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Add spacing based on number of avatars
                Spacer(Modifier.width((displayCreators.size * 16 + 8).dp))

                Text(
                    text = "Posted",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
