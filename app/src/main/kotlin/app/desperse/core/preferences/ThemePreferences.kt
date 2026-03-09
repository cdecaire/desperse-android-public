package app.desperse.core.preferences

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val THEME_CACHE_PREFS = "theme_cache"
private const val THEME_MODE_KEY = "theme_mode"

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/**
 * Theme mode options
 */
enum class ThemeMode {
    SYSTEM,  // Follow system setting
    LIGHT,   // Always light
    DARK     // Always dark
}

/**
 * Blockchain explorer options
 */
enum class ExplorerOption(val displayName: String, val description: String) {
    ORB("Orb", "Simple, clean explorer"),
    SOLSCAN("Solscan", "General purpose explorer"),
    SOLANA_EXPLORER("Solana Explorer", "Official explorer"),
    SOLANAFM("SolanaFM", "Developer-friendly explorer");

    fun getExplorerUrl(address: String): String = when (this) {
        ORB -> "https://orb.helius.xyz/address/$address"
        SOLSCAN -> "https://solscan.io/address/$address"
        SOLANA_EXPLORER -> "https://explorer.solana.com/address/$address"
        SOLANAFM -> "https://solana.fm/address/$address"
    }
}

/**
 * Manages app preferences using DataStore
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val explorerKey = stringPreferencesKey("explorer")
    private val lastSeenVersionCodeKey = intPreferencesKey("last_seen_version_code")

    private val themeCache = context.getSharedPreferences(THEME_CACHE_PREFS, Context.MODE_PRIVATE)

    init {
        // One-time migration: seed SharedPreferences cache from DataStore for existing users
        // who already have a theme preference but no sync cache (e.g. after app update).
        if (!themeCache.contains(THEME_MODE_KEY)) {
            CoroutineScope(Dispatchers.IO).launch {
                val mode = context.appDataStore.data.first()[themeModeKey]
                if (mode != null) {
                    themeCache.edit().putString(THEME_MODE_KEY, mode).commit()
                    // Apply so next launch uses correct window background
                    applyNightMode(parseThemeMode(mode))
                }
            }
        }
    }

    /**
     * Flow of current theme mode preference
     */
    val themeMode: Flow<ThemeMode> = context.appDataStore.data.map { preferences ->
        parseThemeMode(preferences[themeModeKey])
    }

    /**
     * Flow of current explorer preference
     */
    val explorer: Flow<ExplorerOption> = context.appDataStore.data.map { preferences ->
        when (preferences[explorerKey]) {
            "orb" -> ExplorerOption.ORB
            "solscan" -> ExplorerOption.SOLSCAN
            "solana-explorer" -> ExplorerOption.SOLANA_EXPLORER
            "solanafm" -> ExplorerOption.SOLANAFM
            else -> ExplorerOption.ORB // Default
        }
    }

    /**
     * Set theme mode preference. Writes sync cache first (for cold start), then DataStore
     * (for Compose flow), then applies night mode immediately.
     */
    suspend fun setThemeMode(mode: ThemeMode) {
        val modeString = when (mode) {
            ThemeMode.SYSTEM -> "system"
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK -> "dark"
        }
        // Write sync cache first — ensures cold start always has the latest value
        // even if the app crashes before the DataStore write completes
        themeCache.edit().putString(THEME_MODE_KEY, modeString).commit()
        context.appDataStore.edit { preferences ->
            preferences[themeModeKey] = modeString
        }
        // Apply immediately so XML resources resolve correctly
        applyNightMode(mode)
    }

    /**
     * Get the last version code the user has seen the "What's New" sheet for.
     * Returns 0 if never set (first install).
     */
    suspend fun getLastSeenVersionCode(): Int {
        return context.appDataStore.data.map { preferences ->
            preferences[lastSeenVersionCodeKey] ?: 0
        }.first()
    }

    /**
     * Mark the current version as seen so the "What's New" sheet won't show again.
     */
    suspend fun setLastSeenVersionCode(versionCode: Int) {
        context.appDataStore.edit { preferences ->
            preferences[lastSeenVersionCodeKey] = versionCode
        }
    }

    /**
     * Set explorer preference
     */
    suspend fun setExplorer(explorer: ExplorerOption) {
        context.appDataStore.edit { preferences ->
            preferences[explorerKey] = when (explorer) {
                ExplorerOption.ORB -> "orb"
                ExplorerOption.SOLSCAN -> "solscan"
                ExplorerOption.SOLANA_EXPLORER -> "solana-explorer"
                ExplorerOption.SOLANAFM -> "solanafm"
            }
        }
    }
    companion object {
        /**
         * Parse a stored theme mode string into a ThemeMode enum.
         * Defaults to DARK for unknown/null values (new/non-logged-in users).
         */
        fun parseThemeMode(value: String?): ThemeMode = when (value) {
            "light" -> ThemeMode.LIGHT
            "system" -> ThemeMode.SYSTEM
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.DARK
        }

        /**
         * Read the cached theme mode synchronously from SharedPreferences.
         * Call in Application.onCreate() before any Activity starts.
         */
        fun getThemeModeSync(context: Context): ThemeMode {
            val prefs = context.getSharedPreferences(THEME_CACHE_PREFS, Context.MODE_PRIVATE)
            return parseThemeMode(prefs.getString(THEME_MODE_KEY, null))
        }

        /**
         * Apply the night mode setting so XML resources (window background, status bar)
         * resolve to the correct light/dark variant.
         */
        fun applyNightMode(mode: ThemeMode) {
            AppCompatDelegate.setDefaultNightMode(
                when (mode) {
                    ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                }
            )
        }
    }
}

/**
 * Alias for backwards compatibility
 */
typealias ThemePreferences = AppPreferences
