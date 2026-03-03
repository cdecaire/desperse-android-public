package app.desperse.core.wallet

/**
 * Single source of truth for all supported Solana wallet apps.
 *
 * Adding a new wallet requires only adding one entry here — all other components
 * (MwaManager, DeeplinkWalletManager, WalletPreferences, WalletPickerSheet)
 * reference this registry for wallet metadata.
 */
object WalletRegistry {

    /**
     * How a wallet communicates with dApps on Android.
     */
    enum class Protocol {
        /** MWA WebSocket protocol only (Phantom, Jupiter, Seeker). */
        MWA,
        /** Deeplink protocol only — MWA is broken/unsupported (Backpack). */
        DEEPLINK,
        /** Supports both; try MWA first, fall back to deeplinks (Solflare). */
        MWA_WITH_DEEPLINK_FALLBACK,
        /** Present on device but not usable as a wallet (Seed Vault TEE service). */
        EXCLUDED,
    }

    data class WalletEntry(
        val packageName: String,
        val displayName: String,
        /** Lowercase Privy walletClientType identifier (e.g. "phantom", "solflare"). */
        val clientType: String,
        val protocol: Protocol,
        /** Base URL for deeplink protocol, if supported. */
        val deeplinkBaseUrl: String? = null,
        /** URI hosts from walletUriBase for MWA name resolution. */
        val mwaUriHosts: List<String> = emptyList(),
    )

    val entries: List<WalletEntry> = listOf(
        WalletEntry(
            packageName = "app.phantom",
            displayName = "Phantom",
            clientType = "phantom",
            protocol = Protocol.MWA,
            deeplinkBaseUrl = "https://phantom.app/ul/v1",
            mwaUriHosts = listOf("phantom.app"),
        ),
        WalletEntry(
            packageName = "com.solflare.mobile",
            displayName = "Solflare",
            clientType = "solflare",
            protocol = Protocol.MWA_WITH_DEEPLINK_FALLBACK,
            deeplinkBaseUrl = "https://solflare.com/ul/v1",
            mwaUriHosts = listOf("solflare.com", "solflare.dev"),
        ),
        WalletEntry(
            packageName = "ag.jup.jupiter.android",
            displayName = "Jupiter",
            clientType = "jupiter",
            protocol = Protocol.MWA,
            mwaUriHosts = listOf("jup.ag"),
        ),
        WalletEntry(
            packageName = "ag.jup.mobile",
            displayName = "Jupiter",
            clientType = "jupiter",
            protocol = Protocol.MWA,
            mwaUriHosts = listOf("jup.ag"),
        ),
        WalletEntry(
            packageName = "com.solanamobile.wallet",
            displayName = "Seeker Wallet",
            clientType = "seeker",
            protocol = Protocol.MWA,
        ),
        WalletEntry(
            packageName = "app.backpack.mobile",
            displayName = "Backpack",
            clientType = "backpack",
            protocol = Protocol.MWA,
            mwaUriHosts = listOf("backpack.app"),
        ),
        WalletEntry(
            packageName = "app.backpack.mobile.standalone",
            displayName = "Backpack",
            clientType = "backpack",
            protocol = Protocol.MWA,
            mwaUriHosts = listOf("backpack.app"),
        ),
        WalletEntry(
            packageName = "com.ultimate.app",
            displayName = "Ultimate",
            clientType = "ultimate",
            protocol = Protocol.MWA,
            mwaUriHosts = listOf("ultimate.app"),
        ),
        WalletEntry(
            packageName = "com.glow.app",
            displayName = "Glow",
            clientType = "glow",
            protocol = Protocol.MWA,
            mwaUriHosts = listOf("glow.app"),
        ),
        WalletEntry(
            packageName = "com.solanamobile.seedvaultimpl",
            displayName = "Seed Vault",
            clientType = "seedvault",
            protocol = Protocol.EXCLUDED,
        ),
    )

    // Lookup indices (computed once)
    private val byPackage: Map<String, WalletEntry> = entries.associateBy { it.packageName }
    private val byMwaHost: Map<String, WalletEntry> = entries.flatMap { entry ->
        entry.mwaUriHosts.map { host -> host to entry }
    }.toMap()

    fun getByPackage(packageName: String): WalletEntry? = byPackage[packageName]
    fun getByMwaHost(host: String): WalletEntry? = byMwaHost[host]

    fun displayNameForPackage(pkg: String): String = byPackage[pkg]?.displayName ?: pkg
    fun displayNameForMwaHost(host: String): String = byMwaHost[host]?.displayName ?: host
    fun clientTypeForPackage(pkg: String): String = byPackage[pkg]?.clientType ?: "unknown"
    fun clientTypeForMwaHost(host: String): String = byMwaHost[host]?.clientType ?: "unknown"
    fun deeplinkUrlForPackage(pkg: String): String? = byPackage[pkg]?.deeplinkBaseUrl

    /** Whether this wallet should use deeplinks (DEEPLINK or MWA_WITH_DEEPLINK_FALLBACK). */
    fun shouldUseDeeplink(pkg: String): Boolean {
        val protocol = byPackage[pkg]?.protocol ?: return false
        return protocol == Protocol.DEEPLINK || protocol == Protocol.MWA_WITH_DEEPLINK_FALLBACK
    }

    /** Whether this wallet should try MWA first with deeplink fallback. */
    fun shouldTryMwaFirst(pkg: String): Boolean {
        return byPackage[pkg]?.protocol == Protocol.MWA_WITH_DEEPLINK_FALLBACK
    }

    /** Packages to exclude from MWA intent resolution. */
    val excludedMwaPackages: Set<String> = entries
        .filter { it.protocol == Protocol.EXCLUDED || it.protocol == Protocol.DEEPLINK }
        .map { it.packageName }
        .toSet()

    /** Packages that are deeplink-only (not discovered via MWA). */
    val deeplinkOnlyPackages: Set<String> = entries
        .filter { it.protocol == Protocol.DEEPLINK }
        .map { it.packageName }
        .toSet()

    /** Packages that prefer or require deeplinks. */
    val deeplinkPreferredPackages: Set<String> = entries
        .filter { it.protocol == Protocol.DEEPLINK || it.protocol == Protocol.MWA_WITH_DEEPLINK_FALLBACK }
        .map { it.packageName }
        .toSet()

    /** Infer package name from a display label (for legacy DataStore migration). */
    fun inferPackageFromLabel(label: String): String? {
        val normalized = label.lowercase().removeSuffix(" wallet").trim()
        return entries.firstOrNull { it.clientType == normalized || it.displayName.lowercase() == normalized }?.packageName
    }
}
