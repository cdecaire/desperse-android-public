package app.desperse.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

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

    /**
     * Flow of current theme mode preference
     */
    val themeMode: Flow<ThemeMode> = context.appDataStore.data.map { preferences ->
        when (preferences[themeModeKey]) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
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
     * Set theme mode preference
     */
    suspend fun setThemeMode(mode: ThemeMode) {
        context.appDataStore.edit { preferences ->
            preferences[themeModeKey] = when (mode) {
                ThemeMode.SYSTEM -> "system"
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
            }
        }
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
}

/**
 * Alias for backwards compatibility
 */
typealias ThemePreferences = AppPreferences
