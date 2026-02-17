package app.desperse.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.desperse.R

/**
 * FontAwesome font families
 */
object FontAwesomeFonts {
    val Solid = FontFamily(
        Font(R.font.fa_solid_900, FontWeight.Normal)
    )
    val Regular = FontFamily(
        Font(R.font.fa_regular_400, FontWeight.Normal)
    )
    val Brands = FontFamily(
        Font(R.font.fa_brands_400, FontWeight.Normal)
    )
}

/**
 * FontAwesome icon style
 */
enum class FaIconStyle {
    Solid,
    Regular,
    Brands
}

/**
 * FontAwesome icon definitions
 * Unicode values from: https://fontawesome.com/icons
 */
object FaIcons {
    // Navigation & UI
    const val Home = "\uf015"
    const val Search = "\uf002"
    const val MagnifyingGlass = "\uf002"
    const val Bell = "\uf0f3"
    const val BellSolid = "\uf0f3"
    const val User = "\uf007"
    const val UserSolid = "\uf007"
    const val Gear = "\uf013"
    const val Cog = "\uf013"
    const val Bars = "\uf0c9"
    const val Xmark = "\uf00d"
    const val Close = "\uf00d"
    const val ChevronLeft = "\uf053"
    const val ChevronRight = "\uf054"
    const val ChevronDown = "\uf078"
    const val ChevronUp = "\uf077"
    const val ArrowLeft = "\uf060"
    const val ArrowRight = "\uf061"
    const val ArrowUp = "\uf062"
    const val ArrowDown = "\uf063"
    const val EllipsisVertical = "\uf142"
    const val MoreVert = "\uf142"
    const val Plus = "\uf067"
    const val CirclePlus = "\uf055"
    const val Minus = "\uf068"
    const val Check = "\uf00c"
    const val Times = "\uf00d"

    // Social & Actions
    const val Heart = "\uf004"
    const val HeartSolid = "\uf004"
    const val Comment = "\uf075"
    const val CommentSolid = "\uf075"
    const val Share = "\uf064"
    const val ShareNodes = "\uf1e0"
    const val Bookmark = "\uf02e"
    const val BookmarkSolid = "\uf02e"
    const val Flag = "\uf024"
    const val PaperPlane = "\uf1d8"
    const val Send = "\uf1d8"
    const val Reply = "\uf3e5"
    const val Retweet = "\uf079"

    // Media
    const val Image = "\uf03e"
    const val Images = "\uf302"
    const val Play = "\uf04b"
    const val Pause = "\uf04c"
    const val Video = "\uf03d"
    const val Music = "\uf001"
    const val VolumeUp = "\uf028"
    const val VolumeMute = "\uf6a9"
    const val Expand = "\uf065"
    const val Compress = "\uf066"
    const val Camera = "\uf030"
    const val Upload = "\uf093"
    const val Download = "\uf019"
    const val CirclePlay = "\uf144"    // play circle overlay for video thumbnails
    const val File = "\uf15b"          // generic file
    const val FilePdf = "\uf1c1"       // PDF file
    const val FileZipper = "\uf1c6"    // ZIP file
    const val FileLines = "\uf15c"     // text/document file (for EPUB)

    // NFT & Crypto
    const val Gem = "\uf3a5"
    const val Diamond = "\uf3a5"
    const val ImageStack = "\uf302"  // For editions (same as Images)
    const val LayerGroup = "\uf5fd"  // For editions
    const val HexagonImage = "\uf3a5"  // For 1/1 editions (using Gem as fallback)
    const val Cube = "\uf1b2"
    const val Cubes = "\uf1b3"
    const val Wallet = "\uf555"
    const val BagShopping = "\uf290"
    const val Coins = "\uf51e"
    const val MoneyBill = "\uf0d6"
    const val CreditCard = "\uf09d"
    const val Receipt = "\uf543"
    const val Tag = "\uf02b"
    const val Tags = "\uf02c"
    const val Gift = "\uf06b"
    const val Trophy = "\uf091"
    const val Award = "\uf559"
    const val Certificate = "\uf0a3"
    const val Star = "\uf005"
    const val StarSolid = "\uf005"

    // Status & Feedback
    const val CircleCheck = "\uf058"
    const val CircleXmark = "\uf057"
    const val CircleInfo = "\uf05a"
    const val CircleExclamation = "\uf06a"
    const val TriangleExclamation = "\uf071"
    const val Warning = "\uf071"
    const val Info = "\uf129"
    const val Question = "\uf128"
    const val Lock = "\uf023"
    const val LockOpen = "\uf3c1"
    const val Unlock = "\uf09c"
    const val Eye = "\uf06e"
    const val EyeSlash = "\uf070"
    const val Shield = "\uf132"
    const val ShieldCheck = "\uf2f7"

    // Content & Edit
    const val Pen = "\uf304"
    const val Pencil = "\uf303"
    const val Edit = "\uf044"
    const val Trash = "\uf1f8"
    const val TrashCan = "\uf2ed"
    const val Broom = "\uf51a"
    const val Copy = "\uf0c5"
    const val Clipboard = "\uf328"
    const val Link = "\uf0c1"
    const val LinkSlash = "\uf127"
    const val Hashtag = "\u0023"
    const val At = "\u0040"
    const val LinkSimple = "\ue1cd"
    const val ArrowDownToBracket = "\ue094"
    const val ExternalLink = "\uf08e"
    const val ArrowUpRight = "\uf35e"    // arrow-up-right-from-square variant
    const val ArrowUpRightSimple = "\ue09f"  // simple arrow-up-right

