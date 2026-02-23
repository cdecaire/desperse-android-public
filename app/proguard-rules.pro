# proguard-rules.pro

# === APP-SPECIFIC ===

# Keep serializable DTOs and models (safety net for JSON deserialization)
-keep class app.desperse.data.dto.** { *; }
-keep class app.desperse.data.model.** { *; }

# === PRIVY SDK (ships minimal rules, needs protection) ===
-keep class io.privy.** { *; }
-dontwarn io.privy.**

# === ANDROIDX BROWSER / CHROME CUSTOM TABS (used by Privy for OAuth) ===
-keep class androidx.browser.** { *; }
-dontwarn androidx.browser.**

# === MWA / SOLANA MOBILE (ships empty proguard.txt) ===
# com.solana.** covers mobilewalletadapter (clientlib, common) AND
# com.solana.publickey (SolanaPublicKey.base58() used in MWA auth flow —
# without this, R8 strips base58() and Phantom login crashes in release builds)
-keep class com.solana.** { *; }
-keep class com.solanamobile.** { *; }
-keep class com.funkatronics.** { *; }
-dontwarn com.solana.**
-dontwarn com.ditchoom.buffer.**
-dontwarn com.funkatronics.**

# === JNA & LAZYSODIUM (ship NO rules, use JNI/native) ===
-keep class com.sun.jna.** { *; }
-keep class net.java.dev.jna.** { *; }
-dontwarn com.sun.jna.**
-keep class com.goterl.lazysodium.** { *; }
-dontwarn com.goterl.lazysodium.**

# === COIL (ships NO consumer rules) ===
-dontwarn coil.**
