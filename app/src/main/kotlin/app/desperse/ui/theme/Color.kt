package app.desperse.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Desperse color palette - matches web exactly
 * Authored in OKLCH; values converted to sRGB at build time.
 */
object DesperseColors {
    // Zinc (Neutral) palette - hue 264°
    val Zinc50 = Color(0xFFF5F6F7)
    val Zinc100 = Color(0xFFEAEBED)
    val Zinc200 = Color(0xFFD6D7DA)
    val Zinc300 = Color(0xFFB9BABD)
    val Zinc400 = Color(0xFF97989B)
    val Zinc500 = Color(0xFF797A7D)
    val Zinc600 = Color(0xFF5C5E60)
    val Zinc700 = Color(0xFF444547)
    val Zinc800 = Color(0xFF2D2E30)
    val Zinc900 = Color(0xFF1C1D1E)
    val Zinc950 = Color(0xFF0A0B0C)

    // Semantic colors (dark mode - Desperse is always dark)
    val Background = Zinc950
    val Surface = Zinc900
    val SurfaceVariant = Zinc800
    val Border = Zinc700
    val BorderSubtle = Zinc700.copy(alpha = 0.5f)
    val TextPrimary = Zinc50
    val TextSecondary = Zinc400
    val TextMuted = Zinc500

    // Ring/focus colors
    val Ring = Zinc500
}

/**
 * Tone colors for semantic meaning
 * Light values come from re-anchored palette steps; dark values are
 * dedicated chroma-boosted overrides for vibrancy on dark canvases.
 */
object DesperseTones {
    // Light mode tones (re-anchored to clear WCAG AA on white)
    val StandardLight = Color(0xFF00694F)      // caribbean-green-600
    val CollectibleLight = Color(0xFF5D1AE8)   // blue-gem-600
    val EditionLight = Color(0xFF7400AB)       // purple-heart-700
    val WarningLight = Color(0xFF8A3A00)       // flush-orange-700
    val InfoLight = Color(0xFF0065E2)          // azure-radiance-600
    val DestructiveLight = Color(0xFFCE0028)   // torch-red-600

    // Dark-tuned tone overrides
    val ToneEditionDark = Color(0xFFCA11F4)     // oklch(62% 0.295 318) — sRGB-clamped at magenta boundary
    val ToneCollectibleDark = Color(0xFF9B87FF) // oklch(72% 0.225 285)
    val ToneStandardDark = Color(0xFF00DF9E)    // oklch(78% 0.205 168)
    val ToneInfoDark = Color(0xFF00BFFF)        // oklch(76% 0.165 235)
    val ToneWarningDark = Color(0xFFFFAB39)     // oklch(82% 0.165 65)
    val HighlightDarkTone = Color(0xFFE348FF)   // oklch(70% 0.285 318) — sRGB-clamped
    val DestructiveDarkTone = Color(0xFFFF494B) // oklch(68% 0.225 25)

    // Dark mode tone references (use the dark-tuned overrides)
    val StandardDark = ToneStandardDark
    val CollectibleDark = ToneCollectibleDark
    val EditionDark = ToneEditionDark
    val WarningDark = ToneWarningDark
    val InfoDark = ToneInfoDark
    val DestructiveDark = DestructiveDarkTone

    // Legacy static references (dark mode defaults for compatibility)
    val Standard = StandardDark
    val Collectible = CollectibleDark
    val Edition = EditionDark
    val Warning = WarningDark
    val Info = InfoDark
    val Destructive = DestructiveDark
    val Like = DestructiveDark
    val Success = StandardDark
}

/**
 * Full color palettes for when you need specific shades
 */
object DespersePalette {
    // Torch Red (Destructive) - hue 22°
    val TorchRed50 = Color(0xFFFFF3F2)
    val TorchRed100 = Color(0xFFFEE4E3)
    val TorchRed200 = Color(0xFFFDC9C6)
    val TorchRed300 = Color(0xFFFD9B98)
    val TorchRed400 = Color(0xFFF75C61)
    val TorchRed500 = Color(0xFFEC1439)
    val TorchRed600 = Color(0xFFCE0028)
    val TorchRed700 = Color(0xFFA5001E)
    val TorchRed800 = Color(0xFF790014)
    val TorchRed900 = Color(0xFF52020D)
    val TorchRed950 = Color(0xFF2B0003)

    // Blue Gem (Collectible) - hue 285°
    val BlueGem50 = Color(0xFFF4F5FF)
    val BlueGem100 = Color(0xFFE8E9FF)
    val BlueGem200 = Color(0xFFD2D2FF)
    val BlueGem300 = Color(0xFFB2AEFF)
    val BlueGem400 = Color(0xFF8F7EFF)
    val BlueGem500 = Color(0xFF764AFF)
    val BlueGem600 = Color(0xFF5D1AE8)
    val BlueGem700 = Color(0xFF480DB8)
    val BlueGem800 = Color(0xFF310184)
    val BlueGem900 = Color(0xFF1D0057)
    val BlueGem950 = Color(0xFF0B002E)

