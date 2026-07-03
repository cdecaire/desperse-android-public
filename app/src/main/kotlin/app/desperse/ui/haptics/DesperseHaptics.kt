package app.desperse.ui.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

enum class HapticEvent {
    CollectSuccess,
    Like,
    DestructiveConfirm,
    PullToRefresh,
    Error,
}

fun interface DesperseHaptics {
    fun perform(event: HapticEvent)
}

/**
 * Maps the events from DESIGN.md's haptics table to platform constants.
 *
 * Uses [android.view.View.performHapticFeedback] directly so we can reach
 * the richer Android constants (CONFIRM/REJECT/GESTURE_END) that aren't
 * exposed by Compose's `LocalHapticFeedback`. Falls back to API 28-29
 * compatible constants on older devices.
 */
@Composable
fun rememberDesperseHaptics(): DesperseHaptics {
    val view = LocalView.current
    return remember(view) {
        DesperseHaptics { event ->
            val constant = when (event) {
                HapticEvent.CollectSuccess ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        HapticFeedbackConstants.CONFIRM
                    } else {
                        HapticFeedbackConstants.CONTEXT_CLICK
                    }
                HapticEvent.Like ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        HapticFeedbackConstants.GESTURE_END
                    } else {
                        HapticFeedbackConstants.VIRTUAL_KEY
                    }
                HapticEvent.DestructiveConfirm ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        HapticFeedbackConstants.REJECT
                    } else {
                        HapticFeedbackConstants.LONG_PRESS
                    }
                HapticEvent.PullToRefresh ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        HapticFeedbackConstants.GESTURE_END
                    } else {
                        HapticFeedbackConstants.CLOCK_TICK
                    }
                HapticEvent.Error ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        HapticFeedbackConstants.REJECT
                    } else {
                        HapticFeedbackConstants.LONG_PRESS
                    }
            }
            view.performHapticFeedback(constant)
        }
    }
}
