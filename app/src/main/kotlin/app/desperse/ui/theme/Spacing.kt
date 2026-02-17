package app.desperse.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Desperse spacing system - matches web Tailwind spacing
 */
object DesperseSpacing {
    val micro: Dp = 2.dp      // p-0.5
    val xs: Dp = 4.dp         // p-1
    val sm: Dp = 8.dp         // p-2
    val md: Dp = 12.dp        // p-3
    val lg: Dp = 16.dp        // p-4
    val xl: Dp = 20.dp        // p-5
    val xxl: Dp = 24.dp       // p-6
    val xxxl: Dp = 32.dp      // p-8
    val section: Dp = 40.dp   // p-10
    val container: Dp = 48.dp // p-12
}

/**
 * Component-specific spacing constants
 */
object DesperseComponentSpacing {
    // Card
    val cardPadding: Dp = 24.dp
    val cardGap: Dp = 24.dp

    // Button
    val buttonPaddingHorizontal: Dp = 16.dp
    val buttonPaddingVertical: Dp = 8.dp

    // Input
    val inputPaddingHorizontal: Dp = 12.dp
    val inputPaddingVertical: Dp = 4.dp

    // Dialog
    val dialogPadding: Dp = 24.dp
    val dialogGap: Dp = 16.dp

    // Navigation
    val bottomNavPaddingHorizontal: Dp = 8.dp
    val topNavPaddingHorizontal: Dp = 16.dp

    // Post Card
    val postCardHeaderPadding: Dp = 12.dp
    val postCardContentPadding: Dp = 12.dp
    val postCardAvatarSize: Dp = 40.dp
    val postCardAvatarGap: Dp = 12.dp

    // List items
    val listItemPadding: Dp = 12.dp
    val listItemGap: Dp = 12.dp

    // Dropdown
    val dropdownItemPaddingHorizontal: Dp = 16.dp
    val dropdownItemPaddingVertical: Dp = 10.dp
    val dropdownItemGap: Dp = 12.dp
}

/**
 * Size constants
 */
object DesperseSizes {
    // Navigation bar heights
    val topNavHeight: Dp = 56.dp
    val bottomNavHeight: Dp = 72.dp  // Extra height for gesture bar clearance

    // Avatar sizes
    val avatarXs: Dp = 24.dp
    val avatarSm: Dp = 32.dp
    val avatarMd: Dp = 40.dp
    val avatarLg: Dp = 48.dp
    val avatarXl: Dp = 64.dp
    val avatarProfile: Dp = 96.dp

    // Button heights
    val buttonDefault: Dp = 40.dp
    val buttonCta: Dp = 44.dp
    val buttonIcon: Dp = 40.dp
    val buttonIconLg: Dp = 64.dp

    // Input heights
    val inputDefault: Dp = 40.dp

    // Icon sizes
    val iconXs: Dp = 12.dp
    val iconSm: Dp = 16.dp
    val iconMd: Dp = 20.dp
    val iconLg: Dp = 24.dp
    val iconXl: Dp = 32.dp
    val iconFeature: Dp = 48.dp

    // Touch targets
    val minTouchTarget: Dp = 44.dp

    // Tabs
    val tabHeight: Dp = 36.dp
}

/**
 * Border radius values
 */
object DesperseRadius {
    val none: Dp = 0.dp
    val xs: Dp = 4.dp         // rounded-xs
    val sm: Dp = 8.dp         // rounded-sm
    val md: Dp = 12.dp        // rounded-md
    val lg: Dp = 16.dp        // rounded-lg
    val xl: Dp = 20.dp        // rounded-xl
    // CircleShape for full rounded
}
