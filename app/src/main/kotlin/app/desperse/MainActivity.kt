package app.desperse

import android.content.Intent
import android.os.Bundle
import android.os.Trace
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import app.desperse.core.preferences.ThemeMode
import app.desperse.core.preferences.ThemePreferences
import app.desperse.core.wallet.DeeplinkWalletManager
import app.desperse.core.wallet.MwaManager
import app.desperse.ui.navigation.AuthGateViewModel
import app.desperse.ui.navigation.DesperseNavGraph
import app.desperse.ui.theme.DesperseTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var deeplinkWalletManager: DeeplinkWalletManager

    @Inject
    lateinit var mwaManager: MwaManager

    /** Activity-scoped ViewModel - survives recomposition and prevents duplicate instances */
    private val authGateViewModel: AuthGateViewModel by viewModels()

    /** Incremented on each new intent to re-trigger deep link handling */
    private var deepLinkTrigger = mutableIntStateOf(0)

    private val _walletCallbacks = MutableSharedFlow<android.net.Uri>(replay = 1, extraBufferCapacity = 1)

    /**
     * ActivityResultLauncher for MWA wallet intents. Registered before super.onCreate()
     * (before STARTED state) as required by the Activity Result API.
     * Uses startActivityForResult like the official MWA SDK for better lifecycle handling.
     */
    private val mwaLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("MainActivity", "MWA activity result: resultCode=${result.resultCode}")
        // Result is informational only — MWA communication happens via WebSocket.
        // The launcher provides better window/lifecycle management than startActivity().
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Trace.beginSection("MainActivity.onCreate")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Wire the ActivityResultLauncher to MwaManager for proper MWA intent launching
        mwaManager.setActivityResultLauncher(mwaLauncher)

        // Check if this activity was launched with a wallet callback intent
        // (happens when activity is recreated instead of receiving onNewIntent)
        intent?.data?.let { uri ->
            if (uri.scheme == "desperse" && uri.host == "wallet-callback") {
                Log.d("MainActivity", "Wallet callback in onCreate: $uri")
                _walletCallbacks.tryEmit(uri)
                deeplinkWalletManager.onWalletCallback(uri)
            }
        }

        Trace.endSection() // MainActivity.onCreate (pre-setContent)

        Trace.beginSection("MainActivity.setContent")
        setContent {
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()

            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DesperseTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()

                // Handle deep links (re-triggers on new intents via deepLinkTrigger)
                val trigger by deepLinkTrigger
                LaunchedEffect(trigger) {
                    handleDeepLink(intent, navController)
                }

                DesperseNavGraph(
                    navController = navController,
                    walletCallbacks = _walletCallbacks.asSharedFlow(),
                    authGateViewModel = authGateViewModel
                )
            }
        }
        Trace.endSection() // MainActivity.setContent
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Check if this is a wallet deeplink callback
        val uri = intent.data
        if (uri != null && uri.scheme == "desperse" && uri.host == "wallet-callback") {
            Log.d("MainActivity", "Wallet callback received: $uri")
            _walletCallbacks.tryEmit(uri)
            deeplinkWalletManager.onWalletCallback(uri)
            return // Don't trigger normal deep link handling
        }

        deepLinkTrigger.intValue++
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear the launcher reference so MwaManager doesn't hold a stale launcher
        mwaManager.setActivityResultLauncher(null)
    }

    private fun handleDeepLink(intent: Intent?, navController: androidx.navigation.NavController) {
        val uri = intent?.data ?: return

        // Skip wallet callbacks - they're handled separately
        if (uri.scheme == "desperse" && uri.host == "wallet-callback") return

        when {
            // Post: desperse.com/p/{postId}
            uri.pathSegments.getOrNull(0) == "p" -> {
                val postId = uri.pathSegments.getOrNull(1)
                if (postId != null) {
                    navController.navigate("post/$postId")
                }
            }
            // Profile: desperse.com/{slug}
            uri.pathSegments.size == 1 -> {
                val slug = uri.pathSegments[0]
                navController.navigate("profile/$slug")
            }
        }
    }
}
