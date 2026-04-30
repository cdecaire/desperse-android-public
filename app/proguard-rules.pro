# proguard-rules.pro

# Preserve generic type signatures and annotations — required by Retrofit and kotlinx.serialization
-keepattributes Signature, *Annotation*

# === PRIVY SDK (ships minimal rules) ===
-keep class io.privy.** { *; }
-dontwarn io.privy.**

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

# === FILAMENT 3D ENGINE (uses JNI for native rendering) ===
-keep class com.google.android.filament.** { *; }
-dontwarn com.google.android.filament.**
