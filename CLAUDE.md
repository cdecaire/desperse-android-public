# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install on connected device/emulator
./gradlew installDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Configuration

Copy `local.properties.example` to `local.properties` and configure:
- `PRIVY_APP_ID` - Privy authentication app ID (required)
- `PRIVY_APP_CLIENT_ID` - Privy OAuth client ID
- `API_BASE_URL` - Backend API (defaults to https://desperse.com)
- `SENTRY_DSN` - Sentry error tracking DSN (optional)

## Architecture

**MVVM + Clean Architecture** with Jetpack Compose UI.

### Layer Organization

```
app/src/main/kotlin/app/desperse/
├── core/           # Infrastructure layer
│   ├── auth/       # PrivyAuthManager, TokenStorage, AuthState
│   ├── network/    # DesperseApi (Retrofit), ApiResult, AuthInterceptor
│   └── di/         # Hilt modules (NetworkModule, AuthModule)
├── data/           # Data layer
│   ├── model/      # Domain models (User, Post, PostAsset)
│   ├── dto/        # API request/response DTOs
│   └── repository/ # Repository implementations
└── ui/             # Presentation layer
    ├── theme/      # Material 3 dark theme (always dark)
    ├── navigation/ # DesperseNavGraph, AuthGateViewModel
    └── screens/    # Feature screens with ViewModels
```

### Key Architectural Patterns

- **Dependency Injection**: Hilt with `@HiltViewModel` and constructor injection
- **State Management**: `StateFlow` for UI state, `SharedFlow` for one-time events
- **API Calls**: `ApiResult<T>` sealed class wrapper via `safeApiCall` extension
- **Token Storage**: `EncryptedSharedPreferences` with in-memory cache for interceptor sync access
- **Navigation**: Compose Navigation with auth gate and bottom tab bar

### Authentication Flow

`LoginScreen` → `LoginViewModel` → `PrivyAuthManager` → Privy SDK → `TokenStorage`

The auth manager handles email OTP login, embedded Solana wallet creation, and transaction signing.

### Network Architecture

All API calls go through `DesperseApi` (Retrofit) with `AuthInterceptor` adding bearer tokens. Responses use `ApiEnvelope<T>` wrapper with `success`, `data`, `error`, and `requestId` fields.

## Tech Stack

- **Min SDK**: 28 (Privy SDK requirement)
- **Target SDK**: 35, **Compile SDK**: 36
- **Kotlin**: 2.3.10 with KSP 2.3.5
- **AGP**: 8.13.2, **Gradle**: 8.13
- **Java Target**: 17
- **Compose BOM**: 2026.01.01
- **Hilt**: 2.58
- **Privy SDK**: 0.9.2-beta.1 (embedded Solana wallet)
- **Solana Mobile Wallet Adapter**: 2.0.3
- **Retrofit**: 2.11.0 with Kotlinx Serialization 1.10.0

## Testing

Test dependencies configured: JUnit 4, MockK, Turbine (Flow testing), Espresso, Compose UI Test.

```bash
# Run single test class
./gradlew test --tests "app.desperse.ExampleTest"

# Run tests with specific filter
./gradlew test --tests "*ViewModel*"
```

## Deep Linking

- `https://desperse.com/p/{postId}` → Post detail
- `https://desperse.com/{slug}` → User profile
- `desperse://auth` → OAuth callback

## UI Components

### Media Components (`ui/components/media/`)

Media display uses a modular architecture:
- `PostMedia` - Main router that detects media type and delegates to appropriate renderer
- `BlurredBackgroundImage` - Images with dynamic aspect ratio, blur for portrait (>4:5)
- `VideoPlayer` - ExoPlayer-based video with auto-play, mute toggle, poster overlay
- `AudioPlayer` - ExoPlayer-based audio with cover image and controls
- `MediaCarousel` - HorizontalPager with dots indicator for multi-asset posts
- `ImageOptimization` - Vercel CDN URL generation for optimized image loading

### Image Optimization (REQUIRED)

**All images and assets MUST be optimized for their display context.** Never load raw/full-size images directly. This applies to:
- User avatars (everywhere: feed, comments, profile, activity, etc.)
- Post media (feed thumbnails, detail view, carousels)
- NFT images (grid, list, detail views)
- Profile header/banner images
- Token icons
- Any other images loaded from URLs

Use `ImageOptimization.getOptimizedUrlForContext()` with the appropriate `ImageContext`:

```kotlin
val optimizedUrl = remember(originalUrl) {
    ImageOptimization.getOptimizedUrlForContext(originalUrl, ImageContext.FEED_THUMBNAIL)
}
AsyncImage(model = optimizedUrl, ...)
```

Available contexts:
- `FEED_THUMBNAIL` (640px) - Feed images
- `CAROUSEL` (800px) - Full-width carousel images
- `DETAIL` (1200px) - Post detail view
- `AVATAR` (320px) - User avatars
- `COVER` (480px) - Audio/document cover images
- `WALLET_NFT_GRID` (320px) - NFT grid thumbnails
- `WALLET_NFT_LIST` (320px) - NFT list thumbnails
- `WALLET_ACTIVITY` (320px) - Activity thumbnails
- `TOKEN_ICON` (320px) - Token icons
- `PROFILE_HEADER` (800px) - Profile header/banner images
- `PROFILE_GRID` (480px) - Profile grid post thumbnails

When adding new image displays, add a new `ImageContext` if none of the existing ones fit.

### Bottom Sheets

Use `ModalBottomSheet` from Material 3 for native-feeling sheets:
- `WalletSheet` - Wallet balance, tokens, NFTs, activity tabs
- `PostCardMenuSheet` - Post actions (Go to post, Copy link, Report, etc.)
- `MoreMenuSheet` - Settings menu

### PostCardMenuSheet Logic

**Download option** - Only for gated document content:
- Shows when: `mediaType == DOCUMENT` (PDF, ZIP, EPUB) AND `hasDownloadAccess`
- Does NOT show for: images, videos, audio, 3D models (these are viewable, not downloadable)
- If creators want downloadable 3D files, they upload as ZIP

**View on Explorer** - For NFTs with mint addresses:
- Collectibles: uses `post.collectibleAssetId` (first minted cNFT asset ID)
- Editions: uses `post.masterMint` (master edition mint address)
- Only shows when the relevant field is non-null (i.e., NFT has been minted)

```kotlin
val explorerAddress = when (post.type) {
    "collectible" -> post.collectibleAssetId
    "edition" -> post.masterMint
    else -> null
}
val hasExplorerLink = isNft && !explorerAddress.isNullOrBlank()
```

### Compose Performance Patterns

For smooth scrolling in `LazyColumn`:
- Use `@Immutable` annotation on data classes (Post, User, etc.)
- Stabilize lambdas with `remember`: `val onClick = remember(id) { { doSomething(id) } }`
- Add `contentType` to `items()` for better recycling
- Use `FilterQuality.Low` for feed images
- Remember `ImageRequest` objects to avoid recreation

### Fixed Aspect Ratio for Feed Scroll

**Critical:** Media components must use fixed aspect ratios in feed contexts to prevent scroll flickering.

The problem: Dynamic aspect ratio calculation (updating state after image/video loads) causes layout shifts during scroll as LazyColumn recalculates item positions.

Solution in `BlurredBackgroundImage` and `VideoPlayer`:
- `useFixedAspectRatio: Boolean = true` parameter (default true for feed)
- When true, uses fixed 4:5 (0.8) ratio regardless of actual media dimensions
- `PostDetailScreen` uses `useFixedAspectRatio = false` for full media viewing

```kotlin
PostMedia(
    post = post,
    useFixedAspectRatio = true,  // Feed: stable scroll
    // useFixedAspectRatio = false  // Detail: show full media
)
```

Long-term fix: Include aspect ratio in API response so correct ratio is known before render.

## Async Operation Patterns

### CollectState for Blockchain Operations

For operations requiring on-chain confirmation (collect, purchase), use the `CollectState` pattern:

```kotlin
sealed class CollectState {
    data object Idle : CollectState()
    data object Preparing : CollectState()           // API call in progress
    data class Confirming(val id: String) : CollectState()  // Polling for confirmation
    data object Success : CollectState()
    data class Failed(val error: String, val canRetry: Boolean = true) : CollectState()
}
```

**Key patterns:**
- Store state per-item in ViewModel: `collectStates: Map<String, CollectState>`
- Skip if already in progress: check state before starting
- Clear stale states on refresh: remove entries for items with `isCollected: true` from API
- Button styling: `isSuccess` takes priority over `isFailed` for color/text

### Polling Pattern

For operations that require on-chain confirmation:
```kotlin
private fun startPolling(postId: String, collectionId: String) {
    pollingJobs[postId] = viewModelScope.launch {
        val maxPollTime = 60_000L
        val pollInterval = 5_000L
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < maxPollTime) {
            delay(pollInterval)
            repository.checkStatus(collectionId)
                .onSuccess { status ->
                    when (status.status) {
                        "confirmed" -> { /* update to success, return */ }
                        "failed" -> { /* update to failed, return */ }
                        // "pending" - continue polling
                    }
                }
            // On network error, continue polling (don't break)
        }
        // Timeout - show appropriate message
    }
}
```

## API Patterns

### REST API Response Format
All REST endpoints return `ApiEnvelope<T>`:
```json
{
  "success": true,
  "data": { ... },
  "error": { "code": "...", "message": "..." },
  "requestId": "req_..."
}
```

### Transaction Status Recovery
The server checks on-chain status before allowing retries. If Android times out polling but the tx confirmed, the next collect attempt returns `already_collected` with the existing collection data.

### Optimistic Updates
For instant feedback (like, unlike), update UI immediately and revert on API failure. For blockchain operations (collect, purchase), always wait for server confirmation.

### Cross-Screen State Sync (PostUpdateManager)

When user actions in one screen (e.g., liking in detail view) need to sync to another screen (e.g., feed), use `PostUpdateManager`:

```kotlin
// Singleton that broadcasts post updates
@Singleton
class PostUpdateManager @Inject constructor() {
    val updates: SharedFlow<PostUpdate>

    suspend fun emitLikeUpdate(postId: String, isLiked: Boolean, likeCount: Int)
    suspend fun emitCollectUpdate(postId: String, isCollected: Boolean, collectCount: Int)
    suspend fun emitCommentCountUpdate(postId: String, commentCount: Int)
}
```

Both `FeedViewModel` and `PostDetailViewModel` inject and use this for bidirectional sync.

### Periodic Silent Refresh (Stale Time)

Feed uses React Query-like stale time pattern to keep content fresh:

```kotlin
companion object {
    const val STALE_TIME_MS = 30_000L      // Data considered stale after 30s
    const val REFRESH_INTERVAL_MS = 45_000L // Silent refresh every 45s
}
```

Wire up in Composable with `LifecycleEventObserver` for `ON_RESUME`/`ON_PAUSE`.

## Profile Screen Patterns

### Profile Header Action Icons

The profile header displays action icons positioned just below the header image, right-aligned at the same level as the avatar overlap. Own profile shows Activity + Edit icons; other profiles show Message + Follow icons.

### List Screens (Followers/Following/Collectors/Activity)

These screens share a common pattern:
- `LazyColumn` with infinite scroll (load more at 6 items from end)
- Cursor-based pagination via ViewModel
- Loading/error/empty states

```kotlin
enum class FollowListType { Followers, Following, Collectors }
```
