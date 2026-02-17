package app.desperse.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Desperse Light Color Scheme
 */
private val LightColorScheme = lightColorScheme(
    // Primary - Used for emphasis, buttons
    primary = DesperseColors.Zinc950,                    // Dark buttons
    onPrimary = DesperseColors.Zinc50,                   // Light text on dark buttons
    primaryContainer = DesperseColors.Zinc950,
    onPrimaryContainer = DesperseColors.Zinc50,

    // Secondary - Muted elements
    secondary = DesperseColors.Zinc100,                  // Light muted background
    onSecondary = DesperseColors.Zinc950,                // Dark text
    secondaryContainer = DesperseColors.Zinc100,
    onSecondaryContainer = DesperseColors.Zinc950,

    // Tertiary - Accent
    tertiary = DesperseColors.Zinc100,
    onTertiary = DesperseColors.Zinc950,

    // Background & Surface
    background = Color.White,                            // White background
    onBackground = DesperseColors.Zinc950,               // Dark text
    surface = Color.White,                               // White cards
    onSurface = DesperseColors.Zinc950,
    surfaceVariant = DesperseColors.Zinc100,             // Muted (zinc-100)
    onSurfaceVariant = DesperseColors.Zinc600,           // Muted foreground (zinc-600)

    // Outline/Border
    outline = DesperseColors.Zinc200,                    // Border (zinc-200)
    outlineVariant = DesperseColors.Zinc400,             // Ring (zinc-400)

    // Error states
    error = DesperseTones.DestructiveLight,              // Torch red light
    onError = Color.White,

    // Surface containers for elevation tinting
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = DesperseColors.Zinc100,
    surfaceContainerHighest = DesperseColors.Zinc100,
)

/**
 * Desperse Dark Color Scheme
 */
private val DarkColorScheme = darkColorScheme(
    // Primary - Used for emphasis, buttons
    primary = DesperseColors.Zinc50,                     // Light text on dark bg
    onPrimary = DesperseColors.Zinc950,                  // Dark text on light buttons
    primaryContainer = DesperseColors.Zinc50,
    onPrimaryContainer = DesperseColors.Zinc950,

    // Secondary - Muted elements
    secondary = DesperseColors.Zinc800,                  // Muted background
    onSecondary = DesperseColors.Zinc50,                 // Light text
    secondaryContainer = DesperseColors.Zinc800,
    onSecondaryContainer = DesperseColors.Zinc50,

    // Tertiary - Accent
    tertiary = DesperseColors.Zinc800,
    onTertiary = DesperseColors.Zinc50,

    // Background & Surface
    background = DesperseColors.Zinc950,                 // Zinc-950
    onBackground = DesperseColors.Zinc50,
    surface = DesperseColors.Zinc900,                    // Zinc-900 (cards)
    onSurface = DesperseColors.Zinc50,
    surfaceVariant = DesperseColors.Zinc800,             // Zinc-800 (muted)
    onSurfaceVariant = DesperseColors.Zinc400,           // Muted foreground

    // Outline/Border
    outline = DesperseColors.Zinc700,                    // Border (zinc-700)
    outlineVariant = DesperseColors.Zinc500,             // Ring (zinc-500)

    // Error states
    error = DesperseTones.DestructiveDark,               // Torch red dark
    onError = DesperseColors.Zinc50,

    // Surface containers for elevation tinting
    surfaceContainerLowest = DesperseColors.Zinc950,
    surfaceContainerLow = DesperseColors.Zinc900,
    surfaceContainer = DesperseColors.Zinc900,
    surfaceContainerHigh = DesperseColors.Zinc800,
    surfaceContainerHighest = DesperseColors.Zinc800,
)

/**
 * Desperse Shapes - matching style guide border radius
 */
val DesperseShapes = Shapes(
    extraSmall = RoundedCornerShape(DesperseRadius.xs),    // 4dp
    small = RoundedCornerShape(DesperseRadius.sm),         // 8dp
    medium = RoundedCornerShape(DesperseRadius.md),        // 12dp
    large = RoundedCornerShape(DesperseRadius.lg),         // 16dp
    extraLarge = RoundedCornerShape(DesperseRadius.xl),    // 20dp
)

/**
 * Desperse Theme
 *
 * Supports both light and dark modes. Pass darkTheme parameter to control,
 * or let it follow system setting.
 */
@Composable
fun DesperseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DesperseTypography,
        shapes = DesperseShapes,
        content = content
    )
}
