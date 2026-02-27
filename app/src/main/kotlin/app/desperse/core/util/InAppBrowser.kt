package app.desperse.core.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Opens a URL in a Chrome Custom Tab (in-app browser).
 * Falls back to default browser if Custom Tabs is unavailable.
 */
fun Context.openInAppBrowser(url: String) {
    val customTabsIntent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build()
    customTabsIntent.launchUrl(this, Uri.parse(url))
}
