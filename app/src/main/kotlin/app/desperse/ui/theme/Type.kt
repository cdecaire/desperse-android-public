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

/**
 * Mono tier — for on-chain data, IDs, code. Always tabular figures.
 * Not part of Material's Typography (no mono slot), so exposed as top-level vals.
 */
val DesperseMonoMd = TextStyle(
    fontFamily = DesperseMonoFont,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = "tnum"
)

val DesperseMonoSm = TextStyle(
    fontFamily = DesperseMonoFont,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.22.sp,
    fontFeatureSettings = "tnum"
)

/**
 * Desperse Typography - matches DESIGN.md typography spec.
 *
 * Six tiers: Display, Heading, Title, Body, Label, Mono.
 * Body is 400 (Regular); Heading/Title/Label are 600 (Semibold).
 * Per-tier letter spacing (negative for larger sizes, positive eyebrow on label-xs).
 */
val DesperseTypography = Typography(
    // Display tier — marketing only
    displayLarge = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.4).sp
    ),
    displayMedium = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 33.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.99).sp
    ),
    displaySmall = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.7).sp
    ),

    // Heading tier — semantic h1–h4
    headlineLarge = TextStyle( // heading-1
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.575).sp
    ),
    headlineMedium = TextStyle( // heading-2
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.38).sp
    ),
    headlineSmall = TextStyle( // heading-3
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.24).sp
    ),

    // Title tier — repeating UI primitives
    titleLarge = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.19).sp
    ),
    titleMedium = TextStyle( // title-lg
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.16).sp
    ),
    titleSmall = TextStyle( // title-sm
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.07).sp
    ),

    // Body tier — reading text
    bodyLarge = TextStyle( // body-lg
        fontFamily = DesperseFont,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.08).sp
    ),
    bodyMedium = TextStyle( // body-md
        fontFamily = DesperseFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.07).sp
    ),
    bodySmall = TextStyle( // body-sm
        fontFamily = DesperseFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),

    // Label tier — functional UI text
    labelLarge = TextStyle( // label-lg
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.07).sp
    ),
    labelMedium = TextStyle( // label-md
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    /**
     * label-xs — eyebrow style. Spec requires textTransform: uppercase, but
     * Compose TextStyle has no equivalent. Callers MUST apply `.uppercase()`
     * to the text content when using this style.
     */
    labelSmall = TextStyle(
        fontFamily = DesperseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp
    )
)
