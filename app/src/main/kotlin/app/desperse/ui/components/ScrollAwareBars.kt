package app.desperse.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * State for scroll-aware header and bottom bar visibility.
 *
 * Uses a continuous offset approach for smooth, fluid animations.
 * Bars slide in/out based on accumulated scroll delta.
 */
class ScrollAwareBarsState(
    private val topBarHeightPx: Float,
    private val bottomBarHeightPx: Float
) {
    // Current offset as a ratio (0 = fully visible, 1 = fully hidden)
    var offsetRatio by mutableFloatStateOf(0f)
        private set

    // Whether we're currently in "hidden" mode
    val barsVisible: Boolean
        get() = offsetRatio < 0.5f

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y

            // Update offset ratio based on scroll
            // Scrolling down (negative delta) increases offset (hides bars)
            // Scrolling up (positive delta) decreases offset (shows bars)
            val newOffset = (offsetRatio - delta / 500f).coerceIn(0f, 1f)
            offsetRatio = newOffset

            // Don't consume any scroll
            return Offset.Zero
        }
    }

    /**
     * Reset bars to visible state
     */
    fun showBars() {
        offsetRatio = 0f
    }
}

/**
 * Remember a scroll-aware bars state.
 */
@Composable
fun rememberScrollAwareBarsState(
    topBarHeight: Dp = 100.dp,
    bottomBarHeight: Dp = 56.dp
): ScrollAwareBarsState {
    return remember {
        ScrollAwareBarsState(
            topBarHeightPx = topBarHeight.value,
            bottomBarHeightPx = bottomBarHeight.value
        )
    }
}

/**
 * Get animated visibility progress (0 = hidden, 1 = visible)
 * Uses spring animation for fluid motion
 */
@Composable
fun ScrollAwareBarsState.animatedVisibility(): State<Float> {
    return animateFloatAsState(
        targetValue = if (barsVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "barsVisibility"
    )
}
