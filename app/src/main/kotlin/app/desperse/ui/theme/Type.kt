package app.desperse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.desperse.R

/**
 * Figtree font family - bundled locally for instant loading
 */
private val DesperseFont = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold)
)

/**
 * DM Mono font family for monospace text (code, addresses, etc.)
 */
val DesperseMonoFont = FontFamily(
    Font(R.font.dm_mono_regular, FontWeight.Normal),
    Font(R.font.dm_mono_medium, FontWeight.Medium)
)

// Global letter spacing: -0.01em ≈ -0.16sp at 16sp base
private const val LETTER_SPACING = -0.16f

/**
 * Desperse Typography - matches web style guide
 *
 * Key differences from Material defaults:
 * - Body text uses Medium (500) weight, not Normal (400)
 * - Headings use SemiBold (600)
 * - Negative letter spacing (-0.01em)
 * - Sizes tuned for mobile (slightly larger than web desktop)
 */
val DesperseTypography = Typography(
    // Display styles - for large hero text
    displayLarge = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = LETTER_SPACING.sp
    ),
    displayMedium = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 33.sp,
        lineHeight = 40.sp,
        letterSpacing = LETTER_SPACING.sp
    ),
    displaySmall = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = LETTER_SPACING.sp
    ),

    // Headline styles - for section headers
    headlineLarge = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 32.sp,
        letterSpacing = LETTER_SPACING.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 28.sp,
        letterSpacing = LETTER_SPACING.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = LETTER_SPACING.sp
    ),

    // Title styles - for component headers
    titleLarge = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 28.sp,
        letterSpacing = LETTER_SPACING.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = LETTER_SPACING.sp
    ),
    titleSmall = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = LETTER_SPACING.sp
    ),

    // Body styles - for content text
    bodyLarge = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.Medium,  // Note: Medium, not Normal
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = LETTER_SPACING.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.Medium,  // Note: Medium, not Normal
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = LETTER_SPACING.sp
    ),
    bodySmall = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.Medium,  // Note: Medium, not Normal
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = LETTER_SPACING.sp
    ),

    // Label styles - for buttons, inputs, badges
    labelLarge = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = LETTER_SPACING.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = LETTER_SPACING.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp  // Wider tracking for small uppercase text
    )
)
