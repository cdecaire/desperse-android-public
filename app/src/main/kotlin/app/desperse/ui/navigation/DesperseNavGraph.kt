package app.desperse.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.desperse.core.auth.AuthState
import app.desperse.core.auth.PrivyAuthManager
import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.core.preferences.AppPreferences
import app.desperse.core.preferences.ExplorerOption
import app.desperse.core.preferences.ThemeMode
import app.desperse.core.realtime.AblyManager
import app.desperse.data.model.User
import app.desperse.data.repository.UserRepository
import app.desperse.ui.components.DesperseSnackbarHost
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.FeedbackSheet
import app.desperse.ui.components.MoreMenuSheet
import app.desperse.ui.components.ToastManager
import app.desperse.ui.components.ToastVariant
import app.desperse.ui.components.UnreadDot
import app.desperse.data.NotificationCountManager
import app.desperse.data.UnreadMessageManager
import app.desperse.ui.screens.auth.LoginScreen
import app.desperse.ui.screens.create.CreateScreen
import app.desperse.ui.screens.explore.ExploreScreen
import app.desperse.ui.screens.feed.FeedScreen
import android.net.Uri
import app.desperse.ui.screens.messages.ConversationScreen
import app.desperse.ui.screens.messages.MessagesScreen
import app.desperse.ui.screens.notifications.NotificationsScreen
import app.desperse.ui.screens.post.PostDetailScreen
import app.desperse.ui.screens.profile.ActivityScreen
import app.desperse.ui.screens.profile.FollowListScreen
import app.desperse.ui.screens.profile.FollowListType
import app.desperse.ui.screens.profile.ProfileScreen
import app.desperse.ui.screens.settings.AppSettingsScreen
import app.desperse.ui.screens.settings.HelpScreen
import app.desperse.ui.screens.settings.MessagingSettingsScreen
import app.desperse.ui.screens.settings.NotificationSettingsScreen
import app.desperse.ui.screens.settings.ProfileInfoScreen
import app.desperse.ui.screens.settings.SettingsScreen
import app.desperse.ui.screens.settings.WalletsSettingsScreen
import app.desperse.ui.components.WalletSheet
import app.desperse.ui.theme.DesperseSizes
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthGateViewModel @Inject constructor(
    private val privyAuthManager: PrivyAuthManager,
    private val userRepository: UserRepository,
    private val notificationCountManager: NotificationCountManager,
    private val unreadMessageManager: UnreadMessageManager,
    private val postRepository: app.desperse.data.repository.PostRepository,
    private val ablyManager: AblyManager,
    private val api: DesperseApi,
    private val appPreferences: AppPreferences,
    private val mwaManager: app.desperse.core.wallet.MwaManager,
    private val pushTokenManager: app.desperse.core.push.PushTokenManager,
    val toastManager: ToastManager
) : ViewModel() {
    val authState = privyAuthManager.authState

    /** The current user's full profile from the API */
    val currentUser = userRepository.currentUser

    /** Notification counts for UI badges */
    val notificationCounters = notificationCountManager.counters

    /** Whether there are unread messages for bottom nav badge */
    val hasUnreadMessages = unreadMessageManager.hasUnreadMessages

    companion object {
        private const val TAG = "AuthGateViewModel"
    }

    init {
        // Initialize Privy when the auth gate is created
        viewModelScope.launch {
            android.util.Log.d(TAG, "Initializing Privy...")
            privyAuthManager.initialize()
        }

        // When auth state changes to Authenticated, init auth with backend
        var initAuthInProgress = false
        viewModelScope.launch {
            authState.collect { state ->
                android.util.Log.d(TAG, "Auth state changed: ${state::class.simpleName}")
                when (state) {
                    is AuthState.Authenticated -> {
                        // Guard against duplicate calls from rapid state emissions
                        if (initAuthInProgress) {
                            android.util.Log.d(TAG, "initAuth already in progress, skipping")
                            return@collect
                        }
                        initAuthInProgress = true
                        // Initialize auth with backend to sync Privy user
                        // This returns the full user profile
                        android.util.Log.d(TAG, "User authenticated, calling initAuth()")
                        android.os.Trace.beginSection("AuthGate.postAuth")
                        val result = userRepository.initAuth()
                        initAuthInProgress = false
                        android.os.Trace.endSection() // AuthGate.postAuth
                        android.util.Log.d(TAG, "initAuth result: ${if (result.isSuccess) "success" else "failure: ${result.exceptionOrNull()?.message}"}")
                        // Set Sentry user context for crash reports
                        val user = userRepository.currentUser.value
                        if (user != null) {
                            io.sentry.Sentry.setUser(io.sentry.protocol.User().apply {
                                id = user.id
                                username = user.slug
                            })
                        }
                        // Start polling for notification counts
                        notificationCountManager.startPolling()
                        // Start tracking unread messages
                        unreadMessageManager.start()
                        // Sync preferences from server
                        syncPreferencesFromServer()
                        // Register push token for notifications
                        pushTokenManager.ensureTokenRegistered()
                    }
                    is AuthState.Unauthenticated, is AuthState.Error -> {
                        // Clear user data on logout
                        android.util.Log.d(TAG, "User unauthenticated/error, clearing user data")
                        userRepository.clearCurrentUser()
                        // Clear Sentry user context
                        io.sentry.Sentry.setUser(null)
                        // Stop polling
                        notificationCountManager.stopPolling()
                        // Stop tracking unread messages
                        unreadMessageManager.stop()
                        // Disconnect Ably realtime
                        ablyManager.disconnect()
                    }
                    else -> { /* Loading/NotReady - do nothing */ }
                }
            }
        }

        // Connect Ably when user is loaded (for realtime messaging)
        viewModelScope.launch {
            userRepository.currentUser.collect { user ->
                if (user != null) {
                    ablyManager.connect(user.id)
                }
            }
        }
    }

    private fun syncPreferencesFromServer() {
        viewModelScope.launch {
            when (val result = safeApiCall { api.getPreferences() }) {
                is ApiResult.Success -> {
                    val prefs = result.data.preferences

                    // Sync theme
                    val mode = when (prefs.theme) {
                        "light" -> ThemeMode.LIGHT
                        "dark" -> ThemeMode.DARK
                        "system" -> ThemeMode.SYSTEM
                        else -> null
                    }
                    if (mode != null) appPreferences.setThemeMode(mode)

                    // Sync explorer
                    val option = when (prefs.explorer) {
                        "orb" -> ExplorerOption.ORB
                        "solscan" -> ExplorerOption.SOLSCAN
                        "solana-explorer" -> ExplorerOption.SOLANA_EXPLORER
                        "solanafm" -> ExplorerOption.SOLANAFM
                        else -> null
                    }
                    if (option != null) appPreferences.setExplorer(option)
                }
                is ApiResult.Error -> { /* Use local values */ }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            pushTokenManager.unregisterToken()
            ablyManager.disconnect()
            userRepository.clearCurrentUser()
            mwaManager.clearAuthTokens()
            privyAuthManager.logout()
        }
    }

    /** Refresh the current user's profile from the API */
    fun refreshCurrentUser() {
        viewModelScope.launch {
            userRepository.fetchCurrentUser()
        }
    }

    /** Submit beta feedback */
    suspend fun createFeedback(
        rating: Int?,
        message: String?,
        appVersion: String?,
        userAgent: String?
    ): Result<Unit> {
        return postRepository.createFeedback(
            rating = rating,
            message = message,
            appVersion = appVersion,
            userAgent = userAgent
        ).map { }  // Convert Result<String> to Result<Unit>
            .also { result ->
                result.onSuccess {
                    toastManager.showSuccess("Thanks for your feedback!")
                }
                result.onFailure { error ->
                    toastManager.showError(error.message ?: "Failed to send feedback")
                }
            }
    }
}

sealed class Screen(
    val route: String,
    val title: String,
    val icon: String
) {
    object Feed : Screen("feed", "Home", FaIcons.Home)
    object Explore : Screen("explore", "Explore", FaIcons.Search)
    object Create : Screen("create", "Create", FaIcons.Plus)
    object Notifications : Screen("notifications", "Notifications", FaIcons.Bell)
    object Messages : Screen("messages", "Messages", FaIcons.Message)
    object Profile : Screen("profile", "Profile", FaIcons.User)
}

// Bottom nav items (6 items - unconventional but fits the app well)
val bottomNavScreens = listOf(
    Screen.Feed,
    Screen.Explore,
    Screen.Notifications,
    Screen.Messages,
    Screen.Profile
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesperseNavGraph(
    navController: androidx.navigation.NavHostController = rememberNavController(),
    walletCallbacks: SharedFlow<Uri>? = null,
    authGateViewModel: AuthGateViewModel
) {
    val authState by authGateViewModel.authState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Show loading while auth is initializing
    when (authState) {
        is AuthState.NotReady, is AuthState.Loading -> {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            return
        }
        is AuthState.Unauthenticated, is AuthState.Error -> {
            // Show login screen
            LoginScreen(
                onLoginSuccess = {
                    // Navigation will be handled by auth state change
                },
                walletCallbacks = walletCallbacks
            )
            return
        }
        is AuthState.Authenticated -> {
            // Continue to main app
        }
    }

    // Request notification permission on Android 13+ (one-time prompt after login)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { granted ->
            android.util.Log.d("DesperseNavGraph", "Notification permission granted: $granted")
        }
        LaunchedEffect(Unit) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Get current user for avatar (from UserRepository, fetched after auth)
    val currentUser by authGateViewModel.currentUser.collectAsState()

    // Get notification counters for badges
    val notificationCounters by authGateViewModel.notificationCounters.collectAsState()

    // Get unread messages state for badge
    val hasUnreadMessages by authGateViewModel.hasUnreadMessages.collectAsState()

    // More menu state
    var showMoreMenu by remember { mutableStateOf(false) }

    // Wallet sheet state
    var showWalletSheet by remember { mutableStateOf(false) }

    // Feedback sheet state
    var showFeedbackSheet by remember { mutableStateOf(false) }

    // Snackbar state for toasts
    val snackbarHostState = remember { SnackbarHostState() }
    var currentToastVariant by remember { mutableStateOf(ToastVariant.Info) }

    // Collect toast messages and show them
    LaunchedEffect(authGateViewModel) {
        authGateViewModel.toastManager.toasts.collect { toast ->
            currentToastVariant = toast.variant
            snackbarHostState.showSnackbar(
                message = toast.message,
                duration = toast.duration
            )
        }
    }

    // Determine if we should show bottom nav
    val showBottomNav = currentDestination?.route in bottomNavScreens.map { it.route }

    // Shared scroll behavior for coordinating top bar and bottom nav
    // enterAlways hides immediately on scroll down, shows on scroll up (like web)
    // Using smooth spring animation for snap behavior
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        snapAnimationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    // Calculate bottom nav visibility based on top bar collapse state
    // When top bar is fully expanded (heightOffset = 0), bottom nav is visible
    // When top bar is collapsed (heightOffset = -maxHeight), bottom nav is hidden
    val density = LocalDensity.current
    val bottomNavHeightPx = with(density) { DesperseSizes.bottomNavHeight.toPx() }

    // scrollBehavior.state.collapsedFraction: 0 = expanded, 1 = collapsed
    // Use spring animation for smooth, fluid bottom nav transitions
    val targetVisibility = 1f - scrollBehavior.state.collapsedFraction
    val bottomNavVisibility by animateFloatAsState(
        targetValue = targetVisibility,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bottomNavVisibility"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main content
        NavHost(
            navController = navController,
            startDestination = Screen.Feed.route,
            modifier = Modifier.fillMaxSize()
        ) {
            // Main tabs
            composable(Screen.Feed.route) {
                FeedScreen(
                    onPostClick = { postId -> navController.navigate("post/$postId") },
                    onUserClick = { slug -> navController.navigate("profile/$slug") },
                    onCreateClick = { navController.navigate(Screen.Create.route) },
                    onEditPost = { postId -> navController.navigate("post/$postId/edit") },
                    scrollBehavior = scrollBehavior
                )
            }
            composable(Screen.Explore.route) {
                ExploreScreen(
                    onPostClick = { postId -> navController.navigate("post/$postId") },
                    onUserClick = { slug -> navController.navigate("profile/$slug") }
                )
            }
            composable(Screen.Create.route) {
                CreateScreen(
                    onPostCreated = { postId ->
                        navController.navigate("post/$postId") {
                            popUpTo(Screen.Create.route) { inclusive = true }
                        }
                    },
                    onClose = { navController.popBackStack() }
                )
            }
            composable("post/{postId}/edit") { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
                CreateScreen(
                    editPostId = postId,
                    onPostCreated = { navController.popBackStack() },
                    onClose = { navController.popBackStack() }
                )
            }
            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    onPostClick = { postId -> navController.navigate("post/$postId") },
                    onUserClick = { slug -> navController.navigate("profile/$slug") }
                )
            }
            composable(Screen.Messages.route) {
                MessagesScreen(
                    onBack = null, // Main tab - no back button
                    onThreadClick = { thread ->
                        val route = buildConversationRoute(
                            threadId = thread.id,
                            otherName = thread.otherUser.displayName,
                            otherSlug = thread.otherUser.usernameSlug,
                            otherAvatar = thread.otherUser.avatarUrl,
                            isBlocked = thread.isBlocked,
                            isBlockedBy = thread.isBlockedBy
                        )
                        navController.navigate(route)
                    },
                    onNewConversation = { threadId, name, slug, avatar ->
                        val route = buildConversationRoute(
                            threadId = threadId,
                            otherName = name,
                            otherSlug = slug,
                            otherAvatar = avatar,
                            isBlocked = false,
                            isBlockedBy = false
                        )
                        navController.navigate(route)
                    }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    slug = null, // Own profile
                    onPostClick = { postId -> navController.navigate("post/$postId") },
                    onWalletClick = { showWalletSheet = true },
                    onFollowersClick = { slug -> navController.navigate("profile/$slug/followers") },
                    onFollowingClick = { slug -> navController.navigate("profile/$slug/following") },
                    onCollectorsClick = { slug -> navController.navigate("profile/$slug/collectors") },
                    onActivityClick = { navController.navigate("activity") },
                    onEditProfileClick = { navController.navigate("settings/profile-info") }
                )
            }

            // Detail screens
            composable("post/{postId}") { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
                PostDetailScreen(
                    postId = postId,
                    onBack = { navController.popBackStack() },
                    onUserClick = { slug -> navController.navigate("profile/$slug") },
                    onEditPost = { id -> navController.navigate("post/$id/edit") }
                )
            }
            composable("profile/{slug}") { backStackEntry ->
                val slug = backStackEntry.arguments?.getString("slug") ?: return@composable
                ProfileScreen(
                    slug = slug,
                    onPostClick = { postId -> navController.navigate("post/$postId") },
                    onBack = { navController.popBackStack() },
                    onWalletClick = { showWalletSheet = true },
                    onFollowersClick = { s -> navController.navigate("profile/$s/followers") },
                    onFollowingClick = { s -> navController.navigate("profile/$s/following") },
                    onCollectorsClick = { s -> navController.navigate("profile/$s/collectors") },
                    onActivityClick = { navController.navigate("activity") },
                    onEditProfileClick = { navController.navigate("settings/profile-info") },
                    onMessageClick = { info ->
                        val route = buildConversationRoute(
                            threadId = info.threadId,
                            otherName = info.otherName,
                            otherSlug = info.otherSlug,
                            otherAvatar = info.otherAvatar,
                            isBlocked = false,
                            isBlockedBy = false
                        )
                        navController.navigate(route)
                    }
                )
            }

            // Followers/Following/Collectors lists
            composable("profile/{slug}/followers") { backStackEntry ->
                val slug = backStackEntry.arguments?.getString("slug") ?: return@composable
                FollowListScreen(
                    slug = slug,
                    listType = FollowListType.Followers,
                    onUserClick = { userSlug -> navController.navigate("profile/$userSlug") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("profile/{slug}/following") { backStackEntry ->
                val slug = backStackEntry.arguments?.getString("slug") ?: return@composable
                FollowListScreen(
                    slug = slug,
                    listType = FollowListType.Following,
                    onUserClick = { userSlug -> navController.navigate("profile/$userSlug") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("profile/{slug}/collectors") { backStackEntry ->
                val slug = backStackEntry.arguments?.getString("slug") ?: return@composable
                FollowListScreen(
                    slug = slug,
                    listType = FollowListType.Collectors,
                    onUserClick = { userSlug -> navController.navigate("profile/$userSlug") },
                    onBack = { navController.popBackStack() }
                )
            }

            // Activity
            composable("activity") {
                ActivityScreen(
                    onPostClick = { postId -> navController.navigate("post/$postId") },
                    onProfileClick = { slug -> navController.navigate("profile/$slug") },
                    onBack = { navController.popBackStack() }
                )
            }

            // Auth
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Feed.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    walletCallbacks = walletCallbacks
                )
            }

            // Settings
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            // Settings sub-screens
            composable("settings/profile-info") {
                ProfileInfoScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings/wallets") {
                WalletsSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings/notifications") {
                NotificationSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings/messaging") {
                MessagingSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings/app") {
                AppSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings/help") {
                HelpScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Message thread detail (when clicking a conversation)
            composable(
                route = "messages/{threadId}?otherName={otherName}&otherSlug={otherSlug}&otherAvatar={otherAvatar}&blocked={blocked}&blockedBy={blockedBy}",
                arguments = listOf(
                    navArgument("threadId") { type = NavType.StringType },
                    navArgument("otherName") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("otherSlug") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("otherAvatar") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("blocked") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("blockedBy") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val threadId = backStackEntry.arguments?.getString("threadId") ?: return@composable
                val otherName = backStackEntry.arguments?.getString("otherName")
                val otherSlug = backStackEntry.arguments?.getString("otherSlug")
                val otherAvatar = backStackEntry.arguments?.getString("otherAvatar")
                val blocked = backStackEntry.arguments?.getString("blocked")?.toBooleanStrictOrNull() ?: false
                val blockedBy = backStackEntry.arguments?.getString("blockedBy")?.toBooleanStrictOrNull() ?: false
                ConversationScreen(
                    threadId = threadId,
                    otherUserName = otherName,
                    otherUserSlug = otherSlug,
                    otherUserAvatarUrl = otherAvatar,
                    isBlocked = blocked,
                    isBlockedBy = blockedBy,
                    onBack = { navController.popBackStack() },
                    onUserClick = { slug -> navController.navigate("profile/$slug") }
                )
            }
        }

        // Bottom navigation - overlaid at bottom with scroll-aware animation
        if (showBottomNav) {
            DesperseBottomNav(
                currentDestination = currentDestination,
                currentUser = currentUser,
                hasUnreadNotifications = notificationCounters.unreadNotifications > 0,
                hasUnreadMessages = hasUnreadMessages,
                bottomNavVisibility = bottomNavVisibility,
                bottomNavHeightPx = bottomNavHeightPx,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onMoreClick = { showMoreMenu = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Snackbar host for toasts - positioned above bottom nav
        DesperseSnackbarHost(
            hostState = snackbarHostState,
            currentVariant = currentToastVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (showBottomNav) DesperseSizes.bottomNavHeight + 8.dp else 8.dp)
        )

        // More menu bottom sheet
        MoreMenuSheet(
            isOpen = showMoreMenu,
            onDismiss = { showMoreMenu = false },
            onSettingsClick = { navController.navigate("settings") },
            onHelpClick = { navController.navigate("settings") }, // Help is in settings
            onFeedbackClick = { showFeedbackSheet = true },
            onLogoutClick = { authGateViewModel.logout() },
            isAuthenticated = currentUser != null
        )

        // Wallet bottom sheet
        WalletSheet(
            isOpen = showWalletSheet,
            onDismiss = { showWalletSheet = false }
        )

        // Feedback bottom sheet
        FeedbackSheet(
            isOpen = showFeedbackSheet,
            onDismiss = { showFeedbackSheet = false },
            onSubmit = { rating, message, appVersion, userAgent ->
                authGateViewModel.createFeedback(rating, message, appVersion, userAgent)
            }
        )
    }
}

/**
 * Build navigation route for ConversationScreen with user metadata as query params.
 */
private fun buildConversationRoute(
    threadId: String,
    otherName: String?,
    otherSlug: String?,
    otherAvatar: String?,
    isBlocked: Boolean,
    isBlockedBy: Boolean
): String {
    val params = buildList {
        otherName?.let { add("otherName=${Uri.encode(it)}") }
        otherSlug?.let { add("otherSlug=${Uri.encode(it)}") }
        otherAvatar?.let { add("otherAvatar=${Uri.encode(it)}") }
        if (isBlocked) add("blocked=true")
        if (isBlockedBy) add("blockedBy=true")
    }
    return if (params.isNotEmpty()) {
        "messages/$threadId?${params.joinToString("&")}"
    } else {
        "messages/$threadId"
    }
}

/**
 * Custom bottom navigation bar matching web design
 */
@Composable
private fun DesperseBottomNav(
    currentDestination: androidx.navigation.NavDestination?,
    currentUser: User?,
    hasUnreadNotifications: Boolean,
    hasUnreadMessages: Boolean,
    bottomNavVisibility: Float,
    bottomNavHeightPx: Float,
    onNavigate: (String) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .height(DesperseSizes.bottomNavHeight)
            .graphicsLayer {
                translationY = bottomNavHeightPx * (1f - bottomNavVisibility)
                alpha = bottomNavVisibility
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                // Asymmetric padding: less on top, more on bottom for gesture bar clearance
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavScreens.forEach { screen ->
                val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                val isProfile = screen == Screen.Profile
                val isNotifications = screen == Screen.Notifications
                val isMessages = screen == Screen.Messages

                val showBadge = when {
                    isNotifications -> hasUnreadNotifications
                    isMessages -> hasUnreadMessages
                    else -> false
                }

                BottomNavItem(
                    icon = screen.icon,
                    label = screen.title,
                    isSelected = isSelected,
                    avatarUrl = if (isProfile) currentUser?.avatarUrl else null,
                    showBadge = showBadge,
                    onClick = { onNavigate(screen.route) }
                )
            }

            // More menu item
            BottomNavItem(
                icon = FaIcons.Bars,
                label = "More",
                isSelected = false,
                onClick = onMoreClick
            )
        }
    }
}

/**
 * Individual bottom nav item
 */
@Composable
private fun BottomNavItem(
    icon: String,
    label: String,
    isSelected: Boolean,
    avatarUrl: String? = null,
    showBadge: Boolean = false,
    onClick: () -> Unit
) {
    val iconColor = if (isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(DesperseSizes.minTouchTarget)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl != null) {
            // Show avatar for profile
            AsyncImage(
                model = avatarUrl,
                contentDescription = label,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentScale = ContentScale.Crop
            )
        } else {
            // Show icon with optional badge
            Box {
                FaIcon(
                    icon = icon,
                    size = 24.dp,
                    tint = iconColor,
                    style = if (isSelected) FaIconStyle.Solid else FaIconStyle.Regular,
                    contentDescription = label
                )
                // Show red dot badge for notifications
                if (showBadge) {
                    UnreadDot(
                        size = 8.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                    )
                }
            }
        }
    }
}
