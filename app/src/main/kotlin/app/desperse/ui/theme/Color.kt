package app.desperse.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Desperse color palette - matches web exactly
 * Based on Tailwind CSS color system
 */
object DesperseColors {
    // Zinc (Neutral) palette
    val Zinc50 = Color(0xFFFAFAFA)
    val Zinc100 = Color(0xFFF4F4F5)
    val Zinc200 = Color(0xFFE4E4E7)
    val Zinc300 = Color(0xFFD4D4D8)
    val Zinc400 = Color(0xFFA1A1AA)
    val Zinc500 = Color(0xFF71717A)
    val Zinc600 = Color(0xFF52525B)
    val Zinc700 = Color(0xFF3F3F46)
    val Zinc800 = Color(0xFF27272A)
    val Zinc900 = Color(0xFF18181B)
    val Zinc950 = Color(0xFF09090B)

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

    // Legacy aliases (for compatibility)
    val Highlight = Color(0xFF6366F1)       // Indigo-500 - deprecated, use tones
    val HighlightDark = Color(0xFF4F46E5)   // Indigo-600 - deprecated, use tones
}

/**
 * Tone colors for semantic meaning
 * Light and dark variants as per style guide
 */
object DesperseTones {
    // Light mode tones
    val StandardLight = Color(0xFF00CBA2)      // caribbean-green-500
    val CollectibleLight = Color(0xFF6221FF)   // blue-gem-600
    val EditionLight = Color(0xFF8D04EC)       // purple-heart-700
    val WarningLight = Color(0xFFFF8000)       // flush-orange-600
    val InfoLight = Color(0xFF3792FA)          // azure-radiance-500
    val DestructiveLight = Color(0xFFFF003C)   // torch-red-600

    // Dark mode tones
    val StandardDark = Color(0xFF27E4B8)       // caribbean-green-400
    val CollectibleDark = Color(0xFF7346FF)    // blue-gem-500
    val EditionDark = Color(0xFFB439FF)        // purple-heart-500
    val WarningDark = Color(0xFFFF980A)        // flush-orange-500
    val InfoDark = Color(0xFF5DB3FD)           // azure-radiance-400
    val DestructiveDark = Color(0xFFFF2357)    // torch-red-500

    // Legacy static references (dark mode defaults for compatibility)
    val Standard = StandardDark
    val Collectible = CollectibleDark
    val Edition = EditionDark
    val Warning = WarningDark
    val Info = InfoDark
    val Destructive = DestructiveDark
    val Like = DestructiveDark
    val Success = StandardDark  // Success uses the caribbean green (standard) tone
}

/**
 * Full color palettes for when you need specific shades
 */
object DespersePalette {
    // Torch Red (Destructive)
    val TorchRed50 = Color(0xFFFFF0F4)
    val TorchRed100 = Color(0xFFFFDDE5)
    val TorchRed200 = Color(0xFFFFC0CF)
    val TorchRed300 = Color(0xFFFF94AD)
    val TorchRed400 = Color(0xFFFF577F)
    val TorchRed500 = Color(0xFFFF2357)
    val TorchRed600 = Color(0xFFFF003C)
    val TorchRed700 = Color(0xFFD70033)
    val TorchRed800 = Color(0xFFB1032C)
    val TorchRed900 = Color(0xFF920A2A)
    val TorchRed950 = Color(0xFF500013)

    // Blue Gem (Collectible)
    val BlueGem50 = Color(0xFFF3F1FF)
    val BlueGem100 = Color(0xFFE9E6FF)
    val BlueGem200 = Color(0xFFD5D0FF)
    val BlueGem300 = Color(0xFFB7ABFF)
    val BlueGem400 = Color(0xFF947BFF)
    val BlueGem500 = Color(0xFF7346FF)
    val BlueGem600 = Color(0xFF6221FF)
    val BlueGem700 = Color(0xFF540FF2)
    val BlueGem800 = Color(0xFF450CCB)
    val BlueGem900 = Color(0xFF3A0CA3)
    val BlueGem950 = Color(0xFF220471)

    // Purple Heart (Edition/Accent)
    val PurpleHeart50 = Color(0xFFFBF3FF)
    val PurpleHeart100 = Color(0xFFF4E4FF)
    val PurpleHeart200 = Color(0xFFECCEFF)
    val PurpleHeart300 = Color(0xFFDDA7FF)
    val PurpleHeart400 = Color(0xFFC86FFF)
    val PurpleHeart500 = Color(0xFFB439FF)
    val PurpleHeart600 = Color(0xFFA213FF)
    val PurpleHeart700 = Color(0xFF8D04EC)
    val PurpleHeart800 = Color(0xFF7209B7)
    val PurpleHeart900 = Color(0xFF62099A)
    val PurpleHeart950 = Color(0xFF430074)

    // Caribbean Green (Success)
    val CaribbeanGreen50 = Color(0xFFEAFFF8)
    val CaribbeanGreen100 = Color(0xFFCDFEEB)
    val CaribbeanGreen200 = Color(0xFF9FFBDD)
    val CaribbeanGreen300 = Color(0xFF61F4CD)
    val CaribbeanGreen400 = Color(0xFF27E4B8)
    val CaribbeanGreen500 = Color(0xFF00CBA2)
    val CaribbeanGreen600 = Color(0xFF00A585)
    val CaribbeanGreen700 = Color(0xFF00846D)
    val CaribbeanGreen800 = Color(0xFF006858)
    val CaribbeanGreen900 = Color(0xFF00554A)
    val CaribbeanGreen950 = Color(0xFF00302A)

    // Flush Orange (Warning)
    val FlushOrange50 = Color(0xFFFFFAEC)
    val FlushOrange100 = Color(0xFFFFF4D3)
    val FlushOrange200 = Color(0xFFFFE5A5)
    val FlushOrange300 = Color(0xFFFFD16D)
    val FlushOrange400 = Color(0xFFFFB232)
    val FlushOrange500 = Color(0xFFFF980A)
    val FlushOrange600 = Color(0xFFFF8000)
    val FlushOrange700 = Color(0xFFCC5D02)
    val FlushOrange800 = Color(0xFFA1480B)
    val FlushOrange900 = Color(0xFF823D0C)
    val FlushOrange950 = Color(0xFF461D04)

    // Azure Radiance (Info)
    val AzureRadiance50 = Color(0xFFEFF7FF)
    val AzureRadiance100 = Color(0xFFDAEDFF)
    val AzureRadiance200 = Color(0xFFBEE1FF)
    val AzureRadiance300 = Color(0xFF91CFFF)
    val AzureRadiance400 = Color(0xFF5DB3FD)
    val AzureRadiance500 = Color(0xFF3792FA)
    val AzureRadiance600 = Color(0xFF2E7CF0)
    val AzureRadiance700 = Color(0xFF195DDC)
    val AzureRadiance800 = Color(0xFF1B4BB2)
    val AzureRadiance900 = Color(0xFF1C438C)
    val AzureRadiance950 = Color(0xFF162A55)
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