    // Users & Social
    const val Users = "\uf0c0"
    const val UserPlus = "\uf234"
    const val UserMinus = "\uf503"
    const val UserCheck = "\uf4fc"
    const val UserPen = "\uf4ff"
    const val UserGroup = "\uf500"
    const val CircleUser = "\uf2bd"
    const val AddressCard = "\uf2bb"
    const val Envelope = "\uf0e0"
    const val Message = "\uf27a"
    const val MessageCheck = "\uf4a2"
    const val MessagePlus = "\uf4a8"
    const val MessageLines = "\uf4a6"
    const val Comments = "\uf086"
    const val CircleQuestion = "\uf059"
    const val ArrowRightFromBracket = "\uf08b"
    const val SignOut = "\uf08b"

    // Layout
    const val Grid = "\uf00a"      // th - grid view
    const val Grid2 = "\uf009"     // th-large - larger grid
    const val List = "\uf03a"      // list view
    const val ListUl = "\uf0ca"    // list with bullets
    const val Grip = "\uf58d"      // border-all / grip
    const val GripVertical = "\uf58e"  // vertical drag handle

    // Misc
    const val Spinner = "\uf110"
    const val CircleNotch = "\uf1ce"
    const val Fire = "\uf06d"
    const val Bolt = "\uf0e7"
    const val Zap = "\uf0e7"
    const val Sparkles = "\uf890"
    const val Wand = "\uf72b"
    const val Magic = "\uf0d0"
    const val Palette = "\uf53f"
    const val Brush = "\uf55d"
    const val Sliders = "\uf1de"
    const val Filter = "\uf0b0"
    const val Sort = "\uf0dc"
    const val Globe = "\uf0ac"
    const val Language = "\uf1ab"
    const val Calendar = "\uf133"
    const val Clock = "\uf017"
    const val History = "\uf1da"
    const val ClockRotateLeft = "\uf1da"  // Same as History
    const val Repeat = "\uf363"
    const val Sync = "\uf021"
    const val Refresh = "\uf021"
    const val ArrowsRotate = "\uf021"

    // Theme
    const val Sun = "\uf185"
    const val Moon = "\uf186"
    const val SunBright = "\ue28f"
    const val Circle = "\uf111"
    const val CircleHalf = "\uf042"

    // Brands (use FaIconStyle.Brands)
    const val Twitter = "\uf099"
    const val X = "\ue61b"
    const val Discord = "\uf392"
    const val Telegram = "\uf2c6"
    const val Github = "\uf09b"
    const val Google = "\uf1a0"
    const val Apple = "\uf179"
    const val Ethereum = "\uf42e"
}

/**
 * FontAwesome Icon Composable
 *
 * Uses a Box container with centered text to prevent icon clipping.
 * The text style is configured to remove extra font padding and
 * align the glyph properly within the bounding box.
 *
 * @param icon Unicode character from FaIcons
 * @param modifier Modifier for the icon
 * @param size Icon size in dp
 * @param tint Icon color
 * @param style Solid, Regular, or Brands
 * @param contentDescription Accessibility description
 */
@Composable
fun FaIcon(
    icon: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    style: FaIconStyle = FaIconStyle.Solid,
    contentDescription: String? = null
) {
    val fontFamily = when (style) {
        FaIconStyle.Solid -> FontAwesomeFonts.Solid
        FaIconStyle.Regular -> FontAwesomeFonts.Regular
        FaIconStyle.Brands -> FontAwesomeFonts.Brands
    }

    // Convert dp to sp for font size (using density)
    val density = LocalDensity.current
    val fontSizeSp = with(density) { size.toSp() }

    // Container box to prevent clipping and center the icon
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontFamily = fontFamily,
            fontSize = fontSizeSp,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            lineHeight = fontSizeSp,
            style = TextStyle.Default.copy(
                // Remove extra font padding that causes clipping
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false
                ),
                // Trim line height to match the font size exactly
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            )
        )
    }
}

/**
 * Convenience composable for solid icons (most common)
 */
@Composable
fun FaSolidIcon(
    icon: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null
) {
    FaIcon(
        icon = icon,
        modifier = modifier,
        size = size,
        tint = tint,
        style = FaIconStyle.Solid,
        contentDescription = contentDescription
    )
}

/**
 * Convenience composable for regular (outline) icons
 */
@Composable
fun FaRegularIcon(
    icon: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null
) {
    FaIcon(
        icon = icon,
        modifier = modifier,
        size = size,
        tint = tint,
        style = FaIconStyle.Regular,
        contentDescription = contentDescription
    )
}

/**
 * Convenience composable for brand icons
 */
@Composable
fun FaBrandIcon(
    icon: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null
) {
    FaIcon(
        icon = icon,
        modifier = modifier,
        size = size,
        tint = tint,
        style = FaIconStyle.Brands,
        contentDescription = contentDescription
    )
}
