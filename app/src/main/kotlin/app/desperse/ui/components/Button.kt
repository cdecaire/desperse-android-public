package app.desperse.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.desperse.ui.theme.DesperseColors
import app.desperse.ui.theme.DesperseSizes
import app.desperse.ui.theme.DesperseTones

/**
 * Button variants matching style guide
 */
enum class ButtonVariant {
    Default,      // Primary bg, onPrimary text (zinc-50 bg, zinc-950 text)
    Destructive,  // Error bg
    Outline,      // Transparent with border
    Secondary,    // Secondary bg (zinc-800)
    Ghost,        // Transparent, text only
    Link          // Transparent, underlined text
}

/**
 * Button sizes
 */
enum class ButtonSize {
    Default,   // 40dp height
    Cta,       // 44dp height
    Icon,      // 40x40dp square
    IconLarge  // 64x64dp square
}

/**
 * Desperse Button Component
 *
 * Primary button component with multiple variants and sizes.
 * All buttons use pill shape (CircleShape).
 *
 * @param onClick Click handler
 * @param modifier Additional modifiers
 * @param variant Button style variant
 * @param size Button size
 * @param enabled Whether the button is enabled
 * @param isLoading Show loading spinner instead of content
 * @param content Button content (text, icon, etc.)
 */
@Composable
fun DesperseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Default,
    size: ButtonSize = ButtonSize.Default,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val height = when (size) {
        ButtonSize.Default -> DesperseSizes.buttonDefault
        ButtonSize.Cta -> DesperseSizes.buttonCta
        ButtonSize.Icon -> DesperseSizes.buttonIcon
        ButtonSize.IconLarge -> DesperseSizes.buttonIconLg
    }

    val contentPadding = when (size) {
        ButtonSize.Icon, ButtonSize.IconLarge -> PaddingValues(0.dp)
        else -> PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    }

    val buttonModifier = modifier
        .height(height)
        .then(
            if (size == ButtonSize.Icon || size == ButtonSize.IconLarge) {
                Modifier.width(height)
            } else {
                Modifier
            }
        )

    when (variant) {
        ButtonVariant.Default -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = CircleShape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DesperseColors.Zinc50,
                    contentColor = DesperseColors.Zinc950,
                    disabledContainerColor = DesperseColors.Zinc50.copy(alpha = 0.5f),
                    disabledContentColor = DesperseColors.Zinc950.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = DesperseColors.Zinc950
                    )
                } else {
                    content()
                }
            }
        }

        ButtonVariant.Destructive -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = CircleShape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DesperseTones.Destructive,
                    contentColor = Color.White,
                    disabledContainerColor = DesperseTones.Destructive.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    content()
                }
            }
        }

        ButtonVariant.Outline -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = CircleShape,
                contentPadding = contentPadding,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    content()
                }
            }
        }

        ButtonVariant.Secondary -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = CircleShape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DesperseColors.Zinc800,
                    contentColor = DesperseColors.Zinc50,
                    disabledContainerColor = DesperseColors.Zinc800.copy(alpha = 0.5f),
                    disabledContentColor = DesperseColors.Zinc50.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = DesperseColors.Zinc50
                    )
                } else {
                    content()
                }
            }
        }

        ButtonVariant.Ghost -> {
            TextButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = CircleShape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    content()
                }
            }
        }

        ButtonVariant.Link -> {
            TextButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = CircleShape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    content()
                }
            }
        }
    }
}

/**
 * Text button shorthand
 */
@Composable
fun DesperseTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Default,
    size: ButtonSize = ButtonSize.Default,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    DesperseButton(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        isLoading = isLoading
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Icon button component (Material Icons)
 */
@Composable
fun DesperseIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    variant: ButtonVariant = ButtonVariant.Ghost,
    size: ButtonSize = ButtonSize.Icon,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val buttonSize = when (size) {
        ButtonSize.IconLarge -> DesperseSizes.buttonIconLg
        else -> DesperseSizes.buttonIcon
    }

    val iconSize = when (size) {
        ButtonSize.IconLarge -> DesperseSizes.iconXl
        else -> DesperseSizes.iconMd
    }

    when (variant) {
        ButtonVariant.Ghost -> {
            IconButton(
                onClick = onClick,
                modifier = modifier.size(buttonSize),
                enabled = enabled,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = tint,
                    disabledContentColor = tint.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
        else -> {
            DesperseButton(
                onClick = onClick,
                modifier = modifier,
                variant = variant,
                size = size,
                enabled = enabled
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

/**
 * Icon button component (FontAwesome Icons)
 */
@Composable
fun DesperseFaIconButton(
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    variant: ButtonVariant = ButtonVariant.Ghost,
    size: ButtonSize = ButtonSize.Icon,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    style: FaIconStyle = FaIconStyle.Solid
) {
    val buttonSize = when (size) {
        ButtonSize.IconLarge -> DesperseSizes.buttonIconLg
        else -> DesperseSizes.buttonIcon
    }

    val iconSize = when (size) {
        ButtonSize.IconLarge -> DesperseSizes.iconXl
        else -> DesperseSizes.iconMd
    }

    when (variant) {
        ButtonVariant.Ghost -> {
            IconButton(
                onClick = onClick,
                modifier = modifier.size(buttonSize),
                enabled = enabled,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = tint,
                    disabledContentColor = tint.copy(alpha = 0.5f)
                )
            ) {
                FaIcon(
                    icon = icon,
                    size = iconSize,
                    tint = tint,
                    style = style,
                    contentDescription = contentDescription
                )
            }
        }
        else -> {
            DesperseButton(
                onClick = onClick,
                modifier = modifier,
                variant = variant,
                size = size,
                enabled = enabled
            ) {
                FaIcon(
                    icon = icon,
                    size = iconSize,
                    tint = tint,
                    style = style,
                    contentDescription = contentDescription
                )
            }
        }
    }
}