    // Purple Heart (Edition/Accent) - hue 309°
    val PurpleHeart50 = Color(0xFFF8F3FD)
    val PurpleHeart100 = Color(0xFFF1E6FB)
    val PurpleHeart200 = Color(0xFFE4CCF9)
    val PurpleHeart300 = Color(0xFFD4A2FC)
    val PurpleHeart400 = Color(0xFFBF69FA)
    val PurpleHeart500 = Color(0xFFB02EF8)
    val PurpleHeart600 = Color(0xFF9700DC)
    val PurpleHeart700 = Color(0xFF7400AB)
    val PurpleHeart800 = Color(0xFF500078)
    val PurpleHeart900 = Color(0xFF32004E)
    val PurpleHeart950 = Color(0xFF160027)

    // Caribbean Green (Success) - hue 173°
    val CaribbeanGreen50 = Color(0xFFEAFAF5)
    val CaribbeanGreen100 = Color(0xFFD5F3E9)
    val CaribbeanGreen200 = Color(0xFFA8E7D3)
    val CaribbeanGreen300 = Color(0xFF5FD3B3)
    val CaribbeanGreen400 = Color(0xFF00B390)
    val CaribbeanGreen500 = Color(0xFF009373)
    val CaribbeanGreen600 = Color(0xFF00694F)
    val CaribbeanGreen700 = Color(0xFF00523D)
    val CaribbeanGreen800 = Color(0xFF003A2A)
    val CaribbeanGreen900 = Color(0xFF00251A)
    val CaribbeanGreen950 = Color(0xFF001009)

    // Flush Orange (Warning) - hue 52°
    val FlushOrange50 = Color(0xFFFFF3EB)
    val FlushOrange100 = Color(0xFFFFE5D5)
    val FlushOrange200 = Color(0xFFFFCAAA)
    val FlushOrange300 = Color(0xFFFCA169)
    val FlushOrange400 = Color(0xFFF58028)
    val FlushOrange500 = Color(0xFFEB7000)
    val FlushOrange600 = Color(0xFFB14D00)
    val FlushOrange700 = Color(0xFF8A3A00)
    val FlushOrange800 = Color(0xFF662800)
    val FlushOrange900 = Color(0xFF461A00)
    val FlushOrange950 = Color(0xFF230800)

    // Azure Radiance (Info) - hue 250°
    val AzureRadiance50 = Color(0xFFEFF7FF)
    val AzureRadiance100 = Color(0xFFDDEDFF)
    val AzureRadiance200 = Color(0xFFBADBFE)
    val AzureRadiance300 = Color(0xFF80C0FF)
    val AzureRadiance400 = Color(0xFF2A9CFF)
    val AzureRadiance500 = Color(0xFF0080F9)
    val AzureRadiance600 = Color(0xFF0065E2)
    val AzureRadiance700 = Color(0xFF004BB4)
    val AzureRadiance800 = Color(0xFF003483)
    val AzureRadiance900 = Color(0xFF001F55)
    val AzureRadiance950 = Color(0xFF000C2B)
}

/**
 * Theme-aware tone color functions
 * Use these to get the correct tone color based on current theme
 */
@Composable
fun toneStandard(): Color = if (androidx.compose.foundation.isSystemInDarkTheme())
    DesperseTones.StandardDark else DesperseTones.StandardLight

@Composable
fun toneCollectible(): Color = if (androidx.compose.foundation.isSystemInDarkTheme())
    DesperseTones.CollectibleDark else DesperseTones.CollectibleLight

@Composable
fun toneEdition(): Color = if (androidx.compose.foundation.isSystemInDarkTheme())
    DesperseTones.EditionDark else DesperseTones.EditionLight

@Composable
fun toneWarning(): Color = if (androidx.compose.foundation.isSystemInDarkTheme())
    DesperseTones.WarningDark else DesperseTones.WarningLight

@Composable
fun toneInfo(): Color = if (androidx.compose.foundation.isSystemInDarkTheme())
    DesperseTones.InfoDark else DesperseTones.InfoLight

@Composable
fun toneDestructive(): Color = if (androidx.compose.foundation.isSystemInDarkTheme())
    DesperseTones.DestructiveDark else DesperseTones.DestructiveLight

@Composable
fun toneLike(): Color = toneDestructive()

@Composable
fun toneSuccess(): Color = toneStandard()

/**
 * Media overlay color for pills, scrims, etc.
 */
@Composable
fun colorOverlay(): Color = Color.Black.copy(alpha = 0.85f)

/**
 * Subtle divider color - lighter on light mode, darker on dark mode
 */
@Composable
fun colorDivider(): Color = if (androidx.compose.foundation.isSystemInDarkTheme())
    DesperseColors.Zinc800 else DesperseColors.Zinc200

/**
 * Helper functions to get tone colors based on post type
 */
@Composable
fun postTypeTone(postType: String): Color {
    return when (postType) {
        "collectible" -> toneCollectible()
        "edition" -> toneEdition()
        else -> toneStandard()
    }
}
