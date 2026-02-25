package app.desperse.core.wallet

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.walletDataStore: DataStore<Preferences> by preferencesDataStore(name = "wallet_preferences")

/**
 * Manages the user's active wallet selection, persisted via DataStore.
 * Other components observe [activeWallet] to know which wallet to use for transactions.
 */
@Singleton
class WalletPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _activeWallet = MutableStateFlow<WalletInfo?>(null)
    val activeWallet: StateFlow<WalletInfo?> = _activeWallet.asStateFlow()

    // All known wallets for the current user
    private val _wallets = MutableStateFlow<List<WalletInfo>>(emptyList())
    val wallets: StateFlow<List<WalletInfo>> = _wallets.asStateFlow()

    companion object {
        private const val TAG = "WalletPreferences"
        private val KEY_ACTIVE_WALLET_ID = stringPreferencesKey("active_wallet_id")
        private val KEY_ACTIVE_WALLET_ADDRESS = stringPreferencesKey("active_wallet_address")
        private val KEY_ACTIVE_WALLET_TYPE = stringPreferencesKey("active_wallet_type")
        private val KEY_ACTIVE_WALLET_LABEL = stringPreferencesKey("active_wallet_label")
        private val KEY_ACTIVE_WALLET_CONNECTOR = stringPreferencesKey("active_wallet_connector")
        private val KEY_ACTIVE_WALLET_PACKAGE = stringPreferencesKey("active_wallet_package")
    }

    init {
        scope.launch {
            loadActiveWallet()
        }
    }

    // Package name of the active external wallet app (e.g. "app.phantom")
    // Persisted separately because the server doesn't know about Android package names
    private val _activeWalletPackage = MutableStateFlow<String?>(null)

    /**
     * Set the active wallet for transactions.
     */
    suspend fun setActiveWallet(wallet: WalletInfo) {
        _activeWallet.value = wallet
        context.walletDataStore.edit { prefs ->
            prefs[KEY_ACTIVE_WALLET_ID] = wallet.id
            prefs[KEY_ACTIVE_WALLET_ADDRESS] = wallet.address
            prefs[KEY_ACTIVE_WALLET_TYPE] = wallet.type.name
            prefs[KEY_ACTIVE_WALLET_LABEL] = wallet.label
            wallet.connector?.let { prefs[KEY_ACTIVE_WALLET_CONNECTOR] = it }
        }
        Log.d(TAG, "Active wallet set to: ${wallet.label} (${wallet.address.take(8)}...)")
    }

    /**
     * Store the Android package name for the active external wallet.
     * Called after MWA authorization to remember which wallet app to target for future transactions.
     * Also updates the wallet label to match (e.g., "External Wallet" → "Phantom").
     */
    suspend fun setActiveWalletPackage(packageName: String?) {
        _activeWalletPackage.value = packageName
        context.walletDataStore.edit { prefs ->
            if (packageName != null) {
                prefs[KEY_ACTIVE_WALLET_PACKAGE] = packageName
                // Update the label to match the wallet app
                val label = labelFromPackage(packageName)
                if (label != null) {
                    prefs[KEY_ACTIVE_WALLET_LABEL] = label
                    _activeWallet.value?.let { wallet ->
                        _activeWallet.value = wallet.copy(label = label)
                    }
                }
            } else {
                prefs.remove(KEY_ACTIVE_WALLET_PACKAGE)
            }
        }
        Log.d(TAG, "Active wallet package set to: $packageName")
    }

    private fun labelFromPackage(packageName: String): String? {
        return when (packageName) {
            "app.phantom" -> "Phantom"
            "com.solflare.mobile" -> "Solflare"
            "app.backpack.mobile", "app.backpack.mobile.standalone" -> "Backpack"
            "com.ultimate.app" -> "Ultimate"
            "com.glow.app" -> "Glow"
            "com.solanamobile.wallet" -> "Seeker Wallet"
            "ag.jup.mobile", "ag.jup.jupiter.android" -> "Jupiter"
            else -> null
        }
    }

    /**
     * Get the package name for the active external wallet.
     * Tries stored value first, then infers from wallet label as fallback.
     * Returns null for embedded wallets or if package can't be determined.
     */
    fun getActiveWalletPackage(): String? {
        _activeWalletPackage.value?.let { return it }

        // Fallback: infer from wallet label for users who logged in before package storage was added
        val wallet = _activeWallet.value ?: return null
        if (wallet.type != WalletType.EXTERNAL) return null
        return inferPackageFromLabel(wallet.label)
    }

    private fun inferPackageFromLabel(label: String): String? {
        val normalized = label.lowercase().removeSuffix(" wallet").trim()
        return when (normalized) {
            "phantom" -> "app.phantom"
            "solflare" -> "com.solflare.mobile"
            "backpack" -> "app.backpack.mobile"
            "ultimate" -> "com.ultimate.app"
            "glow" -> "com.glow.app"
            "seeker" -> "com.solanamobile.wallet"
            "jupiter" -> "ag.jup.mobile"
            else -> null
        }
    }

    /**
     * Update the list of known wallets and sync active wallet to the server primary.
     */
    fun updateWallets(walletList: List<WalletInfo>) {
        _wallets.value = walletList
        val active = _activeWallet.value
        val serverPrimary = walletList.firstOrNull { it.isPrimary }

        if (active != null && walletList.none { it.id == active.id }) {
            // Active wallet was removed — fall back to primary or first
            val fallback = serverPrimary ?: walletList.firstOrNull()
            _activeWallet.value = fallback
            if (fallback != null) {
                scope.launch { setActiveWallet(fallback) }
            }
        } else if (serverPrimary != null && active?.id != serverPrimary.id) {
            // Primary changed on server — sync local active wallet
            _activeWallet.value = serverPrimary
            scope.launch { setActiveWallet(serverPrimary) }
        } else if (active == null && walletList.isNotEmpty()) {
            // No active wallet set — pick the primary
            val primary = serverPrimary ?: walletList.first()
            _activeWallet.value = primary
            scope.launch { setActiveWallet(primary) }
        }
    }

    /**
     * Get the current active wallet type (defaults to EMBEDDED if none set).
     */
    fun getActiveWalletType(): WalletType {
        return _activeWallet.value?.type ?: WalletType.EMBEDDED
    }

    /**
     * Check whether the user has ever explicitly set an active wallet preference.
     * Returns true if a wallet ID is persisted in DataStore.
     */
    suspend fun hasUserSetPreference(): Boolean {
        return try {
            val prefs = context.walletDataStore.data.first()
            prefs[KEY_ACTIVE_WALLET_ID] != null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check wallet preference", e)
            false
        }
    }

    /**
     * Clear all wallet preferences (e.g., on logout).
     */
    suspend fun clear() {
        _activeWallet.value = null
        _activeWalletPackage.value = null
        _wallets.value = emptyList()
        context.walletDataStore.edit { it.clear() }
    }

    private suspend fun loadActiveWallet() {
        try {
            val prefs = context.walletDataStore.data.first()
            val id = prefs[KEY_ACTIVE_WALLET_ID] ?: return
            val address = prefs[KEY_ACTIVE_WALLET_ADDRESS] ?: return
            val typeName = prefs[KEY_ACTIVE_WALLET_TYPE] ?: return
            val label = prefs[KEY_ACTIVE_WALLET_LABEL] ?: "Wallet"
            val connector = prefs[KEY_ACTIVE_WALLET_CONNECTOR]
            val packageName = prefs[KEY_ACTIVE_WALLET_PACKAGE]

            _activeWallet.value = WalletInfo(
                id = id,
                address = address,
                type = WalletType.valueOf(typeName),
                connector = connector,
                label = label,
                isPrimary = false // Will be updated when wallets are loaded from server
            )
            _activeWalletPackage.value = packageName
            Log.d(TAG, "Loaded active wallet: $label (${address.take(8)}...), package=$packageName")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load active wallet preference", e)
        }
    }
}
