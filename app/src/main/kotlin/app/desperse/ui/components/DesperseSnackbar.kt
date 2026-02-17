package app.desperse.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.desperse.ui.theme.DesperseTones
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Toast variant types matching web app
 */
enum class ToastVariant {
    Success,
    Error,
    Info,
    Warning
}

/**
 * Data class for toast messages
 */
data class ToastMessage(
    val message: String,
    val variant: ToastVariant = ToastVariant.Info,
    val duration: SnackbarDuration = SnackbarDuration.Short
)

/**
 * Singleton manager for showing toasts across the app.
 * ViewModels can inject this to show toasts without needing UI context.
 */
@Singleton
class ToastManager @Inject constructor() {
    private val _toasts = MutableSharedFlow<ToastMessage>(extraBufferCapacity = 10)
    val toasts = _toasts.asSharedFlow()

    fun showSuccess(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        _toasts.tryEmit(ToastMessage(message, ToastVariant.Success, duration))
    }

    fun showError(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        _toasts.tryEmit(ToastMessage(message, ToastVariant.Error, duration))
    }

    fun showInfo(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        _toasts.tryEmit(ToastMessage(message, ToastVariant.Info, duration))
    }

    fun showWarning(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        _toasts.tryEmit(ToastMessage(message, ToastVariant.Warning, duration))
    }
}

/**
 * Custom snackbar host that displays styled toasts
 */
@Composable
fun DesperseSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    currentVariant: ToastVariant = ToastVariant.Info
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        DesperseSnackbar(
            snackbarData = data,
            variant = currentVariant
        )
    }
}

/**
 * Styled snackbar matching web app toast design
 */
@Composable
fun DesperseSnackbar(
    snackbarData: SnackbarData,
    variant: ToastVariant = ToastVariant.Info
) {
    val backgroundColor = MaterialTheme.colorScheme.inverseSurface
    val contentColor = MaterialTheme.colorScheme.inverseOnSurface

    val icon = when (variant) {
        ToastVariant.Success -> FaIcons.Check
        ToastVariant.Error -> FaIcons.CircleExclamation
        ToastVariant.Info -> FaIcons.CircleInfo
        ToastVariant.Warning -> FaIcons.TriangleExclamation
    }

    val iconColor = when (variant) {
        ToastVariant.Success -> DesperseTones.Success
        ToastVariant.Error -> DesperseTones.Destructive
        ToastVariant.Info -> DesperseTones.Info
        ToastVariant.Warning -> DesperseTones.Warning
    }

    Snackbar(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        containerColor = backgroundColor,
        contentColor = contentColor,
        dismissAction = null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with colored background circle
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(24.dp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.size(20.dp)
                ) {
                    drawCircle(color = iconColor)
                }
                FaIcon(
                    icon = icon,
                    size = 10.dp,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = snackbarData.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}
